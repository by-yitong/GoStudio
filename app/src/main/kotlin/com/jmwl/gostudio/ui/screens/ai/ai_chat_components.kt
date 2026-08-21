package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
 *
 * @param show_thinking 是否展示工具执行卡片（由调用方从设置读一次后传入，避免每条消息都读磁盘）
 * @param on_copy/on_share/on_delete/on_regenerate/on_edit 长按菜单动作；为 null 表示该动作不可用
 */
@Composable
fun ai_message_bubble(
    message: ai_message,
    show_thinking: Boolean = true,
    on_copy: (() -> Unit)? = null,
    on_share: (() -> Unit)? = null,
    on_delete: (() -> Unit)? = null,
    on_regenerate: (() -> Unit)? = null,
    on_edit: ((String) -> Unit)? = null
) {
    val has_visible = message.has_visible_text || (show_thinking && message.tool_executions.isNotEmpty())
    if (!has_visible) return

    val colors = app_theme_provider.colors
    val is_user = message.role == ai_message_role.USER

    val bubble_content: @Composable () -> Unit = {
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
                    // reasoning 思考链卡片（DeepSeek reasoning_content / Anthropic thinking）
                    if (show_thinking && message.reasoning.isNotBlank()) {
                        ai_reasoning_card(message.reasoning, message.streaming)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    // 流式中且还没有文本/思考：显示打字指示器
                    if (message.streaming && !message.has_visible_text && message.reasoning.isBlank()) {
                        ai_typing_indicator()
                    }
                    // 文本内容（Markdown 渲染：标题/列表/表格/代码块/行内格式）
                    if (message.has_visible_text) {
                        ai_markdown_text(
                            text = message.text,
                            color = if (message.is_error) colors.danger else colors.card_text_title,
                            streaming = message.streaming
                        )
                    }
                }
            }
        }
    }

    // 有操作回调时，包一层长按菜单
    if (on_copy != null || on_delete != null) {
        ai_message_with_actions(
            message = message,
            on_copy = { on_copy?.invoke() },
            on_share = { on_share?.invoke() },
            on_delete = { on_delete?.invoke() },
            on_regenerate = { on_regenerate?.invoke() },
            on_edit = on_edit,
            content = bubble_content
        )
    } else {
        bubble_content()
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
 * @deprecated 由 [ai_markdown_text] 取代（完整 Markdown 渲染）。保留空壳避免外部引用断裂。
 */
@Composable
fun ai_text_with_code_blocks(text: String, color: Color, streaming: Boolean) {
    ai_markdown_text(text = text, color = color, streaming = streaming)
}

/**
 * reasoning 思考链卡片（可折叠）。流式中默认展开，结束后默认折叠。
 */
@Composable
fun ai_reasoning_card(reasoning: String, streaming: Boolean) {
    val colors = app_theme_provider.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val auto_expand = remember { com.jmwl.gostudio.ai.load_ai_settings(context).auto_expand_thinking }
    var expanded by remember(reasoning.isNotEmpty()) { mutableStateOf(streaming || auto_expand) }

    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded },
        color = colors.editor_bg.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("💭", fontSize = 11.sp)
                Text(
                    text = if (streaming) "思考中…" else "思考过程",
                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    color = colors.subtitle
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(13.dp)
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Text(
                    text = reasoning,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.subtitle,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 5.dp).heightIn(max = 200.dp).verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

/**
 * 打字指示器：三个错相位跳动的圆点。
 */
@Composable
fun ai_typing_indicator() {
    val colors = app_theme_provider.colors
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$i"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(colors.title_highlight.copy(alpha = alpha))
            )
        }
    }
}
