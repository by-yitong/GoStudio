package com.xc.code.lsp.clangd

import com.xc.code.toolchain.runtime.proot_bind_mount
import com.xc.code.toolchain.runtime.proot_command_builder
import com.xc.code.toolchain.runtime.proot_environment
import com.xc.code.toolchain.runtime.rootfs_patcher
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class clangd_stream_connection_provider(
    private val config: clangd_lsp_config,
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

        val command = build_proot_clangd_command()
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
        return requireNotNull(process) { "clangd process has not been started" }
    }

    private fun validate_runtime() {
        val paths = config.runtime_paths
        require(paths.proot_file.isFile) { "proot not found: ${paths.proot_file.absolutePath}" }
        require(paths.proot_loader_file.isFile) { "proot loader not found: ${paths.proot_loader_file.absolutePath}" }
        require(paths.ubuntu_base_dir.isDirectory) { "Ubuntu rootfs not found: ${paths.ubuntu_base_dir.absolutePath}" }
    }

    private fun build_proot_clangd_command(): List<String> {
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
        val project_mounts = listOf(
            proot_bind_mount(config.project_dir),
            proot_bind_mount(config.build_dir)
        ).distinctBy { it.source.absolutePath }
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
