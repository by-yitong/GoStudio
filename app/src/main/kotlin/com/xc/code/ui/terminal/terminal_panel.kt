package com.xc.code.ui.terminal

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import com.xc.code.toolchain.toolchain_manager
import com.xc.code.toolchain.toolchain_runtime_provider
import com.xc.code.toolchain.runtime.proot_environment
import com.xc.code.ui.theme.app_colors
import com.xc.code.ui.theme.app_theme_provider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun terminal_panel(
    state: terminal_state,
    cwd: String,
    modifier: Modifier = Modifier,
    proot_work_dir: String = cwd,
    compact: Boolean = false,
    fill_panel_background: Boolean = compact,
    show_tab_separators: Boolean = compact,
    show_keyboard: Boolean = true,
    text_size: Int = if (compact) 22 else 28,
    key_text_size: TextUnit = if (compact) 10.sp else 14.sp,
    extra_environment: Map<String, String> = emptyMap(),
    close_last_behavior: terminal_close_last_behavior = terminal_close_last_behavior.Recreate,
    on_last_tab_closed: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = app_theme_provider.colors
    val terminal_background_color = Color(colors.terminal_background.toLong() and 0xFFFFFFFFL)
    val main_handler = remember { Handler(Looper.getMainLooper()) }
    var terminal_view_ready by remember { mutableStateOf(false) }
    val safe_cwd = remember(cwd) { cwd.ifBlank { java.io.File(context.filesDir, "home").absolutePath } }
    val safe_proot_work_dir = remember(proot_work_dir) { proot_work_dir.ifBlank { "/home" } }
    val shell_path = remember { toolchain_runtime_provider.paths().proot_file.absolutePath }
    val args = remember(safe_proot_work_dir) { terminal_args(safe_proot_work_dir) }
    val env = remember(extra_environment) { terminal_env(extra_environment) }

    fun mark_terminal_view_ready() {
        main_handler.postDelayed({
            state.terminal_view?.alpha = 1f
            terminal_view_ready = true
        }, 48)
    }

    fun next_tab_title(): Pair<Int, String> {
        val used_numbers = state.terminal_tabs.map { it.number }.toSet()
        var number = 1
        while (number in used_numbers) number += 1
        return number to "Session $number"
    }

    fun attach_terminal_tab(index: Int) {
        if (state.terminal_tabs.isEmpty()) return
        val safe_index = index.coerceIn(0, state.terminal_tabs.lastIndex)
        val tab = state.terminal_tabs.getOrNull(safe_index) ?: return
        val already_attached = state.selected_tab_index == safe_index &&
            state.terminal_session == tab.session &&
            state.terminal_view?.getCurrentSession() == tab.session

        if (already_attached) {
            state.terminal_view?.requestFocus()
            return
        }

        state.selected_tab_index = safe_index
        state.terminal_session = tab.session
        state.terminal_view?.let { view ->
            terminal_view_ready = false
            view.alpha = 0f
            view.setBackgroundColor(colors.terminal_background)
            if (view.getCurrentSession() != tab.session) {
                view.attachSession(tab.session)
            }
            refresh_terminal_view(view, colors)
            mark_terminal_view_ready()
            view.requestFocus()
        }
    }

    fun create_terminal_tab(select: Boolean = true) {
        val (number, title) = next_tab_title()
        val session = create_terminal_session(
            context = context,
            shell_path = shell_path,
            cwd = safe_cwd,
            args = args.copyOf(),
            env = env.copyOf(),
            transcript_rows = 2000,
            on_text_changed = { changed_session ->
                main_handler.post {
                    if (state.terminal_view?.getCurrentSession() == changed_session) {
                        state.terminal_view?.invalidate()
                    }
                }
            },
            on_session_finished = { finished_session ->
                main_handler.post {
                    if (state.terminal_view?.getCurrentSession() == finished_session) {
                        state.terminal_view?.invalidate()
                    }
                }
            }
        )
        state.terminal_tabs.add(terminal_tab(number, title, session))
        state.terminal_sessions.add(session)
        if (select) attach_terminal_tab(state.terminal_tabs.lastIndex)
    }

    fun close_terminal_tab(index: Int) {
        val tab = state.terminal_tabs.getOrNull(index) ?: return
        state.terminal_tabs.removeAt(index)
        state.terminal_sessions.remove(tab.session)
        tab.session.finishIfRunning()

        if (state.terminal_tabs.isEmpty()) {
            state.selected_tab_index = 0
            state.terminal_session = null
            state.terminal_view = null
            when (close_last_behavior) {
                terminal_close_last_behavior.Recreate -> create_terminal_tab(select = true)
                terminal_close_last_behavior.ClosePanel -> on_last_tab_closed()
            }
            return
        }

        val new_index = when {
            state.selected_tab_index > index -> state.selected_tab_index - 1
            state.selected_tab_index >= state.terminal_tabs.size -> state.terminal_tabs.lastIndex
            else -> state.selected_tab_index
        }
        attach_terminal_tab(new_index.coerceIn(0, state.terminal_tabs.lastIndex))
    }

    LaunchedEffect(Unit) {
        if (state.terminal_tabs.isEmpty()) create_terminal_tab(select = true)
    }

    DisposableEffect(Unit) {
        onDispose {
            state.terminal_view = null
            state.ctrl_active = false
            state.alt_active = false
        }
    }

    val panel_modifier = if (fill_panel_background) {
        modifier
            .fillMaxSize()
            .background(terminal_background_color)
    } else {
        modifier.fillMaxSize()
    }

    Column(modifier = panel_modifier) {
        terminal_tabs_bar(
            tabs = state.terminal_tabs,
            selected_tab_index = state.selected_tab_index,
            compact = compact,
            show_separators = show_tab_separators,
            on_select_tab = { attach_terminal_tab(it) },
            on_close_tab = { close_terminal_tab(it) },
            on_new_tab = { create_terminal_tab(select = true) }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(terminal_background_color)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(terminal_background_color),
                factory = {
                    create_terminal_view(
                        context = context,
                        base_text_size = text_size,
                        ctrl_active = { state.ctrl_active },
                        alt_active = { state.alt_active },
                        on_ctrl_reset = { state.ctrl_active = false },
                        on_alt_reset = { state.alt_active = false }
                    ).also { view ->
                        terminal_view_ready = false
                        view.alpha = 0f
                        view.setBackgroundColor(colors.terminal_background)
                        state.terminal_view = view
                        state.terminal_tabs.getOrNull(state.selected_tab_index)?.let { tab ->
                            state.terminal_session = tab.session
                            view.attachSession(tab.session)
                            refresh_terminal_view(view, colors)
                            mark_terminal_view_ready()
                        }
                    }
                },
                update = { view ->
                    if (!terminal_view_ready) view.alpha = 0f
                    view.setBackgroundColor(colors.terminal_background)
                    state.terminal_view = view
                    state.terminal_tabs.getOrNull(state.selected_tab_index)?.let { tab ->
                        state.terminal_session = tab.session
                        if (view.getCurrentSession() != tab.session) {
                            terminal_view_ready = false
                            view.alpha = 0f
                            view.attachSession(tab.session)
                        }
                        refresh_terminal_view(view, colors)
                        mark_terminal_view_ready()
                    }
                }
            )

            if (!terminal_view_ready) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(terminal_background_color)
                )
            }
        }

        if (show_keyboard) {
            terminal_key_rows(
                session = state.terminal_session,
                ctrl_active = state.ctrl_active,
                alt_active = state.alt_active,
                text_size = key_text_size,
                compact = compact,
                on_ctrl_change = { state.ctrl_active = it },
                on_alt_change = { state.alt_active = it }
            )
        }
    }
}

