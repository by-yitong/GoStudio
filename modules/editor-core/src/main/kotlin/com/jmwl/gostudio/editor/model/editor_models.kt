package com.jmwl.gostudio.editor.model

import com.jmwl.gostudio.editor.config.gostudio_backend_config

data class editor_tab_item(
    val path: String,
    val title: String,
    val has_changes: Boolean,
    val pinned: Boolean
)

/** 符号栏第一行默认符号（原硬编码于 editor_symbol_bar，现作为自定义的默认值）。
 *  注意：必须是 public——editor-core 模块的 internal 对 :app 模块不可见。 */
val default_symbol_bar_symbols = listOf(
    "//", "TAB", ":=", ".", ",", ";", ":", "(", ")", "{", "}",
    "[", "]", "\"\"", "''", "<-", "...", "=", "==", "!=",
    "!", "&&", "||", "func", "if", "for", "range", "return"
)

/** 符号栏第二行默认快捷片段：label 显示在栏上，code 为点击插入的完整代码（多行含 \n\t）。 */
val default_symbol_bar_snippets = listOf(
    "err ret" to "if err != nil {\n\treturn\n}",
    "ret err" to "if err != nil {\n\treturn err\n}",
    "range" to "for _, v := range ",
    "func" to "func name() {\n\n}",
    "defer" to "defer func() {\n\t\n}()",
    "go" to "go func() {\n\t\n}()",
    "make" to "make([]T, 0)",
    "map" to "map[string]T{}",
    "sprintf" to "fmt.Sprintf(\"%v\", )",
    "println" to "fmt.Println()"
)

data class editor_settings_state(
    val word_wrap: Boolean = false,
    val line_numbers: Boolean = true,
    val bracket_pair_highlight: Boolean = true,
    val current_line_highlight: Boolean = true,
    val block_lines: Boolean = true,
    val block_end_hints: Boolean = true,
    val sticky_scroll: Boolean = false,
    val whitespace_symbols: Boolean = true,
    val line_separator: Boolean = false,
    val pinch_zoom: Boolean = true,
    val cursor_blink: Boolean = true,
    val auto_indent: Boolean = true,
    val auto_completion: Boolean = true,
    val gopls_enabled: Boolean = true,
    val gopls_completion: Boolean = true,
    val gopls_signature_help: Boolean = true,
    val gopls_document_highlight: Boolean = true,
    val gopls_formatting: Boolean = true,
    val gopls_hover: Boolean = true,
    val gopls_translate_documentation: Boolean = true,
    val gopls_translation_endpoint: String = gostudio_backend_config.TRANSLATION_BASE_URL,
    val gopls_translation_api_key: String = gostudio_backend_config.TRANSLATION_BACKEND_KEY,
    val pack_auto_install: Boolean = true,
    val font_ligatures: Boolean = true,
    val font_size: Float = 14f,
    val tab_size: Int = 4,
    val font_family: String = "jetbrains_mono",
    val imported_font_path: String = "",
    /** 键盘上方符号栏第一行符号（自定义，见 default_symbol_bar_symbols） */
    val symbol_bar_symbols: List<String> = default_symbol_bar_symbols,
    /** 符号栏第二行快捷片段 label→插入代码（方向键固定，不在此配置） */
    val symbol_bar_snippets: List<Pair<String, String>> = default_symbol_bar_snippets
)

