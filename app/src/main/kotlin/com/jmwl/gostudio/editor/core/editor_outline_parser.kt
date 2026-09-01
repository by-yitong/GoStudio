package com.jmwl.gostudio.editor.core

/**
 * 当前文件的代码结构（大纲）符号。
 * gopls documentSymbol 未接入/未连接时的兜底解析也用它，
 * 两种来源统一成同一模型供结构面板消费。
 */
data class editor_outline_symbol(
    val name: String,
    val kind: editor_outline_kind,
    /** 附加信息：方法接收者、函数签名摘要、类型别名目标等。 */
    val detail: String,
    /** 0 起始行号。 */
    val line: Int,
    /** 层级缩进（顶层 0，结构体字段/接口方法 1）。 */
    val depth: Int
)

enum class editor_outline_kind { FUNCTION, METHOD, STRUCT, INTERFACE, TYPE_ALIAS, CONST, VAR, FIELD }

/**
 * 轻量 Go 大纲解析（正则逐行扫描，跳过注释与字符串字面量）。
 * gopls 可用时优先走 LSP documentSymbol，本解析器作为离线兜底。
 */
object editor_outline_parser {

    private val func_pattern = Regex(
        "^func\\s+(?:\\((\\w+)\\s+\\*?(\\w+)\\)\\s+)?(\\w+)\\s*\\(([^)]*)\\)\\s*(.*)$"
    )
    private val type_struct_pattern = Regex("^type\\s+(\\w+)\\s+struct\\s*\\{?")
    private val type_interface_pattern = Regex("^type\\s+(\\w+)\\s+interface\\s*\\{?")
    private val type_alias_pattern = Regex("^type\\s+(\\w+)\\s+(?:\\[(.*?)\\]\\s*)?(\\S.*)$")
    private val const_pattern = Regex("^(?:const|var)\\s+(?:\\((.*?)\\)|(\\w+)\\s*(?::=|=|\\s))")
    private val const_block_pattern = Regex("^(?:const|var)\\s+\\($")
    private val block_item_pattern = Regex("^(\\w+)\\s*(?::=|=|\\s)")
    private val struct_field_pattern = Regex("^(\\w+)\\s+\\S.*$")

    /** 解析 [source] 的大纲符号；[file_name] 用于判断是否 Go 文件（非 Go 返回空表）。 */
    fun parse_go_outline(source: String, file_name: String): List<editor_outline_symbol> {
        if (!file_name.endsWith(".go")) return emptyList()
        val symbols = mutableListOf<editor_outline_symbol>()
        var in_multi_line_comment = false
        var block_kind: editor_outline_kind? = null
        var block_depth_brace = 0

        source.lines().forEachIndexed { index, raw ->
            val line = strip_line(raw)
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachIndexed

            // 多行注释状态机
            if (in_multi_line_comment) {
                if ("*/" in trimmed) in_multi_line_comment = false
                return@forEachIndexed
            }
            if (trimmed.startsWith("/*") && !trimmed.endsWith("*/")) {
                in_multi_line_comment = true
                return@forEachIndexed
            }
            if (trimmed.startsWith("//")) return@forEachIndexed

            // const/var 聚合块内部
            if (block_kind != null) {
                if (trimmed == ")") {
                    block_kind = null
                    return@forEachIndexed
                }
                block_item_pattern.find(trimmed)?.let { match ->
                    symbols += editor_outline_symbol(
                        name = match.groupValues[1],
                        kind = block_kind!!,
                        detail = "",
                        line = index,
                        depth = 0
                    )
                }
                return@forEachIndexed
            }

            if (const_block_pattern.matches(trimmed)) {
                // const ( / var ( 开始聚合块：进入后逐行收集，具体 kind 由首个匹配决定
                block_kind = if (trimmed.startsWith("const")) editor_outline_kind.CONST else editor_outline_kind.VAR
                return@forEachIndexed
            }
            if (block_depth_brace > 0) {
                // struct/interface 块内部：收集字段/方法签名
                if (trimmed.startsWith("}")) {
                    block_depth_brace = 0
                    return@forEachIndexed
                }
                struct_field_pattern.find(trimmed)?.let { match ->
                    val name = match.groupValues[1]
                    if (name !in setOf("}")) {
                        symbols += editor_outline_symbol(
                            name = name,
                            kind = editor_outline_kind.FIELD,
                            detail = trimmed.substringAfter(name).trim(),
                            line = index,
                            depth = 1
                        )
                    }
                }
                return@forEachIndexed
            }

            func_pattern.find(trimmed)?.let { match ->
                val receiver = match.groupValues[2]
                val name = match.groupValues[3]
                val params = match.groupValues[4]
                val returns = match.groupValues[5].trim()
                symbols += editor_outline_symbol(
                    name = name,
                    kind = if (receiver.isBlank()) editor_outline_kind.FUNCTION else editor_outline_kind.METHOD,
                    detail = buildString {
                        if (receiver.isNotBlank()) append("($receiver) ")
                        append("($params)")
                        if (returns.isNotBlank()) append(" ${returns.substringBefore('{').trim()}")
                    }.trim(),
                    line = index,
                    depth = 0
                )
                return@forEachIndexed
            }
            type_struct_pattern.find(trimmed)?.let { match ->
                symbols += editor_outline_symbol(match.groupValues[1], editor_outline_kind.STRUCT, "struct", index, 0)
                if (!trimmed.endsWith("}")) block_depth_brace = 1
                return@forEachIndexed
            }
            type_interface_pattern.find(trimmed)?.let { match ->
                symbols += editor_outline_symbol(match.groupValues[1], editor_outline_kind.INTERFACE, "interface", index, 0)
                if (!trimmed.endsWith("}")) block_depth_brace = 1
                return@forEachIndexed
            }
            type_alias_pattern.find(trimmed)?.let { match ->
                symbols += editor_outline_symbol(
                    match.groupValues[1],
                    editor_outline_kind.TYPE_ALIAS,
                    match.groupValues[3].substringBefore('{').trim(),
                    index,
                    0
                )
                return@forEachIndexed
            }
            const_pattern.find(trimmed)?.let { match ->
                // const x = 1 / var x := 1 单行形式（聚合块在上方分支处理）
                if (match.groupValues[2].isNotBlank()) {
                    symbols += editor_outline_symbol(
                        name = match.groupValues[2],
                        kind = if (trimmed.startsWith("const")) editor_outline_kind.CONST else editor_outline_kind.VAR,
                        detail = "",
                        line = index,
                        depth = 0
                    )
                }
            }
        }
        return symbols
    }

    /** 去掉行内注释与字符串字面量内容（保留引号本身），避免注释/字符串里的关键字被当成符号。 */
    private fun strip_line(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        var quote: Char? = null
        while (i < raw.length) {
            val c = raw[i]
            if (quote != null) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == quote) {
                    sb.append(c)
                    quote = null
                }
                i++
                continue
            }
            when {
                c == '"' || c == '`' || c == '\'' -> { quote = c; sb.append(c) }
                c == '/' && i + 1 < raw.length && raw[i + 1] == '/' -> return sb.toString()
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}
