package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_message
import com.jmwl.gostudio.ai.ai_message_role
import com.jmwl.gostudio.ai.ai_tool_execution
import com.jmwl.gostudio.ai.ai_tool_status
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * 单条消息气泡。
 * - user: 右对齐，强调色背景
 * - assistant: 左对齐，卡片背景，支持代码块/流式光标
 * - tool: 不单独显示气泡（归入 assistant 的工具卡片）
 */
@Composable
fun ai_message_bubble(message: ai_message) {
    // 读思考过程开关：关闭时隐藏工具执行卡片（只显示最终文本回复）
    val context = androidx.compose.ui.platform.LocalContext.current
    val show_thinking = remember(message) {
        com.jmwl.gostudio.ai.load_ai_settings(context).show_thinking_process
    }
    val has_visible = message.has_visible_text || (show_thinking && message.tool_executions.isNotEmpty())
    if (!has_visible) return

    val colors = app_theme_provider.colors
    val is_user = message.role == ai_message_role.USER

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalArrangement = if (is_user) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 14.dp, topEnd = 14.dp,
                bottomStart = if (is_user) 14.dp else 4.dp,
                bottomEnd = if (is_user) 4.dp else 14.dp
            ),
            color = when {
                message.is_error -> colors.danger_bg.copy(alpha = 0.5f)
                is_user -> colors.title_highlight.copy(alpha = 0.15f)
                else -> colors.card_bg
            }
        ) {
            Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                // 工具调用卡片（assistant 消息附带，受思考过程开关控制）
                if (show_thinking && message.tool_executions.isNotEmpty()) {
                    message.tool_executions.forEach { exec ->
                        ai_tool_execution_card(exec)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                // 文本内容（代码块用等宽字体着色）
                if (message.has_visible_text) {
                    ai_text_with_code_blocks(
                        text = message.text,
                        color = if (message.is_error) colors.danger else colors.card_text_title,
                        streaming = message.streaming
                    )
                }
            }
        }
    }
}

/**
 * 工具调用卡片（可折叠，展示工具名/状态/结果）。
 */
@Composable
fun ai_tool_execution_card(exec: ai_tool_execution) {
    val colors = app_theme_provider.colors
    var expanded by remember(exec.call.id) { mutableStateOf(false) }

    val (icon, tint) = when (exec.status) {
        ai_tool_status.RUNNING -> Icons.Default.HourglassEmpty to colors.warning
        ai_tool_status.ERROR -> Icons.Default.ErrorOutline to colors.danger
        ai_tool_status.DONE -> Icons.Default.CheckCircle to colors.success
        else -> Icons.Default.PlayCircle to colors.subtitle
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded },
        color = colors.dialog_clone_bg.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
                Text(
                    text = tool_display_name(exec.call.name),
                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    color = colors.card_text_title
                )
                Text(
                    text = when (exec.status) {
                        ai_tool_status.RUNNING -> "执行中…"
                        ai_tool_status.ERROR -> "失败"
                        ai_tool_status.DONE -> "完成"
                        else -> ""
                    },
                    fontSize = 10.sp, color = tint
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(13.dp)
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    // 工具参数
                    Text(
                        text = exec.call.arguments_json.take(300),
                        fontSize = 9.5.sp, fontFamily = FontFamily.Monospace,
                        color = colors.card_text_subtitle,
                        modifier = Modifier.fillMaxWidth().background(colors.editor_bg.copy(alpha = 0.4f)).padding(6.dp)
                    )
                    // 结果
                    if (exec.result.isNotBlank() || exec.error_message != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = exec.to_result_content().take(2000),
                            fontSize = 9.5.sp, fontFamily = FontFamily.Monospace,
                            color = if (exec.error_message != null) colors.danger else colors.card_text_subtitle,
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                                .heightIn(max = 180.dp)
                                .background(colors.editor_bg.copy(alpha = 0.4f)).padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 工具名的中文展示 */
private fun tool_display_name(name: String): String = when (name) {
    "read" -> "📖 读取文件"
    "write" -> "✏️ 写入文件"
    "edit" -> "🔧 修改代码"
    "bash" -> "▶️ 执行命令"
    "grep" -> "🔍 搜索"
    "ls" -> "📁 列目录"
    else -> "🔨 $name"
}

/**
 * 简易文本渲染：识别 ``` 代码块，用等宽字体+背景色渲染。
 * 流式时末尾加光标 ▌。
 * （完整 markdown 渲染后续可加，第一版用这个够用）
 */
@Composable
fun ai_text_with_code_blocks(text: String, color: Color, streaming: Boolean) {
    val colors = app_theme_provider.colors
    val segments = remember(text) { split_code_blocks(text) }

    Column {
        for (seg in segments) {
            if (seg.is_code) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = colors.editor_bg.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = seg.content,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.editor_text,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else if (seg.content.isNotBlank()) {
                Text(
                    text = seg.content,
                    fontSize = 13.sp,
                    color = color,
                    lineHeight = 18.sp
                )
            }
        }
        if (streaming) {
            Text("▌", fontSize = 13.sp, color = colors.title_highlight)
        }
    }
}

private data class text_segment(val content: String, val is_code: Boolean)

private fun split_code_blocks(text: String): List<text_segment> {
    val result = mutableListOf<text_segment>()
    val regex = Regex("```(\\w*)\\n?([\\s\\S]*?)```")
    var lastEnd = 0
    for (m in regex.findAll(text)) {
        if (m.range.first > lastEnd) {
            result.add(text_segment(text.substring(lastEnd, m.range.first), false))
        }
        result.add(text_segment(m.groupValues[2].trimEnd('\n'), true))
        lastEnd = m.range.last + 1
    }
    if (lastEnd < text.length) result.add(text_segment(text.substring(lastEnd), false))
    // 流式中可能有不完整的 ``` 开头（代码块还没闭合）
    val afterLast = result.lastOrNull()?.takeIf { !it.is_code }
    if (afterLast != null && afterLast.content.contains("```")) {
        val idx = afterLast.content.indexOf("```")
        if (idx >= 0) {
            result[result.lastIndex] = text_segment(afterLast.content.substring(0, idx), false)
            result.add(text_segment(afterLast.content.substring(idx + 3), true))
        }
    }
    return result
}
