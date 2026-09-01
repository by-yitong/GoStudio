package com.jmwl.gostudio.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
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
import android.widget.Button
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.jmwl.gostudio.toolchain.sandbox_dns
import com.jmwl.gostudio.toolchain.toolchain_manager
import com.jmwl.gostudio.toolchain.toolchain_runtime_provider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 「App 运行」宿主界面：
 * 1. 用平台 LayoutInflater 渲染项目里的 layout.xml（标准 Android 布局）
 * 2. 在 proot rootfs 内启动 go build 产出的业务二进制
 * 3. 通过 runtime_bridge 转发 UI 操作（Go -> 界面）与事件（界面 -> Go）
 *
 * 控件在布局里用 android:tag="控件id" 标记，Go 侧用同名 id 操作。
 */
class runtime_host_activity : AppCompatActivity(), runtime_bridge.protocol_handler {

    private lateinit var project_dir: File
    private var views_by_id: Map<String, View> = emptyMap()
    private var bridge: runtime_bridge? = null
    private var log_view: TextView? = null
    private var log_scroll: ScrollView? = null
    private var log_header: TextView? = null
    private var log_expanded = true
    private val started = AtomicBoolean(false)
    private val log_lines = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Material You 动态取色（Android 12+），低版本回退默认 M3 色板
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        val project_path = intent.getStringExtra(EXTRA_PROJECT_DIR)
            ?: run { finish(); return }
        project_dir = File(project_path)
        val layout_file = File(project_dir, "layout.xml")
        if (!layout_file.isFile) {
            finish()
            return
        }

        val layout = runtime_layout_loader(this).load(layout_file)
        views_by_id = layout.views
        wire_click_events()

        setContentView(build_content(layout.root))
    }

    override fun onStart() {
        super.onStart()
        if (!started.compareAndSet(false, true)) return
        val paths = toolchain_runtime_provider.paths()
        val binary = File(project_dir, "bin/${project_dir.name}")
        if (!binary.isFile) {
            append_log("错误: 未找到可执行文件 ${binary.absolutePath}，请先构建项目")
            return
        }

        sandbox_dns.refresh(this)
        val environment = toolchain_manager.project_environment(project_dir.absolutePath).environment
        val b = runtime_bridge(
            paths = paths,
            proot_path = toolchain_manager.proot_path(),
            on_ui_log = { line -> append_log(line) },
            on_exit = { }
        )
        bridge = b
        b.start(
            binary_path = binary.absolutePath,
            working_dir = project_dir.absolutePath,
            extra_environment = environment,
            handler = this
        )
        b.send_lifecycle("create")
        b.send_lifecycle("start")
        append_log("已启动 ${project_dir.name}")
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

    /** 把布局控件的原生事件转发给 Go。 */
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

    /** 业务布局 + 底部日志浮层。 */
    private fun build_content(root: View): View {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(frame, com.google.android.material.R.attr.colorSurface))
        frame.addView(
            root,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val density = resources.displayMetrics.density
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E61B1C1F"))
        }
        val header = TextView(this).apply {
            text = "运行日志    ▾"
            setTextColor(Color.parseColor("#5CCFE6"))
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
            setTextColor(Color.parseColor("#E6E6E6"))
            textSize = 11f
            setPadding((density * 14).toInt(), (density * 8).toInt(), (density * 14).toInt(), (density * 12).toInt())
        }
        scroll.addView(
            log_view,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        scroll.setBackgroundColor(Color.parseColor("#F0101013"))
        panel.addView(
            header,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        panel.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (density * 136).toInt()
            )
        )
        frame.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )
        return frame
    }

    private fun append_log(line: String) {
        val view = log_view ?: return
        synchronized(log_lines) {
            log_lines.addLast(line)
            while (log_lines.size > max_log_lines) log_lines.removeFirst()
            view.text = log_lines.joinToString("\n")
        }
        log_scroll?.post { log_scroll?.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    // ---- Go -> 宿主 UI ----

    override fun on_set_text(vid: String, text: String) {
        val target = views_by_id[vid] as? TextView
        if (target != null) {
            target.text = text
        } else {
            append_log("警告: set_text 找不到控件 \"$vid\"")
        }
    }

    override fun on_set_image(vid: String, url: String) {
        load_network_image(vid, url)
    }

    private fun load_network_image(vid: String, url: String) {
        val target = views_by_id[vid] as? ImageView
        if (target == null) {
            append_log("警告: set_image 找不到控件 \"$vid\"")
            return
        }
        Thread {
            val result = runCatching {
                val parsed = URL(url)
                check(parsed.protocol == "http" || parsed.protocol == "https") { "仅支持 http/https 图片" }
                val connection = parsed.openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("User-Agent", "GoStudio Runtime")
                val bitmap = connection.inputStream.use(BitmapFactory::decodeStream)
                checkNotNull(bitmap) { "图片解码失败" }
            }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { bitmap ->
                    if (!isDestroyed && !isFinishing) {
                        target.setImageBitmap(bitmap)
                        target.invalidate()
                        append_log("图片已更新: $vid")
                    }
                }.onFailure { error ->
                    append_log("网络图片加载失败: ${error.message}")
                }
            }
        }.apply { isDaemon = true }.start()
    }

    override fun on_get_text(vid: String): String {
        val view = views_by_id[vid] as? TextView
            ?: run { append_log("警告: get_text 找不到控件 \"$vid\""); return "" }
        return view.text?.toString() ?: ""
    }

    override fun on_system_call(action: String, msg: JSONObject): String {
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
                    repeat(array.length()) { index -> array.optString(index).takeIf { it.isNotBlank() }?.let(labels::add) }
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
                clipboard.setPrimaryClip(ClipData.newPlainText("GoStudio", msg.optString("text")))
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

    private fun show_native_dialog(title: String, message: String, buttons: List<String>) {
        val builder = AlertDialog.Builder(this)
            .setTitle(title.ifBlank { "提示" })
            .setMessage(message)
            .setOnDismissListener { }
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

    override fun on_quit() {
        finish()
    }

    companion object {
        private const val max_log_lines = 200
        const val EXTRA_PROJECT_DIR = "project_dir"
    }
}
