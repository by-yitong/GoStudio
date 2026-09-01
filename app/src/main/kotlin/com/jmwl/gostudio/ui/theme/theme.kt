package com.jmwl.gostudio.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Material3 色板 = CodeAssist DNA（Teal 强调 + 三阶表面色），供 M3 原生组件
// （LargeTopAppBar/NavigationBar/卡片角色色）读取，与 app_colors 保持同一体系。
private val dark_color_scheme = darkColorScheme(
    primary = Color(0xFF5CCFE6),             // accent teal
    onPrimary = Color(0xFF06272E),           // 深墨（青色上的文字）
    primaryContainer = Color(0xFF123B44),    // 青色调暗容器（主操作卡）
    onPrimaryContainer = Color(0xFF9FE8F5),  // 青色调亮文字
    secondary = Color(0xFF2B2C31),           // surface2
    onSecondary = Color(0xFFE9E9EC),
    tertiary = Color(0xFF6F7079),            // textTertiary
    background = Color(0xFF161719),          // bg
    onBackground = Color(0xFFE9E9EC),
    surface = Color(0xFF232428),             // surface
    onSurface = Color(0xFFE9E9EC),
    surfaceVariant = Color(0xFF1B1C1F),
    onSurfaceVariant = Color(0xFFA0A1AA),    // textSecondary
    surfaceContainer = Color(0xFF26272B),
    surfaceContainerHigh = Color(0xFF2B2C31),// surface2
    surfaceContainerHighest = Color(0xFF34353B), // surface3
    outline = Color(0xFF6F7079),             // textTertiary
    outlineVariant = Color(0xFF3A3B41)       // separatorStrong 实色
)

private val light_color_scheme = lightColorScheme(
    primary = Color(0xFF137E9C),             // accent teal strong
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8EBF4),    // 青色调亮容器
    onPrimaryContainer = Color(0xFF0C3944),
    secondary = Color(0xFFF4F3EF),           // surface2
    onSecondary = Color(0xFF1D1E22),
    tertiary = Color(0xFF97989F),
    background = Color(0xFFECEBE7),          // bg 暖米白
    onBackground = Color(0xFF1D1E22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1E22),
    surfaceVariant = Color(0xFFFAF9F6),
    onSurfaceVariant = Color(0xFF62636B),
    surfaceContainer = Color(0xFFF7F6F3),
    surfaceContainerHigh = Color(0xFFF4F3EF),
    surfaceContainerHighest = Color(0xFFE8E7E1),
    outline = Color(0xFF97989F),
    outlineVariant = Color(0xFFDDDCD6)
)

@Composable
private fun setup_system_bars(dark_theme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(dark_theme) {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark_theme
            controller.isAppearanceLightNavigationBars = !dark_theme
        }
    }
}

enum class app_theme_type {
    DARK,
    LIGHT,
    SYSTEM
}

/**
 * 全局 UI 主题预设。预设只改变色系，深浅仍由 app_theme_type 控制；
 * CUSTOM 使用用户输入的种子色，并按深浅模式自动校准可读性。
 */
enum class app_theme_preset(
    val display_name: String,
    val dark_accent: Color,
    val light_accent: Color
) {
    CLASSIC_TEAL("经典青", Color(0xFF5CCFE6), Color(0xFF1787A6)),
    OCEAN_BLUE("深海蓝", Color(0xFF7AB5FF), Color(0xFF1668D8)),
    VIOLET_NIGHT("星夜紫", Color(0xFFB7A3FF), Color(0xFF6741D9)),
    FOREST_GREEN("森林绿", Color(0xFF77DD9A), Color(0xFF178A57)),
    SUNSET_ORANGE("暖阳橙", Color(0xFFFFA65B), Color(0xFFC25E11)),
    CUSTOM("自定义", Color(0xFF8EA2FF), Color(0xFF4C5FD7))
}

object theme_manager {
    private val _theme = MutableStateFlow(app_theme_type.DARK)
    val theme: StateFlow<app_theme_type> = _theme.asStateFlow()

    private val _preset = MutableStateFlow(app_theme_preset.CLASSIC_TEAL)
    val preset: StateFlow<app_theme_preset> = _preset.asStateFlow()

    private val _custom_accent = MutableStateFlow(0xFF7C9EFF.toInt())
    val custom_accent: StateFlow<Int> = _custom_accent.asStateFlow()

    fun resolve_is_dark(context: Context, type: app_theme_type): Boolean {
        return when (type) {
            app_theme_type.DARK -> true
            app_theme_type.LIGHT -> false
            app_theme_type.SYSTEM -> is_system_dark(context)
        }
    }

