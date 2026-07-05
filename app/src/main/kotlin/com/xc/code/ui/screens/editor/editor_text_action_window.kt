package com.xc.code.ui.screens.editor

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import com.xc.code.editor.core.R
import io.github.rosemoe.sora.R as SoraR
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class editor_text_action_window(
    private val editor: CodeEditor,
    private val current_comment_action: () -> Boolean?,
    private val on_toggle_comment: () -> Unit
) : EditorTextActionWindow(editor) {

    private var comment_button: ImageButton? = null

    init {
        val comment = create_comment_button()
        comment_button = comment
        val scroll_view = getView().findViewById<HorizontalScrollView>(SoraR.id.panel_hv)
        val action_container = scroll_view?.getChildAt(0) as? LinearLayout
        action_container?.addView(comment)
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

    private fun create_action_button(icon_res: Int, description: String, on_click: () -> Unit): ImageButton {
        return ImageButton(editor.context).apply {
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(icon_res)
            contentDescription = description
            val selectable_background = TypedValue()
            editor.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, selectable_background, true)
            setBackgroundResource(selectable_background.resourceId)
            layoutParams = LinearLayout.LayoutParams(45.dp_to_px(), ViewGroup.LayoutParams.MATCH_PARENT)
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
    }

    private fun Int.dp_to_px(): Int {
        return (this * editor.dpUnit).toInt()
    }
}
