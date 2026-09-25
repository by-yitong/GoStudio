package com.jmwl.gostudio.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.toolchain.git_commit_entry
import com.jmwl.gostudio.toolchain.git_commit_file
import com.jmwl.gostudio.toolchain.git_commit_node
import com.jmwl.gostudio.toolchain.git_graph_builder
import com.jmwl.gostudio.toolchain.git_graph_row
import com.jmwl.gostudio.toolchain.git_history_manager
import com.jmwl.gostudio.toolchain.git_ref_entry
import com.jmwl.gostudio.toolchain.git_ref_kind
import com.jmwl.gostudio.toolchain.git_ref_manager
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 历史 Tab 分页页大小（与 git_history_manager.log 的默认 limit 一致）。 */
private const val GIT_LOG_PAGE_SIZE = 100

/** 泳道色板（8 色循环，避开绿/紫）：蓝/琥珀/青/红/粉/金/橙/藏青。 */
private val git_lane_colors = listOf(
    Color(0xFF2563EB), // 蓝
    Color(0xFFF59E0B), // 琥珀
    Color(0xFF06B6D4), // 青
    Color(0xFFEF4444), // 红
    Color(0xFFEC4899), // 粉
    Color(0xFFEAB308), // 金
    Color(0xFFF97316), // 橙
    Color(0xFF1E40AF)  // 藏青
)

private fun git_lane_color(lane: Int): Color = git_lane_colors[lane.mod(git_lane_colors.size)]

private fun build_graph_rows(commits: List<git_commit_entry>): List<git_graph_row> =
    git_graph_builder.build(commits.map { git_commit_node(hash = it.full_hash, parents = it.parents) })

/** 相对时间：<60s 刚刚、<60分 N分钟前、<24h N小时前、<7d N天前、<30d N周前、<365d N个月前、否则 yyyy-MM-dd。 */
internal fun format_relative_time(timestamp_seconds: Long): String {
    val delta_seconds = System.currentTimeMillis() / 1000L - timestamp_seconds
    return when {
        delta_seconds < 60 -> "刚刚"
        delta_seconds < 60 * 60 -> "${delta_seconds / 60}分钟前"
        delta_seconds < 24 * 60 * 60 -> "${delta_seconds / (60 * 60)}小时前"
        delta_seconds < 7 * 24 * 60 * 60 -> "${delta_seconds / (24 * 60 * 60)}天前"
        delta_seconds < 30 * 24 * 60 * 60 -> "${delta_seconds / (7 * 24 * 60 * 60)}周前"
        delta_seconds < 365 * 24 * 60 * 60 -> "${delta_seconds / (30 * 24 * 60 * 60)}个月前"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp_seconds * 1000L))
    }
}

