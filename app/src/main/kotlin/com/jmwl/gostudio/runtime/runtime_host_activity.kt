package com.jmwl.gostudio.runtime

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.jmwl.gostudio.toolchain.sandbox_dns
import com.jmwl.gostudio.toolchain.toolchain_manager
import com.jmwl.gostudio.toolchain.toolchain_runtime_provider
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 「App 运行」宿主界面：
 * 1. 用平台 LayoutInflater 渲染项目里的 layout.xml（标准 Android 布局）
 * 2. 在 proot rootfs 内启动 go build 产出的业务二进制
 * 3. 通过 runtime_bridge 转发 UI 操作（Go -> 界面）与事件（界面 -> Go）
 *
 * 控件在布局里用 android:tag="控件id" 标记，Go 侧用同名 id 操作。
 */
class runtime_host_activity : Activity(), runtime_bridge.protocol_handler {

    private lateinit var project_dir: File
    private var views_by_id: Map<String, View> = emptyMap()
    private var bridge: runtime_bridge? = null
    private var log_view: TextView? = null
    private val started = AtomicBoolean(false)
    private val log_lines = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        append_log("已启动 ${project_dir.name}")
    }

    override fun onDestroy() {
        bridge?.stop()
        super.onDestroy()
    }

    /** 所有 Button（以及可点击控件）的点击事件转发给 Go。 */
    private fun wire_click_events() {
        views_by_id.forEach { (id, view) ->
            if (view is Button || view is ImageButton || view is CheckBox ||
                view is RadioButton || view is Switch || view.isClickable
            ) {
                view.setOnClickListener { bridge?.send_click(id) }
            }
        }
    }

    /** 业务布局 + 底部日志浮层。 */
    private fun build_content(root: View): View {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(Color.parseColor("#101014"))
        frame.addView(
            root,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val log_scroll = ScrollView(this)
        log_view = TextView(this).apply {
            setTextIsSelectable(true)
            setTextColor(Color.parseColor("#E6E6E6"))
            textSize = 11f
            setPadding(24, 20, 24, 20)
        }
        log_scroll.addView(
            log_view,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        log_scroll.setBackgroundColor(Color.parseColor("#CC1C1C22"))
        frame.addView(
            log_scroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.density * 96).toInt(),
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

    override fun on_get_text(vid: String): String {
        val view = views_by_id[vid] as? TextView
            ?: run { append_log("警告: get_text 找不到控件 \"$vid\""); return "" }
        return view.text?.toString() ?: ""
    }

    override fun on_quit() {
        finish()
    }

    companion object {
        private const val max_log_lines = 200
        const val EXTRA_PROJECT_DIR = "project_dir"
    }
}
