package com.xc.code.toolchain.runtime

import java.io.File

data class toolchain_runtime_paths(
    val xcode_dir: File,
    val home_dir: File,
    val ubuntu_base_dir: File,
    val proot_tmp_dir: File,
    val external_storage_dir: File? = null,
    val native_library_dir: File
) {
    val proot_file: File get() = File(native_library_dir, PROOT_EXEC)
    val proot_loader_file: File get() = File(native_library_dir, PROOT_LOADER)

    private companion object {
        private const val PROOT_EXEC = "libproot_exec.so"
        private const val PROOT_LOADER = "libproot_loader.so"
    }
}