/**
 * 全屏 Git 页 · 提交历史 Tab：Canvas 泳道拓扑 + 分页列表（100/页，剩余 <20 预取）+
 * 点行弹提交详情（文件列表）→ 点文件进页内全屏 diff 态（共享 git_diff_viewer）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun git_log_tab(project_root_path: String, refresh_tick: Int = 0) {
    val colors = app_theme_provider.colors
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val list_state = rememberLazyListState()

    var loading by remember { mutableStateOf(true) }
    var commits by remember { mutableStateOf<List<git_commit_entry>>(emptyList()) }
    var rows by remember { mutableStateOf<List<git_graph_row>>(emptyList()) }
    var has_more by remember { mutableStateOf(false) }
    var loading_more by remember { mutableStateOf(false) }
    var refs by remember { mutableStateOf<List<git_ref_entry>>(emptyList()) }

    // 详情弹层
    var selected_commit by remember { mutableStateOf<git_commit_entry?>(null) }
    var detail_files by remember { mutableStateOf<List<git_commit_file>>(emptyList()) }
    var detail_files_loading by remember { mutableStateOf(false) }

    // 页内 diff 态：提交 + 文件路径；非空时覆盖整个内容区（git_page 不感知）
    var diff_target by remember { mutableStateOf<Pair<git_commit_entry, String>?>(null) }
    var diff_text by remember { mutableStateOf("") }
    var diff_loading by remember { mutableStateOf(false) }

    // 首页 / 刷新：refresh_tick 变化时 reset 回顶
    LaunchedEffect(project_root_path, refresh_tick) {
        loading = true
        loading_more = false
        has_more = false
        selected_commit = null
        diff_target = null
        list_state.scrollToItem(0)
        val first_page = git_history_manager.log(project_root_path, skip = 0, limit = GIT_LOG_PAGE_SIZE)
        commits = first_page
        rows = build_graph_rows(first_page)
        has_more = first_page.size == GIT_LOG_PAGE_SIZE
        loading = false
        // 引用列表后置加载：先让提交列表出来
        refs = git_ref_manager.all_refs(project_root_path)
    }

    fun load_next_page() {
        if (loading || loading_more || !has_more) return
        loading_more = true
        scope.launch {
            val page = git_history_manager.log(project_root_path, skip = commits.size, limit = GIT_LOG_PAGE_SIZE)
            val merged = commits + page
            commits = merged
            rows = build_graph_rows(merged)
            has_more = page.size == GIT_LOG_PAGE_SIZE
            loading_more = false
        }
    }

    // 分页预取：最后可见 index 距末尾 <20 且还有下一页时加载（append，skip=已加载数）
    val should_load_more by remember {
        derivedStateOf {
            val info = list_state.layoutInfo
            val last_visible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 &&
                last_visible >= info.totalItemsCount - 20 &&
                has_more && !loading_more
        }
    }
    LaunchedEffect(should_load_more, commits.size) {
        if (should_load_more) load_next_page()
    }

    // 详情文件列表：弹层打开时加载
    LaunchedEffect(selected_commit) {
        val commit = selected_commit ?: return@LaunchedEffect
        detail_files_loading = true
        detail_files = emptyList()
        detail_files = git_history_manager.commit_files(project_root_path, commit.full_hash)
        detail_files_loading = false
    }

    // 页内 diff：进入 diff 态时加载
    LaunchedEffect(diff_target) {
        val target = diff_target ?: return@LaunchedEffect
        diff_loading = true
        diff_text = git_history_manager.commit_file_diff(project_root_path, target.first.full_hash, target.second)
        diff_loading = false
    }

    // 引用按 hash 建索引；HEAD 置前，其余按 LOCAL/TAG/REMOTE 排
    val refs_by_hash = remember(refs) { refs.groupBy { it.hash } }

    val active_diff = diff_target
    if (active_diff != null) {
        // 页内全屏 diff 态：git_page 不感知，由本 Tab 内部状态覆盖整个内容区
        BackHandler { diff_target = null }
        Column(Modifier.fillMaxSize().background(colors.editor_bg)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { diff_target = null }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = colors.editor_icon,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = active_diff.second.substringAfterLast('/'),
                    color = colors.editor_text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = active_diff.first.short_hash,
                    color = colors.editor_hint,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            if (diff_loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("加载 diff...", color = colors.editor_hint, fontSize = 13.sp)
                }
            } else {
                git_diff_viewer(diff_text, Modifier.weight(1f).fillMaxWidth())
            }
        }
    } else {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("读取中...", color = colors.editor_hint, fontSize = 13.sp)
            }
            commits.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // 加载失败（非仓库/未装 git 等）与空仓库都落在这里
                Text("暂无提交记录", color = colors.editor_hint, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            else -> LazyColumn(state = list_state, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(commits) { index, commit ->
                    git_log_row(
                        commit = commit,
                        row = rows.getOrNull(index),
                        next_row = rows.getOrNull(index + 1),
                        commit_refs = refs_by_hash[commit.full_hash].orEmpty().sortedWith(
                            compareByDescending<git_ref_entry> { it.kind == git_ref_kind.LOCAL && it.is_head }
                                .thenBy { it.kind.ordinal }
                        ),
                        on_click = { selected_commit = commit }
                    )
                }
                if (loading_more) {
                    item(key = "loading_more") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("加载更多...", color = colors.editor_hint, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // 提交详情弹层（约 70% 高）：hash 复制 / 作者时间 / body / 文件列表
    selected_commit?.let { commit ->
        ModalBottomSheet(
            onDismissRequest = { selected_commit = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.dialog_bg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = commit.short_hash,
                        color = colors.editor_icon,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(commit.short_hash)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制短 hash",
                            tint = colors.editor_hint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = commit.full_hash,
                        color = colors.editor_hint,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(commit.full_hash)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制完整 hash",
                            tint = colors.editor_hint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "${commit.author} · ${format_relative_time(commit.timestamp)}",
                    color = colors.editor_hint,
                    fontSize = 12.sp
                )
                if (commit.body.isNotBlank()) {
                    Text(
                        text = commit.body,
                        color = colors.editor_hint,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                HorizontalDivider(
                    color = colors.editor_divider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                Text(
                    text = "变更文件 (${detail_files.size})",
                    color = colors.editor_hint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                when {
                    detail_files_loading -> Text(
                        "读取文件...",
                        color = colors.editor_hint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    detail_files.isEmpty() -> Text(
                        "无文件变更",
                        color = colors.editor_hint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    else -> detail_files.forEach { file ->
                        git_commit_file_row(file) {
                            selected_commit = null
                            diff_target = commit to file.path
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/** 单条提交行：左泳道图 + 右侧 hash/作者/时间、subject、RefPill。 */
@Composable
private fun git_log_row(
    commit: git_commit_entry,
    row: git_graph_row?,
    next_row: git_graph_row?,
    commit_refs: List<git_ref_entry>,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = on_click),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 泳道图列：统一按「前 4 个泳道 = 64dp」封顶。
        // 取舍：深分叉时泳道数可能超过 4，若每行按自身 max(active)+1 个泳道取宽，
        // 同一列表里行宽不一会导致右侧内容列左缘抖动；封顶 64dp 后超出泳道画到
        // 列右边缘被 clipToBounds 裁剪（深泳道场景罕见，可接受）。
        git_graph_canvas(
            row = row,
            next_row = next_row,
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = commit.short_hash,
                    color = colors.editor_icon,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = commit.author,
                    color = colors.editor_text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = format_relative_time(commit.timestamp),
                    color = colors.editor_hint,
                    fontSize = 11.sp
                )
            }
            Text(
                text = commit.subject,
                color = colors.editor_text,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
            )
            if (commit_refs.isNotEmpty()) {
                // 同一提交多个 ref 全显示；横向排不下时单个胶囊内 ellipsis（胶囊封顶 120dp）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    commit_refs.forEach { ref -> git_ref_pill(ref) }
                }
            }
        }
    }
}

