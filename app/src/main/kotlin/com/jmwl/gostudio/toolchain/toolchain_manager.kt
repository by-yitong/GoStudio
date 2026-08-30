package com.jmwl.gostudio.toolchain

import com.jmwl.gostudio.core.logging.logger_manager
import java.io.File

/**
 * Go 工具链探测结果。
 *
 * @param go_proot_dir guest 内 GOROOT（apt 装到 /usr/local/go）
 * @param go_bin_proot_dir guest 内 go 可执行目录（/usr/local/go/bin）
 * @param gopls_proot_path guest 内 gopls 路径（/usr/local/go/bin/gopls 或 $GOPATH/bin/gopls）
 */
data class go_toolchain_info(
    val version: String,
    val go_proot_dir: String,
    val go_bin_proot_dir: String,
    val gopls_proot_path: String
)

/**
 * 项目构建环境（Go 版本）。
 *
 * @param environment proot 内的环境变量（PATH/GOROOT/GOPATH/GOPROXY 等）
 * @param go 已装的 Go 工具链；null 表示未装
 * @param missing 缺失项的人类可读描述
 */
data class project_toolchain_environment(
    val environment: Map<String, String>,
    val go: go_toolchain_info?,
    val missing: List<String>
)

/**
 * Go 工具链管理器。
 *
 * Go/gopls 装到 proot rootfs 内。兼容两种安装方式：
 * - apt 安装（golang/golang-go）：GOROOT=/usr/lib/go（软链到 /usr/lib/go-<version>），/usr/bin/go 软链
 * - 手动安装（官方 tarball）：GOROOT=/usr/local/go
 * 探测逻辑见 [installed_go]。
 */
object toolchain_manager {

    private const val PROOT_GOSTUDIO_HOME = "/home/gostudio"
    private const val PROOT_GO_ROOT = "/usr/local/go"
    private const val PROOT_GO_BIN = "$PROOT_GO_ROOT/bin"
    private const val PROOT_APT_GO_ROOT = "/usr/lib/go"
    private const val PROOT_GOPLS = "$PROOT_GO_BIN/gopls"
    private const val PROOT_GOPATH_BIN = "/home/go/bin"

    /**
     * guest 内的基础 PATH（标准 Linux 目录 + Go bin + GOPATH/bin）。
     * apt 装的 go 在 /usr/local/go/bin，gopls 经 `go install` 落到 $GOPATH/bin。
     */
    private val base_proot_path = listOf(
        PROOT_GOPATH_BIN,
        PROOT_GO_BIN,
        "/home/.local/bin",
        "/usr/local/sbin",
        "/usr/local/bin",
        "/bin",
        "/usr/bin",
        "/sbin",
        "/usr/sbin"
    )

    fun proot_path(extra_paths: List<String> = emptyList()): String {
        return (extra_paths + base_proot_path)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(":")
    }

    /**
     * 探测已安装的 Go 工具链，兼容两种安装方式：
     * - 手动安装（官方 tarball）：GOROOT=/usr/local/go，二进制在 /usr/local/go/bin/go
     * - apt 安装（golang / golang-go）：GOROOT=/usr/lib/go 或 /usr/lib/go-<version>，
     *   二进制在 /usr/lib/go/bin/go，/usr/bin/go 为软链。
     *
     * 版本号需 proot 执行 `go version` 才能拿到，这里只判断存在性，版本延迟到运行时。
     *
     * 结果走内存缓存：本函数会被 Compose 重组热路径调用（编辑器界面每次重组都要组装终端环境），
     * 每次都做文件 stat / listFiles 会拖慢光标移动。装完工具链后调用 [invalidate_go_probe] 失效。
     */
    fun installed_go(): go_toolchain_info? {
        if (go_probe_resolved) return go_probe_cache
        return probe_installed_go().also {
            go_probe_cache = it
            go_probe_resolved = true
        }
    }

    /** 工具链安装/卸载后调用，让 [installed_go] 下次重新探测磁盘。 */
    fun invalidate_go_probe() {
        go_probe_resolved = false
        go_probe_cache = null
    }

    @Volatile
    private var go_probe_cache: go_toolchain_info? = null

    @Volatile
    private var go_probe_resolved: Boolean = false

    private fun probe_installed_go(): go_toolchain_info? {
        val rootfs = toolchain_runtime_provider.paths().rootfs_dir

        // 1) 手动安装：/usr/local/go/bin/go
        val manual_go_bin = File(rootfs, "usr/local/go/bin/go")
        if (manual_go_bin.isFile) {
            return go_toolchain_info(
                version = "",
                go_proot_dir = PROOT_GO_ROOT,
                go_bin_proot_dir = PROOT_GO_BIN,
                gopls_proot_path = resolve_gopls_proot_path(rootfs)
            )
        }

        // 2) apt 安装：/usr/lib/go（软链到 /usr/lib/go-<version>）
        val apt_go_root_dir = File(rootfs, "usr/lib/go")
        val apt_go_bin = File(apt_go_root_dir, "bin/go")
        if (apt_go_bin.isFile) {
            return go_toolchain_info(
                version = "",
                go_proot_dir = "$PROOT_APT_GO_ROOT",
                go_bin_proot_dir = "$PROOT_APT_GO_ROOT/bin",
                gopls_proot_path = resolve_gopls_proot_path(rootfs)
            )
        }

        // 3) apt 安装版本化目录：扫描 /usr/lib/go-*/bin/go（兜底，软链未建立时）
        val usr_lib = File(rootfs, "usr/lib")
        if (usr_lib.isDirectory) {
            val versioned = usr_lib.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("go-") }
                ?.firstOrNull { File(it, "bin/go").isFile }
            if (versioned != null) {
                val proot_root = "/usr/lib/${versioned.name}"
                return go_toolchain_info(
                    version = "",
                    go_proot_dir = proot_root,
                    go_bin_proot_dir = "$proot_root/bin",
                    gopls_proot_path = resolve_gopls_proot_path(rootfs)
                )
            }
        }

