package com.jmwl.gostudio.ui.screens.editor

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import com.jmwl.gostudio.editor.core.R
import com.jmwl.gostudio.R as AppR
import io.github.rosemoe.sora.R as SoraR
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class editor_text_action_window(
    private val editor: CodeEditor,
    private val current_comment_action: () -> Boolean?,
    private val on_toggle_comment: () -> Unit,
    private val on_ai_action: ((String, String) -> Unit)? = null,
    private val can_show_details: () -> Boolean = { false },
    private val on_show_details: () -> Unit = {},
    private val can_show_goto: () -> Boolean = { false },
    private val on_goto_definition: () -> Unit = {}
) : EditorTextActionWindow(editor) {

    private var comment_button: ImageButton? = null
    private var ai_button: ImageButton? = null
    private var details_button: ImageButton? = null
    private var goto_button: ImageButton? = null

    init {
        val comment = create_comment_button()
        comment_button = comment
        val ai = create_ai_button()
        ai_button = ai
        val details = create_details_button()
        details_button = details
        val goto = create_goto_button()
        goto_button = goto
        val scroll_view = getView().findViewById<HorizontalScrollView>(SoraR.id.panel_hv)
        val action_container = scroll_view?.getChildAt(0) as? LinearLayout
        action_container?.addView(comment)
        action_container?.addView(ai)
        action_container?.addView(details)
        action_container?.addView(goto)
        apply_action_button_color()
    }

    override fun displayWindow() {
        val should_uncomment = current_comment_action()
        comment_button?.visibility = if (editor.cursor.isSelected && editor.isEditable && should_uncomment != null) {
            update_comment_button_mode(should_uncomment)
            View.VISIBLE
        } else {
            View.GONE
        }
        // AI 按钮：仅在选中文本且配置了回调时显示
        ai_button?.visibility = if (editor.cursor.isSelected && on_ai_action != null) {
            View.VISIBLE
        } else {
            View.GONE
        }
        // 详情按钮：选中文本且 hover 详情可用（.go + gopls 已连接 + 设置开启）时显示
        details_button?.visibility = if (editor.cursor.isSelected && can_show_details()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        // 跳转定义按钮：选中文本且 .go 文件 gopls 已连接时显示（替代原底部胶囊与长按菜单入口）
        goto_button?.visibility = if (editor.cursor.isSelected && can_show_goto()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        super.displayWindow()
    }

    override fun applyColorScheme() {
        super.applyColorScheme()
        if (comment_button != null) {
            apply_action_button_color()
        }
    }

    private fun create_comment_button(): ImageButton {
        return create_action_button(R.drawable.ic_editor_comment, "注释") {
            on_toggle_comment()
            dismiss()
        }
    }

    /** AI 动作按钮：把选中文本发给 AI 助手 */
    private fun create_ai_button(): ImageButton {
        return create_action_button(R.drawable.ic_editor_ai, "问 AI") {
            val cursor = editor.cursor
            if (cursor.isSelected) {
                val text = editor.text
                val start = cursor.left().index
                val end = cursor.right().index
                if (end > start) {
                    val selection = text.subSequence(start, end).toString()
                    if (selection.isNotEmpty() && on_ai_action != null) {
                        on_ai_action("explain", selection)
                    }
                }
            }
            dismiss()
        }
    }

    /** 详情动作按钮：请求当前选区/光标位置的 hover 详情（底部弹窗展示） */
    private fun create_details_button(): ImageButton {
        return create_action_button(AppR.drawable.ic_editor_details, "详情") {
            on_show_details()
            dismiss()
        }
    }

    /** 跳转定义动作按钮：跳到当前选区/光标所在标识符的定义 */
    private fun create_goto_button(): ImageButton {
        return create_action_button(AppR.drawable.ic_editor_goto, "跳转定义") {
            on_goto_definition()
            dismiss()
        }
    }

    private fun create_action_button(icon_res: Int, description: String, on_click: () -> Unit): ImageButton {
        return ImageButton(editor.context).apply {
            setImageResource(icon_res)
            contentDescription = description
            val selectable_background = TypedValue()
            editor.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, selectable_background, true)
            setBackgroundResource(selectable_background.resourceId)
            // 与基类面板按钮同规格：45x45dp 格子（text_compose_panel.xml），图标由 drawable 自身 20dp 控制
            layoutParams = LinearLayout.LayoutParams(45.dp_to_px(), 45.dp_to_px())
            setOnClickListener { on_click() }
        }
    }

    private fun update_comment_button_mode(should_uncomment: Boolean) {
        comment_button?.apply {
            setImageResource(if (should_uncomment) R.drawable.ic_editor_uncomment else R.drawable.ic_editor_comment)
            contentDescription = if (should_uncomment) "取消注释" else "注释"
            apply_action_button_color()
        }
    }

    private fun apply_action_button_color() {
        val color = editor.colorScheme.getColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR)
        comment_button?.setColorFilter(color)
        ai_button?.setColorFilter(color)
        details_button?.setColorFilter(color)
        goto_button?.setColorFilter(color)
    }

    private fun Int.dp_to_px(): Int {
        return (this * editor.dpUnit).toInt()
    }
}