/**
 * 引用胶囊：LOCAL is_head → 填充（editor_sidebar_selected_bg + editor_text）、
 * 其他 LOCAL → 描边、TAG → 描边小标签样式、REMOTE → 描边灰字。
 * 注：annotated tag 的 objectname 是 tag 对象的 hash 而非其指向提交的 hash，
 * 与提交 full_hash 匹配不上（轻量 tag 能匹配），匹配不上就不显示，属预期。
 */
@Composable
private fun git_ref_pill(ref: git_ref_entry) {
    val colors = app_theme_provider.colors
    val shape = RoundedCornerShape(8.dp)
    val is_head_local = ref.kind == git_ref_kind.LOCAL && ref.is_head
    val text_color = when {
        is_head_local || ref.kind == git_ref_kind.LOCAL -> colors.editor_text
        else -> colors.editor_hint
    }
    val modifier = if (is_head_local) {
        Modifier
            .widthIn(max = 120.dp)
            .clip(shape)
            .background(colors.editor_sidebar_selected_bg)
    } else {
        Modifier
            .widthIn(max = 120.dp)
            .border(1.dp, colors.editor_hint, shape)
    }
    Box(modifier = modifier.padding(horizontal = 6.dp, vertical = 1.dp)) {
        Text(
            text = ref.short_name,
            color = text_color,
            fontSize = if (ref.kind == git_ref_kind.TAG) 9.sp else 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 提交详情里的文件行：A/M/D 徽标（参照 editor_git_panel.git_change_row 的徽标做法）+ 路径。 */
@Composable
private fun git_commit_file_row(file: git_commit_file, on_click: () -> Unit) {
    val colors = app_theme_provider.colors
    val tint = when (file.change) {
        'A' -> colors.success
        'D' -> colors.danger
        else -> colors.warning
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = on_click)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(tint.copy(alpha = 0.16f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(file.change.toString(), color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = file.path.substringAfterLast('/'),
            color = colors.editor_text,
            fontSize = 13.sp,
            maxLines = 1
        )
        Text(
            text = file.path.substringBeforeLast('/'),
            color = colors.editor_hint,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/**
 * 单行泳道拓扑（行高 72dp，泳道列宽 16dp，lane N 的 x 中心 = N*16+8dp，颜色 = 色板[N%8]）。
 * 每行一个 Canvas，只画自己那一格，上下半段拼成连续图。
 *
 * 绘制规则（由 git_graph_builder 不变式推出；i+1 越界时按空集）：
 * - L ∈ active[i] ∩ active[i+1] → 整行竖线穿过
 * - L ∈ active[i] − active[i+1] → 算法保证 L == node_lane[i]，画「顶边→节点」半段（线终结在节点）
 * - L ∈ active[i+1] − active[i] → 本行新生：L == node_lane[i] 画「节点→底边」；
 *   否则是分叉出的子泳道，用 cubicTo 从节点画贝塞尔到 (L 的 x, 行底边)，颜色取目标泳道色（允许斜穿）
 *
 * 注：分叉复用刚终结 lane 号时，该号会同时出现在相邻两行 active 集合，属正常，
 * 以各自行数据为准逐行绘制。
 */
@Composable
private fun git_graph_canvas(row: git_graph_row?, next_row: git_graph_row?, modifier: Modifier = Modifier) {
    val active_row = row ?: return
    Canvas(modifier = modifier.clipToBounds()) {
        val lane_width = 16.dp.toPx()
        val stroke_width = 2.dp.toPx()
        val node_radius = 4.dp.toPx()
        val height = size.height

        fun lane_x(lane: Int): Float = lane * lane_width + lane_width / 2f

        val node_lane = active_row.node_lane
        val node_x = lane_x(node_lane)
        val node_y = height / 2f

        val current_set: Set<Int> = active_row.active_lanes.toSet()
        val next_set: Set<Int> = next_row?.active_lanes?.toSet() ?: emptySet()

        // 本行已有泳道：整行穿过，或终结在节点（顶边→节点）
        for (lane in current_set) {
            val x = lane_x(lane)
            if (lane in next_set) {
                drawLine(git_lane_color(lane), Offset(x, 0f), Offset(x, height), stroke_width)
            } else {
                // 算法不变式保证此时 lane == node_lane，线终结在节点
                drawLine(git_lane_color(lane), Offset(x, 0f), Offset(x, node_y), stroke_width)
            }
        }
        // 本行新生、向下延续的泳道：节点→底边，或分叉贝塞尔到目标泳道
        for (lane in next_set) {
            if (lane in current_set) continue
            val x = lane_x(lane)
            if (lane == node_lane) {
                drawLine(git_lane_color(lane), Offset(x, node_y), Offset(x, height), stroke_width)
            } else {
                val path = Path().apply {
                    moveTo(node_x, node_y)
                    val mid_y = (node_y + height) / 2f
                    cubicTo(node_x, mid_y, x, mid_y, x, height)
                }
                drawPath(path, git_lane_color(lane), style = Stroke(stroke_width))
            }
        }
        // 节点：实心圆（半径约 4dp）；合并提交双圈（外圈描边）
        drawCircle(git_lane_color(node_lane), node_radius, Offset(node_x, node_y))
        if (active_row.is_merge) {
            drawCircle(
                git_lane_color(node_lane),
                node_radius + 2.dp.toPx(),
                Offset(node_x, node_y),
                style = Stroke(stroke_width)
            )
        }
    }
}
