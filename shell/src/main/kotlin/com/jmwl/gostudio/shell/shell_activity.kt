package com.jmwl.gostudio.shell

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/**
 * 打包壳：从 assets/app/ 读取 layout.xml 与 Go 二进制。
 * Go 逻辑是纯静态 ARM64 ELF，直接 ProcessBuilder 启动（无需 proot），
 * 与宿主通过 stdin/stdout 的 JSON 行协议通信。
 */
class shell_activity : AppCompatActivity() {

    private lateinit var binary_file: File
    private var views_by_id: Map<String, View> = emptyMap()
    private var bridge: standalone_bridge? = null
    private var log_view: TextView? = null
    private val started = AtomicBoolean(false)
    private val log_lines = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)

        // 1. 复制 assets 到私有目录
        val layout_file = File(filesDir, "layout.xml")
        binary_file = File(filesDir, "app.bin")
        if (!layout_file.isFile) assets.open("app/layout.xml").use { input ->
            layout_file.outputStream().use { input.copyTo(it) }
        }
        if (!binary_file.isFile) assets.open("app/app.bin").use { input ->
            binary_file.outputStream().use { input.copyTo(it) }
        }
        binary_file.setExecutable(true, false)

        // 2. 渲染布局
        val layout = runtime_layout_loader(this).load(layout_file)
        views_by_id = layout.views
        wire_click_events()
        setContentView(build_content(layout.root))
    }

    override fun onStart() {
        super.onStart()
        if (!started.compareAndSet(false, true)) return
        val b = standalone_bridge(
            on_ui_log = { line -> append_log(line) },
            on_exit = { }
        )
        bridge = b
        b.start(binary_file.absolutePath, handler = this)
        append_log("已启动")
    }

    override fun onDestroy() {
        bridge?.stop()
        super.onDestroy()
    }

    private fun wire_click_events() {
        views_by_id.forEach { (id, view) ->
            if (view is Button || view is ImageButton || view is EditText || view.isClickable) {
                view.setOnClickListener { bridge?.send_click(id) }
            }
        }
    }

    private fun build_content(root: View): View {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(
            com.google.android.material.color.MaterialColors.getColor(
                frame, com.google.android.material.R.attr.colorSurface
            )
        )
        frame.addView(root, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        val log_scroll = ScrollView(this)
        log_view = TextView(this).apply {
            setTextIsSelectable(true)
            setTextColor(0xFFE6E6E6.toInt())
            textSize = 11f
            setPadding(24, 20, 24, 20)
        }
        log_scroll.addView(log_view, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        log_scroll.setBackgroundColor(0xCC1C1C22.toInt())
        frame.addView(log_scroll, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.density * 96).toInt(),
            Gravity.BOTTOM
        ))
        return frame
    }

    private fun append_log(line: String) {
        val view = log_view ?: return
        synchronized(log_lines) {
            log_lines.addLast(line)
            while (log_lines.size > 200) log_lines.removeFirst()
            view.text = log_lines.joinToString("\n")
        }
    }

    // ---- Go -> 界面 ----

    fun on_set_text(vid: String, text: String) {
        val target = views_by_id[vid] as? TextView
        if (target != null) target.text = text else append_log("警告: set_text 找不到控件 \"$vid\"")
    }

    fun on_get_text(vid: String): String {
        val view = views_by_id[vid] as? TextView
            ?: run { append_log("警告: get_text 找不到控件 \"$vid\""); return "" }
        return view.text?.toString() ?: ""
    }

    fun on_quit() = finish()
}

/**
 * 与 runtime_bridge 相同的 JSON 行协议，但直接 ProcessBuilder 启动纯 Go 二进制。
 */
class standalone_bridge(
    private val on_ui_log: (String) -> Unit,
    private val on_exit: (Int) -> Unit
) {
    private val main_handler = Handler(Looper.getMainLooper())
    @Volatile private var process: Process? = null
    @Volatile private var closed = false
    private var writer: java.io.BufferedWriter? = null
    private val write_lock = Any()

    fun start(binary_path: String, handler: shell_activity) {
        val proc = ProcessBuilder(binary_path).start()
        process = proc
        writer = java.io.BufferedWriter(
            java.io.OutputStreamWriter(proc.outputStream, Charsets.UTF_8)
        )
        Thread {
            try {
                java.io.BufferedReader(
                    java.io.InputStreamReader(proc.inputStream, Charsets.UTF_8)
                ).use { reader ->
                    while (!closed) {
                        val line = reader.readLine() ?: break
                        if (line.isBlank()) continue
                        handle_line(line, handler)
                    }
                }
            } catch (_: Exception) {
            } finally {
                val code = runCatching { proc.waitFor() }.getOrDefault(-1)
                if (!closed) {
                    main_handler.post {
                        on_ui_log("程序已退出 (code $code)")
                        on_exit(code)
                    }
                }
            }
        }.apply { isDaemon = true; start() }

        Thread {
            try {
                java.io.BufferedReader(
                    java.io.InputStreamReader(proc.errorStream, Charsets.UTF_8)
                ).use { reader ->
                    while (!closed) {
                        val line = reader.readLine() ?: break
                        main_handler.post { on_ui_log(line) }
                    }
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
    }

    fun send_click(vid: String) {
        send(org.json.JSONObject().put("op", "click").put("vid", vid))
    }

    fun stop() {
        if (closed) return
        closed = true
        runCatching { process?.destroy() }
    }

    private fun handle_line(line: String, handler: shell_activity) {
        val msg = runCatching { org.json.JSONObject(line) }.getOrNull() ?: return
        when (msg.optString("op")) {
            "set_text" -> {
                val vid = msg.optString("vid")
                val text = msg.optString("text")
                main_handler.post { handler.on_set_text(vid, text) }
                send_ack(msg.optLong("seq"))
            }
            "get_text" -> {
                val vid = msg.optString("vid")
                val task = FutureTask { handler.on_get_text(vid) }
                main_handler.post(task)
                val text = runCatching { task.get(5, TimeUnit.SECONDS) }.getOrDefault("")
                send_ack(msg.optLong("seq"), text = text)
            }
            "log" -> {
                main_handler.post { on_ui_log(msg.optString("text")) }
                send_ack(msg.optLong("seq"))
            }
            "quit" -> {
                send_ack(msg.optLong("seq"))
                main_handler.post { handler.on_quit() }
            }
        }
    }

    private fun send_ack(seq: Long, ok: Boolean = true, text: String? = null) {
        val ack = org.json.JSONObject().put("op", "ack").put("seq", seq).put("ok", ok)
        if (text != null) ack.put("text", text)
        send(ack)
    }

    private fun send(json: org.json.JSONObject) {
        val w = writer ?: return
        try {
            synchronized(write_lock) {
                w.write(json.toString()); w.write("\n"); w.flush()
            }
        } catch (_: Exception) {
        }
    }
}
