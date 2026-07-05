package com.xc.code.toolchain.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.TimeUnit

class proot_shell_runner(
    private val paths: toolchain_runtime_paths,
    private val path: String,
    private val patcher: rootfs_patcher = rootfs_patcher()
) {
    private val builder = proot_command_builder(paths)

    suspend fun execute(
        command: String,
        working_dir: String = "/home",
        extra_environment: Map<String, String> = emptyMap(),
        timeout_millis: Long = 0L,
        on_log: suspend (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (!paths.proot_file.isFile) {
            on_log("错误: proot 未找到: ${paths.proot_file.absolutePath}")
            return@withContext false
        }
        
        if (!paths.proot_loader_file.isFile) {
            on_log("错误: proot loader 未找到: ${paths.proot_loader_file.absolutePath}")
            return@withContext false
        }
        
        if (!paths.ubuntu_base_dir.isDirectory) {
            on_log("错误: Ubuntu rootfs 未找到: ${paths.ubuntu_base_dir.absolutePath}")
            return@withContext false
        }

        patcher.patch(paths)
        val proot_env = proot_environment(path, extra_environment).as_map(paths)
        val proot_command = builder.command(
            shell_command = command,
            working_dir = working_dir,
            extra_environment = extra_environment
        )
        val process = ProcessBuilder(proot_command)
            .apply { environment().putAll(proot_env) }
            .start()
        val coroutine_context = currentCoroutineContext()

        val stdout = create_log_reader_thread(process.inputStream, "[OUT] ", on_log)
        val stderr = create_log_reader_thread(process.errorStream, "[ERR] ", on_log)
        stdout.start()
        stderr.start()

        try {
            val start_time = System.currentTimeMillis()
            while (process.isAlive) {
                if (!coroutine_context.isActive) {
                    terminate_process(process)
                    kill_build_processes()
                    throw CancellationException("proot command cancelled")
                }
                if (timeout_millis > 0 && System.currentTimeMillis() - start_time >= timeout_millis) {
                    terminate_process(process)
                    kill_build_processes()
                    on_log("执行超时")
                    return@withContext false
                }
                process.waitFor(100, TimeUnit.MILLISECONDS)
            }
            process.exitValue() == 0
        } finally {
            withContext(NonCancellable) {
                terminate_process(process)
                stdout.join(1000)
                stderr.join(1000)
            }
        }
    }

    private fun terminate_process(process: Process) {
        if (!process.isAlive) return
        process.destroy()
        runCatching {
            if (!process.waitFor(500, TimeUnit.MILLISECONDS) && process.isAlive) {
                process.destroyForcibly()
            }
        }.onFailure {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun kill_build_processes() {
        listOf("cmake", "ninja").forEach { name ->
            runCatching {
                ProcessBuilder("pkill", "-f", name)
                    .start()
                    .waitFor(1, TimeUnit.SECONDS)
            }
        }
    }

    private fun format_output_line(prefix: String, message: String, carriage_return: Boolean = false): String {
        val clean_message = message.trimStart()
        val separator = if (clean_message.startsWith("--")) "" else "-- "
        val line = "$prefix$separator$clean_message"
        return if (carriage_return) "\r$line" else line
    }

    private fun create_log_reader_thread(
        stream: InputStream,
        prefix: String,
        on_log: suspend (String) -> Unit
    ): Thread {
        return Thread {
            try {
                stream.reader(Charsets.UTF_8).use { reader ->
                    val pending = StringBuilder()
                    val buffer = CharArray(256)
                    var previous_was_carriage_return = false

                    while (true) {
                        val count = reader.read(buffer)
                        if (count < 0) break

                        for (index in 0 until count) {
                            when (val char = buffer[index]) {
                                '\r' -> {
                                    if (pending.isNotEmpty()) {
                                        val message = pending.toString()
                                        pending.setLength(0)
                                        kotlinx.coroutines.runBlocking { on_log(format_output_line(prefix, message, carriage_return = true)) }
                                    }
                                    previous_was_carriage_return = true
                                }

                                '\n' -> {
                                    if (pending.isNotEmpty()) {
                                        val message = pending.toString()
                                        pending.setLength(0)
                                        kotlinx.coroutines.runBlocking { on_log(format_output_line(prefix, message)) }
                                    }
                                    previous_was_carriage_return = false
                                }

                                else -> {
                                    pending.append(char)
                                    previous_was_carriage_return = false
                                }
                            }
                        }
                    }

                    if (pending.isNotEmpty() && !previous_was_carriage_return) {
                        val message = pending.toString()
                        kotlinx.coroutines.runBlocking { on_log(format_output_line(prefix, message)) }
                    }
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true }
    }
}
