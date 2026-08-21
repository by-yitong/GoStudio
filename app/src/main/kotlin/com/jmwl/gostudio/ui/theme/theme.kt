package com.jmwl.gostudio.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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

// Hallmark-aligned: residual Material3 colorScheme mirrors the app_colors accent + neutrals
// so the two surfaces that still read MaterialTheme.colorScheme.* (install_screen progress bar,
// editor_search_panel surface) no longer render the default Material purple.
private val dark_color_scheme = darkColorScheme(
    primary = Color(0xFF5B8DEF),         // accent (Go-blue)
    onPrimary = Color(0xFFFAFBFC),       // accent-ink
    secondary = Color(0xFF20232B),       // surface
    onSecondary = Color(0xFFE8EAEE),     // ink
    tertiary = Color(0xFF8A8F9A),        // muted
    background = Color(0xFF17181D),      // paper
    onBackground = Color(0xFFE8EAEE),    // ink
    surface = Color(0xFF20232B),         // surface
    onSurface = Color(0xFFE8EAEE),       // ink
    surfaceVariant = Color(0xFF17181D),  // paper
    onSurfaceVariant = Color(0xFF8A8F9A),// muted
    outline = Color(0xFF343841)          // rule
)

private val light_color_scheme = lightColorScheme(
    primary = Color(0xFF1F54E8),         // accent (Go-blue)
    onPrimary = Color(0xFFFFFFFF),       // accent-ink
    secondary = Color(0xFFF1F3F6),       // surface
    onSecondary = Color(0xFF1A1D23),     // ink
    tertiary = Color(0xFF5F6571),        // muted
    background = Color(0xFFFAFBFC),      // paper
    onBackground = Color(0xFF1A1D23),    // ink
    surface = Color(0xFFFFFFFF),         // sunken
    onSurface = Color(0xFF1A1D23),       // ink
    surfaceVariant = Color(0xFFF1F3F6),  // surface
    onSurfaceVariant = Color(0xFF5F6571),// muted
    outline = Color(0xFFE0E3E8)          // rule
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
    
    val is_dark_theme = when (theme) {
        app_theme_type.DARK -> true
        app_theme_type.LIGHT -> false
        app_theme_type.SYSTEM -> isSystemInDarkTheme()
    }
    
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