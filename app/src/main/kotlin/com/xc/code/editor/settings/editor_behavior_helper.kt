package com.xc.code.editor.settings

import com.xc.code.editor.model.editor_settings_state
import com.xc.code.editor.core.is_c_family_file
import com.xc.code.editor.core.c_cpp_completion_keywords

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import io.github.rosemoe.sora.widget.getComponent

internal fun apply_editor_behavior_settings(
    context: Context,
    editor: CodeEditor,
    settings: editor_settings_state,
    file_path: String?,
    current_language: TextMateLanguage?
) {
    val font_size = settings.font_size.coerceIn(10f, 24f)
    val tab_size = settings.tab_size.coerceIn(2, 8)
    var non_printable_flags = 0
    if (settings.whitespace_symbols) {
        non_printable_flags = non_printable_flags or
            CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or
            CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING
    }
    if (settings.line_separator) {
        non_printable_flags = non_printable_flags or CodeEditor.FLAG_DRAW_LINE_SEPARATOR
    }

    editor.setTypefaceText(load_editor_typeface(context, settings))
    editor.setLigatureEnabled(settings.font_ligatures)
    editor.setTextSize(font_size)
    editor.setLineInfoTextSize((font_size - 2f).coerceAtLeast(10f))
    editor.setCursorBlinkPeriod(if (settings.cursor_blink) 420 else 0)
    editor.setScalable(settings.pinch_zoom)
    editor.setScrollBarEnabled(false)
    editor.setVerticalScrollBarEnabled(false)
    editor.setHorizontalScrollBarEnabled(false)
    editor.setWordwrap(settings.word_wrap, settings.word_wrap)
    editor.setLineNumberEnabled(settings.line_numbers)
    editor.setHighlightBracketPair(settings.bracket_pair_highlight)
    editor.props.boldMatchingDelimiters = false
    editor.setHighlightCurrentLine(settings.current_line_highlight)
    editor.setHighlightCurrentBlock(settings.block_lines)
    editor.setBlockLineEnabled(settings.block_lines)
    editor.props.stickyScroll = settings.sticky_scroll
    editor.props.autoIndent = settings.auto_indent
    editor.setNonPrintablePaintingFlags(non_printable_flags)
    editor.setTabWidth(tab_size)
    editor.props.symbolPairAutoCompletion = true
    apply_editor_symbol_pairs(editor, file_path)
    editor.getComponent<EditorAutoCompletion>().setEnabled(settings.auto_completion)
    current_language?.let { language ->
        apply_textmate_language_settings(language, settings, file_path)
    }
    editor.rerunAnalysis()
    editor.invalidate()
}

private fun apply_editor_symbol_pairs(editor: CodeEditor, file_path: String?) {
    editor.props.overrideSymbolPairs.removeAllPairs()
    if (!is_c_family_file(file_path)) return

    editor.props.overrideSymbolPairs.putPair(
        "<",
        SymbolPairMatch.SymbolPair("<", ">", object : SymbolPairMatch.SymbolPair.SymbolPairEx {
            override fun shouldReplace(editor: CodeEditor, currentLine: ContentLine, leftColumn: Int): Boolean {
                return should_complete_angle_pair(editor, currentLine.toString(), leftColumn)
            }
        })
    )
}

private fun is_textmate_non_code_token(editor: CodeEditor, line: Int, column: Int): Boolean {
    val spans = runCatching { editor.getSpansForLine(line) }.getOrNull() ?: return false
    if (spans.isEmpty()) return false

    var current_extra: Any? = null
    for (span in spans) {
        if (span.column > column) break
        current_extra = span.extra
    }

    val token_type = current_extra as? Int ?: return false
    return token_type == StandardTokenType.Comment ||
        token_type == StandardTokenType.String ||
        token_type == StandardTokenType.RegEx
}

private fun should_complete_angle_pair(editor: CodeEditor, line: String, left_column: Int): Boolean {
    val column = left_column.coerceIn(0, line.length)
    if (is_textmate_non_code_token(editor, editor.cursor.leftLine, (column - 1).coerceAtLeast(0))) return false

    val before = line.take(column)
    val trimmed_before = before.trimEnd()
    val after = line.drop(column).trimStart()
    if (after.startsWith(">")) return false
    if (trimmed_before.isEmpty()) return false
    if (trimmed_before.startsWith("#include")) return true
    if (trimmed_before.endsWith("template")) return true
    if (trimmed_before.endsWith("operator")) return false

    val previous = trimmed_before.last()
    if (!previous.isLetterOrDigit() && previous != '_' && previous != ':') return false

    val token_start = trimmed_before.indexOfLast { char ->
        !(char.isLetterOrDigit() || char == '_' || char == ':')
    } + 1
    val token = trimmed_before.substring(token_start)
    if (token.isBlank()) return false
    if (token.all { it.isDigit() }) return false
    if (before.endsWith(" ")) return false

    return token.contains("::") || token.first().isUpperCase() || token in angle_pair_context_names
}

private val angle_pair_context_names = setOf(
    "vector",
    "array",
    "map",
    "unordered_map",
    "set",
    "unordered_set",
    "list",
    "deque",
    "queue",
    "stack",
    "pair",
    "tuple",
    "optional",
    "variant",
    "function",
    "unique_ptr",
    "shared_ptr",
    "weak_ptr",
    "span",
    "basic_string"
)

internal fun apply_textmate_language_settings(
    language: TextMateLanguage,
    settings: editor_settings_state,
    file_path: String?
) {
    val c_family_file = is_c_family_file(file_path)
    language.useTab(true)
    language.setTabSize(settings.tab_size.coerceIn(2, 8))
    language.setAutoCompleteEnabled(settings.auto_completion && c_family_file)
    language.setCompleterKeywords(if (c_family_file) c_cpp_completion_keywords else emptyArray())
}