    private fun is_system_dark(context: Context): Boolean {
        val ui_mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return ui_mode == Configuration.UI_MODE_NIGHT_YES
    }
    
    private val _scale = MutableStateFlow(1f)
    val scale: StateFlow<Float> = _scale.asStateFlow()
    
    private const val THEME_KEY = "theme_type"
    private const val PRESET_KEY = "theme_preset"
    private const val CUSTOM_ACCENT_KEY = "custom_theme_accent"
    private const val SCALE_KEY = "app_scale"
    private const val PREFS = "app_settings"
    
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val theme_ordinal = prefs.getInt(THEME_KEY, app_theme_type.SYSTEM.ordinal)
        _theme.value = app_theme_type.values().getOrNull(theme_ordinal) ?: app_theme_type.SYSTEM

        val preset_name = prefs.getString(PRESET_KEY, app_theme_preset.CLASSIC_TEAL.name)
        _preset.value = app_theme_preset.values().firstOrNull { it.name == preset_name }
            ?: app_theme_preset.CLASSIC_TEAL

        _custom_accent.value = prefs.getInt(CUSTOM_ACCENT_KEY, 0xFF7C9EFF.toInt())
        _scale.value = prefs.getFloat(SCALE_KEY, 1f)
    }
    
    fun set_theme(context: Context, type: app_theme_type) {
        _theme.value = type
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(THEME_KEY, type.ordinal)
            .apply()
    }

    fun set_preset(context: Context, preset: app_theme_preset) {
        _preset.value = preset
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PRESET_KEY, preset.name)
            .apply()
    }

    fun set_custom_accent(context: Context, argb: Int) {
        val color = argb or 0xFF000000.toInt()
        _preset.value = app_theme_preset.CUSTOM
        _custom_accent.value = color
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PRESET_KEY, app_theme_preset.CUSTOM.name)
            .putInt(CUSTOM_ACCENT_KEY, color)
            .apply()
    }
    
    fun get_scale(context: Context): Float {
        return _scale.value
    }
    
    fun set_scale(context: Context, scale: Float) {
        _scale.value = scale
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(SCALE_KEY, scale)
            .apply()
    }
}

/** 支持 #RGB/#RRGGBB/#AARRGGBB；界面统一保存为不透明 ARGB。 */
fun parse_app_theme_color(value: String): Color? {
    val normalized = value.trim().removePrefix("#")
    if (!normalized.matches(Regex("^[0-9A-Fa-f]+$"))) return null
    val argb = when (normalized.length) {
        3 -> "FF" + normalized.map { it.toString() + it }.joinToString("")
        6 -> "FF$normalized"
        8 -> normalized
        else -> return null
    }
    return runCatching { Color(android.graphics.Color.parseColor("#$argb")) }.getOrNull()
}

fun format_app_theme_color(argb: Int): String =
    "#%06X".format(argb and 0xFFFFFF)

private fun rgb_only(color: Color): Int = color.toArgb() and 0xFFFFFF
private fun with_alpha(rgb: Int, alpha: Float): Color =
    Color(((alpha * 255f + 0.5f).toInt().coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF))

private fun readable_on(color: Color): Color =
    if (color.luminance() > 0.58f) Color(0xFF15161A) else Color(0xFFFFFFFF)

/** 自定义种子色按深浅模式微调，避免深底太暗、浅底太刺眼。 */
private fun normalized_accent(color: Color, dark_theme: Boolean): Color {
    val luminance = color.luminance()
    return when {
        dark_theme && luminance < 0.20f -> lerp(color, Color.White, 0.28f)
        !dark_theme && luminance > 0.78f -> lerp(color, Color.Black, 0.24f)
        !dark_theme && luminance < 0.16f -> lerp(color, Color.White, 0.22f)
        else -> color
    }
}

private fun remap_accent(color: Color, old_accent: Color, new_accent: Color): Color {
    return if (rgb_only(color) == rgb_only(old_accent)) {
        with_alpha(new_accent.toArgb(), color.alpha)
    } else {
        color
    }
}

