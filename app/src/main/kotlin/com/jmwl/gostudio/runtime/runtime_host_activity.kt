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
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.Chronometer
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextClock
import android.widget.TextView
import android.widget.TimePicker
import android.widget.VideoView
import android.widget.ViewFlipper
import android.widget.Toast
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import com.jmwl.gostudio.toolchain.sandbox_dns
import com.jmwl.gostudio.toolchain.toolchain_manager
import com.jmwl.gostudio.toolchain.toolchain_runtime_provider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
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
    private val views_by_id = mutableMapOf<String, View>()
    private var bridge: runtime_bridge? = null
    private var log_view: TextView? = null
    private var log_scroll: ScrollView? = null
    private var log_header: TextView? = null
    private var log_expanded = true
    private val started = AtomicBoolean(false)
    private var layout_error: String? = null
    /** 页面栈：app.ShowPage 压入新布局，返回键出栈回到上一页。 */
    private val page_stack = ArrayDeque<runtime_layout_loader.Result>()
    private lateinit var page_container: FrameLayout
    private val log_lines = ArrayDeque<String>()
    private lateinit var floating_windows: floating_window_manager

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

        floating_windows = floating_window_manager(
            activity = this,
            base_dir = project_dir,
            register_views = views_by_id::putAll,
            wire_events = ::wire_widget_events,
            on_event = { id, event, value -> bridge?.send_event(id, event, checked = value) }
        )
        try {
            val first = runtime_layout_loader(this).load(layout_file)
            views_by_id.clear()
            page_container = FrameLayout(this)
            setContentView(build_content(page_container))
            show_page(first)

            // 页面栈优先：有上层页面时返回键出栈，最后一页才退出界面
            onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!pop_page()) finish()
                }
            })
        } catch (e: Exception) {
            // 布局 XML 有语法错误等问题时不崩溃：页内展示错误，返回键照常回到编辑器
            layout_error = describe_layout_error(e)
            setContentView(build_layout_error_page(layout_error!!))
        }
    }

    override fun onStart() {
        super.onStart()
        if (!started.compareAndSet(false, true)) return
        if (layout_error != null) return
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
        floating_windows.notify_permission_changed()
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
        if (::floating_windows.isInitialized) floating_windows.close_all()
        bridge?.send_lifecycle("destroy")
        Handler(Looper.getMainLooper()).postDelayed({ bridge?.stop() }, 150)
        super.onDestroy()
    }

    /** 把已加载的页面压栈显示：注册控件并接好事件。 */
    private fun show_page(page: runtime_layout_loader.Result) {
        page_stack.addLast(page)
        (page.root.parent as? ViewGroup)?.removeView(page.root)
        page_container.removeAllViews()
        page_container.addView(
            page.root,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        views_by_id.putAll(page.views)
        page.views.forEach { (id, view) -> wire_widget_events(id, view) }
    }

    /** 加载并切入项目内另一个布局页；返回 null 表示成功，否则为错误信息。 */
    private fun push_page(layout_name: String): String? {
        val file = File(project_dir, layout_name)
        if (!file.isFile) return "页面布局不存在: $layout_name"
        return try {
            show_page(runtime_layout_loader(this).load(file, project_dir))
            null
        } catch (e: Exception) {
            describe_layout_error(e)
        }
    }

    /** 出栈回到上一页；已是最后一页时返回 false。控件表只回退本页注册的 id。 */
    private fun pop_page(): Boolean {
        if (page_stack.size <= 1) return false
        val popped = page_stack.removeLast()
        popped.views.forEach { (id, view) ->
            if (views_by_id[id] === view) views_by_id.remove(id)
        }
        val top = page_stack.last()
        (top.root.parent as? ViewGroup)?.removeView(top.root)
        page_container.removeAllViews()
        page_container.addView(
            top.root,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        views_by_id.putAll(top.views)
        return true
    }

    /** 用新页面替换栈顶：当前页出栈（返回键不再回到它）。先加载成功再出栈，失败时保留当前页。 */
    private fun replace_page(layout_name: String): String? {
        val file = File(project_dir, layout_name)
        if (!file.isFile) return "页面布局不存在: $layout_name"
        return try {
            val page = runtime_layout_loader(this).load(file, project_dir)
            if (page_stack.isNotEmpty()) {
                val removed = page_stack.removeLast()
                removed.views.forEach { (id, view) ->
                    if (views_by_id[id] === view) views_by_id.remove(id)
                }
            }
            show_page(page)
            null
        } catch (e: Exception) {
            describe_layout_error(e)
        }
    }

    private fun wire_widget_events(id: String, view: View) {
            // AdapterView（Spinner/ListView/GridView）禁止 setOnClickListener，系统会直接抛异常；
            // 列表类的条目点击走下方 item_click
            if (view !is AdapterView<*>) {
                view.setOnClickListener { bridge?.send_event(id, "click") }
            }
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
                is AbsListView -> view.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    bridge?.send_event(
                        id,
                        "item_click",
                        text = view.adapter?.getItem(position)?.toString() ?: "",
                        number = position.toDouble()
                    )
                }
                is Spinner -> {
                    var selection_ready = false
                    view.post { selection_ready = true }
                    view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, row_id: Long) {
                            if (selection_ready) {
                                bridge?.send_event(
                                    id,
                                    "item_click",
                                    text = parent?.getItemAtPosition(position)?.toString() ?: "",
                                    number = position.toDouble()
                                )
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                    }
                }
    }

    /** 业务布局 + 底部日志浮层。 */
    }
    /** 布局解析失败的错误文案：XML 解析异常明确提示为语法错误。 */
    private fun describe_layout_error(e: Exception): String =
        if (e is org.xmlpull.v1.XmlPullParserException) {
            "布局 XML 语法错误：${e.message}"
        } else {
            "布局加载失败：${e.message ?: e.javaClass.simpleName}"
        }

    /** 解析失败时的替代页面：不崩溃，用户按返回键即可回编辑器修布局。 */
    private fun build_layout_error_page(message: String): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }
        container.addView(
            TextView(this).apply {
                text = "布局加载失败"
                textSize = 20f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#FF6B6B"))
            }
        )
        container.addView(
            TextView(this).apply {
                text = message
                textSize = 13f
                setTextColor(Color.parseColor("#E6E6E6"))
                setPadding(0, dp(12), 0, 0)
            }
        )
        container.addView(
            TextView(this).apply {
                text = "请点击返回回到上一页，修复 layout.xml 后重新运行。"
                textSize = 13f
                setTextColor(Color.parseColor("#9AA0A6"))
                setPadding(0, dp(12), 0, 0)
            }
        )
        scroll.setBackgroundColor(
            com.google.android.material.color.MaterialColors.getColor(scroll, com.google.android.material.R.attr.colorSurface)
        )
        scroll.addView(
            container,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        return scroll
    }

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

    override fun on_set_property(vid: String, name: String, value: JSONObject): String {
        val view = views_by_id[vid] ?: error("找不到控件 \"$vid\"")
        val raw = value.opt("value")
        when (name) {
            "visibility" -> view.visibility = when (raw?.toString()) {
                "visible" -> View.VISIBLE
                "invisible" -> View.INVISIBLE
                "gone" -> View.GONE
                else -> error("无效 visibility: $raw")
            }
            "enabled" -> view.isEnabled = raw as? Boolean ?: error("enabled 需要 bool")
            "selected" -> view.isSelected = raw as? Boolean ?: error("selected 需要 bool")
            "alpha" -> view.alpha = (raw as? Number)?.toFloat() ?: error("alpha 需要 number")
            "background" -> view.setBackgroundColor(Color.parseColor(raw?.toString()))
            "text" -> (view as? TextView)?.text = raw?.toString() ?: error("控件不是文本组件")
            "hint" -> (view as? TextView)?.hint = raw?.toString() ?: error("控件不是文本组件")
            "text_size" -> (view as? TextView)?.textSize = (raw as? Number)?.toFloat() ?: error("字号需要 number")
            "checked" -> (view as? CompoundButton)?.isChecked = raw as? Boolean ?: error("控件不是可选组件")
            "progress" -> (view as? ProgressBar)?.progress = (raw as? Number)?.toInt() ?: error("进度需要 number")
            "max" -> (view as? ProgressBar)?.max = (raw as? Number)?.toInt() ?: error("最大值需要 number")
            "rating" -> (view as? RatingBar)?.rating = (raw as? Number)?.toFloat() ?: error("评分需要 number")
            "num_stars" -> (view as? RatingBar)?.numStars = (raw as? Number)?.toInt() ?: error("星星数需要 number")
            "selection" -> when (view) {
                is Spinner -> view.setSelection((raw as? Number)?.toInt() ?: error("下标需要 number"))
                is NumberPicker -> view.value = (raw as? Number)?.toInt() ?: error("数值需要 number")
                else -> error("组件不支持 selection")
            }
            "scale_type" -> (view as? ImageView)?.scaleType = when (raw?.toString()) {
                "center" -> ImageView.ScaleType.CENTER
                "centerCrop" -> ImageView.ScaleType.CENTER_CROP
                "centerInside" -> ImageView.ScaleType.CENTER_INSIDE
                "fitCenter" -> ImageView.ScaleType.FIT_CENTER
                "fitEnd" -> ImageView.ScaleType.FIT_END
                "fitStart" -> ImageView.ScaleType.FIT_START
                "fitXY" -> ImageView.ScaleType.FIT_XY
                else -> error("无效 scaleType: $raw")
            }
            "orientation" -> when (view) {
                is LinearLayout -> view.orientation = if (raw?.toString() == "vertical") LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                else -> error("组件不支持 orientation")
            }
            "value" -> (view as? NumberPicker)?.value = (raw as? Number)?.toInt() ?: error("数值需要 number")
            "format" -> when (view) {
                is Chronometer -> view.format = raw?.toString()
                is TextClock -> view.format24Hour = raw?.toString()
                else -> error("组件不支持 format")
            }
            "video" -> (view as? VideoView)?.setVideoURI(Uri.parse(raw?.toString()))
            "url" -> (view as? WebView)?.loadUrl(raw?.toString() ?: error("URL 不能为空"))
            "date" -> set_widget_date(view, raw?.toString() ?: error("日期不能为空"))
            "time" -> {
                val parts = (raw?.toString() ?: error("时间不能为空")).split(":")
                val picker = view as? TimePicker ?: error("组件不是时间选择器")
                picker.hour = parts[0].toIntOrNull() ?: error("时间格式错误")
                picker.minute = parts.getOrNull(1)?.toIntOrNull() ?: error("时间格式错误")
            }
            else -> error("不支持的属性: $name")
        }
        return "ok"
    }

    override fun on_get_property(vid: String, name: String): String {
        val view = views_by_id[vid] ?: error("找不到控件 \"$vid\"")
        return when (name) {
            "text" -> (view as? TextView)?.text?.toString() ?: error("控件不是文本组件")
            "hint" -> (view as? TextView)?.hint?.toString() ?: error("控件不是文本组件")
            "checked" -> (view as? CompoundButton)?.isChecked?.toString() ?: error("组件不是可选组件")
            "progress" -> (view as? ProgressBar)?.progress?.toString() ?: error("组件不是进度组件")
            "max" -> (view as? ProgressBar)?.max?.toString() ?: error("组件不是进度组件")
            "rating" -> (view as? RatingBar)?.rating?.toString() ?: error("组件不是评分组件")
            "selection" -> when (view) {
                is Spinner -> view.selectedItemPosition.toString()
                is NumberPicker -> view.value.toString()
                else -> error("组件不支持 selection")
            }
            "enabled" -> view.isEnabled.toString()
            "selected" -> view.isSelected.toString()
            "alpha" -> view.alpha.toString()
            "visibility" -> when (view.visibility) {
                View.VISIBLE -> "visible"
                View.INVISIBLE -> "invisible"
                else -> "gone"
            }
            "value" -> (view as? NumberPicker)?.value?.toString() ?: error("组件不是数字选择器")
            "date" -> get_widget_date(view)
            "time" -> (view as? TimePicker)?.let { "%02d:%02d".format(it.hour, it.minute) } ?: error("组件不是时间选择器")
            else -> error("不支持的属性: $name")
        }
    }

    override fun on_invoke(vid: String, action: String, value: JSONObject): String {
        val view = views_by_id[vid] ?: error("找不到控件 \"$vid\"")
        when (action) {
            "toggle" -> (view as? CompoundButton)?.toggle() ?: error("组件不支持 toggle")
            "set_padding" -> {
                val values = value.optJSONArray("value") ?: error("padding 参数错误")
                view.setPadding(
                    values.optInt(0), values.optInt(1), values.optInt(2), values.optInt(3)
                )
            }
            "start" -> when (view) {
                is Chronometer -> view.start()
                is VideoView -> view.start()
                is ViewFlipper -> view.startFlipping()
                else -> error("组件不支持 start")
            }
            "stop" -> when (view) {
                is Chronometer -> view.stop()
                is VideoView -> view.stopPlayback()
                is ViewFlipper -> view.stopFlipping()
                else -> error("组件不支持 stop")
            }
            "pause" -> (view as? VideoView)?.pause() ?: error("组件不是视频")
            "reload" -> (view as? WebView)?.reload() ?: error("组件不是 WebView")
            "go_back" -> (view as? WebView)?.takeIf { it.canGoBack() }?.goBack() ?: error("网页不能后退")
            "go_forward" -> (view as? WebView)?.takeIf { it.canGoForward() }?.goForward() ?: error("网页不能前进")
            "show_next" -> (view as? ViewFlipper)?.showNext() ?: error("组件不是 ViewFlipper")
            "show_previous" -> (view as? ViewFlipper)?.showPrevious() ?: error("组件不是 ViewFlipper")
            "set_range" -> {
                val values = value.optJSONArray("value") ?: error("range 参数错误")
                val picker = view as? NumberPicker ?: error("组件不是数字选择器")
                picker.minValue = values.optInt(0)
                picker.maxValue = values.optInt(1)
            }
            "set_items" -> {
                val array = value.optJSONArray("value") ?: error("items 参数错误")
                val items = mutableListOf<String>()
                repeat(array.length()) { index -> items += array.optString(index) }
                when (view) {
                    is Spinner -> view.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    is AbsListView -> view.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
                    else -> error("组件不支持数据源")
                }
            }
            else -> error("不支持的操作: $action")
        }
        return "ok"
    }

    private fun set_widget_date(view: View, date: String) {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: error("日期格式错误")
        val calendar = java.util.Calendar.getInstance().apply { time = parsed }
        when (view) {
            is DatePicker -> view.updateDate(
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            is CalendarView -> view.date = parsed.time
            else -> error("组件不是日期组件")
        }
    }

    private fun get_widget_date(view: View): String {
        val time = when (view) {
            is DatePicker -> java.util.Calendar.getInstance().apply {
                set(view.year, view.month, view.dayOfMonth)
            }.time
            is CalendarView -> java.util.Date(view.date)
            else -> error("组件不是日期组件")
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(time)
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
            "float_can" -> if (floating_windows.can_show()) "true" else "false"
            "show_page" -> {
                val page_error = push_page(msg.optString("text"))
                if (page_error != null) {
                    append_log("错误: $page_error")
                    error(page_error)
                } else ""
            }
            "replace_page" -> {
                val page_error = replace_page(msg.optString("text"))
                if (page_error != null) {
                    append_log("错误: $page_error")
                    error(page_error)
                } else ""
            }
            "back_page" -> if (pop_page()) "" else error("已是最后一个页面")
            "float_request_permission" -> floating_windows.request_permission()
            "float_show" -> floating_windows.show(msg.optString("vid"), msg)
            "float_set_text" -> floating_windows.set_text(msg.optString("vid"), msg.optString("text"))
            "float_move" -> floating_windows.move(msg.optString("vid"), msg.optInt("x"), msg.optInt("y"))
            "float_close" -> floating_windows.close(msg.optString("vid"), msg.optBoolean("boolean"))
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
