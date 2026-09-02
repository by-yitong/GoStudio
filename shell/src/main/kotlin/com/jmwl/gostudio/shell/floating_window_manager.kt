package com.jmwl.gostudio.shell

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.File

/**
 * 系统悬浮窗管理器。
 *
 * 支持两种内容：
 * 1. 默认文本卡片；
 * 2. 项目 floats 目录中 XML 声明的原生布局，布局里的控件仍可通过 appsdk 控件句柄操作。
 */
class floating_window_manager(
    private val activity: Activity,
    private val base_dir: File,
    private val register_views: (Map<String, View>) -> Unit,
    private val wire_events: (String, View) -> Unit,
    private val on_event: (id: String, event: String, value: Boolean) -> Unit
) {
    private class window_entry(
        val view: View,
        val params: WindowManager.LayoutParams,
        val text_view: TextView?
    )

    private val windows = mutableMapOf<String, window_entry>()
    private val window_manager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var waiting_permission_result = false

    fun can_show(): Boolean = Settings.canDrawOverlays(activity)

    fun request_permission(): String {
        if (can_show()) {
            on_event("", "float_permission_change", true)
            return "ok"
        }
        waiting_permission_result = true
        activity.startActivity(
            android.content.Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.packageName)
            )
        )
        return "ok"
    }

    fun notify_permission_changed() {
        if (!waiting_permission_result) return
        waiting_permission_result = false
        on_event("", "float_permission_change", can_show())
    }

    fun show(id: String, message: JSONObject): String {
        check(id.isNotBlank()) { "悬浮窗 ID 不能为空" }
        check(can_show()) { "未授予悬浮窗权限" }
        check(id !in windows) { "悬浮窗 \"$id\" 已存在" }

        val config = message.optJSONObject("value") ?: JSONObject()
        val density = activity.resources.displayMetrics.density
        var default_body: TextView? = null
        var loaded_root: View? = null
        if (config.optString("layout").isNotBlank()) {
            val layout_file = safe_layout_file(config.optString("layout"))
            val loaded = runtime_layout_loader(activity).load(layout_file, base_dir)
            loaded_root = loaded.root
            register_views(loaded.views)
            loaded.views.forEach { (view_id, view) -> wire_events(view_id, view) }
        } else {
            default_body = TextView(activity).apply {
                setTextColor(Color.parseColor("#E6E6E6"))
                textSize = 14f
                setPadding((18 * density).toInt(), (12 * density).toInt(), (18 * density).toInt(), (14 * density).toInt())
            }
        }

        val show_close = config.optBoolean("show_close", true)
        val title = config.optString("title").ifBlank { id }
        val draggable = config.optBoolean("draggable", true)
        val focusable = config.optBoolean("focusable", false)
        val pending_params = java.util.concurrent.atomic.AtomicReference<WindowManager.LayoutParams>()
        var body_for_entry: TextView? = null
        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F21B1C1F"))
                cornerRadius = 18 * density
                setStroke((1 * density).toInt(), Color.parseColor("#335CCFE6"))
            }
            clipToOutline = true
        }

        if (show_close || title.isNotBlank()) {
            val header = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#332C313A"))
                setPadding((14 * density).toInt(), (7 * density).toInt(), (6 * density).toInt(), (7 * density).toInt())
            }
            val title_view = TextView(activity).apply {
                text = title
                setTextColor(Color.parseColor("#5CCFE6"))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            header.addView(title_view)
            if (show_close) {
                header.addView(
                    TextView(activity).apply {
                        text = "×"
                        setTextColor(Color.parseColor("#B8C0CC"))
                        textSize = 18f
                        setPadding((12 * density).toInt(), 0, (12 * density).toInt(), (2 * density).toInt())
                        setOnClickListener { close(id, notify_go = true) }
                    }
                )
            }
            if (draggable) attach_drag(header, panel) { pending_params.get() }
            panel.addView(
                header,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }

        val requested_width = config.optInt("width", 0)
        val requested_height = config.optInt("height", 0)
        val content_params = if (requested_height > 0) {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        } else {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val root = loaded_root
        if (root == null) {
            val body = default_body ?: error("悬浮窗内容初始化失败")
            body_for_entry = body
            body.text = config.optString("text")
            body.setOnClickListener { on_event(id, "float_click", false) }
            panel.addView(body, content_params)
        } else {
            panel.addView(root, content_params)
        }

        val default_width = if (loaded_root != null) (280 * density).toInt() else ViewGroup.LayoutParams.WRAP_CONTENT
        val params = WindowManager.LayoutParams(
            if (requested_width > 0) (requested_width * density).toInt() else default_width,
            if (requested_height > 0) (requested_height * density).toInt() else ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or if (focusable) {
                0
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            },
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (config.optInt("x", 24) * density).toInt()
            y = (config.optInt("y", 64) * density).toInt()
            if (focusable) {
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
        }

        pending_params.set(params)
        window_manager.addView(panel, params)
        windows[id] = window_entry(panel, params, body_for_entry)
        return "ok"
    }

    fun set_text(id: String, text: String): String {
        val entry = windows[id] ?: error("悬浮窗 \"$id\" 不存在")
        val target = entry.text_view ?: error("悬浮窗 \"$id\" 不是默认文本窗口")
        target.text = text
        return "ok"
    }

    fun move(id: String, x: Int, y: Int): String {
        val entry = windows[id] ?: error("悬浮窗 \"$id\" 不存在")
        val density = activity.resources.displayMetrics.density
        entry.params.x = (x * density).toInt()
        entry.params.y = (y * density).toInt()
        window_manager.updateViewLayout(entry.view, entry.params)
        return "ok"
    }

    fun close(id: String, notify_go: Boolean): String {
        val entry = windows.remove(id) ?: error("悬浮窗 \"$id\" 不存在")
        runCatching { window_manager.removeView(entry.view) }
        if (notify_go) on_event(id, "float_close", false)
        return "ok"
    }

    fun close_all() {
        windows.keys.toList().forEach { id ->
            windows.remove(id)?.let { entry -> runCatching { window_manager.removeView(entry.view) } }
        }
    }

    private fun safe_layout_file(relative_path: String): File {
        val base = base_dir.absoluteFile.canonicalFile
        val file = File(base, relative_path).canonicalFile
        check(file.isFile) { "悬浮窗布局不存在: $relative_path" }
        check(file.path == base.path || file.path.startsWith(base.path + File.separator)) { "悬浮窗布局不能超出项目目录" }
        return file
    }

    private fun attach_drag(view: View, moved_view: View, params_provider: () -> WindowManager.LayoutParams?) {
        var down_raw_x = 0f
        var down_raw_y = 0f
        var start_x = 0
        var start_y = 0
        view.setOnTouchListener { target, event ->
            val params = params_provider()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    down_raw_x = event.rawX
                    down_raw_y = event.rawY
                    params?.let {
                        start_x = it.x
                        start_y = it.y
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.let {
                        it.x = start_x + (event.rawX - down_raw_x).toInt()
                        it.y = start_y + (event.rawY - down_raw_y).toInt()
                        window_manager.updateViewLayout(moved_view, params)
                    }
                    true
                }
                else -> false
            }
        }
    }
}
