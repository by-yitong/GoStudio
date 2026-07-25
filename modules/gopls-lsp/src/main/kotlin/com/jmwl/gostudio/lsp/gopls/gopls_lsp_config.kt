package com.jmwl.gostudio.lsp.gopls

import com.jmwl.gostudio.toolchain.runtime.toolchain_runtime_paths
import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import java.io.File

/**
 * gopls LSP 配置（参照 clangd_lsp_config，适配 Go）。
 *
 * gopls 由 apt 装到 proot rootfs 的 /usr/local/go/bin/gopls（golang 包联动）。
 * 与 clangd 不同：gopls 用 go.mod 定位模块，无需 compile_commands.json 或 --compile-commands-dir。
 *
 * @param runtime_paths proot 运行时路径
 * @param project_dir 项目目录（host 路径，proot 会绑定）
 * @param path guest 内 PATH（含 /usr/local/go/bin）
 * @param gopls_command guest 内 gopls 路径（默认 /usr/local/go/bin/gopls）
 * @param extra_environment 额外环境变量（GOPATH/GOPROXY 等）
 * @param disabled_features 禁用的 LSP 功能
 * @param extra_arguments 额外 gopls 命令行参数
 * @param on_stderr gopls stderr 日志回调
 */
data class gopls_lsp_config(
    val runtime_paths: toolchain_runtime_paths,
    val project_dir: File,
    val path: String,
    val gopls_command: String = "/usr/bin/gopls",
    val extra_environment: Map<String, String> = emptyMap(),
    val disabled_features: Set<LspFeature> = emptySet(),
    val extra_arguments: List<String> = emptyList(),
    val on_stderr: (String) -> Unit = {}
) {
    init {
        require(gopls_command.isNotBlank()) { "gopls command path is required" }
    }

    /**
     * gopls 启动参数。
     * - `-rpc.trace`：可选调试；默认不开。
     * gopls 不需要 clangd 的 --compile-commands-dir，它用 go.mod 自动定位。
     */
    fun arguments(): List<String> {
        return listOf(gopls_command) + extra_arguments
    }
}
