package com.jmwl.gostudio.ui.screens.editor

import com.jmwl.gostudio.editor.model.editor_tab_item
import com.jmwl.gostudio.editor.model.editor_file_node
import com.jmwl.gostudio.project.project_ide_config

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.dialogs.editor.editor_create_entry_dialog
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.roundToInt

private enum class editor_create_dialog_type { FILE, FOLDER }

private data class editor_create_dialog_request(
    val type: editor_create_dialog_type,
    val parent_path: String
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun editor_screen(
    project_name: String,
    project_root_path: String,
    editor: CodeEditor,
    current_file_name: String,
    /**
     * 光标状态以 provider/StateFlow 传入，在最小的叶子组合里读取：
     * 若直接以 Int/Boolean 参数传入，每次光标移动都会让整个 editor_screen 重组
     * （顶栏/标签栏/输出面板全部重新执行），造成主线程过慢、拖动光标卡顿。
     */
    cursor_line_provider: () -> Int,
    cursor_column_provider: () -> Int,
    cursor_selected_provider: () -> Boolean,
    can_goto_definition_flow: kotlinx.coroutines.flow.StateFlow<Boolean>,
    has_changes: Boolean,
    loading: Boolean,
    read_only: Boolean,
    can_undo: Boolean,
    can_redo: Boolean,
    file_nodes: List<editor_file_node>,
    expanded_paths: Set<String>,
    file_tree_loading: Boolean,
    project_exists: Boolean,
    has_open_file: Boolean,
    tabs: List<editor_tab_item>,
    selected_tab_path: String?,
    toolbar_visible: Boolean,
    output_panel_state: editor_output_panel_state,
    terminal_cwd: String,
    terminal_extra_environment: Map<String, String>,
    on_toggle_toolbar: () -> Unit,
    on_project_config_apply: (project_ide_config, () -> Unit) -> Unit,
    on_select_tab: (String) -> Unit,
    on_pin_tab: (String) -> Unit,
    on_close_tab: (String) -> Unit,
    on_close_other_tabs: (String) -> Unit,
    on_close_all_tabs: () -> Unit,
    on_build: () -> Unit,
    on_run: () -> Unit,
    on_save: () -> Unit,
    on_format: () -> Unit,
    on_toggle_read_only: () -> Unit,
    on_undo: () -> Unit,
    on_redo: () -> Unit,
    on_search_change: (String, Boolean, Boolean, Boolean) -> Boolean,
    on_search_previous: () -> Unit,
    on_search_next: () -> Unit,
    on_replace_current: (String) -> Unit,
    on_replace_all: (String) -> Unit,
    on_clear_search: () -> Unit,
    on_insert_symbol: (String) -> Unit,
    on_create_file: (String, String) -> Unit,
    on_create_folder: (String, String) -> Unit,
    on_refresh_files: (String) -> Unit,
    on_rename_file_tree_node: (String, String) -> Unit,
    on_delete_file_tree_node: (String) -> Unit,
    on_directory_click: (String) -> Unit,
    on_file_click: (String) -> Unit,
    on_file_position_click: (String, Int, Int) -> Unit,
    goto_definition_running: Boolean,
    on_goto_definition: () -> Unit,
    ai_agent: com.jmwl.gostudio.ai.ai_agent_loop? = null,
    on_open_ai_settings: () -> Unit = {},
    ai_current_provider: com.jmwl.gostudio.ai.ai_provider = com.jmwl.gostudio.ai.ai_provider.ZHIPU,
    ai_current_model: String = "",
    ai_available_models: Map<com.jmwl.gostudio.ai.ai_provider, List<String>> = emptyMap(),
    ai_configured_providers: Set<com.jmwl.gostudio.ai.ai_provider> = emptySet(),
    on_ai_session_model_change: (com.jmwl.gostudio.ai.ai_provider, String) -> Unit = { _, _ -> },
    ai_global_prompts_dir: java.io.File? = null,
    ai_project_prompts_dir: java.io.File? = null,
    /** 递增触发器：外部想让 AI 弹窗打开时把这个值 +1 */
    ai_open_trigger: Int = 0
) {
    val colors = app_theme_provider.colors
    var drawer_open by remember { mutableStateOf(false) }
    var drawer_width by remember { mutableStateOf(300.dp) }
    var selected_tool by remember { mutableIntStateOf(0) }
    var search_visible by remember { mutableStateOf(false) }
    var search_query by remember { mutableStateOf("") }
    var search_replace_text by remember { mutableStateOf("") }
    var search_expanded by remember { mutableStateOf(false) }
    var search_match_case by remember { mutableStateOf(false) }
    var search_whole_word by remember { mutableStateOf(false) }
    var search_regex by remember { mutableStateOf(false) }
    var search_has_match by remember { mutableStateOf(false) }
    var search_panel_offset by remember { mutableStateOf(Offset.Zero) }
    var create_dialog_request by remember { mutableStateOf<editor_create_dialog_request?>(null) }
    var show_ai_sheet by remember { mutableStateOf(false) }
    // 外部触发打开 AI 弹窗（编辑器选区 AI 动作）
    LaunchedEffect(ai_open_trigger) {
        if (ai_open_trigger > 0) show_ai_sheet = true
    }
    var editor_focused by remember { mutableStateOf(false) }
    val terminal_state = remember_editor_terminal_state()
    val drawer_progress = remember { Animatable(0f) }
    val safe_project_name = project_name.ifBlank { "项目" }
    val density = LocalDensity.current
    val drawer_width_px = with(density) { drawer_width.toPx() }
    val drawer_progress_value = drawer_progress.value
    val editor_offset_px = (drawer_width_px * drawer_progress_value).roundToInt()
    val sidebar_offset_px = (-(drawer_width_px * (1f - drawer_progress_value))).roundToInt()
    val ime_visible = WindowInsets.ime.getBottom(density) > 0
    val show_symbol_bar = has_open_file && editor_focused && ime_visible

    LaunchedEffect(drawer_open) {
        if (drawer_open) {
            editor.clearFocus()
            editor_focused = false
        }
        drawer_progress.animateTo(
            targetValue = if (drawer_open) 1f else 0f,
            animationSpec = tween(220)
        )
    }

    LaunchedEffect(search_visible) {
        if (search_visible) {
            search_expanded = false
            search_panel_offset = Offset.Zero
        }
    }

    LaunchedEffect(has_open_file) {
        if (!has_open_file) {
            search_visible = false
        }
    }

    DisposableEffect(editor) {
        val receipt = editor.subscribeEvent(PublishSearchResultEvent::class.java, EventReceiver { event, _ ->
            search_has_match = runCatching {
                val searcher = event.getSearcher()
                searcher.hasQuery() && searcher.matchedPositionCount > 0
            }.getOrDefault(false)
        })
        onDispose { receipt.unsubscribe() }
    }

    LaunchedEffect(search_visible, has_open_file, search_query, search_match_case, search_whole_word, search_regex) {
        if (search_visible && has_open_file) {
            search_has_match = on_search_change(search_query, search_match_case, search_whole_word, search_regex)
        } else {
            search_has_match = false
            on_clear_search()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(editor_offset_px, 0) }
        ) {
            editor_output_bottom_sheet_scaffold(
                state = output_panel_state,
                terminal_state = terminal_state,
                terminal_cwd = terminal_cwd,
                terminal_extra_environment = terminal_extra_environment,
                show_symbol_bar = show_symbol_bar,
                symbol_bar = {
                    editor_symbol_bar(
                        on_insert = on_insert_symbol
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { output_inner_padding, _ ->
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(output_inner_padding),
                topBar = {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        AnimatedVisibility(
                            visible = toolbar_visible,
                            enter = slideInVertically(
                                animationSpec = tween(220),
                                initialOffsetY = { -it }
                            ) + expandVertically(
                                animationSpec = tween(220),
                                expandFrom = Alignment.Top
                            ),
                            exit = slideOutVertically(
                                animationSpec = tween(220),
                                targetOffsetY = { -it }
                            ) + shrinkVertically(
                                animationSpec = tween(220),
                                shrinkTowards = Alignment.Top
                            )
                        ) {
                            editor_top_bar(
                                title = safe_project_name,
                                subtitle = current_file_name,
                                read_only = read_only,
                                has_open_file = has_open_file,
                                search_visible = search_visible,
                                on_toggle_drawer = { drawer_open = !drawer_open },
                                on_toggle_search = { search_visible = !search_visible },
                                on_build = on_build,
                                on_run = on_run,
                                build_running = output_panel_state.task_running,
                                build_stopping = output_panel_state.task_stopping,
                                on_toggle_read_only = on_toggle_read_only,
                                on_open_ai = { show_ai_sheet = true }
                            )
                        }
                        if (has_open_file) {
                            editor_tabs_bar(
                                tabs = tabs,
                                selected_tab_path = selected_tab_path,
                                toolbar_visible = toolbar_visible,
                                on_toggle_toolbar = on_toggle_toolbar,
                                on_select_tab = on_select_tab,
                                on_pin_tab = on_pin_tab,
                                on_close_tab = on_close_tab,
                                on_close_other_tabs = on_close_other_tabs,
                                on_close_all_tabs = on_close_all_tabs
                            )
                        }
                    }
                },
                contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                containerColor = colors.editor_bg
            ) { inner_padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner_padding)
                        .background(colors.editor_bg)
                ) {
                    if (has_open_file) {
                        code_editor_panel(
                            editor = editor,
                            modifier = Modifier.fillMaxSize(),
                            on_focus_change = { focused -> editor_focused = focused }
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            // 光标相关状态全部在这个叶子组合里读取：
                            // 光标移动只重组这一小块，不会带动整个 editor_screen
                            editor_cursor_overlays(
                                cursor_line_provider = cursor_line_provider,
                                cursor_column_provider = cursor_column_provider,
                                cursor_selected_provider = cursor_selected_provider,
                                can_goto_definition_flow = can_goto_definition_flow,
                                goto_definition_running = goto_definition_running,
                                on_goto_definition = on_goto_definition,
                                can_undo = can_undo,
                                can_redo = can_redo,
                                has_changes = has_changes,
                                can_format = !read_only,
                                show_format = true,
                                show_save = true,
                                on_undo = on_undo,
                                on_redo = on_redo,
                                on_format = on_format,
                                on_save = on_save
                            )
                        }

                        AnimatedVisibility(
                            visible = search_visible,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .offset {
                                    IntOffset(
                                        search_panel_offset.x.roundToInt(),
                                        search_panel_offset.y.roundToInt()
                                    )
                                }
                        ) {
                            editor_search_panel(
                                query = search_query,
                                replacement = search_replace_text,
                                expanded = search_expanded,
                                match_case = search_match_case,
                                whole_word = search_whole_word,
                                regex = search_regex,
                                has_match = search_has_match,
                                replace_enabled = !read_only,
                                on_query_change = { search_query = it },
                                on_replacement_change = { search_replace_text = it },
                                on_expanded_change = { search_expanded = it },
                                on_match_case_change = { search_match_case = it },
                                on_whole_word_change = { enabled ->
                                    search_whole_word = enabled
                                    if (enabled) search_regex = false
                                },
                                on_regex_change = { enabled ->
                                    search_regex = enabled
                                    if (enabled) search_whole_word = false
                                },
                                on_previous = on_search_previous,
                                on_next = on_search_next,
                                on_replace_current = { on_replace_current(search_replace_text) },
                                on_replace_all = { on_replace_all(search_replace_text) },
                                on_close = { search_visible = false },
                                on_drag = { drag_amount -> search_panel_offset += drag_amount }
                            )
                        }
                    } else {
                        empty_editor_placeholder(
                            text = if (loading) "正在加载..." else "未打开任何文件",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        }

        if (drawer_open || drawer_progress_value > 0f) {
            editor_sidebar(
                drawer_width = drawer_width,
            drawer_offset_px = sidebar_offset_px,
            selected_tool = selected_tool,
            project_root_path = project_root_path,
            file_nodes = file_nodes,
            expanded_paths = expanded_paths,
            file_tree_loading = file_tree_loading,
            project_exists = project_exists,
            on_tool_selected = { selected_tool = it },
            on_project_config_apply = on_project_config_apply,
            on_new_file = { parent_path ->
                create_dialog_request = editor_create_dialog_request(editor_create_dialog_type.FILE, parent_path)
            },
            on_new_folder = { parent_path ->
                create_dialog_request = editor_create_dialog_request(editor_create_dialog_type.FOLDER, parent_path)
            },
            on_refresh = on_refresh_files,
            on_rename_node = on_rename_file_tree_node,
            on_delete_node = on_delete_file_tree_node,
            on_directory_click = on_directory_click,
            on_file_click = { path ->
                drawer_open = false
                on_file_click(path)
            },
            on_file_position_click = { path, line, column ->
                drawer_open = false
                on_file_position_click(path, line, column)
            },
            on_drag = { drag_amount ->
                drawer_width = (drawer_width.value + drag_amount.x).coerceIn(300f, 480f).dp
            }
        )
        }

        if (drawer_open) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = drawer_width)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { drawer_open = false })
                    }
            )
        }


        create_dialog_request?.let { request ->
            editor_create_entry_dialog(
                is_folder = request.type == editor_create_dialog_type.FOLDER,
                on_confirm = { name ->
                    create_dialog_request = null
                    if (request.type == editor_create_dialog_type.FOLDER) {
                        on_create_folder(request.parent_path, name)
                    } else {
                        on_create_file(request.parent_path, name)
                    }
                },
                on_dismiss = { create_dialog_request = null }
            )
        }

        // 底部 AI 助手弹窗（类 iOS 大页面弹窗）
        if (show_ai_sheet) {
            val agent = ai_agent
            if (agent != null) {
                val ai_sheet_state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { show_ai_sheet = false },
                    sheetState = ai_sheet_state,
                    containerColor = colors.editor_bg,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .background(
                                    colors.editor_hint.copy(alpha = 0.4f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                                )
                        )
                    }
                ) {
                    com.jmwl.gostudio.ui.screens.ai.ai_chat_panel(
                        agent = agent,
                        on_open_settings = {
                            show_ai_sheet = false
                            on_open_ai_settings()
                        },
                        current_provider = ai_current_provider,
                        current_model = ai_current_model,
                        available_models = ai_available_models,
                        configured_providers = ai_configured_providers,
                        on_session_model_change = on_ai_session_model_change,
                        project_dir = java.io.File(project_root_path),
                        global_prompts_dir = ai_global_prompts_dir,
                        project_prompts_dir = ai_project_prompts_dir,
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f)
                    )
                }
            } else {
                show_ai_sheet = false
            }
        }
    }
}

