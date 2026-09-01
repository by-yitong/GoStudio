package com.jmwl.gostudio.runtime

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jmwl.gostudio.toolchain.runtime.proot_process_runner
import com.jmwl.gostudio.toolchain.runtime.toolchain_runtime_paths
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/**
 * 「App 运行」桥：在 proot rootfs 内启动用户编译出的 Go 二进制，
 * 通过 stdin/stdout 的 JSON 行协议与宿主界面双向通信。
 *
 * Go -> 宿主：set_text / get_text / log / quit（宿主回复 ack）
 * 宿主 -> Go：click（通知点击事件）
 */
class runtime_bridge(
    private val paths: toolchain_runtime_paths,
    private val proot_path: String,
    private val on_ui_log: (String) -> Unit,
    private val on_exit: (Int) -> Unit
) {
    interface protocol_handler {
        fun on_set_text(vid: String, text: String)
        fun on_get_text(vid: String): String
        fun on_system_call(action: String, msg: JSONObject): String
        fun on_quit()
    }

    private val main_handler = Handler(Looper.getMainLooper())
    @Volatile private var process: Process? = null
    @Volatile private var closed = false
    private var writer: BufferedWriter? = null
    private var reader_thread: Thread? = null
    private var stderr_thread: Thread? = null
    private val write_lock = Any()

    fun start(
        binary_path: String,
        working_dir: String,
        extra_environment: Map<String, String>,
        handler: protocol_handler
    ) {
        check(process == null) { "bridge already started" }
        val runner = proot_process_runner(paths, proot_path)
        val proc = runner.start(
            command = proot_process_runner.exec_command(binary_path),
            working_dir = working_dir,
            extra_environment = extra_environment
        )
        process = proc
        writer = BufferedWriter(OutputStreamWriter(proc.outputStream, Charsets.UTF_8))

        reader_thread = Thread {
            try {
                BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8)).use { reader ->
                    while (!closed) {
                        val line = reader.readLine() ?: break
                        if (line.isBlank()) continue
                        handle_line(line, handler)
                    }
                }
            } catch (_: InterruptedException) {
            } catch (e: Exception) {
                if (!closed) log_ui("桥接读取错误: ${e.message}")
            } finally {
                val code = runCatching { proc.waitFor() }.getOrDefault(-1)
                if (!closed) {
                    log_ui("程序已退出 (code $code)")
                    main_handler.post { on_exit(code) }
                }
            }
        }.apply { isDaemon = true; start() }

        stderr_thread = Thread {
            try {
                BufferedReader(InputStreamReader(proc.errorStream, Charsets.UTF_8)).use { reader ->
                    while (!closed) {
                        val line = reader.readLine() ?: break
                        log_ui(line)
                    }
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
    }

    fun send_event(id: String, event: String, text: String = "", number: Double = 0.0, checked: Boolean = false) {
        send(
            JSONObject()
                .put("op", "event")
                .put("vid", id)
                .put("event", event)
                .put("text", text)
                .put("number", number)
                .put("boolean", checked)
        )
    }

    fun send_lifecycle(event: String) {
        send_event("", event)
    }

    fun stop() {
        if (closed) return
        closed = true
        runCatching { process?.destroy() }
        runCatching {
            // proot --kill-on-exit 兜底已足够，再等一小会儿让线程收尾
            reader_thread?.join(300)
            stderr_thread?.join(300)
        }
    }

    private fun handle_line(line: String, handler: protocol_handler) {
        val msg = runCatching { JSONObject(line) }.getOrNull() ?: return
        when (msg.optString("op")) {
            "set_text" -> {
                val vid = msg.optString("vid")
                val text = msg.optString("text")
                post_to_main { handler.on_set_text(vid, text) }
                send_ack(msg.optLong("seq"))
            }
            "get_text" -> {
                val vid = msg.optString("vid")
                val text = fetch_on_main { handler.on_get_text(vid) } ?: ""
                send_ack(msg.optLong("seq"), text = text)
            }
            "log" -> {
                log_ui(msg.optString("text"))
                send_ack(msg.optLong("seq"))
            }
            "system" -> {
                val action = msg.optString("action")
                val result = fetch_on_main { handler.on_system_call(action, msg) }
                send_ack(msg.optLong("seq"), ok = result != null, text = result)
            }
            "quit" -> {
                send_ack(msg.optLong("seq"))
                post_to_main { handler.on_quit() }
            }
        }
    }

    private fun post_to_main(block: () -> Unit) {
        main_handler.post(block)
    }

    /** 在主线程执行并同步取回结果，供 get_text 这类需要返回值的操作使用。 */
    private fun fetch_on_main(block: () -> String): String? {
        val task = FutureTask(block)
        main_handler.post(task)
        return try {
            task.get(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w("runtime_bridge", "main thread fetch timeout", e)
            null
        }
    }

    private fun send_ack(seq: Long, ok: Boolean = true, text: String? = null) {
        val ack = JSONObject().put("op", "ack").put("seq", seq).put("ok", ok)
        if (text != null) ack.put("text", text)
        send(ack)
    }

    private fun send(json: JSONObject) {
        val w = writer ?: return
        try {
            synchronized(write_lock) {
                w.write(json.toString())
                w.write("\n")
                w.flush()
            }
        } catch (e: Exception) {
            if (!closed) Log.w("runtime_bridge", "send failed", e)
        }
    }

    private fun log_ui(message: String) {
        main_handler.post { on_ui_log(message) }
    }
}
