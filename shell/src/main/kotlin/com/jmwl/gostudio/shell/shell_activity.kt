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
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.Chronometer
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.SeekBar
import android.widget.TextClock
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import android.widget.ViewFlipper
import android.webkit.WebView
import android.app.Activity
import android.app.AlertDialog
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/**
 * 打包壳：从 assets/app/ 读取 layout.xml 与 Go 二进制。
 * Go 逻辑是纯静态 ARM64 ELF，直接 ProcessBuilder 启动（无需 proot），
 * 与宿主通过 stdin/stdout 的 JSON 行协议通信。
 */
class shell_activity : Activity() {

    private lateinit var binary_file: File
    private val views_by_id = mutableMapOf<String, View>()
    private var bridge: standalone_bridge? = null
    private val started = AtomicBoolean(false)
    private var layout_error: String? = null
    /** 页面栈：app.ShowPage 压入新布局，返回键出栈回到上一页。 */
    private val page_stack = ArrayDeque<runtime_layout_loader.Result>()
    private lateinit var page_container: FrameLayout
    private lateinit var floating_windows: floating_window_manager
    /** Go 侧 PlayAudio 的音频播放器；单实例，新的 PlayAudio 替换上一段。 */
    private var audio_player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 复制 assets 到私有目录（每次启动都覆盖：覆盖安装后 filesDir
        //    会残留上一版的 app.bin/layout.xml，不覆盖就永远跑旧程序）
        val layout_file = File(filesDir, "layout.xml")
        binary_file = File(filesDir, "app.bin")
        assets.open("app/layout.xml").use { input ->
            layout_file.outputStream().use { input.copyTo(it) }
        }
        // app.bin 走「临时文件 + rename」：转屏等配置变化触发 Activity 重建时，
        // 旧 Go 进程可能仍在执行 app.bin（进程在其 onDestroy 后 150ms 才被杀），
        // Linux 禁止写打开运行中的可执行文件（ETXTBSY），直接覆写会抛异常闪退；
        // rename 替换正在执行的文件是允许的，旧进程继续用旧 inode。
        val binary_tmp = File(filesDir, "app.bin.tmp")
        assets.open("app/app.bin").use { input ->
            binary_tmp.outputStream().use { input.copyTo(it) }
        }
        binary_tmp.setExecutable(true, false)
        if (!binary_tmp.renameTo(binary_file)) {
            binary_file.delete()
            binary_tmp.renameTo(binary_file)
        }
        binary_file.setExecutable(true, false)
        copy_asset_dir("app/images", File(filesDir, "images"), overwrite = true)
        copy_asset_dir("app/audio", File(filesDir, "audio"), overwrite = true)
        // 页面布局：项目根目录的其他 xml 一并解包，供 app.ShowPage 使用
        assets.list("app")?.filter { it.endsWith(".xml") }?.forEach { name ->
            assets.open("app/$name").use { input ->
                File(filesDir, name).outputStream().use { input.copyTo(it) }
            }
        }
        binary_file.setExecutable(true, false)

