package com.xc.code.editor.core

import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.TextInlayHint
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.delay

internal suspend fun build_editor_block_end_hints(editor: CodeEditor): InlayHintsContainer? {
    val blocks = wait_for_sora_code_blocks(editor) ?: return null
    val lines = List(editor.text.lineCount) { index -> editor.text.getLineString(index) }
    return build_block_end_hints_from_sora_blocks(lines, blocks)
}

private suspend fun wait_for_sora_code_blocks(editor: CodeEditor): List<CodeBlock>? {
    repeat(block_end_hint_sora_block_wait_attempts) {
        val blocks = editor.styles?.blocks
        if (!blocks.isNullOrEmpty()) return blocks.filterNotNull()
        delay(block_end_hint_sora_block_wait_delay_ms)
    }
    return editor.styles?.blocks?.filterNotNull()
}

private data class block_end_hint_candidate(
    val block: CodeBlock,
    val label: String
)

private fun build_block_end_hints_from_sora_blocks(
    lines: List<String>,
    blocks: List<CodeBlock>
): InlayHintsContainer {
    val candidates = blocks
        .mapNotNull { block -> block.to_block_end_hint_candidate(lines) }
        .filter_outer_block_end_hints()

    return InlayHintsContainer().also { hints ->
        candidates.forEach { candidate ->
            val hint_line = candidate.block.endLine + 1
            if (hint_line in lines.indices) {
                hints.add(TextInlayHint(hint_line, lines[hint_line].length, " ${candidate.label}"))
            }
        }
    }
}

private fun CodeBlock.to_block_end_hint_candidate(lines: List<String>): block_end_hint_candidate? {
    if (startLine !in lines.indices || endLine !in lines.indices) return null

    val header_end_column = block_end_hint_header_end_column(lines[startLine], startColumn)
    if (is_c_comment_position(lines, startLine, header_end_column)) return null

    val label = block_end_hint_label(lines, startLine, header_end_column)
    return label.takeIf { it.isNotBlank() }?.let { block_end_hint_candidate(this, it) }
}

private fun List<block_end_hint_candidate>.filter_outer_block_end_hints(): List<block_end_hint_candidate> {
    return filter { candidate ->
        none { outer_candidate ->
            outer_candidate !== candidate && outer_candidate.block.contains_code_block(candidate.block)
        }
    }
}

private fun CodeBlock.contains_code_block(block: CodeBlock): Boolean {
    val starts_before = startLine < block.startLine ||
        startLine == block.startLine && startColumn <= block.startColumn
    val ends_after = endLine > block.endLine ||
        endLine == block.endLine && endColumn >= block.endColumn
    val same_range = startLine == block.startLine &&
        startColumn == block.startColumn &&
        endLine == block.endLine &&
        endColumn == block.endColumn
    return starts_before && ends_after && !same_range
}

private fun is_c_comment_position(lines: List<String>, target_line: Int, target_column: Int): Boolean {
    var in_block_comment = false
    for (line_index in 0..target_line) {
        val line = lines[line_index]
        val scan_end = if (line_index == target_line) target_column.coerceIn(0, line.length) else line.length
        var column = 0
        while (column < scan_end) {
            if (in_block_comment) {
                val block_comment_end = line.indexOf("*/", column)
                if (block_comment_end < 0 || block_comment_end >= scan_end) return line_index == target_line
                in_block_comment = false
                column = block_comment_end + 2
                continue
            }

            val line_comment_start = line.indexOf("//", column).takeIf { it >= 0 && it < scan_end }
            val block_comment_start = line.indexOf("/*", column).takeIf { it >= 0 && it < scan_end }
            when {
                line_comment_start != null && (block_comment_start == null || line_comment_start < block_comment_start) -> {
                    return line_index == target_line
                }
                block_comment_start != null -> {
                    in_block_comment = true
                    column = block_comment_start + 2
                }
                else -> break
            }
        }
    }
    return in_block_comment
}

private fun block_end_hint_header_end_column(line: String, fallback_column: Int): Int {
    val brace_column = line.indexOf('{')
    return when {
        brace_column >= 0 -> brace_column
        fallback_column > 0 -> fallback_column.coerceAtMost(line.length)
        else -> line.length
    }
}

private fun block_end_hint_label(lines: List<String>, line_index: Int, header_end_column: Int): String {
    return block_end_hint_header(lines, line_index, header_end_column)
}

private fun block_end_hint_header(lines: List<String>, line_index: Int, header_end_column: Int): String {
    val current_line = lines[line_index].substringBefore("//")
    val current_header = current_line.take(header_end_column.coerceIn(0, current_line.length)).trim()
    if (current_header.isNotBlank()) {
        return normalize_block_end_hint_header(current_header)
    }

    val header_lines = mutableListOf<String>()
    val first_header_line = (line_index - max_block_end_hint_header_lookback_lines).coerceAtLeast(0)
    for (index in line_index - 1 downTo first_header_line) {
        val header_line = lines[index].substringBefore("//").trim()
        if (header_line.isBlank() || header_line.startsWith("#")) break
        header_lines.add(0, header_line)
        if (header_line.endsWith(";") || header_line.endsWith("}")) break
    }
    return normalize_block_end_hint_header(header_lines.joinToString(" "))
}

private fun normalize_block_end_hint_header(header: String): String {
    val statement = header.substringAfterLast(';').substringAfterLast('}').trim()
    return statement.replace(Regex("\\s+"), " ").trim()
}

private const val block_end_hint_sora_block_wait_delay_ms = 50L
private const val block_end_hint_sora_block_wait_attempts = 40
private const val max_block_end_hint_header_lookback_lines = 6
