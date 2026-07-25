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
 * 旧 NDK 工具链信息（兼容字段，GoStudio 永远返回 null）。
 * editor_activity 的 cmake/clangd 死代码引用此类型，阶段5清理时一并删除。
 */
data class ndk_toolchain_info(
    val version: String,
    val aliases: Set<String>,
    val host_dir: File,
    val proot_dir: String,
    val llvm_bin_host_dir: File,
    val llvm_bin_proot_dir: String,
    val cmake_toolchain_file: String?
)

/**
 * 项目构建环境（Go 版本）。
 *
 * @param environment proot 内的环境变量（PATH/GOROOT/GOPATH/GOPROXY 等）
 * @param go 已装的 Go 工具链；null 表示未装
 * @param missing 缺失项的人类可读描述
 * @param ndk 兼容字段（永远 null；editor_activity 的 cmake 死代码引用，阶段5清理）
 */
data class project_toolchain_environment(
    val environment: Map<String, String>,
    val go: go_toolchain_info?,
    val missing: List<String>,
    val ndk: ndk_toolchain_info? = null
)

/**
 * Go 工具链管理器（替代 XCode 的 cmake/ndk 探测）。
 *
 * Go/gopls 由 apt 装到 proot rootfs 内（/usr/local/go），不需要像 NDK 那样扫描多版本目录。
 * 探测逻辑：检查 rootfs 内 /usr/local/go/bin/go 是否存在，读 `go version` 输出取版本号。
 */
object toolchain_manager {

    private const val PROOT_GOSTUDIO_HOME = "/home/gostudio"
    private const val PROOT_GO_ROOT = "/usr/local/go"
    private const val PROOT_GO_BIN = "$PROOT_GO_ROOT/bin"
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
     * 探测已安装的 Go 工具链（检查 rootfs 内 /usr/local/go/bin/go）。
     * 注意：版本号需 proot 执行 `go version` 才能拿到，这里只判断存在性，版本延迟到运行时。
     */
    fun installed_go(): go_toolchain_info? {
        val go_bin_host = File(toolchain_runtime_provider.paths().ubuntu_base_dir, "usr/local/go/bin/go")
        if (!go_bin_host.isFile) return null
        // gopls 可能在 /usr/local/go/bin/gopls（apt 装的 golang 联动）或 /home/go/bin/gopls（go install）
        val gopls_host_bin = File(toolchain_runtime_provider.paths().ubuntu_base_dir, "usr/local/go/bin/gopls")
        val gopls_proot = if (gopls_host_bin.isFile) PROOT_GOPLS else "$PROOT_GOPATH_BIN/gopls"
        return go_toolchain_info(
            version = "", // 版本由 go version 运行时取，这里留空
            go_proot_dir = PROOT_GO_ROOT,
            go_bin_proot_dir = PROOT_GO_BIN,
            gopls_proot_path = gopls_proot
        )
    }

    /** Go 是否已安装。 */
    fun is_go_installed(): Boolean = installed_go() != null

    /** gopls 是否已安装（/usr/local/go/bin/gopls 或 /home/go/bin/gopls）。 */
    fun is_gopls_installed(): Boolean {
        val rootfs = toolchain_runtime_provider.paths().ubuntu_base_dir
        return File(rootfs, "usr/local/go/bin/gopls").isFile ||
            File(toolchain_runtime_provider.paths().home_dir, "go/bin/gopls").isFile
    }

    /** git 是否已安装（rootfs 内 /usr/bin/git）。 */
    fun is_git_installed(): Boolean =
        File(toolchain_runtime_provider.paths().ubuntu_base_dir, "usr/bin/git").isFile

    /**
     * 组装项目构建环境。
     *
     * @param project_path 项目 host 路径（用于日志，Go 不像 cmake 需要工具链文件）
     */
    fun project_environment(project_path: String): project_toolchain_environment {
        val go = installed_go()
        val missing = mutableListOf<String>()
        if (go == null) missing += "Go 未安装，请在工具页安装 golang"

        val environment = linkedMapOf(
            "GOSTUDIO_HOME" to PROOT_GOSTUDIO_HOME,
            "GOROOT" to PROOT_GO_ROOT,
            "GOPATH" to "/home/go",
            "GOBIN" to PROOT_GOPATH_BIN,
            "GOPROXY" to "https://goproxy.cn,direct",
            "GOSUMDB" to "sum.golang.google.cn",
            "CGO_ENABLED" to "0",
            "PATH" to proot_path()
        )
        return project_toolchain_environment(
            environment = environment,
            go = go,
            missing = missing
        )
    }

    /** 兼容旧调用（main_activity / main_tools_screen 引用）；GoStudio 不分版本，返回空列表。 */
    fun available_cmake_versions(): List<String> = emptyList()
    fun available_ndk_versions(): List<String> = emptyList()
    fun installed_ndk_version_keys(): Set<String> = emptySet()
    fun is_cmake_installed(): Boolean = false
    fun is_ndk_installed(): Boolean = false

    /** 清理旧 .bashrc/.profile 里的 XCode 工具链环境块（迁移期兼容）。 */
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
