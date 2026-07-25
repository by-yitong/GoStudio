package io.github.rosemoe.sora.lsp.editor.codeaction

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

interface CodeActionLayout {
    /** Bind this layout to its hosting window. */
    fun attach(window: CodeActionWindow)
    /** Create the root view that will be rendered in the popup window. */
    fun createView(inflater: LayoutInflater): View
    /** Apply colors and typeface from the editor. */
    fun applyColorScheme(colorScheme: EditorColorScheme, typeface: Typeface)
    /**
     * Render the provided list of code actions.
     * @param diagnosticText 当前位置的诊断详情（合并面板顶部显示），空表示无诊断
     */
    fun renderActions(actions: List<CodeActionItem>, diagnosticText: String = "")
    /** Notify layout that editor text size changed. */
    fun onTextSizeChanged(oldSize: Float, newSize: Float)
}
