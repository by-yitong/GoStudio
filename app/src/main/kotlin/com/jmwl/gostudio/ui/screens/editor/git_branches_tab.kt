package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.toolchain.git_ref_entry
import com.jmwl.gostudio.toolchain.git_ref_kind
import com.jmwl.gostudio.toolchain.git_ref_manager
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.launch

// ==========================================================================
// 纯函数区：层级树构建（不依赖 Android，JVM 单测见 tools/git_ref_tree_test）
// ==========================================================================

/** 树节点：文件夹（is_leaf=false、ref=null）或叶子（对应一条 git_ref_entry）。 */
internal data class git_ref_tree_node(
    val name: String,            // 本段名
    val full_path: String,       // 完整分支名（叶子才有意义）
    val is_leaf: Boolean,
    val ref: git_ref_entry?,     // 叶子非空
    val children: List<git_ref_tree_node>
)

/**
 * 分支名按 "/" 折成层级树：feature/login、feature/signup → feature ▸ login/signup。
 * 排序规则：文件夹在前，同组内按名字字典序。
 */
internal fun build_ref_tree(refs: List<git_ref_entry>): List<git_ref_tree_node> =
    build_ref_level(refs, "")

/** 递归构建 prefix 层之下的节点；prefix 为空串表示顶层。 */
private fun build_ref_level(refs: List<git_ref_entry>, prefix: String): List<git_ref_tree_node> {
    val leaves = mutableListOf<git_ref_entry>()                      // 本层叶子（无更深路径）
    val deeper = mutableMapOf<String, MutableList<git_ref_entry>>()  // 段名 → 该子文件夹下的引用
    for (ref in refs) {
        val rest = if (prefix.isEmpty()) ref.short_name else ref.short_name.removePrefix("$prefix/")
        val segment = rest.substringBefore('/')
        if (rest == segment) leaves += ref else deeper.getOrPut(segment) { mutableListOf() } += ref
    }
    val leaf_nodes = leaves.map { ref ->
        git_ref_tree_node(
            name = ref.short_name.substringAfterLast('/'),
            full_path = ref.short_name,
            is_leaf = true,
            ref = ref,
            children = emptyList()
        )
    }
    // 与文件夹同名的叶子（git 同命名空间不允许，跨命名空间理论可能）并入该文件夹首位，不丢数据
    val consumed = mutableSetOf<String>()
    val folder_nodes = deeper.entries.map { (segment, group) ->
        val child_prefix = if (prefix.isEmpty()) segment else "$prefix/$segment"
        val self_leaves = leaf_nodes.filter { it.full_path == child_prefix }.onEach { consumed += it.full_path }
        git_ref_tree_node(
            name = segment,
            full_path = child_prefix,
            is_leaf = false,
            ref = null,
            children = self_leaves + build_ref_level(group, child_prefix)
        )
    }
    return folder_nodes.sortedBy { it.name } +
        leaf_nodes.filter { it.full_path !in consumed }.sortedBy { it.name }
}

/** 文件夹节点的后代叶子数（用于「feature (2)」计数）。 */
internal fun count_ref_leaves(node: git_ref_tree_node): Int =
    if (node.is_leaf) 1 else node.children.sumOf { count_ref_leaves(it) }

/** 可见行：树按展开状态拍平成列表行，供 LazyColumn 渲染。 */
private data class git_ref_tree_row(
    val node: git_ref_tree_node,
    val depth: Int,
    val is_expanded: Boolean
)

private fun flatten_ref_tree(
    nodes: List<git_ref_tree_node>,
    expanded: Map<String, Boolean>
): List<git_ref_tree_row> {
    val rows = mutableListOf<git_ref_tree_row>()
    fun walk(level: List<git_ref_tree_node>, depth: Int) {
        for (node in level) {
            val open = expanded[node.full_path] ?: false
            rows += git_ref_tree_row(node, depth, open)
            if (!node.is_leaf && open) walk(node.children, depth + 1)
        }
    }
    walk(nodes, 0)
    return rows
}

// ==========================================================================
// UI 区：分支 Tab
// ==========================================================================

