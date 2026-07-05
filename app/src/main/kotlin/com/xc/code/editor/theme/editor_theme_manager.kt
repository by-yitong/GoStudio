package com.xc.code.editor.theme

import com.xc.code.core.logging.logger_manager

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.tm4e.core.registry.IThemeSource
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

private data class editor_theme_color_definition(
    val key: String,
    val title: String,
    val description: String
)

data class editor_theme_color_item(
    val key: String,
    val title: String,
    val description: String,
    val value: String
)

data class editor_theme_token_color_item(
    val index: Int,
    val title: String,
    val scope: String,
    val value: String
)

data class editor_theme_preview_palette(
    val background: String,
    val foreground: String,
    val cursor: String,
    val selection: String,
    val line_highlight: String,
    val indent_line: String,
    val block_line: String,
    val line_number: String,
    val active_line_number: String,
    val keyword: String,
    val type: String,
    val function: String,
    val string: String,
    val number: String,
    val comment: String,
    val constant: String,
    val operator: String,
    val punctuation: String
)

object editor_theme_manager {
    private const val DEFAULT_THEME_ASSET = "textmate/themes/xcode.json"
    private const val USER_THEME_DIR = "textmate/themes"
    private const val USER_THEME_FILE = "xcode.json"
    private const val THEME_NAME = "xcode_user"

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private val color_definitions = listOf(
        editor_theme_color_definition("editor.background", "编辑器背景", "代码编辑区的整体背景色"),
        editor_theme_color_definition("editor.foreground", "默认文字", "没有语法高亮命中时的代码文字颜色"),
        editor_theme_color_definition("editorCursor.foreground", "光标", "插入光标的颜色"),
        editor_theme_color_definition("editor.selectionHandleBackground", "选择手柄", "文本选择拖动手柄的颜色"),
        editor_theme_color_definition("editor.selectionBackground", "选中背景", "选中文本时的背景色"),
        editor_theme_color_definition("editor.lineHighlightBackground", "当前行背景", "光标所在行的背景色"),
        editor_theme_color_definition("editor.findMatchBackground", "当前搜索匹配", "当前搜索结果的背景色"),
        editor_theme_color_definition("editor.findMatchBorder", "当前搜索边框", "当前搜索结果的边框颜色"),
        editor_theme_color_definition("editor.findMatchHighlightBackground", "其它搜索匹配", "其它搜索结果的背景色"),
        editor_theme_color_definition("editor.findMatchHighlightBorder", "其它搜索边框", "其它搜索结果的边框颜色"),
        editor_theme_color_definition("highlightedDelimitersForeground", "括号匹配", "匹配括号和定界符的高亮颜色"),
        editor_theme_color_definition("editorIndentGuide.background", "缩进参考线", "普通缩进参考线颜色"),
        editor_theme_color_definition("editorIndentGuide.activeBackground", "当前缩进线", "当前层级/代码块辅助线颜色"),
        editor_theme_color_definition("editorWhitespace.foreground", "空白符号", "空格和缩进可见标记颜色"),
        editor_theme_color_definition("editorLineNumber.foreground", "行号", "普通行号文字颜色"),
        editor_theme_color_definition("editorLineNumber.activeForeground", "当前行号", "当前行号文字颜色"),
        editor_theme_color_definition("editor.lineDivider", "行分割线", "编辑器内部弱分割线颜色"),
        editor_theme_color_definition("scrollbarSlider.background", "滚动条", "滚动条普通状态颜色"),
        editor_theme_color_definition("scrollbarSlider.activeBackground", "滚动条激活", "拖动滚动条时的颜色"),
        editor_theme_color_definition("editor.wordHighlightBackground", "单词高亮", "相同单词弱高亮背景色"),
        editor_theme_color_definition("editor.wordHighlightStrongBackground", "强单词高亮", "相同单词强高亮背景色"),
        editor_theme_color_definition("editorError.foreground", "错误诊断", "代码错误波浪线和诊断弹窗边框颜色"),
        editor_theme_color_definition("editorWarning.foreground", "警告诊断", "代码警告波浪线和诊断弹窗边框颜色"),
        editor_theme_color_definition("editorInfo.foreground", "提示诊断", "代码提示波浪线和诊断弹窗边框颜色"),
        editor_theme_color_definition("editorSuggestWidget.background", "补全背景", "补全窗口背景色"),
        editor_theme_color_definition("editorSuggestWidget.foreground", "补全文字", "补全窗口普通文字颜色"),
        editor_theme_color_definition("editorSuggestWidget.highlightForeground", "补全高亮文字", "补全匹配字符颜色"),
        editor_theme_color_definition("editorSuggestWidget.selectedBackground", "补全选中项", "补全窗口选中项背景色"),
        editor_theme_color_definition("tooltipBackground", "提示背景", "编辑器提示弹窗背景色"),
        editor_theme_color_definition("tooltipBriefMessageColor", "提示主文字", "提示弹窗主要文字颜色"),
        editor_theme_color_definition("tooltipDetailedMessageColor", "提示说明文字", "提示弹窗说明文字颜色"),
        editor_theme_color_definition("tooltipActionColor", "提示操作文字", "提示弹窗操作文字颜色")
    )

