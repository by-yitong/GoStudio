package com.xc.code.toolchain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object proot_manager {
    suspend fun execute_command(
        command: String,
        on_log: (String) -> Unit
    ): Boolean {
        return execute_command_with_environment(
            command = command,
            working_dir = "/home",
            extra_environment = emptyMap(),
            on_log = on_log
        )
    }

    suspend fun execute_command_with_environment(
        command: String,
        working_dir: String = "/home",
        extra_environment: Map<String, String> = emptyMap(),
        on_log: (String) -> Unit
    ): Boolean {
        return try {
            toolchain_runtime_provider.shell_runner().execute(
                command = command,
                working_dir = working_dir,
                extra_environment = extra_environment,
                on_log = { message ->
                    withContext(Dispatchers.Main) {
                        on_log(message)
                    }
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                on_log("执行错误: ${e.message}")
            }
            false
        }
    }
}
