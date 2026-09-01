package com.jmwl.gostudio.ui.screens.editor

import com.jmwl.gostudio.editor.core.editor_outline_symbol
import com.jmwl.gostudio.editor.model.editor_file_node
import com.jmwl.gostudio.project.project_ide_config

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.project_file_tree.project_file_tree_colors
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.theme.motion

/** 左侧抽屉的工具页签。六个工具统一放在顶部，可横向滑动发现。 */
internal enum class editor_sidebar_tool {
    FILE,
    SEARCH,
    STRUCTURE,
    GIT,
    LOG,
    TERMINAL
}

private data class editor_tool_item(
    val tool: editor_sidebar_tool,
    val icon: ImageVector,
    val label: String
)

@Composable
internal fun editor_sidebar(
    drawer_width: Dp,
    drawer_offset_px: Int,
    selected_tool: editor_sidebar_tool,
    output_panel_state: editor_output_panel_state,
    terminal_state: editor_terminal_state,
    terminal_cwd: String,
    terminal_extra_environment: Map<String, String>,
    project_root_path: String,
    file_nodes: List<editor_file_node>,
    expanded_paths: Set<String>,
    file_tree_loading: Boolean,
    project_exists: Boolean,
    structure_file_name: String?,
    structure_symbols: List<editor_outline_symbol>,
    on_structure_navigate: (Int) -> Unit,
    on_project_search: suspend (String) -> List<editor_project_search_hit>,
    file_diagnostics: List<com.jmwl.gostudio.lsp.gopls.gopls_diagnostic>,
    on_diagnostic_click: (com.jmwl.gostudio.lsp.gopls.gopls_diagnostic) -> Unit,
    on_tool_selected: (editor_sidebar_tool) -> Unit,
    on_new_file: (String) -> Unit,
    on_new_folder: (String) -> Unit,
    on_refresh: (String) -> Unit,
    on_rename_node: (String, String) -> Unit,
    on_delete_node: (String) -> Unit,
    on_directory_click: (String) -> Unit,
    on_file_click: (String) -> Unit,
    on_file_position_click: (String, Int, Int) -> Unit,
    on_drag: (Offset) -> Unit
) {

    val colors = app_theme_provider.colors
    // 顶部工具：Git 后面还有日志/终端，窄屏时左右滑动即可发现
    val tools = remember {
        listOf(
            editor_tool_item(editor_sidebar_tool.FILE, Icons.Default.Folder, "文件"),
            editor_tool_item(editor_sidebar_tool.SEARCH, Icons.Default.Search, "搜索"),
            editor_tool_item(editor_sidebar_tool.STRUCTURE, Icons.Default.AccountTree, "结构"),
            editor_tool_item(editor_sidebar_tool.GIT, Icons.Default.CallSplit, "Git"),
            editor_tool_item(editor_sidebar_tool.LOG, Icons.AutoMirrored.Filled.ListAlt, "日志"),
            editor_tool_item(editor_sidebar_tool.TERMINAL, Icons.Default.Terminal, "终端")
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(drawer_width)
            .offset { IntOffset(drawer_offset_px, 0) },
        color = colors.editor_bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // 分段切换器置顶（参考 CodeAssist SegmentedPanelSwitcher）
                editor_sidebar_segmented_switcher(
                    tools = tools,
                    selected = selected_tool,
                    on_select = on_tool_selected
                )

                // 面板内容：淡入淡出切换（key 用稳定枚举，避免重组重启动画）
                AnimatedContent(
                    targetState = selected_tool,
                    transitionSpec = { fadeIn(tween(motion.BASE)) togetherWith fadeOut(tween(motion.FAST)) },
                    label = "sidebarPanelSwitch",
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { tool ->
                    Box(Modifier.fillMaxSize()) {
                        when (tool) {
                            editor_sidebar_tool.FILE -> file_tree_panel(
                                nodes = file_nodes,
                                project_root_path = project_root_path,
                                expanded_paths = expanded_paths,
                                loading = file_tree_loading,
                                project_exists = project_exists,
                                on_new_file = on_new_file,
                                on_new_folder = on_new_folder,
                                on_refresh = on_refresh,
                                on_rename_node = on_rename_node,
                                on_delete_node = on_delete_node,
                                on_directory_click = on_directory_click,
                                on_file_click = on_file_click,
                                on_file_position_click = on_file_position_click,
                                colors = project_file_tree_colors(
                                    dialog_bg = colors.dialog_bg,
                                    editor_button_bg = colors.editor_button_bg,
                                    editor_divider = colors.editor_divider,
                                    editor_hint = colors.editor_hint,
                                    editor_icon = colors.editor_icon,
                                    editor_text = colors.editor_text,
                                    danger = colors.danger,
                                    danger_bg = colors.danger_bg
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                            editor_sidebar_tool.SEARCH -> editor_search_project_panel(
                                project_root_path = project_root_path,
                                on_search = on_project_search,
                                on_open_hit = { hit ->
                                    on_file_position_click(hit.path, hit.line - 1, 0)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            editor_sidebar_tool.STRUCTURE -> editor_structure_panel(
                                file_name = structure_file_name,
                                symbols = structure_symbols,
                                on_navigate = on_structure_navigate,
                                modifier = Modifier.fillMaxSize()
                            )
                            editor_sidebar_tool.GIT -> editor_git_panel(
                                project_root_path = project_root_path,
                                on_open_file = on_file_click,
                                modifier = Modifier.fillMaxSize()
                            )
                            editor_sidebar_tool.LOG -> editor_log_panel(
                                state = output_panel_state,
                                diagnostics = file_diagnostics,
                                on_diagnostic_click = on_diagnostic_click,
                                modifier = Modifier.fillMaxSize()
                            )
                            editor_sidebar_tool.TERMINAL -> editor_terminal_panel(
                                state = terminal_state,
                                cwd = terminal_cwd,
                                extra_environment = terminal_extra_environment,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.navigationBarsPadding())
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(28.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag_amount ->
                            change.consume()
                            on_drag(drag_amount)
                        }
                    },
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MoreVert, contentDescription = "调整宽度", tint = colors.editor_hint, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** 抽屉顶部工具分段切换器：固定宽度标签 + 横向滚动，后置工具可通过滑动发现。 */
@Composable
private fun editor_sidebar_segmented_switcher(
    tools: List<editor_tool_item>,
    selected: editor_sidebar_tool,
    on_select: (editor_sidebar_tool) -> Unit
) {
    val scroll_state = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(selected) {
        val index = tools.indexOfFirst { it.tool == selected }.coerceAtLeast(0)
        val item_width = with(density) { 78.dp.toPx() }
        val target = index * item_width
        scroll_state.animateScrollTo(target.toInt())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .horizontalScroll(scroll_state)
    ) {
        tools.forEach { tool ->
            val active = tool.tool == selected
            val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier = Modifier
                    .width(78.dp)
                    .fillMaxHeight()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { on_select(tool.tool) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.label,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = tool.label,
                    color = tint,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
