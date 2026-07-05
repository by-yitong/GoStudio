package com.xc.code.activity

import android.content.Context
import android.view.ViewGroup
import com.xc.code.editor.model.editor_settings_state
import com.xc.code.editor.session.editor_open_tab
import com.xc.code.editor.settings.apply_editor_behavior_settings
import com.xc.code.editor.settings.apply_editor_colors
import com.xc.code.ui.screens.editor.editor_text_action_window
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

internal class editor_tab_lifecycle(
    private val context: Context,
    private val settings: () -> editor_settings_state,
    private val create_textmate_language: (String) -> TextMateLanguage,
    private val with_applying_content: (() -> Unit) -> Unit,
    private val on_content_changed: () -> Unit,
    private val on_selection_changed: (CodeEditor) -> Unit,
    private val current_comment_action: () -> Boolean?,
    private val on_toggle_comment: () -> Unit,
    private val initial_styles_timeout_ms: Long
) {
    fun create_editor(file_path: String?): CodeEditor {
        return CodeEditor(context).apply {
            registerInlayHintRenderers(
                TextInlayHintRenderer.DefaultInstance,
                ColorInlayHintRenderer.DefaultInstance
            )
            setUndoEnabled(true)
            setBlockLineWidth(1.2f)
            setEditorLanguage(create_textmate_language(file_path ?: "prewarm.cpp"))
            apply_editor_colors(context, this)
            apply_editor_behavior_settings(
                context = context,
                editor = this,
                settings = settings(),
                file_path = file_path,
                current_language = editorLanguage as? TextMateLanguage
            )
            replaceComponent(
                EditorTextActionWindow::class.java,
                editor_text_action_window(
                    editor = this,
                    current_comment_action = current_comment_action,
                    on_toggle_comment = on_toggle_comment
                )
            )
            val code_editor = this
            subscribeEvent(ContentChangeEvent::class.java, EventReceiver { _, _ ->
                on_content_changed()
            })
            subscribeEvent(SelectionChangeEvent::class.java, EventReceiver { _, _ ->
                on_selection_changed(code_editor)
            })
        }
    }

    suspend fun prepare_for_display(tab: editor_open_tab): CodeEditor {
        return prepare(tab).also { tab_editor ->
            wait_for_initial_styles(tab_editor)
        }
    }

    fun prepare(tab: editor_open_tab): CodeEditor {
        return create_editor(tab.file_path).also { tab_editor ->
            tab.editor = tab_editor
            set_content(tab_editor, Content(tab.content))
        }
    }

    fun release(tab: editor_open_tab) {
        tab.editor?.let { tab_editor ->
            runCatching { (tab_editor.parent as? ViewGroup)?.removeView(tab_editor) }
            set_content(tab_editor, "")
            runCatching { tab_editor.release() }
        }
        tab.editor = null
    }

    fun set_content(target: CodeEditor, content: CharSequence) {
        with_applying_content {
            target.setText(content)
        }
    }

    private suspend fun wait_for_initial_styles(target: CodeEditor) {
        withTimeoutOrNull(initial_styles_timeout_ms) {
            while (target.getStyles() == null) {
                delay(16)
            }
        }
    }
}
