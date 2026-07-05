package com.xc.code.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

class terminal_tab(
    val number: Int,
    val title: String,
    val session: TerminalSession
)

class terminal_state {
    var terminal_view by mutableStateOf<TerminalView?>(null)
    var selected_tab_index by mutableIntStateOf(0)
    var terminal_session by mutableStateOf<TerminalSession?>(null)
    val terminal_tabs = mutableStateListOf<terminal_tab>()
    val terminal_sessions = mutableStateListOf<TerminalSession>()
    var ctrl_active by mutableStateOf(false)
    var alt_active by mutableStateOf(false)

    fun dispose() {
        terminal_sessions.toList().forEach { it.finishIfRunning() }
        terminal_sessions.clear()
        terminal_tabs.clear()
        terminal_session = null
        terminal_view = null
        selected_tab_index = 0
        ctrl_active = false
        alt_active = false
    }
}

@Composable
fun remember_terminal_state(): terminal_state {
    val state = remember { terminal_state() }
    DisposableEffect(Unit) {
        onDispose { state.dispose() }
    }
    return state
}