        return null
    }

    /**
     * 解析 gopls 路径，兼容多种安装位置（优先 go install 的最新版，避免 apt 旧版崩溃）：
     * - go install gopls@latest：/home/go/bin/gopls（优先，版本最新最稳）
     * - apt gopls 包：/usr/bin/gopls
     * - 手动放 GOROOT：/usr/local/go/bin/gopls
     */
    private fun resolve_gopls_proot_path(rootfs: File): String {
        if (File(toolchain_runtime_provider.paths().home_dir, "go/bin/gopls").isFile) return "$PROOT_GOPATH_BIN/gopls"
        if (File(rootfs, "usr/bin/gopls").isFile) return "/usr/bin/gopls"
        if (File(rootfs, "usr/local/go/bin/gopls").isFile) return PROOT_GOPLS
        return "$PROOT_GOPATH_BIN/gopls"
    }

    /** Go 是否已安装。 */
    fun is_go_installed(): Boolean = installed_go() != null

    /** gopls 是否已安装（/home/go/bin/gopls 或 /usr/bin/gopls 或 /usr/local/go/bin/gopls）。 */
    fun is_gopls_installed(): Boolean {
        val rootfs = toolchain_runtime_provider.paths().rootfs_dir
        return File(toolchain_runtime_provider.paths().home_dir, "go/bin/gopls").isFile ||
            File(rootfs, "usr/bin/gopls").isFile ||
            File(rootfs, "usr/local/go/bin/gopls").isFile
    }

    /** git 是否已安装（rootfs 内 /usr/bin/git）。 */
    fun is_git_installed(): Boolean =
        File(toolchain_runtime_provider.paths().rootfs_dir, "usr/bin/git").isFile

    /**
     * 组装项目构建环境。
     *
     * @param project_path 项目 host 路径（用于日志）
     */
    fun project_environment(project_path: String): project_toolchain_environment {
        val go = installed_go()
        val missing = mutableListOf<String>()
        if (go == null) missing += "Go 未安装，请在工具页安装 golang"

        // GOROOT 动态匹配实际安装方式（apt=/usr/lib/go，手动=/usr/local/go）
        val goroot = go?.go_proot_dir ?: PROOT_GO_ROOT
        val environment = linkedMapOf(
            "GOSTUDIO_HOME" to PROOT_GOSTUDIO_HOME,
            "GOROOT" to goroot,
            "GOPATH" to "/home/go",
            "GOBIN" to PROOT_GOPATH_BIN,
            "GOPROXY" to "https://goproxy.cn,direct",
            "GOSUMDB" to "sum.golang.google.cn",
            // 禁止 go 自动下载新工具链：proot 跑 toolchain 切换会段错误，强制用本地 go。
            "GOTOOLCHAIN" to "local",
            "CGO_ENABLED" to "0",
            "PATH" to proot_path(go?.go_bin_proot_dir?.let { listOf(it) } ?: emptyList())
        )
        return project_toolchain_environment(
            environment = environment,
            go = go,
            missing = missing
        )
    }

    /**
     * 清理 .bashrc/.profile 里的工具链环境块（迁移期兼容）。
     * 注：前两组 XCode/xcode marker 必须保留，用于清理旧版（还叫 XCode 时）写入用户 shell 配置的残留块。
     */
    fun cleanup_removed_toolchain_environment() {
        val markers_list = listOf(
            "# >>> XCode toolchain environment >>>" to "# <<< XCode toolchain environment <<<",
            "# >>> xcode toolchain environment >>>" to "# <<< xcode toolchain environment <<<",
            "# >>> GoStudio toolchain environment >>>" to "# <<< GoStudio toolchain environment <<<"
        )
        listOf(".bashrc", ".profile").forEach { file_name ->
            val file = File(toolchain_runtime_provider.paths().home_dir, file_name)
            if (!file.exists()) return@forEach
            val current = file.readText()
            val cleaned = markers_list.fold(current) { content, (start, end) ->
                remove_block(content, start, end)
            }
            if (cleaned != current) file.writeText(cleaned.trimEnd() + "\n")
        }
    }

    private fun remove_block(content: String, start_marker: String, end_marker: String): String {
        val start = content.indexOf(start_marker)
        val end = content.indexOf(end_marker)
        if (start < 0 || end < start) return content
        val after_end = end + end_marker.length
        val cleaned = (content.substring(0, start).trimEnd() + "\n" + content.substring(after_end).trimStart()).trimEnd()
        return remove_block(cleaned, start_marker, end_marker)
    }
}