    fun init(context: Context) {
        ensure_user_theme(context)
        reload_textmate_theme(context)
    }

    fun user_theme_file(context: Context): File {
        return File(File(context.filesDir, USER_THEME_DIR), USER_THEME_FILE)
    }

    fun load_color_items(context: Context): List<editor_theme_color_item> {
        val colors = load_color_map(context)
        return color_definitions.map { definition ->
            editor_theme_color_item(
                key = definition.key,
                title = definition.title,
                description = definition.description,
                value = colors[definition.key].orEmpty()
            )
        }
    }

    fun load_token_color_items(context: Context): List<editor_theme_token_color_item> {
        val json = read_theme_json(context, ensure_user_theme(context))
        val token_colors = json.optJSONArray("tokenColors") ?: return emptyList()
        val result = mutableListOf<editor_theme_token_color_item>()
        for (index in 0 until token_colors.length()) {
            val item = token_colors.optJSONObject(index) ?: continue
            val settings = item.optJSONObject("settings") ?: continue
            val foreground = settings.optString("foreground").takeIf { it.isNotBlank() } ?: continue
            val scopes = token_scopes(item.opt("scope"))
            val title = item.optString("name").takeIf { it.isNotBlank() }
                ?: scopes.firstOrNull().orEmpty().ifBlank { "语法颜色 ${index + 1}" }
            result.add(
                editor_theme_token_color_item(
                    index = index,
                    title = title,
                    scope = scopes.joinToString(", "),
                    value = foreground
                )
            )
        }
        return result
    }

    fun load_color_object(context: Context): JSONObject? {
        return runCatching {
            read_theme_json(context, ensure_user_theme(context)).optJSONObject("colors")
        }.getOrNull()
    }

    fun load_preview_palette(context: Context): editor_theme_preview_palette {
        val colors = load_color_map(context)
        val json = read_theme_json(context, ensure_user_theme(context))

        fun editor_color(key: String, fallback: String): String {
            return colors[key].orEmpty().ifBlank { fallback }
        }

        fun token_color(fallback: String, vararg scopes: String): String {
            return find_token_color(json, scopes.toList()) ?: fallback
        }

        return editor_theme_preview_palette(
            background = editor_color("editor.background", "#1A242E"),
            foreground = editor_color("editor.foreground", "#E8EDF2"),
            cursor = editor_color("editorCursor.foreground", "#4A9EFF"),
            selection = editor_color("editor.selectionBackground", "#2A3A4A"),
            line_highlight = editor_color("editor.lineHighlightBackground", "#1E2A36"),
            indent_line = editor_color("editorIndentGuide.background", "#2A3A4A"),
            block_line = editor_color("editorIndentGuide.activeBackground", "#5A7A9A"),
            line_number = editor_color("editorLineNumber.foreground", "#5A7A9A"),
            active_line_number = editor_color("editorLineNumber.activeForeground", "#4A9EFF"),
            keyword = token_color("#E5B567", "keyword.control", "keyword"),
            type = token_color("#7DB7E8", "storage.type", "entity.name.type", "support.type"),
            function = token_color("#7FD6C2", "entity.name.function", "meta.function-call"),
            string = token_color("#A5C25C", "string"),
            number = token_color("#D19A66", "constant.numeric"),
            comment = token_color("#6A7A8A", "comment"),
            constant = token_color("#FF9E64", "constant.language"),
            operator = token_color("#89DDFF", "keyword.operator"),
            punctuation = token_color("#FFD166", "punctuation.section")
        )
    }