enum class terminal_close_last_behavior {
    Recreate,
    ClosePanel
}

private fun terminal_args(cwd: String): Array<String> {
    return toolchain_runtime_provider.command_builder().interactive_args(cwd)
}

private fun terminal_env(extra_environment: Map<String, String> = emptyMap()): Array<String> {
    return proot_environment(
        path = toolchain_manager.proot_path(),
        extra = extra_environment
    ).as_array(toolchain_runtime_provider.paths())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun terminal_tabs_bar(
    tabs: List<terminal_tab>,
    selected_tab_index: Int,
    compact: Boolean,
    show_separators: Boolean,
    on_select_tab: (Int) -> Unit,
    on_close_tab: (Int) -> Unit,
    on_new_tab: () -> Unit
) {
    val colors = app_theme_provider.colors
    val divider_width = 0.5.dp
    val add_end_padding = 0.dp
    val row_modifier = if (show_separators) {
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .drawWithContent {
                drawContent()
                val stroke_width = divider_width.toPx()
                val y = size.height - stroke_width / 2f
                drawLine(
                    color = colors.terminal_tab_separator,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke_width
                )
            }
    } else {
        Modifier
            .fillMaxWidth()
            .height(34.dp)
    }

    Row(
        modifier = row_modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    terminal_tab_item(
                        tab = tab,
                        selected = selected_tab_index == index,
                        compact = true,
                        show_separator = false,
                        unified_background = !show_separators,
                        on_click = { on_select_tab(index) },
                        on_close = { on_close_tab(index) }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (tabs.isNotEmpty()) {
                    val safe_selected_tab_index = selected_tab_index.coerceIn(0, tabs.lastIndex)
                    PrimaryScrollableTabRow(
                        selectedTabIndex = safe_selected_tab_index,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        containerColor = Color.Transparent,
                        contentColor = colors.terminal_tab_selected_icon,
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = {
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(safe_selected_tab_index, matchContentSize = false)
                                    .fillMaxHeight()
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopCenter)
                                ) {
                                    drawLine(
                                        color = colors.terminal_tab_selected_icon,
                                        start = Offset(0f, size.height / 2f),
                                        end = Offset(size.width, size.height / 2f),
                                        strokeWidth = size.height
                                    )
                                }
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            terminal_tab_item(
                                tab = tab,
                                selected = safe_selected_tab_index == index,
                                compact = false,
                                show_separator = show_separators && index < tabs.lastIndex,
                                unified_background = !show_separators,
                                on_click = { on_select_tab(index) },
                                on_close = { on_close_tab(index) }
                            )
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = on_new_tab,
            modifier = Modifier
                .padding(end = add_end_padding)
                .size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建标签",
                tint = colors.terminal_tab_add_icon,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun terminal_tab_item(
    tab: terminal_tab,
    selected: Boolean,
    compact: Boolean,
    show_separator: Boolean,
    unified_background: Boolean,
    on_click: () -> Unit,
    on_close: () -> Unit
) {
    val colors = app_theme_provider.colors
    val icon_color = if (selected) colors.terminal_tab_selected_icon else colors.terminal_tab_unselected_content
    val text_color = if (selected) colors.terminal_tab_selected_text else colors.terminal_tab_unselected_content
    val item_width = if (compact) Modifier.widthIn(min = 72.dp, max = 148.dp) else Modifier.widthIn(min = 72.dp, max = 148.dp)
    val text_width = if (compact) 112.dp else 118.dp
    val end_padding = if (compact) 30.dp else 36.dp
    val close_size = if (compact) 26.dp else 28.dp
    val close_icon_size = if (compact) 13.dp else 14.dp
    val close_end_padding = if (compact) 2.dp else 4.dp

    Box(
        modifier = item_width
            .fillMaxHeight()
            .background(
                if (unified_background) Color.Transparent
                else if (selected) colors.terminal_tab_selected_bg
                else colors.terminal_tab_unselected_bg
            )
            .drawWithContent {
                drawContent()
                if (selected) {
                    val indicator_height = 2.dp.toPx()
                    drawLine(
                        color = colors.terminal_tab_selected_icon,
                        start = Offset(0f, indicator_height / 2f),
                        end = Offset(size.width, indicator_height / 2f),
                        strokeWidth = indicator_height
                    )
                } else if (show_separator) {
                    val stroke_width = 0.5.dp.toPx()
                    val separator_height = 16.dp.toPx()
                    val x = size.width - stroke_width / 2f
                    val y = (size.height - separator_height) / 2f
                    drawLine(
                        color = colors.terminal_tab_separator,
                        start = Offset(x, y),
                        end = Offset(x, y + separator_height),
                        strokeWidth = stroke_width
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = on_click
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .wrapContentWidth()
                .widthIn(max = text_width)
                .padding(start = 8.dp, end = end_padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Terminal, contentDescription = null, tint = icon_color, modifier = Modifier.size(14.dp))
            Text(
                text = tab.title,
                fontSize = 12.sp,
                color = text_color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        IconButton(
            onClick = on_close,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = close_end_padding)
                .size(close_size)
        ) {
            Icon(Icons.Default.Close, contentDescription = "关闭标签", tint = colors.terminal_tab_unselected_content, modifier = Modifier.size(close_icon_size))
        }
    }
}

@Composable
private fun terminal_key_rows(
    session: TerminalSession?,
    ctrl_active: Boolean,
    alt_active: Boolean,
    text_size: TextUnit,
    compact: Boolean,
    on_ctrl_change: (Boolean) -> Unit,
    on_alt_change: (Boolean) -> Unit
) {
    val row_padding = if (compact) PaddingValues(horizontal = 4.dp, vertical = 2.dp) else PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    val item_padding = if (compact) PaddingValues(horizontal = 2.dp, vertical = 4.dp) else PaddingValues(horizontal = 4.dp, vertical = 4.dp)

    Column(modifier = Modifier.fillMaxWidth().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(row_padding),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            terminal_key_button("ESC", false, { session?.writeCodePoint(false, 27) }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("/", false, { session?.write("/") }, on_long_click = { session?.write("/") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("-", false, { session?.write("-") }, on_long_click = { session?.write("-") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("HOME", false, { write_terminal_key(session, ctrl_active, alt_active, "H") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("↑", false, { write_terminal_key(session, ctrl_active, alt_active, "A") }, on_long_click = { write_terminal_key(session, ctrl_active, alt_active, "A") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("END", false, { write_terminal_key(session, ctrl_active, alt_active, "F") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("PGUP", false, { write_terminal_page_key(session, ctrl_active, alt_active, true) }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(row_padding),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            terminal_key_button("TAB", false, { session?.write("\t") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("CTRL", ctrl_active, { on_ctrl_change(!ctrl_active) }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("ALT", alt_active, { on_alt_change(!alt_active) }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("←", false, { write_terminal_key(session, ctrl_active, alt_active, "D") }, on_long_click = { write_terminal_key(session, ctrl_active, alt_active, "D") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("↓", false, { write_terminal_key(session, ctrl_active, alt_active, "B") }, on_long_click = { write_terminal_key(session, ctrl_active, alt_active, "B") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("→", false, { write_terminal_key(session, ctrl_active, alt_active, "C") }, on_long_click = { write_terminal_key(session, ctrl_active, alt_active, "C") }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
            terminal_key_button("PGDN", false, { write_terminal_page_key(session, ctrl_active, alt_active, false) }, modifier = Modifier.weight(1f), text_size = text_size, content_padding = item_padding)
        }
    }
}

private fun write_terminal_key(session: TerminalSession?, ctrl_active: Boolean, alt_active: Boolean, code: String) {
    when {
        ctrl_active && alt_active -> session?.write("\u001bO8$code")
        ctrl_active -> session?.write("\u001bO5$code")
        alt_active -> session?.write("\u001bO3$code")
        else -> session?.write("\u001bO$code")
    }
}

private fun write_terminal_page_key(session: TerminalSession?, ctrl_active: Boolean, alt_active: Boolean, page_up: Boolean) {
    val base = if (page_up) "5" else "6"
    when {
        ctrl_active && alt_active -> session?.write("\u001b[$base;8~")
        ctrl_active -> session?.write("\u001b[$base;5~")
        alt_active -> session?.write("\u001b[$base;3~")
        else -> session?.write("\u001b[$base~")
    }
}

private fun create_terminal_view(
    context: Context,
    base_text_size: Int,
    ctrl_active: () -> Boolean,
    alt_active: () -> Boolean,
    on_ctrl_reset: () -> Unit,
    on_alt_reset: () -> Unit
): TerminalView {
    return TerminalView(context, null).apply {
        isFocusable = true
        isFocusableInTouchMode = true
        setTextSize(base_text_size)
        setTypeface(Typeface.MONOSPACE)
        setIsTerminalViewKeyLoggingEnabled(true)

        val view_client = object : TerminalViewClient {
            override fun onScale(scale: Float): Float {
                val new_scale = scale.coerceIn(1.0f, 10.0f)
                val new_text_size = (base_text_size * new_scale).toInt().coerceIn(12, 68)
                this@apply.setTextSize(new_text_size)
                this@apply.invalidate()
                return new_scale
            }

            override fun onSingleTapUp(e: MotionEvent) {
                this@apply.requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this@apply, InputMethodManager.SHOW_IMPLICIT)
            }

            override fun onKeyDown(key_code: Int, e: KeyEvent, session: TerminalSession): Boolean {
                return key_code == KeyEvent.KEYCODE_ENTER && !session.isRunning
            }

            override fun onKeyUp(key_code: Int, e: KeyEvent): Boolean = false
            override fun onLongPress(event: MotionEvent): Boolean {
                this@apply.startTextSelectionMode(event)
                return true
            }

            override fun copyModeChanged(copy_mode: Boolean) {}
            override fun shouldBackButtonBeMappedToEscape(): Boolean = false
            override fun shouldEnforceCharBasedInput(): Boolean = false
            override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
            override fun isTerminalViewSelected(): Boolean = true
            override fun readControlKey(): Boolean {
                val value = ctrl_active()
                on_ctrl_reset()
                return value
            }
            override fun readAltKey(): Boolean {
                val value = alt_active()
                on_alt_reset()
                return value
            }
            override fun readShiftKey(): Boolean = false
            override fun readFnKey(): Boolean = false
            override fun onCodePoint(code_point: Int, ctrl_down: Boolean, session: TerminalSession): Boolean = false
            override fun onEmulatorSet() {}
            override fun logError(tag: String, message: String) {}
            override fun logWarn(tag: String, message: String) {}
            override fun logInfo(tag: String, message: String) {}
            override fun logDebug(tag: String, message: String) {}
            override fun logVerbose(tag: String, message: String) {}
            override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
            override fun logStackTrace(tag: String, e: Exception) {}
        }
        setTerminalViewClient(view_client)
    }
}

private fun create_terminal_session(
    context: Context,
    shell_path: String,
    cwd: String,
    args: Array<String>,
    env: Array<String>,
    transcript_rows: Int,
    on_text_changed: (TerminalSession) -> Unit,
    on_session_finished: (TerminalSession) -> Unit
): TerminalSession {
    val session_client = object : TerminalSessionClient {
        override fun onTextChanged(session: TerminalSession) = on_text_changed(session)
        override fun onTitleChanged(session: TerminalSession) {}
        override fun onSessionFinished(session: TerminalSession) = on_session_finished(session)
        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("terminal", text))
        }
        override fun onPasteTextFromClipboard(session: TerminalSession?) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                session?.write(clip.getItemAt(0).text.toString())
            }
        }
        override fun onBell(session: TerminalSession) {}
        override fun onColorsChanged(session: TerminalSession) {}
        override fun onTerminalCursorStateChange(state: Boolean) {}
        override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
        override fun getTerminalCursorStyle(): Int = 2
        override fun logError(tag: String, message: String) {}
        override fun logWarn(tag: String, message: String) {}
        override fun logInfo(tag: String, message: String) {}
        override fun logDebug(tag: String, message: String) {}
        override fun logVerbose(tag: String, message: String) {}
        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
        override fun logStackTrace(tag: String, e: Exception) {}
    }

    return TerminalSession(shell_path, cwd, args, env, transcript_rows, session_client)
}

private fun refresh_terminal_view(view: TerminalView, colors: app_colors) {
    apply_terminal_colors(view, colors)
    view.post { apply_terminal_colors(view, colors) }
    view.postDelayed({ apply_terminal_colors(view, colors) }, 10)
}

private fun apply_terminal_colors(view: TerminalView, colors: app_colors) {
    view.setBackgroundColor(colors.terminal_background)
    try {
        val emulator = view.mEmulator ?: return
        emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_CURSOR] = colors.terminal_cursor
        emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND] = colors.terminal_foreground
        emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND] = colors.terminal_background
        view.invalidate()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
private fun terminal_key_button(
    text: String,
    is_active: Boolean,
    on_click: () -> Unit,
    on_long_click: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    text_size: TextUnit = 12.sp,
    content_padding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
) {
    val colors = app_theme_provider.colors
    var is_pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var long_press_job by remember { mutableStateOf<Job?>(null) }
    val background_color = if (is_pressed) colors.key_button_pressed_bg else Color.Transparent
    val text_color = when {
        is_pressed -> colors.key_button_pressed_text
        is_active -> colors.key_button_active_text
        else -> colors.key_button_normal_text
    }

    Box(
        modifier = modifier
            .padding(1.dp)
            .background(background_color, RoundedCornerShape(4.dp))
            .padding(content_padding)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        is_pressed = true
                        if (on_long_click != null) {
                            long_press_job = scope.launch {
                                delay(500)
                                while (true) {
                                    on_long_click()
                                    delay(100)
                                }
                            }
                        }
                        tryAwaitRelease()
                        long_press_job?.cancel()
                        long_press_job = null
                        is_pressed = false
                    },
                    onTap = { on_click() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = text_color,
            fontSize = text_size,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
