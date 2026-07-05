package com.xc.code.ui.screens.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xc.code.ui.terminal.remember_terminal_state
import com.xc.code.ui.terminal.terminal_close_last_behavior
import com.xc.code.ui.terminal.terminal_panel
import com.xc.code.ui.terminal.terminal_state

internal typealias editor_terminal_state = terminal_state

@Composable
internal fun remember_editor_terminal_state(): editor_terminal_state {
    return remember_terminal_state()
}

@Composable
internal fun editor_terminal_panel(
    state: editor_terminal_state,
    cwd: String,
    extra_environment: Map<String, String>,
    modifier: Modifier = Modifier
) {
    terminal_panel(
        state = state,
        cwd = cwd,
        compact = true,
        fill_panel_background = true,
        show_tab_separators = true,
        show_keyboard = true,
        text_size = 22,
        extra_environment = extra_environment,
        close_last_behavior = terminal_close_last_behavior.Recreate,
        modifier = modifier
    )
}
