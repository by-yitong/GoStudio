package com.jmwl.gostudio.shell

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
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
    private var log_scroll: ScrollView? = null
    private var log_header: TextView? = null
    private var log_expanded = true
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
        copy_asset_dir("app/images", File(filesDir, "images"))
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
        b.send_lifecycle("create")
        b.send_lifecycle("start")
        append_log("已启动")
    }

    override fun onResume() {
        super.onResume()
        bridge?.send_lifecycle("resume")
    }

    override fun onPause() {
        bridge?.send_lifecycle("pause")
        super.onPause()
    }

    override fun onStop() {
        bridge?.send_lifecycle("stop")
        super.onStop()
    }

    override fun onDestroy() {
        bridge?.send_lifecycle("destroy")
        Handler(Looper.getMainLooper()).postDelayed({ bridge?.stop() }, 150)
        super.onDestroy()
    }

    private fun copy_asset_dir(asset_path: String, target_dir: File) {
        if (!target_dir.isDirectory) target_dir.mkdirs() else return
        assets.list(asset_path)?.forEach { name ->
            val source = "$asset_path/$name"
            val target = File(target_dir, name)
            if (target.isFile) return@forEach
            assets.open(source).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun wire_click_events() {
        views_by_id.forEach { (id, view) ->
            view.setOnClickListener { bridge?.send_event(id, "click") }
            view.setOnLongClickListener {
                bridge?.send_event(id, "long_click")
                true
            }

            when (view) {
                is TextView -> view.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                    override fun afterTextChanged(s: Editable?) {
                        bridge?.send_event(id, "text_change", text = s?.toString() ?: "")
                    }
                })
                is CompoundButton -> view.setOnCheckedChangeListener { _, checked ->
                    bridge?.send_event(id, "checked_change", checked = checked)
                }
                is SeekBar -> view.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) bridge?.send_event(id, "progress_change", number = progress.toDouble())
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
                is RatingBar -> view.setOnRatingBarChangeListener { _, rating, fromUser ->
                    if (fromUser) bridge?.send_event(id, "rating_change", number = rating.toDouble())
                }
                is DatePicker -> view.init(
                    view.year, view.month, view.dayOfMonth
                ) { _, year, month, day ->
                    bridge?.send_event(id, "date_change", text = "%04d-%02d-%02d".format(year, month + 1, day))
                }
                is TimePicker -> view.setOnTimeChangedListener { _, hour, minute ->
                    bridge?.send_event(id, "time_change", text = "%02d:%02d".format(hour, minute))
                }
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

        val density = resources.displayMetrics.density
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1B1C1F.toInt())
        }
        val header = TextView(this).apply {
            text = "运行日志    ▾"
            setTextColor(0xFF5CCFE6.toInt())
            textSize = 12f
            setPadding((density * 14).toInt(), (density * 9).toInt(), (density * 14).toInt(), (density * 9).toInt())
            setOnClickListener {
                log_expanded = !log_expanded
                log_scroll?.visibility = if (log_expanded) View.VISIBLE else View.GONE
                text = if (log_expanded) "运行日志    ▾" else "运行日志    ▸"
            }
        }
        log_header = header
        val scroll = ScrollView(this)
        log_scroll = scroll
        log_view = TextView(this).apply {
            setTextIsSelectable(true)
            setTextColor(0xFFE6E6E6.toInt())
            textSize = 11f
            setPadding((density * 14).toInt(), (density * 8).toInt(), (density * 14).toInt(), (density * 12).toInt())
        }
        scroll.addView(log_view, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        scroll.setBackgroundColor(0xFFF0101013.toInt())
        panel.addView(header, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        panel.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (density * 136).toInt()
        ))
        frame.addView(panel, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
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
        log_scroll?.post { log_scroll?.fullScroll(ScrollView.FOCUS_DOWN) }
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

    private fun show_native_dialog(title: String, message: String, buttons: List<String>) {
        val builder = AlertDialog.Builder(this)
            .setTitle(title.ifBlank { "提示" })
            .setMessage(message)
        when (buttons.size) {
            1 -> builder.setPositiveButton(buttons[0]) { dialog, _ ->
                dialog.dismiss()
                bridge?.send_event("", "dialog", text = buttons[0])
            }
            else -> {
                builder.setPositiveButton(buttons[0]) { dialog, _ ->
                    dialog.dismiss()
                    bridge?.send_event("", "dialog", text = buttons[0])
                }
                builder.setNegativeButton(buttons[1]) { dialog, _ ->
                    dialog.dismiss()
                    bridge?.send_event("", "dialog", text = buttons[1])
                }
                if (buttons.size > 2) {
                    builder.setNeutralButton(buttons[2]) { dialog, _ ->
                        dialog.dismiss()
                        bridge?.send_event("", "dialog", text = buttons[2])
                    }
                }
            }
        }
        builder.show()
    }

    fun on_quit() = finish()

    fun on_system_call(action: String, msg: JSONObject): String {
        return when (action) {
            "toast" -> {
                Toast.makeText(
                    this,
                    msg.optString("text"),
                    if (msg.optInt("duration") == 1) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
                ""
            }
            "alert" -> {
                show_native_dialog(
                    title = msg.optString("title"),
                    message = msg.optString("text"),
                    buttons = listOf("确定")
                )
                ""
            }
            "dialog" -> {
                val labels = mutableListOf<String>()
                msg.optJSONArray("value")?.let { array ->
                    repeat(array.length()) { index ->
                        array.optString(index).takeIf { it.isNotBlank() }?.let(labels::add)
                    }
                }
                if (labels.isEmpty()) labels += "确定"
                show_native_dialog(
                    title = msg.optString("title"),
                    message = msg.optString("text"),
                    buttons = labels.take(3)
                )
                ""
            }
            "vibrate" -> {
                val duration = msg.optInt("duration", 200).coerceIn(0, 10_000)
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration.toLong())
                }
                ""
            }
            "clipboard_set" -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("App", msg.optString("text")))
                ""
            }
            "clipboard_get" -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: ""
            }
            "open_url" -> {
                val uri = Uri.parse(msg.optString("text"))
                check(uri.scheme == "http" || uri.scheme == "https") { "仅支持 http/https 链接" }
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                ""
            }
            "share" -> {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, msg.optString("title"))
                    putExtra(Intent.EXTRA_TEXT, msg.optString("text"))
                }
                startActivity(Intent.createChooser(intent, msg.optString("title", "分享")))
                ""
            }
            "device_info" -> {
                JSONObject()
                    .put("manufacturer", Build.MANUFACTURER)
                    .put("model", Build.MODEL)
                    .put("android", Build.VERSION.RELEASE ?: "")
                    .put("sdk", Build.VERSION.SDK_INT)
                    .put("package_name", packageName)
                    .put("version_name", packageManager.getPackageInfo(packageName, 0).versionName ?: "")
                    .put("width", resources.displayMetrics.widthPixels)
                    .put("height", resources.displayMetrics.heightPixels)
                    .put("density", resources.displayMetrics.density.toDouble())
                    .toString()
            }
            else -> error("不支持的系统 API: $action")
        }
    }
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

    fun send_event(id: String, event: String, text: String = "", number: Double = 0.0, checked: Boolean = false) {
        send(
            org.json.JSONObject()
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
    }

    private fun handle_line(line: String, handler: shell_activity) {
        // 协议必须是 JSON 行；fmt.Println 等普通输出直接显示到运行日志。
        val msg = runCatching { org.json.JSONObject(line) }.getOrNull() ?: run {
            main_handler.post { on_ui_log(line) }
            return
        }
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
            "system" -> {
                val action = msg.optString("action")
                val task = FutureTask { handler.on_system_call(action, msg) }
                main_handler.post(task)
                val result = runCatching { task.get(5, TimeUnit.SECONDS) }.getOrNull()
                send_ack(msg.optLong("seq"), ok = result != null, text = result)
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
