package com.termux.app.gostudio.executor

import android.content.Context
import android.util.Log
import com.termux.shared.termux.TermuxConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * 通过 Termux shell 执行命令
 * 直接使用 Termux 的 PREFIX 环境变量，不需要 proot
 */
class TermuxShellExecutor(private val context: Context) {

    companion object {
        private const val TAG = "TermuxShell"
    }

    /** Go 是否已安装 */
    fun isGoInstalled(): Boolean {
        return File("${TermuxConstants.TERMUX_PREFIX_DIR_PATH}/bin/go").exists()
    }

    /** gopls 是否已安装 */
    fun isGoplsInstalled(): Boolean {
        return File("${TermuxConstants.TERMUX_HOME_DIR_PATH}/go/bin/gopls").exists()
    }

    /** 执行命令并等待结果 */
    suspend fun executeAndWait(
        command: String,
        timeoutMs: Long = 60_000L
    ): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val pb = ProcessBuilder(
                    TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash", "-c", command
                )
                pb.environment().putAll(buildEnv())
                pb.redirectErrorStream(true)
                val process = pb.start()

                val output = StringBuilder()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String? = null
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                val exitCode = process.waitFor()
                Pair(output.toString(), if (exitCode != 0) "exit code: $exitCode" else "")
            } catch (e: Exception) {
                Log.e(TAG, "执行失败", e)
                Pair("", "执行失败: ${e.message}")
            }
        }
    }

    /** 创建流式执行进程 */
    fun createStreamingProcess(
        command: String,
        onOutput: (String) -> Unit,
        onComplete: (Int) -> Unit
    ): ShellProcess {
        val proc = ShellProcess(buildEnv(), command, onOutput, onComplete)
        proc.start()
        return proc
    }

    /** 构建环境变量 */
    fun buildEnv(): Map<String, String> = mapOf(
        "PREFIX" to TermuxConstants.TERMUX_PREFIX_DIR_PATH,
        "HOME" to TermuxConstants.TERMUX_HOME_DIR_PATH,
        "TMPDIR" to TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH,
        "PATH" to "${TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH}:${TermuxConstants.TERMUX_HOME_DIR_PATH}/go/bin:/system/bin:/system/xbin",
        "LD_LIBRARY_PATH" to TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/lib",
        "TERM" to "xterm-256color",
        "GOPROXY" to "https://goproxy.cn,direct"
    )

    /** 流式进程 */
    class ShellProcess(
        private val env: Map<String, String>,
        private val command: String,
        private val onOutput: (String) -> Unit,
        private val onComplete: (Int) -> Unit
    ) {
        private var running = false
        private var process: Process? = null

        fun start() {
            running = true
            Thread {
                try {
                    val pb = ProcessBuilder(
                        TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash", "-c", command
                    )
                    pb.environment().putAll(env)
                    pb.redirectErrorStream(true)
                    process = pb.start()

                    val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                    var line: String? = null
                    while (running && reader.readLine().also { line = it } != null) {
                        onOutput("$line\n")
                    }
                    val exitCode = if (running) process!!.waitFor() else -1
                    onComplete(exitCode)
                } catch (e: Exception) {
                    onOutput("执行异常: ${e.message}\n")
                    onComplete(-1)
                }
            }.apply { isDaemon = true; start() }
        }

        fun stop() {
            running = false
            try {
                process?.destroy()
                Thread.sleep(500)
                process?.destroyForcibly()
            } catch (_: Exception) {}
        }
    }
}
