package com.xc.code.ui.screens.editor

import android.view.ViewGroup
import android.widget.FrameLayout
import com.xc.code.editor.model.editor_tab_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.xc.code.ui.theme.app_theme_provider
import io.github.rosemoe.sora.widget.CodeEditor

@Composable
fun editor_tabs_bar(
    tabs: List<editor_tab_item>,
    selected_tab_path: String?,
    toolbar_visible: Boolean,
    on_toggle_toolbar: () -> Unit,
    on_select_tab: (String) -> Unit,
    on_pin_tab: (String) -> Unit,
    on_close_tab: (String) -> Unit,
    on_close_other_tabs: (String) -> Unit,
    on_close_all_tabs: () -> Unit
) {

    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp, max = 34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(38.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = on_toggle_toolbar
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (toolbar_visible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (toolbar_visible) "隐藏工具栏" else "显示工具栏",
                tint = colors.editor_tab_add_icon,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tabs.isEmpty()) {
                Text(
                    text = "未打开任何文件",
                    color = colors.editor_tab_unselected_content,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            tabs.forEachIndexed { index, tab ->
                val selected = tab.path == selected_tab_path
                val icon_color = if (selected) colors.editor_tab_selected_icon else colors.editor_tab_unselected_content
                val text_color = if (selected) colors.editor_tab_selected_text else colors.editor_tab_unselected_content
                val can_close_tab = !tab.pinned
                val has_closable_others = tabs.any { it.path != tab.path && !it.pinned }
                var menu_expanded by remember(tab.path) { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .widthIn(min = 104.dp, max = 240.dp)
                        .fillMaxHeight()
                        .background(if (selected) colors.editor_tab_selected_bg else colors.editor_tab_unselected_bg)
                        .drawWithContent {
                            drawContent()
                            if (selected) {
                                val indicator_height = 2.dp.toPx()
                                drawLine(
                                    color = colors.editor_tab_selected_icon,
                                    start = Offset(0f, indicator_height / 2f),
                                    end = Offset(size.width, indicator_height / 2f),
                                    strokeWidth = indicator_height
                                )
                            }
                            if (!selected && index < tabs.lastIndex) {
                                val stroke_width = 1.dp.toPx()
                                val separator_height = 16.dp.toPx()
                                val x = size.width - stroke_width / 2f
                                val y = (size.height - separator_height) / 2f
                                drawLine(
                                    color = colors.editor_tab_separator,
                                    start = Offset(x, y),
                                    end = Offset(x, y + separator_height),
                                    strokeWidth = stroke_width
                                )
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!selected) {
                                on_select_tab(tab.path)
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp, end = 34.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tab.pinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "已置顶",
                                tint = colors.editor_icon,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Icon(
                            painter = painterResource(editor_file_icon_res(tab.title)),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .padding(start = if (tab.pinned) 4.dp else 0.dp)
                                .size(14.dp)
                        )

                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            color = text_color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .widthIn(max = 150.dp)
                        )

                        if (tab.has_changes) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2F80FF))
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp)
                    ) {
                        IconButton(
                            onClick = { menu_expanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "标签菜单",
                                tint = colors.editor_tab_unselected_content,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menu_expanded,
                            onDismissRequest = { menu_expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (tab.pinned) "取消置顶" else "置顶") },
                                onClick = {
                                    menu_expanded = false
                                    on_pin_tab(tab.path)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("关闭当前") },
                                enabled = can_close_tab,
                                onClick = {
                                    menu_expanded = false
                                    on_close_tab(tab.path)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("关闭其他") },
                                enabled = has_closable_others,
                                onClick = {
                                    menu_expanded = false
                                    on_close_other_tabs(tab.path)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editor_top_bar(
    title: String,
    subtitle: String,
    read_only: Boolean,
    has_open_file: Boolean,
    search_visible: Boolean,
    on_toggle_drawer: () -> Unit,
    on_toggle_search: () -> Unit,
    on_build: () -> Unit,
    on_configure_cmake: () -> Unit,
    build_running: Boolean,
    build_stopping: Boolean,
    on_toggle_read_only: () -> Unit
) {

    val colors = app_theme_provider.colors
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = on_toggle_drawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "侧边栏",
                    tint = colors.editor_toolbar_icon
                )
            }
        },
        actions = {
            if (build_running) {
                IconButton(onClick = on_build) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = if (build_stopping) "正在停止" else "停止",
                        tint = Color(0xFFFF5252)
                    )
                }
            } else {
                IconButton(onClick = on_build) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "编译",
                        tint = colors.success
                    )
                }
                IconButton(onClick = on_configure_cmake) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "初始化 CMake",
                        tint = colors.editor_toolbar_icon
                    )
                }
            }
            if (has_open_file) {
                IconButton(onClick = on_toggle_read_only) {
                    Icon(
                        imageVector = if (read_only) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "只读",
                        tint = if (read_only) colors.editor_icon else colors.editor_toolbar_icon
                    )
                }
                IconButton(onClick = on_toggle_search) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = if (search_visible) colors.editor_icon else colors.editor_toolbar_icon
                    )
                }
            }
        },
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.editor_bg)
    )
}

@Composable
fun code_editor_panel(
    editor: CodeEditor,
    modifier: Modifier = Modifier,
    on_focus_change: (Boolean) -> Unit = {}
) {
    val current_on_focus_change by rememberUpdatedState(on_focus_change)

    DisposableEffect(editor) {
        editor.setOnFocusChangeListener { _, has_focus ->
            current_on_focus_change(has_focus)
        }
        current_on_focus_change(editor.isFocused)
        onDispose {
            editor.setOnFocusChangeListener(null)
        }
    }

    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier,
        update = { container ->
            if (editor.parent !== container) {
                (editor.parent as? ViewGroup)?.removeView(editor)
                container.removeAllViews()
                container.addView(
                    editor,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
            current_on_focus_change(editor.isFocused)
        }
    )
}
