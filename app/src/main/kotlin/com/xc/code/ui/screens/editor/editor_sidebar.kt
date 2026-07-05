package com.xc.code.ui.screens.editor

import com.xc.code.editor.model.editor_settings_state
import com.xc.code.editor.model.editor_file_node
import com.xc.code.project.project_ide_config

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.project_file_tree.project_file_tree_colors
import com.xc.code.ui.theme.app_theme_provider

private data class editor_tool_item(
    val icon: ImageVector,
    val label: String
)

@Composable
fun editor_sidebar(
    drawer_width: Dp,
    drawer_offset_px: Int,
    selected_tool: Int,
    project_root_path: String,
    file_nodes: List<editor_file_node>,
    expanded_paths: Set<String>,
    file_tree_loading: Boolean,
    project_exists: Boolean,
    editor_settings: editor_settings_state,
    on_tool_selected: (Int) -> Unit,
    on_editor_settings_change: (editor_settings_state) -> Unit,
    on_project_config_apply: (project_ide_config, () -> Unit) -> Unit,
    on_import_editor_font: () -> Unit,
    on_open_editor_theme_settings: () -> Unit,
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
    val tools = remember {
        listOf(
            editor_tool_item(Icons.Default.Folder, "文件"),
            editor_tool_item(Icons.Default.Tune, "项目配置"),
            editor_tool_item(Icons.Outlined.AutoAwesome, "助手"),
            editor_tool_item(Icons.Default.Settings, "设置")
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                        .heightIn(min = 48.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tools.forEachIndexed { index, tool ->
                        val selected = selected_tool == index
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) colors.editor_sidebar_selected_bg else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = {
                                        if (!selected) on_tool_selected(index)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.label,
                                tint = if (selected) colors.editor_icon else colors.editor_hint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    color = colors.editor_divider,
                    thickness = 0.5.dp
                )

                when (selected_tool) {
                    0 -> file_tree_panel(
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
                    1 -> editor_project_config_panel(
                        project_root_path = project_root_path,
                        on_apply = on_project_config_apply,
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> sidebar_placeholder("助手")
                    3 -> editor_settings_panel(
                        settings = editor_settings,
                        on_settings_change = on_editor_settings_change,
                        on_import_font = on_import_editor_font,
                        on_open_theme_settings = on_open_editor_theme_settings,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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


@Composable
private fun sidebar_placeholder(title: String) {
    val colors = app_theme_provider.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Description, contentDescription = null, tint = colors.editor_hint, modifier = Modifier.size(28.dp))
            Text(title, color = colors.editor_text, fontSize = 14.sp)
            Text("待接入", color = colors.editor_hint, fontSize = 12.sp)
        }
    }
}
