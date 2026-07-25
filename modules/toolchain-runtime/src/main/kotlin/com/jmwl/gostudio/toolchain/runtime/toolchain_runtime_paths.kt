package com.jmwl.gostudio.toolchain.runtime

import java.io.File

data class toolchain_runtime_paths(
    val gostudio_dir: File,
    val home_dir: File,
    val ubuntu_base_dir: File,
    val proot_tmp_dir: File,
    val external_storage_dir: File? = null,
    val native_library_dir: File
) {
    val proot_file: File get() = File(native_library_dir, PROOT_EXEC)
    val proot_loader_file: File get() = File(native_library_dir, PROOT_LOADER)

    /**
     * 将 host 文件系统路径转换为 proot guest 内的路径。
     *
     * 已知的绑定关系（见 proot_command_builder.base_args）：
     *   host gostudio_dir  →  guest /home/gostudio
     *   host home_dir      →  guest /home
     *
     * 项目统一存放在 gostudio_dir/projects/<name>，guest 内对应 /home/gostudio/projects/<name>。
     * 若路径不在任何已知绑定根下，原样返回（调用方需保证路径在 guest 内存在）。
     */
    fun host_to_guest_path(host_path: String): String {
        val gostudio_root = gostudio_dir.absolutePath.trimEnd('/')
        val home_root = home_dir.absolutePath.trimEnd('/')
        val normalized = host_path.trimEnd('/')
        return when {
            normalized == gostudio_root -> "/home/gostudio"
            normalized.startsWith("$gostudio_root/") -> "/home/gostudio" + normalized.removePrefix(gostudio_root)
            normalized == home_root -> "/home"
            normalized.startsWith("$home_root/") -> "/home" + normalized.removePrefix(home_root)
            else -> host_path
        }
    }

    private companion object {
        private const val PROOT_EXEC = "libproot_exec.so"
        private const val PROOT_LOADER = "libproot_loader.so"
    }
}
