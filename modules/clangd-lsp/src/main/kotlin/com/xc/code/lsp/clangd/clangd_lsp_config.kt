package com.xc.code.lsp.clangd

import com.xc.code.toolchain.runtime.toolchain_runtime_paths
import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import java.io.File

data class clangd_lsp_config(
    val runtime_paths: toolchain_runtime_paths,
    val project_dir: File,
    val build_dir: File,
    val path: String,
    val ndk_llvm_bin_proot_dir: String,
    val extra_environment: Map<String, String> = emptyMap(),
    val disabled_features: Set<LspFeature> = emptySet(),
    val extra_arguments: List<String> = emptyList(),
    val on_stderr: (String) -> Unit = {}
) {
    init {
        require(ndk_llvm_bin_proot_dir.isNotBlank()) { "NDK LLVM bin path is required for clangd" }
    }

    val compile_commands_dir: String
        get() = build_dir.absolutePath

    val clangd_command: String
        get() = "${ndk_llvm_bin_proot_dir.trimEnd('/')}/clangd"

    fun arguments(): List<String> {
        return listOf(
            clangd_command,
            "--compile-commands-dir=$compile_commands_dir",
            "--background-index",
            "--header-insertion=never"
        ) + extra_arguments
    }
}
