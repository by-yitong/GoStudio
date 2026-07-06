package com.jmwl.gostudio.toolchain

import android.content.Context
import android.os.Environment
import com.jmwl.gostudio.toolchain.runtime.proot_command_builder
import com.jmwl.gostudio.toolchain.runtime.proot_shell_runner
import com.jmwl.gostudio.toolchain.runtime.toolchain_runtime_paths
import java.io.File

object toolchain_runtime_provider {
    private lateinit var runtime_paths: toolchain_runtime_paths

    fun init(
        context: Context,
        gostudio_dir: File,
        home_dir: File,
        ubuntu_base_dir: File,
        proot_tmp_dir: File,
        external_storage_dir: File? = Environment.getExternalStorageDirectory()
    ) {
        runtime_paths = toolchain_runtime_paths(
            gostudio_dir = gostudio_dir,
            home_dir = home_dir,
            ubuntu_base_dir = ubuntu_base_dir,
            proot_tmp_dir = proot_tmp_dir,
            external_storage_dir = external_storage_dir,
            native_library_dir = File(context.applicationInfo.nativeLibraryDir)
        )
    }

    fun paths(): toolchain_runtime_paths {
        check(::runtime_paths.isInitialized) { "toolchain_runtime_provider is not initialized" }
        return runtime_paths
    }

    fun command_builder(): proot_command_builder {
        return proot_command_builder(paths())
    }

    fun shell_runner(): proot_shell_runner {
        return proot_shell_runner(
            paths = paths(),
            path = toolchain_manager.proot_path()
        )
    }
}
