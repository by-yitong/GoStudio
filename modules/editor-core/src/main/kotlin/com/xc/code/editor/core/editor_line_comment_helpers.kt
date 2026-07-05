package com.xc.code.editor.core

data class editor_line_comment_state(
    val lines: List<Pair<Int, String>>,
    val should_uncomment: Boolean
)

fun create_line_comment_state(lines: List<Pair<Int, String>>): editor_line_comment_state? {
    val non_blank_lines = lines.filter { (_, line_text) -> line_text.isNotBlank() }
    if (non_blank_lines.isEmpty()) return null

    val has_actionable_line = non_blank_lines.any { (_, line_text) ->
        !is_line_commented(line_text) || is_toggleable_line_comment(line_text)
    }
    if (!has_actionable_line) return null

    val should_uncomment = non_blank_lines.all { (_, line_text) -> is_toggleable_line_comment(line_text) }
    return editor_line_comment_state(lines = lines, should_uncomment = should_uncomment)
}

fun is_line_commented(line_text: String): Boolean {
    val indent_length = line_comment_indent_length(line_text)
    return indent_length < line_text.length && line_text.startsWith("//", indent_length)
}

fun is_toggleable_line_comment(line_text: String): Boolean {
    val indent_length = line_comment_indent_length(line_text)
    if (indent_length >= line_text.length || !line_text.startsWith("//", indent_length)) return false

    val after_marker_index = indent_length + 2
    val marker_next = line_text.getOrNull(after_marker_index)
    return marker_next != '/' && marker_next != '!'
}

fun line_comment_indent_length(line_text: String): Int {
    val first_content = line_text.indexOfFirst { char -> char != ' ' && char != '\t' }
    return if (first_content == -1) line_text.length else first_content
}
