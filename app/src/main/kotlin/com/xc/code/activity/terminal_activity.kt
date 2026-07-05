package com.xc.code.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xc.code.ui.terminal.remember_terminal_state
import com.xc.code.ui.terminal.terminal_close_last_behavior
import com.xc.code.ui.terminal.terminal_panel
import com.xc.code.ui.theme.app_theme_provider

class terminal_activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            app_theme_provider {
                terminal_screen_content()
            }
        }
    }

    @Composable
    private fun terminal_screen_content() {
        val terminal_state = remember_terminal_state()
        val colors = app_theme_provider.colors
        val terminal_background = Color(colors.terminal_background.toLong() and 0xFFFFFFFFL)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = terminal_background
        ) { inner_padding ->
            terminal_panel(
                state = terminal_state,
                cwd = java.io.File(filesDir, "home").absolutePath,
                proot_work_dir = "/home",
                compact = false,
                fill_panel_background = true,
                show_tab_separators = true,
                show_keyboard = true,
                text_size = 28,
                close_last_behavior = terminal_close_last_behavior.ClosePanel,
                on_last_tab_closed = { finish() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner_padding)
            )
        }
    }
}