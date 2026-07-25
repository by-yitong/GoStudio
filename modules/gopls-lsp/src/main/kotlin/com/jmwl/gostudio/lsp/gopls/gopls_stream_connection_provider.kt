package com.jmwl.gostudio.lsp.gopls

import com.jmwl.gostudio.toolchain.runtime.proot_bind_mount
import com.jmwl.gostudio.toolchain.runtime.proot_command_builder
import com.jmwl.gostudio.toolchain.runtime.proot_environment
import com.jmwl.gostudio.toolchain.runtime.rootfs_patcher
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * gopls 进程启动器（参照 clangd_stream_connection_provider）。
 *
 * 通过 proot 在 Ubuntu rootfs 内启动 gopls，其 stdin/stdout 作为 JSON-RPC 传输。
 *
 * 关键（解决旧工程 gopls 路径警告）：
 * - proot 工作目录 = 项目根（与 rootUri 一致），gopls 的 os.Getwd() 直接是项目根
 * - 绑定 project_dir，gopls 通过 go.mod 自动定位模块
 * - 新工程无 Termux $PREFIX，gopls 在纯 proot 视图运行，realpath 不泄漏 host 路径
 */
class gopls_stream_connection_provider(
    private val config: gopls_lsp_config,
    private val patcher: rootfs_patcher = rootfs_patcher()
) : StreamConnectionProvider {
    private var process: Process? = null
    private var stderr_thread: Thread? = null

    override val inputStream: InputStream
        get() = require_process().inputStream

    override val outputStream: OutputStream
        get() = require_process().outputStream

    override val isClosed: Boolean
        get() = process?.isAlive != true

    @Throws(IOException::class)
    override fun start() {
        if (process?.isAlive == true) return
        validate_runtime()
        patcher.patch(config.runtime_paths)

        val command = build_proot_gopls_command()
        val process_environment = proot_environment(
            path = config.path,
            extra = config.extra_environment
        ).as_map(config.runtime_paths)

        val started = ProcessBuilder(command)
            .directory(config.project_dir)
            .apply { environment().putAll(process_environment) }
            .start()

        process = started
        stderr_thread = create_stderr_thread(started).also { it.start() }
    }

    override fun close() {
        val running = process ?: return
        runCatching { running.outputStream.close() }
        if (running.isAlive) {
            running.destroy()
            runCatching {
                if (!running.waitFor(500, TimeUnit.MILLISECONDS) && running.isAlive) {
                    running.destroyForcibly()
                }
            }.onFailure {
                if (running.isAlive) running.destroyForcibly()
            }
        }
        stderr_thread?.join(500)
        stderr_thread = null
        process = null
    }

    private fun require_process(): Process {
        return requireNotNull(process) { "gopls process has not been started" }
    }

    private fun validate_runtime() {
        val paths = config.runtime_paths
        require(paths.proot_file.isFile) { "proot not found: ${paths.proot_file.absolutePath}" }
        require(paths.proot_loader_file.isFile) { "proot loader not found: ${paths.proot_loader_file.absolutePath}" }
        require(paths.ubuntu_base_dir.isDirectory) { "Ubuntu rootfs not found: ${paths.ubuntu_base_dir.absolutePath}" }
    }

    /**
     * 构建 proot+gopls 命令行。
     *
     * 关键：把项目目录 bind 到 guest 内的【同名 host 路径】（/data/.../projects/bbb），
     * 而不是映射到 /home/gostudio/...。这样 gopls 进程看到的文件路径与 sora LSP 客户端
     * 发送的 file URI（host 路径）完全一致，避免「No active builds contain ...」错误。
     * working_dir 也用 host 路径（已被 bind 进 guest，cd 能成功）。
     */
    private fun build_proot_gopls_command(): List<String> {
        val builder = proot_command_builder(config.runtime_paths)
        val environment = linkedMapOf(
            "HOME" to "/home",
            "PATH" to config.path,
            "TERM" to "xterm-256color",
            "LANG" to "C.UTF-8",
            "LC_ALL" to "C.UTF-8",
            "TMPDIR" to "/tmp"
        ).apply {
            putAll(config.extra_environment.filterKeys { it.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")) })
        }
        // 绑定项目目录到 guest 同名路径，保证 gopls 与 sora 的文件路径视图一致
        val project_mounts = listOf(proot_bind_mount(config.project_dir))
        return listOf(config.runtime_paths.proot_file.absolutePath) +
            builder.base_args(
                working_dir = config.project_dir.absolutePath,
                extra_mounts = project_mounts
            ) +
            listOf("/usr/bin/env", "-i") +
            environment.map { (key, value) -> "$key=$value" } +
            config.arguments()
    }

    private fun create_stderr_thread(process: Process): Thread {
        return Thread {
            runCatching {
                process.errorStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        if (line.isNotBlank()) config.on_stderr(line)
                    }
                }
            }
        }.apply { isDaemon = true }
    }
}
