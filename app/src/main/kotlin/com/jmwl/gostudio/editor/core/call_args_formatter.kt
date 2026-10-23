package com.jmwl.gostudio.editor.core

/**
 * 函数调用参数「一行一个」展开/折叠的纯文本变换（Go 多行参数风格）。
 *
 * 手机上编辑多参数调用很痛苦：把 `foo(a, b)` 展开成每个参数独占一行便于逐个修改，
 * 改完再折叠回单行。纯文本变换，不依赖 gopls；零 Android 依赖，可独立单测。
 *
 * 扫描时跳过 Go 的词法区域：双引号字符串（含 \" 转义）、反引号 raw string、
 * 单引号 rune（如 `','`）、`//` 行注释、块注释——这些区域内的括号/逗号一律不算。
 */
data class call_args_range(
    /** 开括号 `(` 的偏移（含括号本身） */
    val open_offset: Int,
    /** 闭括号 `)` 的偏移（含括号本身） */
    val close_offset: Int
)

/**
 * 参数区变换结果：
 * - [new_call_text]：用于替换 `[open_offset, close_offset]` 的新文本（含两端括号）；
 * - [new_cursor]：变换后光标相对 open_offset 的偏移（展开时停在最后一个参数行尾逗号后，折叠时停在闭括号前）。
 */
data class call_args_transform(
    val new_call_text: String,
    val new_cursor: Int
)

object call_args_formatter {

    /**
     * 找包含 cursor 的**最内层**匹配圆括号对（只认 `()`，不认 `[]` `{}`）。
     * 括号不匹配（开括号无闭括号配对）或找不到 → null。
     * cursor 语义为文本偏移：位于开括号之后（不含）到闭括号处（含）之间算在括号内。
     */
    fun find_enclosing_call(content: String, cursor: Int): call_args_range? {
        val caret = cursor.coerceIn(0, content.length)
        val stack = ArrayDeque<Int>()
        var enclosing: call_args_range? = null
        for_each_code_char(content) { index, c ->
            if (c == '(') {
                stack.addLast(index)
            } else if (c == ')') {
                val open = stack.removeLastOrNull()
                // 内层括号对先闭合：第一个包住 caret 的完整括号对即最内层
                if (open != null && enclosing == null && open < caret && caret <= index) {
                    enclosing = call_args_range(open, index)
                }
            }
        }
        return enclosing
    }

