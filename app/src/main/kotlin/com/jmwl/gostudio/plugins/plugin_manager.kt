package com.jmwl.gostudio.plugins

import android.content.Context
import com.jmwl.gostudio.core.logging.logger_manager
import org.json.JSONArray
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * 插件管理器。
 *
 * 插件安装目录：`<filesDir>/home/gostudio/plugins/`
 * 目录结构（数据包插件，不执行代码）：
 * ```
 * plugins/
 * └── com.example.myskills/
 *     ├── manifest.json    # 必需
 *     └── skills/          # 可选能力目录
 *         └── my-skill/SKILL.md
 * ```
 *
 * 启用状态持久化在 `plugins/enabled.json`（记录被禁用的 id 列表，默认启用）。
 */
object plugin_manager {

    private const val LOG_TAG = "plugin_manager"
    private const val ENABLED_FILE = "enabled.json"

    private lateinit var plugins_dir: File
    private var disabled_ids: MutableSet<String> = mutableSetOf()
    private var plugins: List<plugin_instance> = emptyList()

    /** 版本监听（数据变更通知，供 UI 刷新） */
    private val listeners = mutableListOf<() -> Unit>()

    fun init(context: Context) {
        plugins_dir = File(context.filesDir, "home/gostudio/plugins")
        if (!plugins_dir.exists()) plugins_dir.mkdirs()
        load_enabled_state()
        discover()
    }

    /** 确保 lazy 初始化（未 init 时兜底，比如测试/预览） */
    private fun ensure_dir(context: Context? = null): File {
        if (!::plugins_dir.isInitialized) {
            val ctx = context ?: com.jmwl.gostudio.gostudio_application.instance
            plugins_dir = File(ctx.filesDir, "home/gostudio/plugins")
            plugins_dir.mkdirs()
            load_enabled_state()
        }
        return plugins_dir
    }

    /** 重新扫描插件目录 */
    fun discover() {
        val dir = ensure_dir()
        val result = mutableListOf<plugin_instance>()
        dir.listFiles()?.filter { it.isDirectory }?.forEach { sub ->
            val mf = File(sub, "manifest.json")
            if (mf.isFile) {
                plugin_manifest.from_file(mf)?.let { manifest ->
                    // manifest.id 必须与目录名一致，防止目录伪装
                    if (manifest.id == sub.name) {
                        result.add(
                            plugin_instance(
                                manifest = manifest,
                                dir = sub,
                                enabled = manifest.id !in disabled_ids
                            )
                        )
                    } else {
                        logger_manager.w(LOG_TAG, "插件目录名 ${sub.name} 与 manifest id ${manifest.id} 不一致，跳过")
                    }
                }
            }
        }
        plugins = result.sortedBy { it.manifest.name.lowercase() }
    }

    /** 当前插件列表（已排序） */
    fun all(): List<plugin_instance> = plugins

    /** 启用插件的 skills 目录列表（skills 扩展点消费） */
    fun skill_dirs(context: Context? = null): List<File> {
        if (plugins.isEmpty()) discover()
        return plugins.filter { it.enabled }.mapNotNull { it.skill_dir() }
    }

    /** 启用/禁用插件 */
    fun set_enabled(id: String, enabled: Boolean): Boolean {
        if (plugins.none { it.id == id }) return false
        if (enabled) disabled_ids.remove(id) else disabled_ids.add(id)
        save_enabled_state()
        discover()
        notify_changed()
        return true
    }

    /**
     * 从 ZIP 安装插件。
     *
     * ZIP 内可以是：
     * - 直接含 manifest.json 的根结构
     * - 或外层包一个目录（常见于 GitHub 下载的 zip）
     *
     * 校验通过后安装到 plugins/<id>/。已存在同 id 插件则覆盖更新。
     */
    fun install(context: Context, zip_stream: InputStream): Result<plugin_manifest> {
        return runCatching {
            val dir = ensure_dir(context)

            // 1. 解压到临时目录
            val staging = File(dir, ".staging-${System.currentTimeMillis()}")
            staging.mkdirs()
            try {
                unzip(zip_stream, staging)

                // 2. 定位 manifest.json（根目录或唯一子目录）
                val source_dir = locate_plugin_root(staging)
                    ?: throw IllegalArgumentException("ZIP 中未找到有效的 manifest.json")

                val manifest = plugin_manifest.from_file(File(source_dir, "manifest.json"))
                    ?: throw IllegalArgumentException("manifest.json 无效（缺 id/name 或格式错误）")

                // 3. 移入正式目录（同 id 覆盖）
                val target = File(dir, manifest.id)
                if (target.exists()) target.deleteRecursively()
                if (!source_dir.renameTo(target)) {
                    // 跨设备 rename 失败时复制
                    source_dir.copyRecursively(target, overwrite = true)
                    source_dir.deleteRecursively()
                }

                discover()
                notify_changed()
                logger_manager.i(LOG_TAG, "插件安装成功: ${manifest.id} v${manifest.version}")
                manifest
            } finally {
                staging.deleteRecursively()
            }
        }
    }

    /** 卸载插件（删除目录） */
    fun uninstall(id: String): Boolean {
        val dir = ensure_dir()
        val target = File(dir, id)
        if (!target.isDirectory) return false
        val ok = runCatching { target.deleteRecursively() }.getOrDefault(false)
        if (ok) {
            disabled_ids.remove(id)
            save_enabled_state()
            discover()
            notify_changed()
            logger_manager.i(LOG_TAG, "插件已卸载: $id")
        }
        return ok
    }

    /** 注册插件列表变更监听 */
    fun add_listener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun remove_listener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notify_changed() {
        listeners.toList().forEach { runCatching(it) }
    }

    /** 解压 ZIP，带 zip-slip 路径穿越防护 */
    private fun unzip(stream: InputStream, target_dir: File) {
        ZipInputStream(stream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val out_file = File(target_dir, entry.name)
                val canonical = out_file.canonicalFile
                if (!canonical.path.startsWith(target_dir.canonicalPath + File.separator)) {
                    throw SecurityException("ZIP 条目路径非法: ${entry.name}")
                }
                if (entry.isDirectory) {
                    canonical.mkdirs()
                } else {
                    canonical.parentFile?.mkdirs()
                    canonical.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    /** 定位插件根：staging 本身有 manifest.json，或其下唯一目录有 */
    private fun locate_plugin_root(staging: File): File? {
        if (File(staging, "manifest.json").isFile) return staging
        val dirs = staging.listFiles()?.filter { it.isDirectory } ?: return null
        val candidates = dirs.filter { File(it, "manifest.json").isFile }
        return when {
            candidates.size == 1 -> candidates.first()
            // 允许外层目录 + 隐藏文件（如 macOS 的 __MACOSX）
            dirs.size >= 1 && candidates.size == 1 -> candidates.first()
            else -> null
        }
    }

    /** 启用状态：enabled.json 记录被禁用的 id（新装默认启用） */
    private fun enabled_file(): File = File(ensure_dir(), ENABLED_FILE)

    private fun load_enabled_state() {
        val file = enabled_file()
        disabled_ids = runCatching {
            val array = JSONArray(file.readText())
            mutableSetOf<String>().apply {
                for (i in 0 until array.length()) add(array.optString(i))
            }
        }.getOrDefault(mutableSetOf())
    }

    private fun save_enabled_state() {
        runCatching {
            enabled_file().writeText(JSONArray(disabled_ids).toString())
        }.onFailure {
            logger_manager.e(LOG_TAG, "保存插件启用状态失败: ${it.message}", it)
        }
    }
}