    fun update_color(context: Context, key: String, value: String): Boolean {
        val normalized = normalize_color(value) ?: return false
        return runCatching {
            val theme_file = ensure_user_theme(context)
            val json = read_theme_json(context, theme_file)
            val colors = json.optJSONObject("colors") ?: JSONObject().also { json.put("colors", it) }
            colors.put(key, normalized)
            write_theme_json(theme_file, json)
            reload_textmate_theme(context)
            true
        }.getOrElse { error ->
            logger_manager.e("editor_theme_manager", "Failed to update editor theme color: ${error.message}", error)
            false
        }
    }

    fun update_token_color(context: Context, index: Int, value: String): Boolean {
        val normalized = normalize_color(value) ?: return false
        return runCatching {
            val theme_file = ensure_user_theme(context)
            val json = read_theme_json(context, theme_file)
            val token_colors = json.optJSONArray("tokenColors") ?: return false
            val item = token_colors.optJSONObject(index) ?: return false
            val settings = item.optJSONObject("settings") ?: JSONObject().also { item.put("settings", it) }
            settings.put("foreground", normalized)
            write_theme_json(theme_file, json)
            reload_textmate_theme(context)
            true
        }.getOrElse { error ->
            logger_manager.e("editor_theme_manager", "Failed to update token color: ${error.message}", error)
            false
        }
    }

    fun reset_to_default(context: Context): Boolean {
        return runCatching {
            copy_default_theme(context, user_theme_file(context))
            reload_textmate_theme(context)
            true
        }.getOrElse { error ->
            logger_manager.e("editor_theme_manager", "Failed to reset editor theme: ${error.message}", error)
            false
        }
    }

    fun set_current_theme(context: Context): Boolean {
        return runCatching {
            val registry = ThemeRegistry.getInstance()
            if (!registry.setTheme(THEME_NAME)) {
                reload_textmate_theme(context)
            } else {
                true
            }
        }.getOrElse { error ->
            logger_manager.e("editor_theme_manager", "Failed to set editor theme: ${error.message}", error)
            false
        }
    }

    fun reload_textmate_theme(context: Context): Boolean {
        return runCatching {
            val theme_file = ensure_user_theme(context)
            val registry = ThemeRegistry.getInstance()
            val existing = registry.findThemeByFileName(THEME_NAME)
            if (existing != null) {
                existing.load()
                existing.setDark(true)
                registry.setTheme(existing)
            } else {
                val model = ThemeModel(create_theme_source(theme_file), THEME_NAME)
                model.setDark(true)
                model.load()
                registry.loadTheme(model, true)
                registry.setTheme(model)
            }
            _version.value = _version.value + 1
            true
        }.getOrElse { error ->
            logger_manager.e("editor_theme_manager", "Failed to reload editor theme: ${error.message}", error)
            false
        }
    }

    fun normalize_color(value: String): String? {
        val hex = value.trim().removePrefix("#")
        if (hex.length != 6 && hex.length != 8) return null
        if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        return "#${hex.uppercase(Locale.US)}"
    }

