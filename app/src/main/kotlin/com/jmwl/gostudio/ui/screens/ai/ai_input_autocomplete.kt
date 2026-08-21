package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider
import java.io.File

/** 自动补全候选项 */
data class ai_completion_item(
    val label: String,
    val detail: String = "",
    val insert: String,
    val icon_type: String // "file" / "command"
)

/**
 * 解析输入框文本，在光标位置识别触发符：
 * - `@` 触发文件补全：扫描 `@` 后的词，匹配项目文件
 * - `/` 触发命令补全：扫描 `/` 后的词，匹配 prompt 模板 + 内置命令
 *
 * @return 补全列表 + 触发起点偏移（候选词在原文中的起始位置），null 表示无补全
 */
fun compute_completions(
    text: String,
    cursor: Int,
    project_dir: File?,
    global_prompts_dir: File?,
    project_prompts_dir: File?
): Pair<List<ai_completion_item>, Int>? {
    if (cursor <= 0 || cursor > text.length) return null
    // 向左扫触发符（同行内，遇到空格/换行停止）
    var i = cursor - 1
    while (i >= 0) {
        val c = text[i]
        if (c == '\n' || c == ' ' || c == '\t') return null
        if (c == '@' || c == '/') {
            val trigger_pos = i
            val prefix = text.substring(i + 1, cursor).lowercase()
            return when (c) {
                '@' -> file_completions(prefix, project_dir)?.let { it to trigger_pos }
                '/' -> command_completions(prefix, global_prompts_dir, project_prompts_dir)?.let { it to trigger_pos }
                else -> null
            }
        }
        i--
    }
    return null
}

/** 文件补全：列出项目下匹配 prefix 的文件（限制深度和数量） */
private fun file_completions(prefix: String, project_dir: File?): List<ai_completion_item>? {
    if (project_dir == null || !project_dir.isDirectory) return null
    val results = mutableListOf<ai_completion_item>()
    val seen = mutableSetOf<String>()
    try {
        project_dir.walkTopDown().maxDepth(4).forEach { f ->
            if (results.size >= 15) return results
            if (!f.isFile) return@forEach
            // 跳过隐藏、构建产物
            val rel = f.relativeTo(project_dir).path
            if (rel.startsWith(".") || rel.contains("/.git/") || rel.startsWith("bin/") ||
                rel.contains("/node_modules/") || f.length() > 200_000) return@forEach
            val name = f.name
            if (prefix.isBlank() || name.lowercase().contains(prefix) || rel.lowercase().contains(prefix)) {
                if (seen.add(rel)) {
                    results.add(ai_completion_item(
                        label = name,
                        detail = rel,
                        insert = "@$rel ",
                        icon_type = "file"
                    ))
                }
            }
        }
    } catch (_: Exception) { }
    return if (results.isEmpty()) null else results
}

/** 命令补全：prompt 模板 + 内置命令 */
private fun command_completions(
    prefix: String,
    global_prompts_dir: File?,
    project_prompts_dir: File?
): List<ai_completion_item>? {
    val results = mutableListOf<ai_completion_item>()
    val seen = mutableSetOf<String>()
    // 项目 prompts 优先
    val dirs = listOfNotNull(project_prompts_dir, global_prompts_dir)
    for (dir in dirs) {
        if (!dir.isDirectory) continue
        try {
            dir.listFiles { f -> f.isFile && f.name.endsWith(".md") }?.forEach { f ->
                val name = f.nameWithoutExtension
                if ((prefix.isBlank() || name.lowercase().startsWith(prefix)) && seen.add(name)) {
                    results.add(ai_completion_item(
                        label = "/$name",
                        detail = "命令模板",
                        insert = "/$name ",
                        icon_type = "command"
                    ))
                }
            }
        } catch (_: Exception) { }
    }
    // 内置 skill 提示
    if (prefix.isBlank() || "skill".startsWith(prefix)) {
        results.add(ai_completion_item(
            label = "/skill:<名称>",
            detail = "激活技能",
            insert = "/skill:",
            icon_type = "command"
        ))
    }
    return if (results.isEmpty()) null else results
}

/**
 * 输入补全浮层：根据当前文本+光标位置显示候选，选中后回调插入。
 */
@Composable
fun ai_input_completion_overlay(
    text: String,
    cursor: Int,
    project_dir: File?,
    global_prompts_dir: File?,
    project_prompts_dir: File?,
    on_select: (insert_text: String, trigger_start: Int, cursor: Int) -> Unit
) {
    val colors = app_theme_provider.colors
    val computed = remember(text, cursor) {
        compute_completions(text, cursor, project_dir, global_prompts_dir, project_prompts_dir)
    }
    val (items, trigger_start) = computed ?: (null to 0)

    DropdownMenu(
        expanded = items != null && items.isNotEmpty(),
        onDismissRequest = { /* 由文本变化自然关闭 */ }
    ) {
        items?.forEach { item ->
            DropdownMenuItem(
                text = {
                    androidx.compose.foundation.layout.Column {
                        Text(item.label, fontSize = 13.sp, color = colors.dialog_text)
                        if (item.detail.isNotBlank()) {
                            Text(item.detail, fontSize = 10.sp, color = colors.subtitle)
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (item.icon_type == "file") Icons.Default.Article else Icons.Default.Terminal,
                        contentDescription = null,
                        tint = colors.subtitle,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                onClick = { on_select(item.insert, trigger_start, cursor) }
            )
        }
    }
}
