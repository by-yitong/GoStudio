package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.toolchain.git_change_entry
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.launch

/**
 * 源代码管理面板（参考 CodeAssist 左侧 Source 面板槽位，git 逻辑走 proot git_manager）：
 * 分支名 + 变更分组列表（未暂存/已暂存/未跟踪）→ 点文件查看着色 diff →
 * 底部提交消息框 + 全部暂存并提交。未初始化仓库时提供 git init 入口。
 */
@Composable
internal fun editor_git_panel(
    project_root_path: String,
    on_open_file: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var git_missing by remember { mutableStateOf(false) }
    var not_a_repo by remember { mutableStateOf(false) }
    var branch by remember { mutableStateOf("") }
    var ahead_count by remember { mutableStateOf(0) }
    var behind_count by remember { mutableStateOf(0) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var entries by remember { mutableStateOf<List<git_change_entry>>(emptyList()) }
    var branch_menu_open by remember { mutableStateOf(false) }
    var creating_branch by remember { mutableStateOf(false) }
    var new_branch_name by remember { mutableStateOf("") }
    var committing by remember { mutableStateOf(false) }
    var operation_busy by remember { mutableStateOf(false) }
    var operation_error by remember { mutableStateOf<String?>(null) }
    var commit_message by remember { mutableStateOf("") }
    var commit_error by remember { mutableStateOf<String?>(null) }
    var viewing_diff by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // path to cached
    var diff_text by remember { mutableStateOf("") }
    var diff_loading by remember { mutableStateOf(false) }
    var action_busy by remember { mutableStateOf(false) }
    var action_error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            git_missing = !com.jmwl.gostudio.toolchain.git_manager.is_git_available()
            if (!git_missing) {
                not_a_repo = !com.jmwl.gostudio.toolchain.git_manager.is_repository(project_root_path)
                if (!not_a_repo) {
                    val status = com.jmwl.gostudio.toolchain.git_manager.status(project_root_path)
                    branch = status.branch
                    entries = status.entries
                    ahead_count = status.ahead_count
                    behind_count = status.behind_count
                    branches = com.jmwl.gostudio.toolchain.git_manager.branches(project_root_path)
                }
            }
            loading = false
        }
    }

    LaunchedEffect(project_root_path) { refresh() }

    fun open_diff(path: String, cached: Boolean) {
        viewing_diff = path to cached
        diff_loading = true
        scope.launch {
            diff_text = com.jmwl.gostudio.toolchain.git_manager.diff(
                project_root_path,
                java.io.File(project_root_path, path).absolutePath,
                cached
            )
            diff_loading = false
        }
    }

    fun run_file_action(
        path: String,
        action: suspend (String) -> Boolean,
        on_success: () -> Unit = {}
    ) {
        if (action_busy) return
        action_busy = true
        action_error = null
        scope.launch {
            val ok = action(path)
            action_busy = false
            if (ok) {
                on_success()
                refresh()
            } else {
                action_error = "Git 操作失败，请查看日志"
            }
        }
    }

    fun run_repo_action(
        action: suspend () -> Boolean,
        on_success: () -> Unit = {}
    ) {
        if (operation_busy) return
        operation_busy = true
        operation_error = null
        scope.launch {
            val ok = action()
            operation_busy = false
            if (ok) {
                on_success()
                refresh()
            } else {
                operation_error = "Git 操作失败，请查看日志"
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        // 顶栏：分支 + 刷新（diff 查看态换成返回）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (viewing_diff != null) {
                IconButton(onClick = { viewing_diff = null }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.editor_icon, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = viewing_diff!!.first.substringAfterLast('/'),
                    color = colors.editor_text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                val tracking_text = buildList {
                    if (ahead_count > 0) add("↑$ahead_count")
                    if (behind_count > 0) add("↓$behind_count")
                    if (entries.isNotEmpty()) add("${entries.size} 项变更")
                }.joinToString(" · ")
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = branches.isNotEmpty() && !operation_busy) {
                                branch_menu_open = true
                            }
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = branch.ifBlank { "源代码管理" },
                            color = colors.editor_text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (branches.isNotEmpty()) {
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = "切换分支",
                                tint = colors.editor_hint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = branch_menu_open,
                        onDismissRequest = { branch_menu_open = false }
                    ) {
                        branches.forEach { item_branch ->
                            DropdownMenuItem(
                                text = { Text(item_branch) },
                                onClick = {
                                    branch_menu_open = false
                                    if (item_branch != branch) {
                                        run_repo_action(action = {
                                            com.jmwl.gostudio.toolchain.git_manager.checkout(project_root_path, item_branch)
                                        })
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("新建分支...") },
                            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = colors.editor_icon) },
                            onClick = {
                                branch_menu_open = false
                                new_branch_name = ""
                                creating_branch = true
                            }
                        )
                    }
                }
                if (tracking_text.isNotEmpty()) {
                    Text(
                        text = tracking_text,
                        color = colors.editor_hint,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = { if (viewing_diff != null) viewing_diff = null else refresh() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = colors.editor_hint, modifier = Modifier.size(18.dp))
            }
        }

        viewing_diff?.let { current_diff ->
            val entry = entries.firstOrNull { it.path == current_diff.first }
            if (entry != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        entry.is_untracked -> git_panel_action(
                            label = if (action_busy) "处理中..." else "暂存文件",
                            enabled = !action_busy,
                            emphasized = true
                        ) {
                            run_file_action(entry.path, action = {
                                com.jmwl.gostudio.toolchain.git_manager.stage(project_root_path, listOf(it))
                            })
                        }
                        entry.is_staged -> git_panel_action(
                            label = if (action_busy) "处理中..." else "取消暂存",
                            enabled = !action_busy
                        ) {
                            run_file_action(entry.path, action = {
                                com.jmwl.gostudio.toolchain.git_manager.unstage(project_root_path, listOf(it))
                            })
                        }
                        else -> {
                            git_panel_action(
                                label = if (action_busy) "处理中..." else "暂存",
                                enabled = !action_busy
                            ) {
                                run_file_action(entry.path, action = {
                                    com.jmwl.gostudio.toolchain.git_manager.stage(project_root_path, listOf(it))
                                })
                            }
                            git_panel_action(
                                label = "放弃修改",
                                danger = true,
                                enabled = !action_busy
                            ) {
                                run_file_action(
                                    entry.path,
                                    action = {
                                        com.jmwl.gostudio.toolchain.git_manager.discard(project_root_path, listOf(it))
                                    },
                                    on_success = { viewing_diff = null }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (viewing_diff == null && !not_a_repo && !git_missing && !loading) {
            operation_error?.let {
                Text(
                    text = it,
                    color = colors.danger,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                git_panel_action(
                    label = if (operation_busy) "处理中..." else "拉取",
                    enabled = !operation_busy
                ) {
                    run_repo_action(action = { com.jmwl.gostudio.toolchain.git_manager.pull(project_root_path) })
                }
                git_panel_action(
                    label = "推送",
                    enabled = !operation_busy
                ) {
                    run_repo_action(action = { com.jmwl.gostudio.toolchain.git_manager.push(project_root_path) })
                }
                git_panel_action(
                    label = "全部暂存",
                    enabled = !operation_busy && entries.isNotEmpty()
                ) {
                    run_repo_action(action = { com.jmwl.gostudio.toolchain.git_manager.stage_all(project_root_path) })
                }
            }
        }

        if (creating_branch) {
            AlertDialog(
                onDismissRequest = { creating_branch = false },
                title = { Text("新建分支") },
                text = {
                    OutlinedTextField(
                        value = new_branch_name,
                        onValueChange = { new_branch_name = it },
                        singleLine = true,
                        placeholder = { Text("分支名") }
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = new_branch_name.isNotBlank() && !operation_busy,
                        onClick = {
                            creating_branch = false
                            run_repo_action(action = {
                                com.jmwl.gostudio.toolchain.git_manager.create_branch(project_root_path, new_branch_name)
                            })
                        }
                    ) { Text("创建") }
                },
                dismissButton = {
                    TextButton(onClick = { creating_branch = false }) { Text("取消") }
                }
            )
        }

        action_error?.let {
            Text(
                text = it,
                color = colors.danger,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("读取中...", color = colors.editor_hint, fontSize = 13.sp)
            }
            git_missing -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "git 未安装\n到「开发工具」页安装 git",
                    color = colors.editor_hint,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            not_a_repo -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("当前项目不是 git 仓库", color = colors.editor_hint, fontSize = 13.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.editor_button_bg)
                            .clickable {
                                scope.launch {
                                    loading = true
                                    if (com.jmwl.gostudio.toolchain.git_manager.init_repository(project_root_path)) refresh()
                                    else loading = false
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("初始化仓库", color = colors.editor_icon, fontSize = 13.sp)
                    }
                }
            }
            viewing_diff != null -> {
                if (diff_loading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("加载 diff...", color = colors.editor_hint, fontSize = 13.sp)
                    }
                } else {
                    if (diff_text.isBlank()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "新文件暂存后可查看 diff",
                                color = colors.editor_hint,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        diff_content(diff_text, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            else -> {
                val staged = entries.filter { it.is_staged }
                val unstaged = entries.filter { it.is_modified && !it.is_staged }
                val untracked = entries.filter { it.is_untracked }

                if (entries.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("没有变更", color = colors.editor_hint, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        change_group("已暂存", staged) { entry ->
                            open_diff(entry.path, cached = true)
                        }
                        change_group("更改", unstaged) { entry ->
                            open_diff(entry.path, cached = false)
                        }
                        change_group("未跟踪", untracked) { entry ->
                            open_diff(entry.path, cached = false)
                        }
                    }
                }

                // 提交区
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.editor_sidebar_selected_bg.copy(alpha = 0.3f))
                        .padding(10.dp)
                ) {
                    commit_error?.let {
                        Text(it, color = colors.danger, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .background(colors.editor_button_bg, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        if (commit_message.isEmpty()) {
                            Text("提交消息...", color = colors.editor_hint, fontSize = 13.sp)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = commit_message,
                            onValueChange = { commit_message = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = colors.editor_text, fontSize = 13.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.editor_icon),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (committing) colors.editor_button_bg else colors.editor_sidebar_selected_bg)
                            .clickable(enabled = !committing && commit_message.isNotBlank() && entries.isNotEmpty()) {
                                committing = true
                                commit_error = null
                                scope.launch {
                                    val ok = com.jmwl.gostudio.toolchain.git_manager.commit_all(project_root_path, commit_message)
                                    committing = false
                                    if (ok) {
                                        commit_message = ""
                                        refresh()
                                    } else {
                                        commit_error = "提交失败，请查看日志"
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.editor_icon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (committing) "提交中..." else "全部暂存并提交 (${entries.size})",
                            color = colors.editor_icon,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

}

private fun androidx.compose.foundation.lazy.LazyListScope.change_group(
    title: String,
    group_entries: List<git_change_entry>,
    on_entry_click: (git_change_entry) -> Unit
) {
    if (group_entries.isEmpty()) return
    item(key = "group-$title") {
        val colors = app_theme_provider.colors
        Text(
            text = "$title (${group_entries.size})",
            color = colors.editor_hint,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
        )
    }
    items(group_entries, key = { "$title-${it.path}" }) { entry ->
        git_change_row(entry) { on_entry_click(entry) }
    }
}

@Composable
private fun git_panel_action(
    label: String,
    enabled: Boolean,
    danger: Boolean = false,
    emphasized: Boolean = false,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    emphasized -> colors.editor_sidebar_selected_bg
                    danger -> colors.danger_bg
                    else -> colors.editor_button_bg
                }
            )
            .clickable(enabled = enabled, onClick = on_click)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (danger) colors.danger else colors.editor_icon,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun git_change_row(entry: git_change_entry, on_click: () -> Unit) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = on_click)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (badge, tint) = when {
            entry.is_untracked -> "U" to colors.editor_hint
            entry.is_staged -> entry.staged_status.toString() to colors.success
            entry.worktree_status == 'D' -> "D" to colors.danger
            else -> "M" to colors.warning
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(tint.copy(alpha = 0.16f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(badge, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = entry.path.substringAfterLast('/'),
            color = colors.editor_text,
            fontSize = 13.sp
        )
        Text(
            text = entry.path.substringBeforeLast('/'),
            color = colors.editor_hint,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/** git diff 着色展示：+绿 / -红 / @@ 青 / 头部灰。 */
@Composable
private fun diff_content(diff: String, modifier: Modifier = Modifier) {
    val colors = app_theme_provider.colors
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        diff.lines().forEachIndexed { index, line ->
            val tint = when {
                line.startsWith("+") -> colors.success
                line.startsWith("-") -> colors.danger
                line.startsWith("@@") -> colors.editor_icon
                line.startsWith("diff ") || line.startsWith("index ") -> colors.editor_hint
                else -> colors.editor_text
            }
            Text(
                text = line.ifBlank { " " },
                color = tint,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
