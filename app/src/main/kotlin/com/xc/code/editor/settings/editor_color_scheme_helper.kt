package com.xc.code.editor.settings

import android.content.Context
import com.xc.code.editor.theme.editor_theme_manager
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

internal fun apply_editor_colors(context: Context, editor: CodeEditor) {
    editor_theme_manager.set_current_theme(context)
    val scheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
    apply_textmate_extra_editor_colors(context, scheme)
    editor.colorScheme = scheme
}

private fun apply_textmate_extra_editor_colors(context: Context, scheme: EditorColorScheme) {
    val theme_colors = editor_theme_manager.load_color_object(context) ?: return

    fun theme_color(vararg keys: String): Int? {
        keys.forEach { key ->
            val color = parse_textmate_color(theme_colors.optString(key))
            if (color != null) return color
        }
        return null
    }

    fun set_color(type: Int, vararg keys: String) {
        theme_color(*keys)?.let { scheme.setColor(type, it) }
    }

    set_color(EditorColorScheme.SELECTION_HANDLE, "editor.selectionHandleBackground", "editorCursor.foreground")
    set_color(EditorColorScheme.LINE_DIVIDER, "editor.lineDivider")
    set_color(EditorColorScheme.SCROLL_BAR_THUMB, "scrollbarSlider.background")
    set_color(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, "scrollbarSlider.activeBackground")
    set_color(EditorColorScheme.MATCHED_TEXT_BACKGROUND, "editor.findMatchBackground", "editor.findMatchHighlightBackground")
    set_color(EditorColorScheme.MATCHED_TEXT_BORDER, "editor.findMatchBorder", "editor.findMatchHighlightBorder")
    set_color(EditorColorScheme.SIDE_BLOCK_LINE, "editorIndentGuide.activeBackground")
    set_color(EditorColorScheme.PROBLEM_ERROR, "editorError.foreground")
    set_color(EditorColorScheme.PROBLEM_WARNING, "editorWarning.foreground")
    set_color(EditorColorScheme.PROBLEM_TYPO, "editorInfo.foreground", "editorHint.foreground")
    set_color(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND, "tooltipBackground", "editor.background")
    set_color(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG, "tooltipBriefMessageColor", "editor.foreground")
    set_color(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG, "tooltipDetailedMessageColor", "editor.foreground")
    set_color(EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION, "tooltipActionColor", "editorCursor.foreground")
}

private fun parse_textmate_color(value: String): Int? {
    val trimmed = value.trim()
    if (!trimmed.startsWith("#")) return null
    val hex = trimmed.removePrefix("#")
    return runCatching {
        when (hex.length) {
            6 -> (0xFF000000L or hex.toLong(16)).toInt()
            8 -> {
                val rgba = hex.toLong(16)
                val red = (rgba shr 24) and 0xFF
                val green = (rgba shr 16) and 0xFF
                val blue = (rgba shr 8) and 0xFF
                val alpha = rgba and 0xFF
                ((alpha shl 24) or (red shl 16) or (green shl 8) or blue).toInt()
            }
            else -> null
        }
    }.getOrNull()
}