/**
 * 光标区叶子组合：行号 chip、跳转定义 chip、悬浮按钮。
 * 光标状态（行/列/选区/可跳转）只在这里读取，光标每次移动仅重组此组合，
 * 避免整个 editor_screen（顶栏/标签栏/输出面板）跟着重新执行。
 */
@Composable
private fun editor_cursor_overlays(
    cursor_line_provider: () -> Int,
    cursor_column_provider: () -> Int,
    cursor_selected_provider: () -> Boolean,
    can_goto_definition_flow: kotlinx.coroutines.flow.StateFlow<Boolean>,
    goto_definition_running: Boolean,
    on_goto_definition: () -> Unit,
    can_undo: Boolean,
    can_redo: Boolean,
    has_changes: Boolean,
    can_format: Boolean,
    show_format: Boolean,
    show_save: Boolean,
    on_undo: () -> Unit,
    on_redo: () -> Unit,
    on_format: () -> Unit,
    on_save: () -> Unit
) {
    cursor_chip(
        line = cursor_line_provider(),
        column = cursor_column_provider()
    )

    val can_goto by can_goto_definition_flow.collectAsState()
    AnimatedVisibility(visible = can_goto) {
        goto_definition_chip(
            running = goto_definition_running,
            on_click = on_goto_definition
        )
    }

    editor_floating_actions(
        can_undo = can_undo,
        can_redo = can_redo,
        has_changes = has_changes,
        can_format = can_format,
        format_selection = cursor_selected_provider(),
        show_format = show_format,
        show_save = show_save,
        on_undo = on_undo,
        on_redo = on_redo,
        on_format = on_format,
        on_save = on_save
    )
}