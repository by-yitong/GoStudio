package com.termux.app.gostudio.editor

import androidx.compose.ui.graphics.Color

/**
 * Go 语法高亮颜色
 */
object GoSyntaxColors {
    val KEYWORD = Color(0xFF569CD6)
    val STRING = Color(0xFFCE9178)
    val COMMENT = Color(0xFF6A9955)
    val NUMBER = Color(0xFFB5CEA8)
    val TYPE = Color(0xFF4EC9B0)
    val FUNCTION = Color(0xFFDCDCAA)
    val NORMAL = Color(0xFFD4D4D4)
}

// Go 关键字
private val KEYWORDS = setOf(
    "break", "case", "chan", "const", "continue", "default", "defer",
    "else", "fallthrough", "for", "func", "go", "goto", "if",
    "import", "interface", "map", "package", "range", "return",
    "select", "struct", "switch", "type", "var"
)

// Go 内建类型
private val BUILTIN_TYPES = setOf(
    "bool", "byte", "complex64", "complex128", "error", "float32", "float64",
    "int", "int8", "int16", "int32", "int64", "rune", "string",
    "uint", "uint8", "uint16", "uint32", "uint64", "uintptr"
)

// Go 内建函数
private val BUILTIN_FUNCTIONS = setOf(
    "append", "cap", "close", "copy", "delete", "imag", "len",
    "make", "new", "panic", "print", "println", "real", "recover"
)

/**
 * 对 Go 代码进行简单的语法高亮
 */
fun highlightGo(code: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    builder.append(code)

    var i = 0
    while (i < code.length) {
        val c = code[i]

        when {
            c.isWhitespace() -> { i++ }
            c == '/' && i + 1 < code.length && code[i + 1] == '/' -> {
                val start = i
                while (i < code.length && code[i] != '\n') i++
                addStyle(builder, start, i, GoSyntaxColors.COMMENT)
            }
            c == '"' -> {
                val start = i
                i++
                while (i < code.length && code[i] != '"') {
                    if (code[i] == '\\' && i + 1 < code.length) i += 2
                    else i++
                }
                if (i < code.length) i++
                addStyle(builder, start, i, GoSyntaxColors.STRING)
            }
            c == '\'' -> {
                val start = i
                i++
                while (i < code.length && code[i] != '\'') {
                    if (code[i] == '\\' && i + 1 < code.length) i += 2
                    else i++
                }
                if (i < code.length) i++
                addStyle(builder, start, i, GoSyntaxColors.STRING)
            }
            c == '`' -> {
                val start = i
                i++
                while (i < code.length && code[i] != '`') i++
                if (i < code.length) i++
                addStyle(builder, start, i, GoSyntaxColors.STRING)
            }
            c.isDigit() -> {
                val start = i
                while (i < code.length && (code[i].isDigit() || code[i] == '.' ||
                    code[i] in 'a'..'f' || code[i] in 'A'..'F' ||
                    code[i] == 'x' || code[i] == 'X')) {
                    i++
                }
                addStyle(builder, start, i, GoSyntaxColors.NUMBER)
            }
            c.isLetter() || c == '_' -> {
                val start = i
                while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                val word = code.substring(start, i)
                val color = when {
                    KEYWORDS.contains(word) -> GoSyntaxColors.KEYWORD
                    BUILTIN_TYPES.contains(word) -> GoSyntaxColors.TYPE
                    BUILTIN_FUNCTIONS.contains(word) -> GoSyntaxColors.FUNCTION
                    else -> null
                }
                if (color != null) {
                    addStyle(builder, start, i, color)
                }
            }
            else -> { i++ }
        }
    }

    return builder.toAnnotatedString()
}

private fun addStyle(
    builder: androidx.compose.ui.text.AnnotatedString.Builder,
    start: Int,
    end: Int,
    color: Color
) {
    if (start >= end) return
    builder.addStyle(
        androidx.compose.ui.text.SpanStyle(color = color),
        start,
        end.coerceAtMost(builder.length)
    )
}