/**
 * Git 分支 Tab（全屏 Git 页壳的「分支」页签，交互参考 GitKraken / jieapi-aicode，代码自研）：
 * 顶部当前分支概览卡（名称、↑ahead ↓behind、本地/远程/标签计数、新建分支/标签入口）+
 * 本地/远程/标签三个折叠 Section，分支名按 "/" 折成层级树；
 * 点叶子弹操作表：本地=切换/重命名/删除（未合并支持二次确认强制删除）、远程=检出为本地分支、tag=删除。
 * 操作成功后 on_state_changed() 通知壳页并自行重载。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun git_branches_tab(
    project_root_path: String,
    refresh_tick: Int = 0,
    on_state_changed: () -> Unit = {}
) {
    val colors = app_theme_provider.colors
    val scope = rememberCoroutineScope()
    var refs by remember { mutableStateOf<List<git_ref_entry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var local_expanded by rememberSaveable { mutableStateOf(true) }
    var remote_expanded by rememberSaveable { mutableStateOf(false) }
    var tags_expanded by rememberSaveable { mutableStateOf(false) }
    val folder_expanded = remember { mutableStateMapOf<String, Boolean>() }

    var action_busy by remember { mutableStateOf(false) }
    var action_error by remember { mutableStateOf<String?>(null) }
    var sheet_ref by remember { mutableStateOf<git_ref_entry?>(null) }
    var renaming_ref by remember { mutableStateOf<git_ref_entry?>(null) }
    var rename_text by remember { mutableStateOf("") }
    var deleting_ref by remember { mutableStateOf<git_ref_entry?>(null) }
    var force_deleting_ref by remember { mutableStateOf<git_ref_entry?>(null) }
    var show_create_branch by remember { mutableStateOf(false) }
    var new_branch_name by remember { mutableStateOf("") }
    var show_create_tag by remember { mutableStateOf(false) }
    var new_tag_name by remember { mutableStateOf("") }

    fun load_refs(show_progress: Boolean) {
        scope.launch {
            if (show_progress) loading = true
            refs = git_ref_manager.all_refs(project_root_path)
            loading = false
        }
    }

    LaunchedEffect(project_root_path, refresh_tick) { load_refs(show_progress = true) }

    /** 统一操作执行：防重复点击；成功 → 回调 + 通知壳页 + 静默重载；失败 → 显示 error_text（空串则交给 on_fail）。 */
    fun run_action(
        action: suspend () -> Boolean,
        on_success: () -> Unit = {},
        error_text: String = "Git 操作失败，请查看日志",
        on_fail: () -> Unit = {}
    ) {
        if (action_busy) return
        action_busy = true
        action_error = null
        scope.launch {
            val ok = action()
            action_busy = false
            if (ok) {
                on_success()
                on_state_changed()
                load_refs(show_progress = false)
            } else {
                if (error_text.isNotEmpty()) action_error = error_text
                on_fail()
            }
        }
    }

    val local_refs = refs.filter { it.kind == git_ref_kind.LOCAL }
    val remote_refs = refs.filter { it.kind == git_ref_kind.REMOTE }
    val tag_refs = refs.filter { it.kind == git_ref_kind.TAG }
    val current_ref = local_refs.firstOrNull { it.is_head }

    Column(Modifier.fillMaxSize()) {
        branches_overview_card(
            current_ref = current_ref,
            local_count = local_refs.size,
            remote_count = remote_refs.size,
            tag_count = tag_refs.size,
            action_busy = action_busy,
            on_create_branch = {
                new_branch_name = ""
                show_create_branch = true
            },
            on_create_tag = {
                new_tag_name = ""
                show_create_tag = true
            }
        )
        action_error?.let {
            Text(
                text = it,
                color = colors.danger,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("读取中...", color = colors.editor_hint, fontSize = 13.sp)
            }
            refs.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "未找到分支",
                    color = colors.editor_hint,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                ref_section(
                    section_key = "local",
                    title = "本地",
                    section_refs = local_refs,
                    expanded = local_expanded,
                    on_toggle_section = { local_expanded = !local_expanded },
                    folder_expanded = folder_expanded,
                    on_toggle_folder = { path -> folder_expanded[path] = !(folder_expanded[path] ?: false) },
                    on_leaf_click = { sheet_ref = it }
                )
                ref_section(
                    section_key = "remote",
                    title = "远程",
                    section_refs = remote_refs,
                    expanded = remote_expanded,
                    on_toggle_section = { remote_expanded = !remote_expanded },
                    folder_expanded = folder_expanded,
                    on_toggle_folder = { path -> folder_expanded[path] = !(folder_expanded[path] ?: false) },
                    on_leaf_click = { sheet_ref = it }
                )
                ref_section(
                    section_key = "tags",
                    title = "标签",
                    section_refs = tag_refs,
                    expanded = tags_expanded,
                    on_toggle_section = { tags_expanded = !tags_expanded },
                    folder_expanded = folder_expanded,
                    on_toggle_folder = { path -> folder_expanded[path] = !(folder_expanded[path] ?: false) },
                    on_leaf_click = { sheet_ref = it }
                )
            }
        }

        // ===== 操作表（点叶子弹出） =====
        sheet_ref?.let { target ->
            val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { sheet_ref = null },
                sheetState = sheet_state,
                containerColor = colors.dialog_bg,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp)
                ) {
                    Text(
                        text = target.short_name,
                        color = colors.dialog_text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val kind_label = when {
                        target.kind == git_ref_kind.LOCAL && target.is_head -> "当前分支"
                        target.kind == git_ref_kind.LOCAL -> "本地分支"
                        target.kind == git_ref_kind.REMOTE -> "远程分支"
                        else -> "标签"
                    }
                    Text(
                        text = kind_label,
                        color = colors.dialog_hint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    if (action_busy) {
                        Text(
                            "处理中...",
                            color = colors.editor_hint,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    HorizontalDivider(color = colors.editor_divider)
                    when {
                        target.kind == git_ref_kind.LOCAL && target.is_head -> {
                            sheet_action("重命名", Icons.Default.Edit, enabled = !action_busy) {
                                sheet_ref = null
                                renaming_ref = target
                                rename_text = target.short_name
                            }
                        }
                        target.kind == git_ref_kind.LOCAL -> {
                            sheet_action("切换到该分支", Icons.Default.SwapHoriz, enabled = !action_busy) {
                                run_action(
                                    action = {
                                        com.jmwl.gostudio.toolchain.git_manager.checkout(
                                            project_root_path, target.short_name
                                        )
                                    },
                                    on_success = { sheet_ref = null }
                                )
                            }
                            sheet_action("重命名", Icons.Default.Edit, enabled = !action_busy) {
                                sheet_ref = null
                                renaming_ref = target
                                rename_text = target.short_name
                            }
                            sheet_action("删除", Icons.Default.Delete, danger = true, enabled = !action_busy) {
                                sheet_ref = null
                                deleting_ref = target
                            }
                        }
                        target.kind == git_ref_kind.REMOTE -> {
                            sheet_action("检出为本地分支", Icons.Default.CloudDownload, enabled = !action_busy) {
                                run_action(
                                    action = {
                                        git_ref_manager.checkout_remote(project_root_path, target.short_name)
                                    },
                                    on_success = { sheet_ref = null }
                                )
                            }
                        }
                        else -> {
                            sheet_action("删除", Icons.Default.Delete, danger = true, enabled = !action_busy) {
                                sheet_ref = null
                                deleting_ref = target
                            }
                        }
                    }
                }
            }
        }

        // ===== 重命名分支 =====
        renaming_ref?.let { target ->
            AlertDialog(
                onDismissRequest = { if (!action_busy) renaming_ref = null },
                title = { Text("重命名分支") },
                text = {
                    OutlinedTextField(
                        value = rename_text,
                        onValueChange = { rename_text = it },
                        singleLine = true,
                        placeholder = { Text("新分支名") },
                        enabled = !action_busy
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = rename_text.isNotBlank() && !action_busy,
                        onClick = {
                            run_action(
                                action = {
                                    git_ref_manager.rename_branch(
                                        project_root_path, target.short_name, rename_text
                                    )
                                },
                                on_success = { renaming_ref = null }
                            )
                        }
                    ) { Text(if (action_busy) "处理中..." else "重命名") }
                },
                dismissButton = {
                    TextButton(onClick = { renaming_ref = null }, enabled = !action_busy) { Text("取消") }
                }
            )
        }

        // ===== 删除确认（分支 / 标签） =====
        deleting_ref?.let { target ->
            val is_tag = target.kind == git_ref_kind.TAG
            AlertDialog(
                onDismissRequest = { if (!action_busy) deleting_ref = null },
                title = { Text(if (is_tag) "删除标签" else "删除分支") },
                text = {
                    Text("确定删除${if (is_tag) "标签" else "分支"} ${target.short_name}？")
                },
                confirmButton = {
                    TextButton(
                        enabled = !action_busy,
                        onClick = {
                            if (is_tag) {
                                run_action(
                                    action = { git_ref_manager.delete_tag(project_root_path, target.short_name) },
                                    on_success = { deleting_ref = null }
                                )
                            } else {
                                run_action(
                                    action = {
                                        git_ref_manager.delete_branch(
                                            project_root_path, target.short_name, force = false
                                        )
                                    },
                                    on_success = { deleting_ref = null },
                                    error_text = "",
                                    on_fail = {
                                        deleting_ref = null
                                        force_deleting_ref = target
                                    }
                                )
                            }
                        }
                    ) { Text(if (action_busy) "处理中..." else "删除", color = colors.danger) }
                },
                dismissButton = {
                    TextButton(onClick = { deleting_ref = null }, enabled = !action_busy) { Text("取消") }
                }
            )
        }

        // ===== 强制删除（未合并分支二次确认） =====
        force_deleting_ref?.let { target ->
            AlertDialog(
                onDismissRequest = { if (!action_busy) force_deleting_ref = null },
                title = { Text("强制删除分支") },
                text = {
                    Text(
                        "删除失败，若分支未完全合并可强制删除。\n" +
                            "强制删除 ${target.short_name}？未合并到其他分支的提交将无法通过该分支访问。"
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !action_busy,
                        onClick = {
                            run_action(
                                action = {
                                    git_ref_manager.delete_branch(
                                        project_root_path, target.short_name, force = true
                                    )
                                },
                                on_success = { force_deleting_ref = null },
                                error_text = "强制删除失败，请查看日志"
                            )
                        }
                    ) { Text(if (action_busy) "处理中..." else "强制删除", color = colors.danger) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { force_deleting_ref = null },
                        enabled = !action_busy
                    ) { Text("取消") }
                }
            )
        }

        // ===== 新建分支表单 =====
        if (show_create_branch) {
            val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { if (!action_busy) show_create_branch = false },
                sheetState = sheet_state,
                containerColor = colors.dialog_bg,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp)
                ) {
                    Text(
                        "新建分支",
                        color = colors.dialog_text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "基于当前分支创建并切换",
                        color = colors.dialog_hint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )
                    OutlinedTextField(
                        value = new_branch_name,
                        onValueChange = { new_branch_name = it },
                        singleLine = true,
                        placeholder = { Text("分支名") },
                        enabled = !action_busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.editor_button_bg)
                                .clickable(enabled = !action_busy) { show_create_branch = false }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text("取消", color = colors.editor_hint, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.editor_sidebar_selected_bg)
                                .clickable(
                                    enabled = new_branch_name.isNotBlank() && !action_busy
                                ) {
                                    run_action(
                                        action = {
                                            com.jmwl.gostudio.toolchain.git_manager.create_branch(
                                                project_root_path, new_branch_name
                                            )
                                        },
                                        on_success = { show_create_branch = false }
                                    )
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (action_busy) "处理中..." else "创建",
                                color = colors.editor_icon,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ===== 新建标签表单（建在当前 HEAD） =====
        if (show_create_tag) {
            val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val head_note = if (current_ref != null) {
                "标签将创建在当前 HEAD（${current_ref.short_name}）"
            } else {
                "标签将创建在当前 HEAD"
            }
            ModalBottomSheet(
                onDismissRequest = { if (!action_busy) show_create_tag = false },
                sheetState = sheet_state,
                containerColor = colors.dialog_bg,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp)
                ) {
                    Text(
                        "新建标签",
                        color = colors.dialog_text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        head_note,
                        color = colors.dialog_hint,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )
                    OutlinedTextField(
                        value = new_tag_name,
                        onValueChange = { new_tag_name = it },
                        singleLine = true,
                        placeholder = { Text("标签名") },
                        enabled = !action_busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.editor_button_bg)
                                .clickable(enabled = !action_busy) { show_create_tag = false }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text("取消", color = colors.editor_hint, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.editor_sidebar_selected_bg)
                                .clickable(enabled = new_tag_name.isNotBlank() && !action_busy) {
                                    run_action(
                                        action = { git_ref_manager.create_tag(project_root_path, new_tag_name) },
                                        on_success = { show_create_tag = false }
                                    )
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (action_busy) "处理中..." else "创建",
                                color = colors.editor_icon,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 顶部概览卡：当前分支名（粗体 / 分离 HEAD）、↑ahead ↓behind、本地·远程·标签计数、新建入口。 */
@Composable
private fun branches_overview_card(
    current_ref: git_ref_entry?,
    local_count: Int,
    remote_count: Int,
    tag_count: Int,
    action_busy: Boolean,
    on_create_branch: () -> Unit,
    on_create_tag: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.editor_sidebar_selected_bg.copy(alpha = 0.3f))
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = current_ref?.short_name ?: "分离 HEAD",
                    color = colors.editor_text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (current_ref != null && current_ref.upstream.isNotBlank() &&
                    (current_ref.ahead > 0 || current_ref.behind > 0)
                ) {
                    Text(
                        text = "↑${current_ref.ahead} ↓${current_ref.behind}",
                        color = colors.editor_hint,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = "本地 $local_count · 远程 $remote_count · 标签 $tag_count",
                color = colors.editor_hint,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        IconButton(
            onClick = on_create_branch,
            enabled = !action_busy,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "新建分支",
                tint = colors.editor_icon,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = on_create_tag,
            enabled = !action_busy,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Label,
                contentDescription = "新建标签",
                tint = colors.editor_icon,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 一个折叠 Section：头（名称 + 数量 + 箭头）+ 展开后的层级树行（空则显示「无」）。 */
private fun LazyListScope.ref_section(
    section_key: String,
    title: String,
    section_refs: List<git_ref_entry>,
    expanded: Boolean,
    on_toggle_section: () -> Unit,
    folder_expanded: Map<String, Boolean>,
    on_toggle_folder: (String) -> Unit,
    on_leaf_click: (git_ref_entry) -> Unit
) {
    item(key = "$section_key-header") {
        val colors = app_theme_provider.colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = on_toggle_section)
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title (${section_refs.size})",
                color = colors.editor_hint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = colors.editor_hint,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (expanded) 0f else -90f)
            )
        }
    }
    if (expanded) {
        if (section_refs.isEmpty()) {
            item(key = "$section_key-empty") {
                Text(
                    text = "无",
                    color = app_theme_provider.colors.editor_hint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 8.dp)
                )
            }
        } else {
            val rows = flatten_ref_tree(build_ref_tree(section_refs), folder_expanded)
            items(
                rows,
                key = { row ->
                    "$section_key-${row.depth}-${row.node.ref?.kind}-" +
                        "${row.node.full_path}-${if (row.node.is_leaf) "leaf" else "dir"}"
                }
            ) { row ->
                if (row.node.is_leaf) {
                    ref_leaf_row(row.node, row.depth, on_leaf_click)
                } else {
                    ref_folder_row(row.node, row.depth, row.is_expanded, on_toggle_folder)
                }
            }
        }
    }
}

/** 文件夹行：展开箭头 + 文件夹名 +（后代叶子数），点行切换展开。 */
@Composable
private fun ref_folder_row(
    node: git_ref_tree_node,
    depth: Int,
    is_expanded: Boolean,
    on_toggle: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { on_toggle(node.full_path) }
            .padding(start = (16 + depth * 14).dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = if (is_expanded) "收起" else "展开",
            tint = colors.editor_hint,
            modifier = Modifier
                .size(16.dp)
                .rotate(if (is_expanded) 0f else -90f)
        )
        Text(
            text = node.name,
            color = colors.editor_text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "(${count_ref_leaves(node)})",
            color = colors.editor_hint,
            fontSize = 11.sp
        )
    }
}

/** 叶子行：远程云图标 + 名字 +「当前」徽标（本地 is_head）+ ahead/behind 小字（本地有 upstream）。 */
@Composable
private fun ref_leaf_row(
    node: git_ref_tree_node,
    depth: Int,
    on_click: (git_ref_entry) -> Unit
) {
    val colors = app_theme_provider.colors
    val ref = node.ref ?: return
    val is_current = ref.kind == git_ref_kind.LOCAL && ref.is_head
    val is_remote = ref.kind == git_ref_kind.REMOTE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (is_current) colors.editor_sidebar_selected_bg else Color.Transparent)
            .clickable { on_click(ref) }
            .padding(start = (4 + depth * 14).dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (is_remote) {
            Icon(
                Icons.Default.Cloud,
                contentDescription = "远程分支",
                tint = colors.editor_hint,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Spacer(Modifier.width(20.dp))
        }
        Text(
            text = node.name,
            color = colors.editor_text,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (is_current) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.editor_button_bg)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "当前",
                    color = colors.editor_icon,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (ref.kind == git_ref_kind.LOCAL && ref.upstream.isNotBlank() &&
            (ref.ahead > 0 || ref.behind > 0)
        ) {
            Text(
                text = "↑${ref.ahead} ↓${ref.behind}",
                color = colors.editor_hint,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

/** 操作表里的一行动作。 */
@Composable
private fun sheet_action(
    label: String,
    icon: ImageVector,
    danger: Boolean = false,
    enabled: Boolean = true,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = on_click)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) colors.danger else colors.editor_icon,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (danger) colors.danger else colors.dialog_text,
            fontSize = 14.sp
        )
    }
}
