package com.jmwl.gostudio.toolchain

import android.content.Context
import com.jmwl.gostudio.toolchain.runtime.MirrorCandidate
import com.jmwl.gostudio.toolchain.runtime.MirrorSpeedResult
import com.jmwl.gostudio.toolchain.runtime.MirrorSpeedTest

/** 一个内置 GOPROXY 源。 */
data class goproxy_source(
    val name: String,
    /** GOPROXY 的完整值（含 ,direct 回退） */
    val url: String,
    val note: String
)

/**
 * GOPROXY 模块代理源的存储与切换。
 *
 * 选中的源持久化在 SharedPreferences，并在内存缓存（[current]）——
 * [toolchain_manager.project_environment] 是 Compose 重组热路径，
 * 不能每次都读磁盘。切换时同时用 `go env -w` 写进 proot 的 go env
 * 配置文件，让交互终端里的 go 命令也走同一个源。
 */
object goproxy_store {

    private const val prefs_name = "goproxy_settings"
    private const val key_url = "goproxy_url"
    private const val key_name = "goproxy_name"

    /**
     * 内置源（首个为默认）。
     * 注：JFrog GoCenter（gocenter.io）已于 2023 年底停止服务，不再收录。
     */
    val builtin_sources: List<goproxy_source> = listOf(
        goproxy_source("七牛 Goproxy 中国", "https://goproxy.cn,direct", "国内加速，默认推荐"),
        goproxy_source("阿里云", "https://mirrors.aliyun.com/goproxy/", "阿里公共镜像"),
        goproxy_source("goproxy.io", "https://goproxy.io,direct", "全球 CDN 加速"),
        goproxy_source("Go 官方", "https://proxy.golang.org,direct", "官方源，国内直连不稳定")
    )

    const val default_url = "https://goproxy.cn,direct"

    @Volatile
    private var current_url: String = default_url

    @Volatile
    private var current_name: String = builtin_sources.first().name

    /** app 启动时加载持久化的选择。 */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(prefs_name, Context.MODE_PRIVATE)
        current_url = prefs.getString(key_url, null) ?: default_url
        current_name = prefs.getString(key_name, null) ?: builtin_sources.first().name
    }

    /** 当前生效的 GOPROXY 值（供环境变量注入，优先级高于 go env 文件）。 */
    fun current(): String = current_url

    /** 当前源的显示名（内置名或「自定义」）。 */
    fun current_display_name(): String = current_name

    /** 持久化选择并更新内存缓存。 */
    fun set(context: Context, name: String, url: String) {
        current_url = url
        current_name = name
        context.getSharedPreferences(prefs_name, Context.MODE_PRIVATE)
            .edit()
            .putString(key_url, url)
            .putString(key_name, name)
            .apply()
    }

    /** 与代理配套的校验和库：goproxy.cn 配国内 sumdb 镜像，其余用官方。 */
    fun sumdb_for(goproxy_url: String): String =
        if (goproxy_url.contains("goproxy.cn")) "sum.golang.google.cn" else "sum.golang.org"

    /**
     * 并发测速：对每个源 GET 一个稳定存在的小资源（golang/protobuf 的 @latest，
     * 返回几十字节的 JSON），记录耗时；不可用（超时/非 2xx）记 null。
     *
     * @param custom_url 额外参与测速的自定义源（null/空则只测内置源）
     * @return 按耗时升序（可用在前）的结果，candidate.name 即 [goproxy_source.name]
     */
    suspend fun speed_test(custom_url: String? = null): List<MirrorSpeedResult> {
        val probe_path = "github.com/golang/protobuf/@latest"
        val candidates = builtin_sources.map { source ->
            MirrorCandidate(source.name, proxy_host(source.url), "${proxy_base(source.url)}/$probe_path")
        }.toMutableList()
        val custom = custom_url?.trim().orEmpty()
        if (custom.isNotEmpty()) {
            candidates += MirrorCandidate("自定义", proxy_host(custom), "${proxy_base(custom)}/$probe_path")
        }
        return MirrorSpeedTest.testAndSort(candidates)
    }

    /** 从 GOPROXY 值取主机名（去掉 ,direct 等回退项）。 */
    private fun proxy_host(goproxy_url: String): String {
        val base = proxy_base(goproxy_url)
        return base.removePrefix("https://").removePrefix("http://").substringBefore('/')
    }

    /** 从 GOPROXY 值取代理基地址（去回退项与末尾斜杠）。 */
    private fun proxy_base(goproxy_url: String): String =
        goproxy_url.trim().substringBefore(',').trimEnd('/')
}
