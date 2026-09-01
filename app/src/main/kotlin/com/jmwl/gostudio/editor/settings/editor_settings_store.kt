package com.jmwl.gostudio.editor.settings

import com.jmwl.gostudio.editor.model.editor_settings_state

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

internal const val editor_settings_prefs_name = "editor_settings"

internal fun load_editor_settings(context: Context): editor_settings_state {
    val defaults = editor_settings_state()
    val prefs = context.getSharedPreferences(editor_settings_prefs_name, Context.MODE_PRIVATE)
    val imported_font_path = prefs.getString("imported_font_path", defaults.imported_font_path).orEmpty()
    val translation_prefs = translation_key_prefs(context)
    val translation_endpoint = prefs.getString(
        "gopls_translation_endpoint",
        defaults.gopls_translation_endpoint
    ).orEmpty().ifBlank { defaults.gopls_translation_endpoint }
    val translation_api_key = translation_prefs.getString(
        "gopls_translation_api_key",
        defaults.gopls_translation_api_key
    ).orEmpty().ifBlank { defaults.gopls_translation_api_key }

    // 一次性迁移：gopls_hover 默认值由 false 改为 true（光标停在函数名上显示函数介绍）。
    // 仅在本次升级首次运行时强制设 true，之后用户在设置里手动改的值会被尊重。
    if (!prefs.getBoolean("gopls_hover_default_migrated", false)) {
        prefs.edit().putBoolean("gopls_hover", defaults.gopls_hover).putBoolean("gopls_hover_default_migrated", true).apply()
    }

    return editor_settings_state(
        word_wrap = prefs.getBoolean("word_wrap", defaults.word_wrap),
        line_numbers = prefs.getBoolean("line_numbers", defaults.line_numbers),
        bracket_pair_highlight = prefs.getBoolean("bracket_pair_highlight", defaults.bracket_pair_highlight),
        current_line_highlight = prefs.getBoolean("current_line_highlight", defaults.current_line_highlight),
        block_lines = prefs.getBoolean("block_lines", defaults.block_lines),
        block_end_hints = prefs.getBoolean("block_end_hints", defaults.block_end_hints),
        sticky_scroll = prefs.getBoolean("sticky_scroll", defaults.sticky_scroll),
        whitespace_symbols = prefs.getBoolean("whitespace_symbols", prefs.getBoolean("non_printable", defaults.whitespace_symbols)),
        line_separator = prefs.getBoolean("line_separator", prefs.getBoolean("non_printable", defaults.line_separator)),
        pinch_zoom = prefs.getBoolean("pinch_zoom", defaults.pinch_zoom),
        cursor_blink = prefs.getBoolean("cursor_blink", defaults.cursor_blink),
        auto_indent = prefs.getBoolean("auto_indent", defaults.auto_indent),
        auto_completion = prefs.getBoolean("auto_completion", defaults.auto_completion),
        gopls_enabled = prefs.getBoolean("gopls_enabled", defaults.gopls_enabled),
        gopls_completion = prefs.getBoolean("gopls_completion", defaults.gopls_completion),
        gopls_signature_help = prefs.getBoolean("gopls_signature_help", defaults.gopls_signature_help),
        gopls_document_highlight = prefs.getBoolean("gopls_document_highlight", defaults.gopls_document_highlight),
        gopls_formatting = prefs.getBoolean("gopls_formatting", defaults.gopls_formatting),
        gopls_hover = prefs.getBoolean("gopls_hover", defaults.gopls_hover),
        gopls_translate_documentation = prefs.getBoolean("gopls_translate_documentation", defaults.gopls_translate_documentation),
        gopls_translation_endpoint = translation_endpoint,
        gopls_translation_api_key = translation_api_key,
        font_ligatures = prefs.getBoolean("font_ligatures", defaults.font_ligatures),
        font_size = prefs.getFloat("font_size", defaults.font_size).coerceIn(10f, 24f),
        tab_size = prefs.getInt("tab_size", defaults.tab_size).coerceIn(2, 8),
        font_family = normalize_editor_font_family(
            font_family = prefs.getString("font_family", defaults.font_family).orEmpty(),
            imported_font_path = imported_font_path
        ),
        imported_font_path = imported_font_path
    )
}

internal fun save_editor_settings(context: Context, settings: editor_settings_state) {
    context.getSharedPreferences(editor_settings_prefs_name, Context.MODE_PRIVATE)
        .edit()
        .putBoolean("word_wrap", settings.word_wrap)
        .putBoolean("line_numbers", settings.line_numbers)
        .putBoolean("bracket_pair_highlight", settings.bracket_pair_highlight)
        .putBoolean("current_line_highlight", settings.current_line_highlight)
        .putBoolean("block_lines", settings.block_lines)
        .putBoolean("block_end_hints", settings.block_end_hints)
        .putBoolean("sticky_scroll", settings.sticky_scroll)
        .putBoolean("whitespace_symbols", settings.whitespace_symbols)
        .putBoolean("line_separator", settings.line_separator)
        .putBoolean("pinch_zoom", settings.pinch_zoom)
        .putBoolean("cursor_blink", settings.cursor_blink)
        .putBoolean("auto_indent", settings.auto_indent)
        .putBoolean("gopls_enabled", settings.gopls_enabled)
        .putBoolean("gopls_completion", settings.gopls_completion)
        .putBoolean("gopls_signature_help", settings.gopls_signature_help)
        .putBoolean("gopls_document_highlight", settings.gopls_document_highlight)
        .putBoolean("gopls_formatting", settings.gopls_formatting)
        .putBoolean("gopls_hover", settings.gopls_hover)
        .putBoolean("gopls_translate_documentation", settings.gopls_translate_documentation)
        .putString("gopls_translation_endpoint", settings.gopls_translation_endpoint.trim())
        .putBoolean("gopls_migrated", true)
        .putBoolean("font_ligatures", settings.font_ligatures)
        .putFloat("font_size", settings.font_size)
        .putInt("tab_size", settings.tab_size)
        .putString("font_family", settings.font_family)
        .putString("imported_font_path", settings.imported_font_path)
        .apply()

    val translation_prefs = translation_key_prefs(context)
    if (translation_prefs.getString("gopls_translation_api_key", "") != settings.gopls_translation_api_key) {
        translation_prefs
            .edit()
            .putString("gopls_translation_api_key", settings.gopls_translation_api_key)
            .apply()
    }
}

private fun normalize_editor_font_family(font_family: String, imported_font_path: String): String {
    return when (font_family) {
        "jetbrains_mono", "roboto" -> font_family
        "imported" -> if (imported_font_path.isNotBlank() && File(imported_font_path).exists()) {
            font_family
        } else {
            editor_settings_state().font_family
        }
        else -> editor_settings_state().font_family
    }
}

@Volatile
private var cached_translation_key_prefs: android.content.SharedPreferences? = null

private fun translation_key_prefs(context: Context): android.content.SharedPreferences {
    cached_translation_key_prefs?.let { return it }
    val prefs = runCatching {
        EncryptedSharedPreferences.create(
            context,
            "gopls_translation_settings",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("gopls_translation_settings_fallback", Context.MODE_PRIVATE)
    }
    cached_translation_key_prefs = prefs
    return prefs
}
