package com.jmwl.gostudio.ui.screens.editor

import android.view.ViewGroup
import android.widget.FrameLayout
import com.jmwl.gostudio.editor.core.R
import com.jmwl.gostudio.editor.model.editor_tab_item

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jmwl.gostudio.ui.theme.app_theme_provider
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
    on_close_all_tabs: () -> Unit,
    modifier: Modifier = Modifier
) {

    val colors = app_theme_provider.colors
    Row(
        modifier = modifier
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
                                    .background(colors.editor_tab_selected_icon)
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

/**
 * 工作区顶栏（对齐 CodeAssist EditorTopBar compact 版式）：
 * 左侧迷你屏抽屉切换图标（随抽屉开度实时动画）· 项目名 · 保存 · 运行 · AI · 更多菜单。
 * 项目配置、只读收纳进「更多」下拉菜单；搜索、格式化保留在编辑器右上角。
 */
@Composable
fun editor_top_bar(
    project_name: String,
    has_changes: Boolean,
    on_save: () -> Unit,
    read_only: Boolean,
    has_open_file: Boolean,
    on_toggle_drawer: () -> Unit,
    drawer_fraction: () -> Float = { 0f },
    on_build: () -> Unit,
    on_run: () -> Unit,
    on_test: () -> Unit = {},
    build_running: Boolean,
    build_stopping: Boolean,
    on_toggle_read_only: () -> Unit = {},
    on_open_ai: () -> Unit = {},
    on_open_project_config: () -> Unit = {}
) {
    val colors = app_theme_provider.colors
    val accent = MaterialTheme.colorScheme.primary
    var run_menu_open by remember { mutableStateOf(false) }
    var more_menu_open by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            editor_sidebar_toggle_button(
                fraction = drawer_fraction,
                on_click = on_toggle_drawer
            )

            // 项目名占据弹性中部，超长截断，右侧按钮簇不被挤压
            Text(
                text = project_name,
                color = colors.editor_text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )

            // 保存：有未保存改动时点亮主色
            editor_top_bar_icon_button(
                icon = Icons.Default.Save,
                content_description = "保存",
                tint = if (has_changes) accent else colors.editor_toolbar_icon,
                on_click = on_save
            )

            // 运行/构建/测试 收进下拉菜单（参考 CodeAssist RunControl）；任务运行中显示停止按钮
            if (build_running) {
                editor_top_bar_icon_button(
                    icon = Icons.Default.Stop,
                    content_description = if (build_stopping) "正在停止" else "停止",
                    tint = colors.danger,
                    on_click = on_build
                )
            } else {
                Box {
                    editor_top_bar_icon_button(
                        icon = Icons.Default.PlayArrow,
                        content_description = "运行菜单",
                        tint = accent,
                        on_click = { run_menu_open = true }
                    )
                    DropdownMenu(
                        expanded = run_menu_open,
                        onDismissRequest = { run_menu_open = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("运行 (go run)") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp), tint = accent) },
                            onClick = { run_menu_open = false; on_run() }
                        )
                        DropdownMenuItem(
                            text = { Text("构建 (go build)") },
                            leadingIcon = { Icon(Icons.Default.Build, null, Modifier.size(16.dp), tint = colors.editor_toolbar_icon) },
                            onClick = { run_menu_open = false; on_build() }
                        )
                        DropdownMenuItem(
                            text = { Text("测试 (go test)") },
                            leadingIcon = { Icon(Icons.Default.Science, null, Modifier.size(16.dp), tint = colors.editor_icon) },
                            onClick = { run_menu_open = false; on_test() }
                        )
                    }
                }
            }

            editor_top_bar_icon_button(
                icon = Icons.Default.AutoAwesome,
                content_description = "AI 助手",
                tint = colors.editor_toolbar_icon,
                on_click = on_open_ai
            )

            Box {
                editor_top_bar_icon_button(
                    icon = Icons.Default.MoreVert,
                    content_description = "更多",
                    tint = colors.editor_toolbar_icon,
                    on_click = { more_menu_open = true }
                )
                DropdownMenu(
                    expanded = more_menu_open,
                    onDismissRequest = { more_menu_open = false }
                ) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("项目配置") },
                        leadingIcon = { Icon(Icons.Default.Tune, null, Modifier.size(16.dp), tint = colors.editor_toolbar_icon) },
                        onClick = { more_menu_open = false; on_open_project_config() }
                    )
                    if (has_open_file) {
                        DropdownMenuItem(
                            text = { Text(if (read_only) "退出只读" else "只读模式") },
                            leadingIcon = {
                                Icon(
                                    if (read_only) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, Modifier.size(16.dp),
                                    tint = if (read_only) accent else colors.editor_toolbar_icon
                                )
                            },
                            onClick = { more_menu_open = false; on_toggle_read_only() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun editor_top_bar_icon_button(
    icon: ImageVector,
    content_description: String,
    tint: Color,
    on_click: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = on_click
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = content_description,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(21.dp)
        )
    }
}

/**
 * 抽屉切换按钮：一枚「迷你屏幕」图标，线框内的分隔线即抽屉边缘——
 * 随抽屉开度 0→1 向右滑动并染上主色（手势拖动抽屉时图标实时跟随）。
 * 移植自 CodeAssist SidebarToggleButton；fraction 以 lambda 传入、
 * 仅在绘制阶段读取，抽屉动画不会触发顶栏重组。
 */
@Composable
private fun editor_sidebar_toggle_button(
    fraction: () -> Float,
    on_click: () -> Unit
) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = on_click
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 19.dp, height = 15.dp)) {
            val f = fraction().coerceIn(0f, 1f)
            val stroke = 1.5.dp.toPx()
            val inset = stroke / 2f
            val corner = CornerRadius(3.5.dp.toPx())
            // 分隔线 = 抽屉边缘：随开度右移（风格化的行程，保证 19dp 内可读）
            val divider = inset + (size.width - 2 * inset) * (0.34f + 0.30f * f)
            val frame = Path().apply {
                addRoundRect(
                    RoundRect(
                        inset,
                        inset,
                        size.width - inset,
                        size.height - inset,
                        corner
                    )
                )
            }
            clipPath(frame) {
                drawRect(
                    color = lerp(outline.copy(alpha = 0.32f), accent, f),
                    topLeft = Offset(inset, inset),
                    size = Size(divider - inset, size.height - 2 * inset)
                )
            }
            drawRoundRect(
                color = outline,
                topLeft = Offset(inset, inset),
                size = Size(size.width - 2 * inset, size.height - 2 * inset),
                cornerRadius = corner,
                style = Stroke(stroke)
            )
            drawLine(outline, Offset(divider, inset), Offset(divider, size.height - inset), stroke)
        }
    }
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
