package com.jmwl.gostudio.editor.core

/** Go 语言补全关键字（用于 sora 编辑器基础词法补全；语义补全由 gopls 提供）。 */
val go_completion_keywords = arrayOf(
    "break", "case", "chan", "const", "continue", "default", "defer", "else",
    "fallthrough", "for", "func", "go", "goto", "if", "import", "interface",
    "map", "package", "range", "return", "select", "struct", "switch", "type",
    "var",
    "true", "false", "iota", "nil",
    "append", "cap", "close", "complex", "copy", "delete", "imag", "len",
    "make", "new", "panic", "print", "println", "real", "recover",
    "bool", "byte", "complex64", "complex128", "error", "float32", "float64",
    "int", "int8", "int16", "int32", "int64", "rune", "string",
    "uint", "uint8", "uint16", "uint32", "uint64", "uintptr",
    "any", "comparable"
)
