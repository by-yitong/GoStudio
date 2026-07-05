package com.xc.code.editor.model

data class editor_tab_item(
    val path: String,
    val title: String,
    val has_changes: Boolean,
    val pinned: Boolean
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
    val clangd_enabled: Boolean = true,
    val clangd_completion: Boolean = true,
    val clangd_signature_help: Boolean = true,
    val clangd_document_highlight: Boolean = true,
    val clangd_formatting: Boolean = true,
    val clangd_hover: Boolean = false,
    val font_ligatures: Boolean = true,
    val font_size: Float = 14f,
    val tab_size: Int = 4,
    val font_family: String = "jetbrains_mono",
    val imported_font_path: String = ""
)

