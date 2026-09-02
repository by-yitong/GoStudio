package com.jmwl.gostudio.editor.model

import com.jmwl.gostudio.editor.config.gostudio_backend_config

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
    val imported_font_path: String = ""
)

