package com.jmwl.gostudio.toolchain.runtime

import java.io.File

/**
 * 启动一个长驻 guest 进程（stdin/stdout 由调用方持有），
 * 供「App 运行」等需要与 rootfs 内程序双向通信的场景使用。
 *
 * 与 proot_shell_runner 的区别：不等待退出、不吞并输出流，
 * 调用方拿到原始 Process 后自行收发数据。
 */
class proot_process_runner(
    private val paths: toolchain_runtime_paths,
    private val path: String,
    private val patcher: rootfs_patcher = rootfs_patcher()
) {
    /**
     * 在 guest 内执行 command（建议用 exec 前缀，让进程直接替换 shell，
     * 这样 destroy 时能准确杀掉目标程序）。
     */
    fun start(
        command: String,
        working_dir: String = "/home",
        extra_environment: Map<String, String> = emptyMap()
    ): Process {
        require(paths.proot_file.isFile) { "proot 未找到: ${paths.proot_file.absolutePath}" }
        require(paths.proot_loader_file.isFile) { "proot loader 未找到: ${paths.proot_loader_file.absolutePath}" }
        require(paths.rootfs_dir.isDirectory) { "rootfs 未找到: ${paths.rootfs_dir.absolutePath}" }

        patcher.patch(paths)
        val proot_env = proot_environment(path, extra_environment).as_map(paths)
        val proot_command = proot_command_builder(paths).command(
            shell_command = command,
            working_dir = working_dir,
            extra_environment = extra_environment
        )
        return ProcessBuilder(proot_command)
            .apply { environment().putAll(proot_env) }
            .start()
    }

    companion object {
        /** 组装 exec 形式的 guest 命令，避免 bash 成为中间父进程。 */
        fun exec_command(binary_path: String, args: List<String> = emptyList()): String {
            val quoted = listOf(binary_path) + args
            return "exec " + quoted.joinToString(" ") { shell_quote(it) }
        }

        private fun shell_quote(value: String): String {
            return "'" + value.replace("'", "'\\''") + "'"
        }
    }
}
