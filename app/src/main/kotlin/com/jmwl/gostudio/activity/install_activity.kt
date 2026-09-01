package com.jmwl.gostudio.activity

import android.content.Intent
import android.os.Bundle
import com.jmwl.gostudio.ui.toast.app_toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.jmwl.gostudio.toolchain.proot_manager
import com.jmwl.gostudio.toolchain.install_go_toolchain
import com.jmwl.gostudio.toolchain.configure_best_apk_mirror
import com.jmwl.gostudio.toolchain.toolchain_manager
import com.jmwl.gostudio.toolchain.runtime.format_rootfs_size
import com.jmwl.gostudio.toolchain.runtime.format_rootfs_speed
import com.jmwl.gostudio.toolchain.runtime.rootfs_install_event
import com.jmwl.gostudio.toolchain.runtime.rootfs_installer
import com.jmwl.gostudio.toolchain.runtime.toolchain_runtime_paths
import com.jmwl.gostudio.ui.screens.install.install_screen
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class install_activity : ComponentActivity() {
    private var logs by mutableStateOf<List<String>>(emptyList())
    private var is_downloading by mutableStateOf(false)
    private var is_extracting by mutableStateOf(false)
    private var is_configuring by mutableStateOf(false)
    private var current_progress by mutableFloatStateOf(0f)
    private var elapsed_seconds by mutableStateOf(0L)
    private var install_start_ms = 0L

    private val home_dir_path: File get() = File(filesDir, "home")
    private val gostudio_dir_path: File get() = File(home_dir_path, "gostudio")
    private val rootfs_dir_path: File get() = File(gostudio_dir_path, "alpine-rootfs")
    private val legacy_ubuntu_dir_path: File get() = File(gostudio_dir_path, "ubuntu-base")

    // Alpine minirootfs 元数据来自 dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/
    // latest-releases.yaml（版本升级时同步更新 sha256）。
    private val alpine_branch = "v3.24"
    private val alpine_version = "3.24.1"
    private val expected_sha256 = "f55a90f69052c5bd6f92cb09a8f47065970830b194c917a006fb94028e721259"
    private val alpine_filename = "alpine-minirootfs-${alpine_version}-aarch64.tar.gz"
    private val mirrors = listOf(
        "https://mirrors.tuna.tsinghua.edu.cn/alpine/$alpine_branch/releases/aarch64/$alpine_filename",
        "https://mirrors.ustc.edu.cn/alpine/$alpine_branch/releases/aarch64/$alpine_filename",
        "https://mirrors.aliyun.com/alpine/$alpine_branch/releases/aarch64/$alpine_filename",
        "https://dl-cdn.alpinelinux.org/alpine/$alpine_branch/releases/aarch64/$alpine_filename"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            app_theme_provider {
                install_screen(
                    logs = logs,
                    is_downloading = is_downloading,
                    is_extracting = is_extracting,
                    is_configuring = is_configuring,
                    current_progress = current_progress,
                    elapsed_seconds = elapsed_seconds,
                    on_export_logs = ::export_logs
                )
            }
        }
        start_install_timer()
        start_download()
    }

    /** 安装耗时计时：从进入安装流程起每秒刷新，Activity 销毁时随 lifecycleScope 取消。 */
    private fun start_install_timer() {
        if (install_start_ms == 0L) install_start_ms = System.currentTimeMillis()
        lifecycleScope.launch {
            while (true) {
                elapsed_seconds = (System.currentTimeMillis() - install_start_ms) / 1000
                delay(1000)
            }
        }
    }

    private fun add_log(text: String) {
        logs = if (text.startsWith("\r") && logs.isNotEmpty()) {
            logs.dropLast(1) + text.removePrefix("\r")
        } else {
            logs + text
        }
    }

    private fun add_proot_log(line: String) {
        if (line.contains("[OUT]") || line.contains("[ERR]")) {
            val clean = line.replace("[OUT] ", "").replace("[ERR] ", "")
            if (clean.isNotBlank()) add_log(clean)
        } else {
            add_log(line)
        }
    }

    private fun start_download() {
        lifecycleScope.launch {
            val (rootfs_ok, configured) = withContext(Dispatchers.IO) {
                rootfs_dir_path.is_alpine_rootfs() to
                    File(rootfs_dir_path, ".gostudio_installed").exists()
            }

            // 环境已完整：立即进主界面，不显示安装流程。
            // 场景：安装完成瞬间进程被杀、navigate_to_main 没执行，install_activity
            // 残留在任务栈顶——之后从桌面点图标直接恢复此页（不经过 splash 判定），
            // 不短路的话用户每次打开都会看到"又安装一遍"。
            if (rootfs_ok && configured) {
                navigate_to_main()
                return@launch
            }

            // 提升为带可见通知的前台服务：安装期间防止 ROM 查杀导致反复中断
            com.jmwl.gostudio.gostudio_application.instance.keep_alive_service_
                ?.show_install_notification()

            // rootfs 已解压但配置未完成（安装中途进程被杀后重进此页）：跳过下载/解压，
            // 直接进配置阶段补装缺的部分。防止「清理旧目录」把装好的环境删掉重来。
            if (rootfs_ok) {
                add_log("检测到已解压的 Alpine 环境，跳过下载")
                configure_environment()
                return@launch
            }

            is_downloading = true
            is_extracting = false

            // 彻底替换 Ubuntu：检测到旧环境的目录直接清理（含已装工具链，~数百 MB）。
            if (legacy_ubuntu_dir_path.exists()) {
                withContext(Dispatchers.IO) {
                    add_log("检测到旧版 Ubuntu 环境，正在清理...")
                    legacy_ubuntu_dir_path.deleteRecursively()
                }
            }

            val installer = rootfs_installer(
                toolchain_runtime_paths(
                    gostudio_dir = gostudio_dir_path,
                    home_dir = home_dir_path,
                    rootfs_dir = rootfs_dir_path,
                    proot_tmp_dir = File(gostudio_dir_path, "proot-tmps"),
                    external_storage_dir = null,
                    native_library_dir = File(applicationInfo.nativeLibraryDir)
                )
            )
            val success = installer.install_rootfs(
                mirrors = mirrors,
                file_name = alpine_filename,
                expected_digest = expected_sha256
            ) { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        is rootfs_install_event.log -> add_log(event.message)
                        is rootfs_install_event.download -> {
                            is_extracting = false
                            current_progress = event.percent / 100f
                            if (event.percent > 0) {
                                add_log("\r下载进度: ${event.percent}% (${format_rootfs_size(event.downloaded_size)}/${format_rootfs_size(event.total_size)}, ${format_rootfs_speed(event.speed)})")
                            }
                        }
                        is rootfs_install_event.extract -> {
                            is_extracting = true
                            current_progress = event.percent / 100f
                            if (event.current_file.isNotBlank()) {
                                add_log("\r解压: ${event.current_file}")
                            }
                        }
                    }
                }
            }
            is_downloading = false
            is_extracting = false
            if (success) configure_environment()
        }
    }

    private fun configure_environment() {
        lifecycleScope.launch {
            is_configuring = true
            add_log("检查环境配置状态...")

            try {
                val base_flag = File(rootfs_dir_path, ".gostudio_base_packages")
                val installed_flag = File(rootfs_dir_path, ".gostudio_installed")

                suspend fun run_required_command(status: String, command: String): Boolean {
                    add_log(status)
                    val success = proot_manager.execute_command(command, ::add_proot_log)
                    if (!success) add_log("${status.removeSuffix("...")}失败")
                    return success
                }

                // ── 阶段 1：基础包。独立标记，装过即跳过（续装秒过）──
                val base_ready = withContext(Dispatchers.IO) { base_flag.exists() }
                if (base_ready) {
                    add_log("基础包已就绪，跳过")
                } else {
                    // 先测速切镜像：minirootfs 自带官方源，国内直连不稳（DNS/超时）
                    configure_best_apk_mirror { line -> runOnUiThread { add_log(line) } }

                    // 基础包：bash（终端与 wrapper 生态依赖）、CA 证书（https）、tzdata（时区）。
                    // wget/tar/unzip/xz 用 busybox 自带 applet，无需安装。
                    if (!run_required_command(
                            "安装基础包（bash / ca-certificates / tzdata）...",
                            "apk update && apk add bash ca-certificates tzdata"
                        )
                    ) return@launch
                    withContext(Dispatchers.IO) { base_flag.createNewFile() }
                }

                // ── 阶段 2：Go 工具链。按磁盘探测补缺，已装的部分不再重装 ──
                toolchain_manager.invalidate_go_probe()
                if (toolchain_manager.is_go_installed() && toolchain_manager.is_gopls_installed()) {
                    add_log("Go 工具链已就绪，跳过")
                    // 上次装完但写标记前被杀的情况：这里补写，下次启动 splash 直接放行
                    if (!installed_flag.exists()) {
                        withContext(Dispatchers.IO) { installed_flag.createNewFile() }
                    }
                } else {
                    add_log("安装 Go 工具链（go + gopls + git）...")
                    val go_ok = install_go_toolchain(
                        onLog = { line -> runOnUiThread { add_proot_log(line) } },
                        onProgress = { }
                    )
                    if (go_ok) {
                        add_log("Go 工具链安装完成")
                        withContext(Dispatchers.IO) { installed_flag.createNewFile() }
                    } else {
                        // 不写 installed 标记：下次启动回此页只补装缺的部分。
                        // 仍放行进主界面（基础包完好，工具页可随时补装）。
                        add_log("Go 工具链安装失败，可稍后在「开发工具」页重试")
                    }
                }

                add_log("所有安装步骤完成")
                // 工具链状态可能变化，让 installed_go() 探测缓存失效
                toolchain_manager.invalidate_go_probe()
                // 安装结束，退出前台服务身份（恢复静默保活）
                com.jmwl.gostudio.gostudio_application.instance.keep_alive_service_
                    ?.hide_notification()
                delay(2000)
                navigate_to_main()
            } catch (e: Exception) {
                add_log("环境配置失败: ${e.message ?: e.javaClass.simpleName}")
                add_log("请重试")
            } finally {
                is_configuring = false
            }
        }
    }

    private fun navigate_to_main() {
        val intent = Intent(this, main_activity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun export_logs() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(gostudio_dir_path, "cache/install_log_$timestamp.txt")
            file.parentFile?.mkdirs()
            file.writeText(logs.joinToString("\n"))
            app_toast.show(this, "已导出到: ${file.absolutePath}", app_toast.LENGTH_LONG)
        } catch (e: Exception) {
            app_toast.show(this, "导出失败: ${e.message}", app_toast.LENGTH_SHORT)
        }
    }
}