        // 2. 渲染布局
        floating_windows = floating_window_manager(
            activity = this,
            base_dir = filesDir,
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
        } catch (e: Exception) {
            // 布局 XML 有语法错误等问题时不崩溃：页内展示错误，返回键照常退出
            layout_error = describe_layout_error(e)
            setContentView(build_layout_error_page(layout_error!!))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!pop_page()) super.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        if (!started.compareAndSet(false, true)) return
        if (layout_error != null) return
        val b = standalone_bridge(
            on_ui_log = { line -> append_log(line) },
            on_exit = { }
        )
        bridge = b
        b.start(binary_file.absolutePath, handler = this)
        b.send_lifecycle("create")
        b.send_lifecycle("start")
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
        stop_audio_player()
        bridge?.send_lifecycle("destroy")
        Handler(Looper.getMainLooper()).postDelayed({ bridge?.stop() }, 150)
        super.onDestroy()
    }

    private fun copy_asset_dir(asset_path: String, target_dir: File, overwrite: Boolean = false) {
        if (!target_dir.isDirectory) target_dir.mkdirs()
        assets.list(asset_path)?.forEach { name ->
            val source = "$asset_path/$name"
            val target = File(target_dir, name)
            if (target.isFile && !overwrite) return@forEach
            assets.open(source).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
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

    /** 加载并切入包内另一个布局页；返回 null 表示成功，否则为错误信息。 */
    private fun push_page(layout_name: String): String? {
        val file = File(filesDir, layout_name)
        if (!file.isFile) return "页面布局不存在: $layout_name"
        return try {
            show_page(runtime_layout_loader(this).load(file, filesDir))
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
        val file = File(filesDir, layout_name)
        if (!file.isFile) return "页面布局不存在: $layout_name"
        return try {
            val page = runtime_layout_loader(this).load(file, filesDir)
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
                is runtime_video_view -> view.on_event = { event, number ->
                    bridge?.send_event(id, event, number = number)
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
    }

    /** 布局解析失败的错误文案：XML 解析异常明确提示为语法错误。 */
    private fun describe_layout_error(e: Exception): String =
        if (e is org.xmlpull.v1.XmlPullParserException) {
            "布局 XML 语法错误：${e.message}"
        } else {
            "布局加载失败：${e.message ?: e.javaClass.simpleName}"
        }

    /** 解析失败时的替代页面：不崩溃，用户按返回键即可退出。 */
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
                text = "请重新打包前修复布局 XML 语法错误。"
                textSize = 13f
                setTextColor(Color.parseColor("#9AA0A6"))
                setPadding(0, dp(12), 0, 0)
            }
        )
        scroll.setBackgroundColor(Color.parseColor("#1B1C1F"))
        scroll.addView(
            container,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        return scroll
    }

    private fun build_content(root: View): View {
        val frame = FrameLayout(this)
        // 平台主题背景色（无 material 依赖）：跟随系统日夜间 colorBackground
        val typed = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typed, true)
        if (typed.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT &&
            typed.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT
        ) {
            frame.setBackgroundColor(typed.data)
        }
        frame.addView(root, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        return frame
    }

    /**
     * 独立打包的成品 App 不显示调试日志面板（运行日志只在 GoStudio 运行模式里展示），
     * 这里仅落到 logcat 供 adb 排查。
     */
    private fun append_log(line: String) {
        android.util.Log.d("GoStudioShell", line)
    }

    // ---- Go -> 界面 ----

    fun on_set_text(vid: String, text: String) {
        val target = views_by_id[vid] as? TextView
        if (target != null) target.text = text else append_log("警告: set_text 找不到控件 \"$vid\"")
    }

    fun on_set_image(vid: String, url: String) {
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
                connection.setRequestProperty("User-Agent", "GoStudio App")
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

    fun on_get_text(vid: String): String {
        val view = views_by_id[vid] as? TextView
            ?: run { append_log("警告: get_text 找不到控件 \"$vid\""); return "" }
        return view.text?.toString() ?: ""
    }

    fun on_set_property(vid: String, name: String, value: JSONObject): String {
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
            "video" -> (view as? runtime_video_view)?.set_video_uri(Uri.parse(raw?.toString()))
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

    fun on_get_property(vid: String, name: String): String {
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
            "position" -> (view as? runtime_video_view)?.current_position()?.toString() ?: error("组件不是视频")
            "duration" -> (view as? runtime_video_view)?.duration()?.toString() ?: error("组件不是视频")
            "is_playing" -> (view as? runtime_video_view)?.is_playing()?.toString() ?: error("组件不是视频")
            "date" -> get_widget_date(view)
            "time" -> (view as? TimePicker)?.let { "%02d:%02d".format(it.hour, it.minute) } ?: error("组件不是时间选择器")
            else -> error("不支持的属性: $name")
        }
    }

    fun on_invoke(vid: String, action: String, value: JSONObject): String {
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
                is runtime_video_view -> view.start()
                is ViewFlipper -> view.startFlipping()
                else -> error("组件不支持 start")
            }
            "stop" -> when (view) {
                is Chronometer -> view.stop()
                is runtime_video_view -> view.stop_playback()
                is ViewFlipper -> view.stopFlipping()
                else -> error("组件不支持 stop")
            }
            "pause" -> (view as? runtime_video_view)?.pause() ?: error("组件不是视频")
            "seek" -> {
                val video = view as? runtime_video_view ?: error("组件不是视频")
                video.seek_to((value.opt("value") as? Number)?.toInt() ?: error("seek 需要 number 毫秒"))
            }
            "show_controls" -> (view as? runtime_video_view)?.show_controls() ?: error("组件不是视频")
            "hide_controls" -> (view as? runtime_video_view)?.hide_controls() ?: error("组件不是视频")
            "set_controls" -> {
                val video = view as? runtime_video_view ?: error("组件不是视频")
                video.set_controls_enabled(value.opt("value") as? Boolean ?: error("set_controls 需要 bool"))
            }
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

    /** 停止并释放当前音频播放器；stop 过的 MediaPlayer 不能复用，下次播放重建。 */
    private fun stop_audio_player() {
        audio_player?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        audio_player = null
    }

    /** 播放音频：http/https 网络地址直接播，其余按项目相对路径（与 images/ 资源同一基准）解析。 */
    private fun play_audio(source: String, loop: Boolean) {
        stop_audio_player()
        val player = MediaPlayer()
        try {
            if (source.startsWith("http://") || source.startsWith("https://")) {
                player.setDataSource(source)
            } else {
                val raw_file = File(source.removePrefix("./"))
                val audio_file = if (raw_file.isAbsolute) raw_file else File(filesDir, raw_file.path)
                check(audio_file.isFile) { "音频文件不存在: ${audio_file.absolutePath}" }
                player.setDataSource(audio_file.absolutePath)
            }
            player.isLooping = loop
            player.setOnErrorListener { failed, _, _ ->
                append_log("音频播放出错，已停止")
                runCatching { failed.release() }
                if (audio_player === failed) audio_player = null
                true
            }
            // prepareAsync 不阻塞主线程（system 调用在主线程有 5s 超时），就绪后在回调里开始播放
            player.setOnPreparedListener { it.start() }
            player.prepareAsync()
            audio_player = player
        } catch (e: Exception) {
            runCatching { player.release() }
            throw e
        }
    }

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
            "audio_play" -> {
                play_audio(msg.optString("text"), msg.optBoolean("boolean"))
                ""
            }
            "audio_pause" -> {
                val player = audio_player ?: error("没有正在播放的音频")
                runCatching { player.pause() }.getOrElse { error("音频尚未准备好") }
                ""
            }
            "audio_resume" -> {
                val player = audio_player ?: error("没有正在播放的音频")
                runCatching { player.start() }.getOrElse { error("音频尚未准备好") }
                ""
            }
            "audio_stop" -> {
                stop_audio_player()
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
        val proc = ProcessBuilder(binary_path).apply {
            val dns = system_dns_servers(handler)
            if (dns.isNotEmpty()) environment()["GOSTUDIO_DNS"] = dns.joinToString(",")
            // proot 里以 GOOS=linux 编译的纯 Go 二进制只会找 Linux 证书路径
            // （Android 上不存在），HTTPS 会报 x509 unknown authority；
            // 显式指向 Android 系统 CA 目录（Android 14+ 在 conscrypt apex，更早版本在 /system）
            certificate_dir()?.let { environment()["SSL_CERT_DIR"] = it }
        }.start()
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

    /**
     * Android 没有 /etc/resolv.conf，Go 裸进程解析不了域名。
     * 取系统 DNS 服务器（活动网络优先，IPv4 优先）经 GOSTUDIO_DNS 传给 Go 进程，
     * appsdk 的 netdns.go 会用它接管 net.DefaultResolver。
     */
    /** Android 系统 CA 证书目录：Go 的 x509 经 SSL_CERT_DIR 读取。 */
    private fun certificate_dir(): String? =
        listOf("/apex/com.android.conscrypt/cacerts", "/system/etc/security/cacerts")
            .firstOrNull { java.io.File(it).isDirectory }

    private fun system_dns_servers(context: android.content.Context): List<String> {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return emptyList()
        val active = runCatching { cm.activeNetwork }.getOrNull()
        val servers = runCatching { cm.allNetworks }.getOrDefault(emptyArray())
            .sortedByDescending { it == active }
            .flatMap { network ->
                runCatching { cm.getLinkProperties(network)?.dnsServers ?: emptyList() }
                    .getOrDefault(emptyList())
            }
            .mapNotNull { it.hostAddress }
            .distinct()
        val ipv4 = servers.filter { !it.contains(':') }
        return (if (ipv4.isNotEmpty()) ipv4 else servers).take(3)
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
            "set_image" -> {
                val vid = msg.optString("vid")
                val url = msg.optString("text")
                main_handler.post { handler.on_set_image(vid, url) }
                send_ack(msg.optLong("seq"))
            }
            "set_property" -> {
                val vid = msg.optString("vid")
                val name = msg.optString("action")
                val task = FutureTask { handler.on_set_property(vid, name, msg) }
                main_handler.post(task)
                val result = runCatching { task.get(5, TimeUnit.SECONDS) }.getOrNull()
                send_ack(msg.optLong("seq"), ok = result != null, text = result ?: "error")
            }
            "get_property" -> {
                val vid = msg.optString("vid")
                val name = msg.optString("action")
                val task = FutureTask { handler.on_get_property(vid, name) }
                main_handler.post(task)
                val result = runCatching { task.get(5, TimeUnit.SECONDS) }.getOrNull()
                send_ack(msg.optLong("seq"), ok = result != null, text = result ?: "error")
            }
            "invoke" -> {
                val vid = msg.optString("vid")
                val action = msg.optString("action")
                val task = FutureTask { handler.on_invoke(vid, action, msg) }
                main_handler.post(task)
                val result = runCatching { task.get(5, TimeUnit.SECONDS) }.getOrNull()
                send_ack(msg.optLong("seq"), ok = result != null, text = result ?: "error")
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