/** 在保留原层次和透明度的前提下，把整套 UI 色重映射到当前主题。 */
private fun themed_app_colors(base: app_colors, accent: Color, dark_theme: Boolean): app_colors {
    val old_accent = if (dark_theme) Color(0xFF5CCFE6) else Color(0xFF1C9BBD)
    val remap = { color: Color -> remap_accent(color, old_accent, accent) }
    val bg = lerp(base.gradient_start, accent, if (dark_theme) 0.07f else 0.05f)
    val surface = lerp(base.card_bg, accent, if (dark_theme) 0.08f else 0.06f)
    val surface_pressed = lerp(base.card_pressed, accent, if (dark_theme) 0.10f else 0.08f)
    val surface_2 = lerp(base.top_button_bg, accent, if (dark_theme) 0.10f else 0.08f)
    val editor_bg = lerp(base.editor_bg, accent, if (dark_theme) 0.07f else 0.05f)

    return base.copy(
        gradient_start = bg,
        gradient_middle = bg,
        gradient_end = bg,
        title_highlight = accent,
        card_bg = surface,
        card_pressed = surface_pressed,
        card_icon_bg = accent,
        top_button_bg = surface_2,
        search_button_active = accent,
        search_button_bg_active = remap(base.search_button_bg_active),
        input_border = accent,
        dialog_bg = lerp(base.dialog_bg, accent, if (dark_theme) 0.09f else 0.07f),
        dialog_icon = accent,
        dialog_cancel = accent,
        dialog_clone_bg = accent,
        dialog_clone_text = readable_on(accent),
        dialog_card_bg = bg,
        dialog_input_bg = surface,
        dialog_input_border = accent,
        dialog_input_icon = accent,
        terminal_cursor = accent.toArgb(),
        key_button_pressed_bg = surface_pressed,
        key_button_active_text = accent,
        terminal_tab_unselected_bg = surface_2,
        terminal_tab_selected_icon = accent,
        editor_bg = editor_bg,
        editor_icon = accent,
        editor_panel_overlay = with_alpha(bg.toArgb(), base.editor_panel_overlay.alpha),
        editor_button_bg = surface_2,
        editor_tab_unselected_bg = surface_2,
        editor_tab_selected_icon = accent,
        editor_sidebar_selected_bg = remap(base.editor_sidebar_selected_bg)
    )
}

private fun themed_material_scheme(dark_theme: Boolean, accent: Color): androidx.compose.material3.ColorScheme {
    val base = if (dark_theme) dark_color_scheme else light_color_scheme
    val background = lerp(base.background, accent, if (dark_theme) 0.07f else 0.05f)
    val surface = lerp(base.surface, accent, if (dark_theme) 0.08f else 0.06f)
    val container = lerp(accent, background, if (dark_theme) 0.78f else 0.84f)
    return base.copy(
        primary = accent,
        onPrimary = readable_on(accent),
        primaryContainer = container,
        onPrimaryContainer = if (dark_theme) lerp(accent, Color.White, 0.25f) else lerp(accent, Color.Black, 0.38f),
        secondary = lerp(base.secondary, accent, 0.08f),
        background = background,
        surface = surface,
        surfaceVariant = lerp(base.surfaceVariant, accent, 0.06f),
        surfaceContainer = lerp(base.surfaceContainer, accent, if (dark_theme) 0.08f else 0.06f),
        surfaceContainerHigh = lerp(base.surfaceContainerHigh, accent, if (dark_theme) 0.10f else 0.08f),
        surfaceContainerHighest = lerp(base.surfaceContainerHighest, accent, if (dark_theme) 0.12f else 0.10f),
        outline = lerp(base.outline, accent, 0.12f)
    )
}

val local_app_theme_color = compositionLocalOf { light_app_colors }

object app_theme_provider {
    val colors: app_colors
        @Composable
        get() = local_app_theme_color.current
}

@Composable
fun app_theme_provider(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val theme by theme_manager.theme.collectAsState()
    val scale_value by theme_manager.scale.collectAsState()
    
    val is_dark_theme = theme_manager.resolve_is_dark(context, theme)
    val preset by theme_manager.preset.collectAsState()
    val custom_accent by theme_manager.custom_accent.collectAsState()
    val seed = if (preset == app_theme_preset.CUSTOM) {
        Color(custom_accent)
    } else if (is_dark_theme) {
        preset.dark_accent
    } else {
        preset.light_accent
    }
    val accent = normalized_accent(seed, is_dark_theme)
    
    setup_system_bars(is_dark_theme)
    
    val scaled_density = LocalDensity.current.density * scale_value
    val base_colors = if (is_dark_theme) dark_app_colors else light_app_colors
    val app_colors = themed_app_colors(base_colors, accent, is_dark_theme)
    val material_scheme = themed_material_scheme(is_dark_theme, accent)
    
    CompositionLocalProvider(
        local_app_theme_color provides app_colors
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = scaled_density,
                fontScale = LocalDensity.current.fontScale
            )
        ) {
            MaterialTheme(
                colorScheme = material_scheme,
                typography = typography,
                content = content
            )
        }
    }
}