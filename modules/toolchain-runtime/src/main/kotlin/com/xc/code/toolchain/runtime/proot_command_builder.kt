package com.xc.code.toolchain.runtime

class proot_command_builder(
    private val paths: toolchain_runtime_paths
) {
    fun base_args(
        working_dir: String = "/home",
        include_xcode_mount: Boolean = true,
        extra_mounts: List<proot_bind_mount> = emptyList()
    ): List<String> {
        val args = mutableListOf(
            "--android-profile",
            "--link2symlink",
            "--kill-on-exit",
            "-0",
            "-r", paths.ubuntu_base_dir.absolutePath,
            "-b", "/sys",
            "-b", "/dev",
            "-b", "/proc",
            "-w", working_dir.ifBlank { "/home" },
            "-b", "${paths.home_dir.absolutePath}:/home"
        )

        if (include_xcode_mount) {
            args += "-b"
            args += "${paths.xcode_dir.absolutePath}:/home/xcode"
        }

        paths.external_storage_dir?.let { external_storage ->
            if (external_storage.exists()) {
                args += "-b"
                args += external_storage.absolutePath
            }
        }

        extra_mounts.forEach { mount ->
            if (mount.source.exists()) {
                args += "-b"
                args += mount.as_argument()
            }
        }

        return args
    }

    fun command(
        shell_command: String,
        working_dir: String = "/home",
        include_xcode_mount: Boolean = true,
        extra_mounts: List<proot_bind_mount> = emptyList(),
        extra_environment: Map<String, String> = emptyMap()
    ): List<String> {
        val dollar = '$'
        val wrapper = "cd -- \"${dollar}1\" && eval \"${dollar}2\""
        return listOf(paths.proot_file.absolutePath) +
            base_args(working_dir, include_xcode_mount, extra_mounts) +
            clean_shell_env_args(extra_environment = extra_environment) +
            listOf(
                "/bin/bash",
                "-l",
                "-c",
                wrapper,
                "xcode",
                working_dir.ifBlank { "/home" },
                shell_command
            )
    }

    fun interactive_args(
        working_dir: String = "/home",
        include_xcode_mount: Boolean = true,
        extra_mounts: List<proot_bind_mount> = emptyList()
    ): Array<String> {
        return (base_args(working_dir, include_xcode_mount, extra_mounts) +
            clean_shell_env_args() +
            listOf("/bin/bash")).toTypedArray()
    }
}
