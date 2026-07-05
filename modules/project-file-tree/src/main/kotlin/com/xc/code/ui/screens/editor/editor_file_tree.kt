package com.xc.code.ui.screens.editor

import com.xc.code.editor.model.editor_file_node

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.project_file_tree.R
import com.xc.code.project_file_tree.project_file_tree_colors
import com.xc.code.ui.dialogs.editor.editor_file_tree_action_sheet
import com.xc.code.ui.dialogs.editor.editor_file_tree_delete_sheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val FILE_TREE_SEARCH_RESULT_LIMIT = 24
private const val FILE_TREE_SEARCH_SCAN_LIMIT = 1500
private const val FILE_TREE_CONTENT_MAX_BYTES = 256 * 1024L

private enum class file_tree_search_mode { FILE, CONTENT }

private data class file_tree_search_result(
    val name: String,
    val path: String,
    val detail: String,
    val line: Int? = null,
    val column: Int = 0
)

private data class file_tree_content_match(
    val line: Int,
    val column: Int,
    val preview: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun file_tree_panel(
    nodes: List<editor_file_node>,
    project_root_path: String,
    expanded_paths: Set<String>,
    loading: Boolean,
    project_exists: Boolean,
    on_new_file: (String) -> Unit,
    on_new_folder: (String) -> Unit,
    on_refresh: (String) -> Unit,
    on_rename_node: (String, String) -> Unit,
    on_delete_node: (String) -> Unit,
    on_directory_click: (String) -> Unit,
    on_file_click: (String) -> Unit,
    on_file_position_click: (String, Int, Int) -> Unit,
    colors: project_file_tree_colors,
    modifier: Modifier = Modifier
) {
    val focus_manager = LocalFocusManager.current
    val horizontal_scroll = rememberScrollState()
    val max_depth = nodes.maxOfOrNull { it.depth } ?: 0
    val tree_content_width = (520 + max_depth * 24).dp
    var action_node by remember { mutableStateOf<editor_file_node?>(null) }
    var delete_node by remember { mutableStateOf<editor_file_node?>(null) }
    var editing_path by remember { mutableStateOf<String?>(null) }
    var editing_name by remember { mutableStateOf("") }
    var panel_bounds by remember { mutableStateOf<Rect?>(null) }
    var rename_field_bounds by remember { mutableStateOf<Rect?>(null) }
    var search_box_bounds by remember { mutableStateOf<Rect?>(null) }
    var file_search_query by remember { mutableStateOf("") }
    var file_search_mode by remember { mutableStateOf(file_tree_search_mode.FILE) }
    var file_search_results by remember { mutableStateOf<List<file_tree_search_result>>(emptyList()) }
    val trimmed_file_search_query = file_search_query.trim()

    fun cancel_rename() {
        editing_path = null
        editing_name = ""
        rename_field_bounds = null
    }

    LaunchedEffect(editing_path) {
        if (editing_path == null) {
            rename_field_bounds = null
        }
    }

    LaunchedEffect(project_root_path, project_exists, trimmed_file_search_query, file_search_mode) {
        if (!project_exists || trimmed_file_search_query.isBlank()) {
            file_search_results = emptyList()
            return@LaunchedEffect
        }
        file_search_results = emptyList()
        file_search_results = withContext(Dispatchers.IO) {
            runCatching {
                search_file_tree(project_root_path, trimmed_file_search_query, file_search_mode)
            }.getOrDefault(emptyList())
        }
    }

    if (!project_exists) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("项目目录不存在", color = colors.editor_hint, fontSize = 13.sp)
        }
        return
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { panel_bounds = it.boundsInRoot() }
            .pointerInput(editing_path, rename_field_bounds, search_box_bounds, panel_bounds) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
                        if (down != null) {
                            val panel = panel_bounds
                            val root_position = if (panel != null) {
                                Offset(down.position.x + panel.left, down.position.y + panel.top)
                            } else {
                                down.position
                            }

                            val input_bounds = rename_field_bounds
                            val clicked_rename_field = editing_path != null && input_bounds != null && input_bounds.contains(root_position)

                            val search_bounds = search_box_bounds
                            if (!clicked_rename_field && search_bounds != null && !search_bounds.contains(root_position)) {
                                focus_manager.clearFocus()
                            }

                            if (editing_path != null && !clicked_rename_field) {
                                event.changes.forEach { it.consume() }
                                cancel_rename()
                            }
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            file_tree_search_panel(
                query = file_search_query,
                mode = file_search_mode,
                results = file_search_results,
                on_query_change = { file_search_query = it },
                on_mode_change = { file_search_mode = it },
                on_search_box_bounds_change = { search_box_bounds = it },
                on_result_click = { result ->
                    val line = result.line
                    if (line != null) {
                        on_file_position_click(result.path, line, result.column)
                    } else {
                        on_file_click(result.path)
                    }
                },
                colors = colors,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (nodes.isEmpty() && !loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无文件", color = colors.editor_hint, fontSize = 13.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontal_scroll)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(tree_content_width),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(nodes, key = { it.path }) { node ->
                                file_tree_row(
                                    node = node,
                                    expanded = node.path in expanded_paths,
                                    editing = editing_path == node.path,
                                    editing_name = editing_name,
                                    on_editing_name_change = { editing_name = it },
                                    on_confirm_rename = {
                                        val new_name = editing_name.trim()
                                        if (new_name.isNotEmpty() && new_name != node.name) {
                                            on_rename_node(node.path, new_name)
                                        }
                                        cancel_rename()
                                    },
                                    on_cancel_rename = ::cancel_rename,
                                    on_rename_field_bounds_change = { rename_field_bounds = it },
                                    on_new_file = on_new_file,
                                    on_new_folder = on_new_folder,
                                    on_refresh = on_refresh,
                                    colors = colors,
                                    on_click = {
                                        if (node.is_directory) {
                                            on_directory_click(node.path)
                                        } else {
                                            on_file_click(node.path)
                                        }
                                    },
                                    on_long_press = {
                                        if (node.depth > 0) {
                                            action_node = node
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                        color = colors.editor_icon,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        action_node?.let { node ->
            editor_file_tree_action_sheet(
                node = node,
                colors = colors,
                on_dismiss = { action_node = null },
                on_rename = {
                    editing_path = node.path
                    editing_name = node.name
                    action_node = null
                },
                on_delete = {
                    delete_node = node
                    action_node = null
                }
            )
        }

        delete_node?.let { node ->
            editor_file_tree_delete_sheet(
                node = node,
                colors = colors,
                on_dismiss = { delete_node = null },
                on_confirm = {
                    on_delete_node(node.path)
                    delete_node = null
                }
            )
        }

    }
}

@Composable
private fun file_tree_search_panel(
    query: String,
    mode: file_tree_search_mode,
    results: List<file_tree_search_result>,
    on_query_change: (String) -> Unit,
    on_mode_change: (file_tree_search_mode) -> Unit,
    on_search_box_bounds_change: (Rect?) -> Unit,
    on_result_click: (file_tree_search_result) -> Unit,
    colors: project_file_tree_colors,
    modifier: Modifier = Modifier
) {
    val input_shape = RoundedCornerShape(8.dp)
    var search_focused by remember { mutableStateOf(false) }
    val next_mode = if (mode == file_tree_search_mode.FILE) file_tree_search_mode.CONTENT else file_tree_search_mode.FILE
    val placeholder = if (mode == file_tree_search_mode.FILE) "搜索文件..." else "搜索内容..."

    DisposableEffect(Unit) {
        onDispose { on_search_box_bounds_change(null) }
    }

    Column(
        modifier = modifier.padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .onGloballyPositioned { on_search_box_bounds_change(it.boundsInRoot()) }
                .clip(input_shape)
                .background(colors.editor_button_bg)
                .then(if (search_focused) Modifier.border(1.dp, colors.editor_icon, input_shape) else Modifier)
                .padding(start = 9.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.editor_hint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isBlank()) {
                    Text(
                        text = placeholder,
                        color = colors.editor_hint,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = on_query_change,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.editor_text, fontSize = 12.sp),
                    cursorBrush = SolidColor(colors.editor_icon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { search_focused = it.isFocused }
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = { on_mode_change(next_mode) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = if (mode == file_tree_search_mode.FILE) "文件搜索" else "内容搜索",
                    tint = if (mode == file_tree_search_mode.CONTENT) colors.editor_icon else colors.editor_hint,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        if (results.isNotEmpty()) {
            file_tree_search_results_card(
                results = results,
                on_result_click = on_result_click,
                colors = colors,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun file_tree_search_results_card(
    results: List<file_tree_search_result>,
    on_result_click: (file_tree_search_result) -> Unit,
    colors: project_file_tree_colors,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier
            .heightIn(max = 168.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.editor_button_bg)
    ) {
        itemsIndexed(results, key = { _, result -> result.path }) { index, result ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 34.dp, end = 10.dp),
                        color = colors.editor_divider,
                        thickness = 0.5.dp
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = { on_result_click(result) }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(editor_file_icon_res(result.name)),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = result.name,
                            color = colors.editor_text,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val detail_parts = result.detail.split("\n", limit = 2)
                        Text(
                            text = detail_parts.first(),
                            color = colors.editor_hint,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        detail_parts.getOrNull(1)?.let { preview ->
                            Text(
                                text = preview,
                                color = colors.editor_hint,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun search_file_tree(
    project_root_path: String,
    query: String,
    mode: file_tree_search_mode
): List<file_tree_search_result> {
    val root = File(project_root_path)
    if (!root.isDirectory) return emptyList()
    val files = collect_file_tree_search_files(root)
    return when (mode) {
        file_tree_search_mode.FILE -> search_file_tree_names(root, files, query)
        file_tree_search_mode.CONTENT -> search_file_tree_content(root, files, query)
    }
}

private fun search_file_tree_names(
    root: File,
    files: List<File>,
    query: String
): List<file_tree_search_result> {
    return files.asSequence()
        .filter { it.name.contains(query, ignoreCase = true) }
        .take(FILE_TREE_SEARCH_RESULT_LIMIT)
        .map { file ->
            file_tree_search_result(
                name = file.name,
                path = file.absolutePath,
                detail = file_tree_relative_path(root, file)
            )
        }
        .toList()
}

private fun search_file_tree_content(
    root: File,
    files: List<File>,
    query: String
): List<file_tree_search_result> {
    return files.asSequence()
        .filter(::can_search_file_content)
        .mapNotNull { file ->
            find_file_tree_content_match(file, query)?.let { match ->
                file_tree_search_result(
                    name = file.name,
                    path = file.absolutePath,
                    detail = "${file_tree_relative_path(root, file)}  第${match.line + 1}行\n${match.preview}",
                    line = match.line,
                    column = match.column
                )
            }
        }
        .take(FILE_TREE_SEARCH_RESULT_LIMIT)
        .toList()
}

private fun collect_file_tree_search_files(root: File): List<File> {
    val files = mutableListOf<File>()

    fun visit(dir: File) {
        if (files.size >= FILE_TREE_SEARCH_SCAN_LIMIT) return
        val children = dir.listFiles()
            ?.filterNot { it.name.startsWith(".") }
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?: return

        for (child in children) {
            if (files.size >= FILE_TREE_SEARCH_SCAN_LIMIT) return
            if (child.isDirectory) {
                visit(child)
            } else {
                files.add(child)
            }
        }
    }

    visit(root)
    return files
}

private fun can_search_file_content(file: File): Boolean {
    if (!file.isFile || file.length() > FILE_TREE_CONTENT_MAX_BYTES) return false
    val name = file.name.lowercase()
    return name == "cmakelists.txt" || name.endsWith(".c") || name.endsWith(".cc") ||
        name.endsWith(".cpp") || name.endsWith(".cxx") || name.endsWith(".h") ||
        name.endsWith(".hh") || name.endsWith(".hpp") || name.endsWith(".hxx") ||
        name.endsWith(".cmake") || name.endsWith(".txt") || name.endsWith(".md") ||
        name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".gradle") ||
        name.endsWith(".kt") || name.endsWith(".java") || name.endsWith(".sh") ||
        name.endsWith(".mk")
}

private fun find_file_tree_content_match(file: File, query: String): file_tree_content_match? {
    return runCatching {
        file.useLines { lines ->
            lines.withIndex()
                .firstOrNull { indexed -> indexed.value.contains(query, ignoreCase = true) }
                ?.let { indexed ->
                    val column = indexed.value.indexOf(query, ignoreCase = true).coerceAtLeast(0)
                    val preview = indexed.value.trim().replace(Regex("\\s+"), " ").take(180)
                    file_tree_content_match(
                        line = indexed.index,
                        column = column,
                        preview = preview
                    )
                }
        }
    }.getOrNull()
}

private fun file_tree_relative_path(root: File, file: File): String {
    return runCatching {
        root.toPath().relativize(file.toPath()).toString()
    }.getOrDefault(file.name)
}

@Composable
private fun file_tree_tool_button(
    on_click: () -> Unit,
    colors: project_file_tree_colors,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = on_click
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun file_tree_row(
    node: editor_file_node,
    expanded: Boolean,
    editing: Boolean,
    editing_name: String,
    on_editing_name_change: (String) -> Unit,
    on_confirm_rename: () -> Unit,
    on_cancel_rename: () -> Unit,
    on_rename_field_bounds_change: (Rect?) -> Unit,
    on_new_file: (String) -> Unit,
    on_new_folder: (String) -> Unit,
    on_refresh: (String) -> Unit,
    colors: project_file_tree_colors,
    on_click: () -> Unit,
    on_long_press: () -> Unit
) {
    val file_icon_res = editor_file_icon_res(node.name)
    val icon_tint = if (node.is_directory) colors.editor_icon else colors.editor_hint
    val indent_width = if (node.depth > 0) (node.depth * 24).dp else 8.dp
    val icon_gap = 8.dp
    val icon_slot_width = if (node.is_directory) 41.dp else 34.dp
    val icon_size = 18.dp
    val row_click_modifier = if (editing) {
        Modifier
    } else {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            onClick = on_click,
            onLongClick = on_long_press
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .then(row_click_modifier)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .width(indent_width + icon_gap + icon_slot_width)
                .fillMaxHeight()
        ) {
            if (node.depth > 0) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = 1.dp.toPx()
                    val row_center_y = size.height / 2f
                    val line_step = 28.dp.toPx()
                    val half_step = 12.dp.toPx()
                    val curve = 8.dp.toPx()
                    val file_icon_start_x = indent_width.toPx() + icon_gap.toPx() + icon_slot_width.toPx() - icon_size.toPx()
                    val connector_end_x = if (node.is_directory) {
                        node.depth * line_step
                    } else {
                        file_icon_start_x
                    }

                    node.tree_guides.forEachIndexed { level, has_more_siblings ->
                        val center_x = level * line_step + half_step
                        val is_current_level = level == node.tree_guides.lastIndex
                        if (is_current_level) {
                            if (has_more_siblings) {
                                drawLine(
                                    color = colors.editor_divider,
                                    start = Offset(center_x, 0f),
                                    end = Offset(center_x, size.height),
                                    strokeWidth = stroke
                                )
                                drawLine(
                                    color = colors.editor_divider,
                                    start = Offset(center_x, row_center_y),
                                    end = Offset(connector_end_x, row_center_y),
                                    strokeWidth = stroke
                                )
                            } else {
                                val curve_start_y = (row_center_y - curve).coerceAtLeast(0f)
                                drawLine(
                                    color = colors.editor_divider,
                                    start = Offset(center_x, 0f),
                                    end = Offset(center_x, curve_start_y),
                                    strokeWidth = stroke
                                )
                                val path = Path().apply {
                                    moveTo(center_x, curve_start_y)
                                    quadraticTo(
                                        center_x,
                                        row_center_y,
                                        center_x + curve,
                                        row_center_y
                                    )
                                    lineTo(connector_end_x, row_center_y)
                                }
                                drawPath(
                                    path = path,
                                    color = colors.editor_divider,
                                    style = Stroke(
                                        width = stroke,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        } else if (has_more_siblings) {
                            drawLine(
                                color = colors.editor_divider,
                                start = Offset(center_x, 0f),
                                end = Offset(center_x, size.height),
                                strokeWidth = stroke
                            )
                        }
                    }
                }
            }

            if (node.is_directory) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.editor_hint,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indent_width + icon_gap)
                        .size(16.dp)
                )
                Icon(
                    painter = painterResource(if (expanded) R.drawable.ic_folder_opened else R.drawable.ic_folder),
                    contentDescription = null,
                    tint = Color(0xFF004DEA),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(icon_size)
                )
            } else {
                Icon(
                    painter = painterResource(file_icon_res),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(icon_size)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (editing) {
            file_tree_inline_rename_field(
                value = editing_name,
                on_value_change = on_editing_name_change,
                on_done = on_confirm_rename,
                on_cancel = on_cancel_rename,
                on_bounds_change = on_rename_field_bounds_change,
                colors = colors
            )
        } else {
            Text(
                text = node.name,
                color = colors.editor_text,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (node.is_directory) {
                file_tree_child_count(count = node.child_count, colors = colors)
            } else {
                file_tree_file_meta(size = node.file_size, colors = colors)
            }
        }

        if (node.is_directory && expanded) {
            Spacer(modifier = Modifier.width(22.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                file_tree_tool_button(on_click = { on_new_folder(node.path) }, colors = colors) {
                    Icon(
                        painter = painterResource(R.drawable.ic_new_folder),
                        contentDescription = "在此文件夹新建文件夹",
                        tint = Color(0xFF004DEA),
                        modifier = Modifier.size(15.dp)
                    )
                }
                file_tree_tool_button(on_click = { on_new_file(node.path) }, colors = colors) {
                    Icon(
                        painter = painterResource(R.drawable.ic_new_file),
                        contentDescription = "在此文件夹新建文件",
                        tint = Color(0xFF8D8AA4),
                        modifier = Modifier.size(15.dp)
                    )
                }
                file_tree_tool_button(on_click = { on_refresh(node.path) }, colors = colors) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = "刷新此文件夹",
                        tint = Color(0xFF8D8AA4),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun file_tree_file_meta(size: Long, colors: project_file_tree_colors) {

    Text(
        text = format_file_tree_file_size(size),
        color = colors.editor_hint,
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 96.dp)
    )
}

private fun format_file_tree_file_size(size: Long): String {
    val safe_size = size.coerceAtLeast(0L)
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        safe_size < 1024L -> "$safe_size B"
        safe_size < mb -> format_file_tree_size_value(safe_size / kb, "KB")
        safe_size < gb -> format_file_tree_size_value(safe_size / mb, "MB")
        else -> format_file_tree_size_value(safe_size / gb, "GB")
    }
}

private fun format_file_tree_size_value(value: Double, unit: String): String {
    val number = if (value >= 10.0) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value).trimEnd('0').trimEnd('.')
    }
    return "$number $unit"
}

@Composable
private fun file_tree_inline_rename_field(
    value: String,
    on_value_change: (String) -> Unit,
    on_done: () -> Unit,
    on_cancel: () -> Unit,
    on_bounds_change: (Rect?) -> Unit,
    colors: project_file_tree_colors
) {
    val focus_requester = remember { FocusRequester() }
    var done_sent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focus_requester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose { on_bounds_change(null) }
    }

    val text_width = (value.length.coerceAtLeast(1) * 7 + 16).dp
    val field_width = text_width.coerceIn(48.dp, 180.dp)

    Box(
        modifier = Modifier
            .width(field_width)
            .height(18.dp)
            .onGloballyPositioned { on_bounds_change(it.boundsInRoot()) }
    ) {
        BasicTextField(
            value = value,
            onValueChange = on_value_change,
            singleLine = true,
            textStyle = TextStyle(color = colors.editor_text, fontSize = 13.sp),
            cursorBrush = SolidColor(colors.editor_icon),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                done_sent = true
                on_done()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus_requester),
            decorationBox = { inner_text_field ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    inner_text_field()
                }
            }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.editor_icon)
        )
    }
}