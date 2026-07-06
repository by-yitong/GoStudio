package com.jmwl.gostudio.editor.core

fun resolve_editor_symbol_commit_text(symbol: String): String {
    return when (symbol) {
        "TAB" -> "\t"
        "()" -> "("
        "{}" -> "{"
        "[]" -> "["
        "<>" -> "<"
        "\"\"" -> "\""
        "''" -> "'"
        else -> symbol
    }
}