    /**
     * 按深度 0 的逗号切分参数区文本（正确跳过字符串/rune/注释与嵌套括号）。
     * 参数区为空白（`foo()`）→ 空列表；末尾若是尾逗号产生的纯空白片段会被去掉
     * （Go 多行风格的行尾逗号在折叠时不应产生空参数）。
     */
    fun split_top_level_args(arg_text: String): List<String> {
        if (arg_text.isBlank()) return emptyList()
        val parts = mutableListOf<String>()
        var depth = 0
        var segment_start = 0
        for_each_code_char(arg_text) { index, c ->
            when (c) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> if (depth > 0) depth--
                ',' -> if (depth == 0) {
                    parts.add(arg_text.substring(segment_start, index))
                    segment_start = index + 1
                }
            }
        }
        parts.add(arg_text.substring(segment_start))
        // 去掉尾逗号带来的最后一片空白
        if (parts.isNotEmpty() && parts.last().isBlank()) parts.removeAt(parts.lastIndex)
        return if (parts.all { it.isBlank() }) emptyList() else parts
    }

    /**
     * 智能切换入口：光标不在调用括号内或参数区为空 → null（调用方给 toast）。
     *
     * 判定规则：取开括号后到**第一个顶层逗号**（无逗号则到闭括号）之间的文本——
     * 含换行 → 当前是展开态 → 执行折叠；不含换行 → 执行展开。
     * 这保证反复点击在两态间切换，即使参数内部有嵌套多行调用。
     */
    fun format_call_args(content: String, cursor: Int): call_args_transform? {
        val range = find_enclosing_call(content, cursor) ?: return null
        val arg_text = content.substring(range.open_offset + 1, range.close_offset)
        val args = split_top_level_args(arg_text)
        if (args.isEmpty()) return null

        val probe_end = first_top_level_comma_index(arg_text)
            .let { if (it < 0) arg_text.length else it }
        val should_expand = !arg_text.substring(0, probe_end).contains('\n')
        return if (should_expand) {
            expand_call(content, range, args)
        } else {
            collapse_call(range, args)
        }
    }

    /** `foo(a, b)` ⟹ `foo(\n\ta,\n\tb,\n)`：缩进 = 调用行前导空白 + 一个 \t；每个参数 trim 后独占一行、行尾加逗号；`)` 单独一行、与调用行同缩进。 */
    private fun expand_call(
        content: String,
        range: call_args_range,
        args: List<String>
    ): call_args_transform {
        val line_start = content.lastIndexOf('\n', range.open_offset).let { if (it < 0) 0 else it + 1 }
        var ws_end = line_start
        while (ws_end < content.length && (content[ws_end] == ' ' || content[ws_end] == '\t')) ws_end++
        val base_indent = content.substring(line_start, ws_end)
        val arg_indent = base_indent + "\t"

        val body = args.joinToString("\n") { arg_indent + it.trim() + "," }
        return call_args_transform(
            new_call_text = "(\n" + body + "\n" + base_indent + ")",
            new_cursor = "(\n".length + body.length
        )
    }

    /** 参数区多行时折叠回单行：每个顶层参数只 trim 两端（内部结构原样保留），`", "` 连接。 */
    private fun collapse_call(
        range: call_args_range,
        args: List<String>
    ): call_args_transform {
        val joined = args.joinToString(", ") { it.trim() }
        return call_args_transform(
            new_call_text = "(" + joined + ")",
            new_cursor = "(".length + joined.length
        )
    }

    /** 第一个深度 0 逗号的下标；没有则 -1。 */
    private fun first_top_level_comma_index(arg_text: String): Int {
        var depth = 0
        var found = -1
        for_each_code_char(arg_text) { index, c ->
            if (found >= 0) return@for_each_code_char
            when (c) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> if (depth > 0) depth--
                ',' -> if (depth == 0) found = index
            }
        }
        return found
    }

    /**
     * 遍历所有「代码字符」（跳过字符串/rune/注释区域），对每个代码字符回调 (下标, 字符)。
     */
    private fun for_each_code_char(content: String, visitor: (Int, Char) -> Unit) {
        var i = 0
        while (i < content.length) {
            val c = content[i]
            when {
                c == '"' -> i = skip_double_quoted(content, i) + 1
                c == '`' -> i = skip_raw_string(content, i) + 1
                c == '\'' -> i = skip_rune(content, i) + 1
                c == '/' && i + 1 < content.length && content[i + 1] == '/' ->
                    i = skip_line_comment(content, i) + 1
                c == '/' && i + 1 < content.length && content[i + 1] == '*' ->
                    i = skip_block_comment(content, i) + 1
                else -> {
                    visitor(i, c)
                    i++
                }
            }
        }
    }

    /** 各 skip 函数返回该词法区域最后一个字符的下标（未闭合时返回末尾），调用方 +1 前进。 */

    private fun skip_double_quoted(content: String, start: Int): Int {
        var i = start + 1
        while (i < content.length) {
            val c = content[i]
            if (c == '\\') {
                i += 2
            } else if (c == '"') {
                return i
            } else {
                i++
            }
        }
        return content.length - 1
    }

    private fun skip_raw_string(content: String, start: Int): Int {
        var i = start + 1
        while (i < content.length && content[i] != '`') i++
        return minOf(i, content.length - 1)
    }

    private fun skip_rune(content: String, start: Int): Int {
        var i = start + 1
        while (i < content.length) {
            val c = content[i]
            if (c == '\\') {
                i += 2
            } else if (c == '\'') {
                return i
            } else {
                i++
            }
        }
        return content.length - 1
    }

    private fun skip_line_comment(content: String, start: Int): Int {
        var i = start
        while (i < content.length && content[i] != '\n') i++
        // 不吞换行符本身：停在换行前一个字符
        return i - 1
    }

    private fun skip_block_comment(content: String, start: Int): Int {
        var i = start + 2
        while (i < content.length) {
            if (content[i] == '*' && i + 1 < content.length && content[i + 1] == '/') {
                return i + 1
            }
            i++
        }
        return content.length - 1
    }
}