    fun parse_color_argb(value: String): Int? {
        val normalized = normalize_color(value) ?: return null
        val hex = normalized.removePrefix("#")
        return runCatching {
            when (hex.length) {
                6 -> (0xFF000000L or hex.toLong(16)).toInt()
                8 -> {
                    val rgba = hex.toLong(16)
                    val red = (rgba shr 24) and 0xFF
                    val green = (rgba shr 16) and 0xFF
                    val blue = (rgba shr 8) and 0xFF
                    val alpha = rgba and 0xFF
                    ((alpha shl 24) or (red shl 16) or (green shl 8) or blue).toInt()
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun find_token_color(json: JSONObject, candidates: List<String>): String? {
        val token_colors = json.optJSONArray("tokenColors") ?: return null
        for (index in 0 until token_colors.length()) {
            val item = token_colors.optJSONObject(index) ?: continue
            val settings = item.optJSONObject("settings") ?: continue
            val foreground = settings.optString("foreground").takeIf { it.isNotBlank() } ?: continue
            val scopes = token_scopes(item.opt("scope"))
            if (candidates.any { candidate -> scopes.any { scope -> scope == candidate } }) {
                return foreground
            }
            if (candidates.any { candidate -> scopes.any { scope -> candidate.startsWith("$scope.") } }) {
                return foreground
            }
        }
        return null
    }

    private fun token_scopes(value: Any?): List<String> {
        return when (value) {
            is String -> value.split(',').map { it.trim() }.filter { it.isNotBlank() }
            is org.json.JSONArray -> buildList {
                for (index in 0 until value.length()) {
                    val scope = value.optString(index).trim()
                    if (scope.isNotBlank()) add(scope)
                }
            }
            else -> emptyList()
        }
    }

    private fun ensure_user_theme(context: Context): File {
        val theme_file = user_theme_file(context)
        if (!theme_file.exists()) {
            copy_default_theme(context, theme_file)
        }
        merge_theme_defaults(context, theme_file)
        return theme_file
    }

    private fun copy_default_theme(context: Context, target: File) {
        target.parentFile?.mkdirs()
        context.assets.open(DEFAULT_THEME_ASSET).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun merge_theme_defaults(context: Context, theme_file: File) {
        val defaults = read_default_theme_json(context)
        val current = read_theme_json_or_default(context, theme_file)
        var changed = false

        if (!current.has("name")) {
            current.put("name", defaults.optString("name", "XCode"))
            changed = true
        }

        val current_colors = current.optJSONObject("colors") ?: JSONObject().also {
            current.put("colors", it)
            changed = true
        }
        val default_colors = defaults.optJSONObject("colors")
        if (default_colors != null) {
            val keys = default_colors.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (!current_colors.has(key)) {
                    current_colors.put(key, default_colors.optString(key))
                    changed = true
                }
            }
        }

        if (!current.has("tokenColors") && defaults.has("tokenColors")) {
            current.put("tokenColors", defaults.optJSONArray("tokenColors"))
            changed = true
        }

        if (changed) {
            write_theme_json(theme_file, current)
        }
    }

    private fun load_color_map(context: Context): Map<String, String> {
        val theme_file = ensure_user_theme(context)
        val current_colors = read_theme_json(context, theme_file).optJSONObject("colors")
        val default_colors = read_default_theme_json(context).optJSONObject("colors")
        return color_definitions.associate { definition ->
            val value = current_colors?.optString(definition.key).orEmpty()
                .ifBlank { default_colors?.optString(definition.key).orEmpty() }
            definition.key to value
        }
    }

    private fun read_default_theme_json(context: Context): JSONObject {
        return JSONObject(
            context.assets.open(DEFAULT_THEME_ASSET)
                .bufferedReader()
                .use { it.readText() }
        )
    }

    private fun read_theme_json(context: Context, theme_file: File): JSONObject {
        return read_theme_json_or_default(context, theme_file)
    }

    private fun read_theme_json_or_default(context: Context, theme_file: File): JSONObject {
        val content = runCatching { theme_file.readText() }.getOrNull()
        if (content.isNullOrBlank()) {
            return read_default_theme_json(context)
        }
        return runCatching { JSONObject(content) }.getOrElse { read_default_theme_json(context) }
    }

    private fun write_theme_json(theme_file: File, json: JSONObject) {
        theme_file.parentFile?.mkdirs()
        theme_file.writeText(json.toString(2))
    }

    private fun create_theme_source(theme_file: File): IThemeSource {
        return object : IThemeSource {
            override fun getReader(): BufferedReader {
                return BufferedReader(InputStreamReader(theme_file.inputStream()))
            }

            override fun getFilePath(): String = theme_file.absolutePath
        }
    }
}
