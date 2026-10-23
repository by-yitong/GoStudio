package io.github.rosemoe.sora.lsp.utils

import io.github.rosemoe.sora.text.Content

/**
 * 判断光标是否位于未闭合的圆括号 ( ) 内部。
 *
 * 从光标位置向前扫描，对 `(` 计数减一、对 `)` 计数加一。
 * 计数小于 0 表示遇到了尚未闭合的 `(`，即光标在括号内。
 * 遇到 `{` 或 `}` 时停止（函数体边界，括号不会跨函数体）。
 */
fun isCursorInsideParens(content: Content, cursorIndex: Int): Boolean {
    val len = content.length
    val safeIndex = cursorIndex.coerceIn(0, len)
    var depth = 0
    var i = safeIndex - 1
    while (i >= 0) {
        val ch = content[i]
        when (ch) {
            ')' -> depth++
            '(' -> {
                depth--
                if (depth < 0) return true
            }
            '{', '}' -> return false
        }
        i--
    }
    return false
}
