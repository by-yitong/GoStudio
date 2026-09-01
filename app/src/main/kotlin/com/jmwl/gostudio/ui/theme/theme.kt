package com.jmwl.gostudio.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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

object theme_manager {
    private val _theme = MutableStateFlow(app_theme_type.DARK)
    val theme: StateFlow<app_theme_type> = _theme.asStateFlow()

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
    private const val SCALE_KEY = "app_scale"
    private const val PREFS = "app_settings"
    
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ordinal = prefs.getInt(THEME_KEY, app_theme_type.SYSTEM.ordinal)
        _theme.value = app_theme_type.values()[ordinal]
        val saved_scale = prefs.getFloat(SCALE_KEY, 1f)
        _scale.value = saved_scale
    }
    
    fun set_theme(context: Context, type: app_theme_type) {
        _theme.value = type
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(THEME_KEY, type.ordinal)
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
    
    setup_system_bars(is_dark_theme)
    
    val scaled_density = LocalDensity.current.density * scale_value
    val app_colors = if (is_dark_theme) dark_app_colors else light_app_colors
    val material_scheme = if (is_dark_theme) dark_color_scheme else light_color_scheme
    
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