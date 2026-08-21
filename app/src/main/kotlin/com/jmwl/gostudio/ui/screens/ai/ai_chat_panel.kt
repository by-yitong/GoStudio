package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_agent_loop
import com.jmwl.gostudio.ai.ai_provider
import com.jmwl.gostudio.ai.ai_settings_state
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.launch

/**
 * AI 聊天面板（消息流 + 输入框）。同时用于：
 * - 编辑器侧边栏「助手」抽屉（嵌入版）
 * - 主界面 agent 路由（全屏版，外层包 Scaffold）
 *
 * @param agent agent loop 实例
 * @param on_open_settings 打开 AI 设置页的回调
 * @param current_provider 当前会话生效的提供商
 * @param current_model 当前会话生效的模型
 * @param available_models 每个供应商可用的模型（默认列表 + 动态获取），用于选择器展示
 * @param on_session_model_change 会话内切换提供商/模型的回调
 */
@Composable
fun ai_chat_panel(
    agent: ai_agent_loop,
    on_open_settings: () -> Unit,
    current_provider: ai_provider,
    current_model: String,
    available_models: Map<ai_provider, List<String>> = emptyMap(),
    configured_providers: Set<ai_provider> = emptySet(),
    on_session_model_change: (ai_provider, String) -> Unit = { _, _ -> },
    project_dir: java.io.File? = null,
    global_prompts_dir: java.io.File? = null,
    project_prompts_dir: java.io.File? = null,
    suggestion_prompts: List<String> = default_suggestion_prompts,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    val is_running by agent.is_running.collectAsState()
    val list_state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // 思考过程开关：顶层读一次（避免每条消息组合里都读磁盘）
    val show_thinking = remember { com.jmwl.gostudio.ai.load_ai_settings(context).show_thinking_process }
    // 最后一条消息文本长度（流式增长时也触发滚动）
    val last_text_len = agent.messages.lastOrNull()?.text?.length ?: 0

    // 新消息 or 流式增长时自动滚到底
    LaunchedEffect(agent.messages.size, last_text_len) {
        if (agent.messages.isNotEmpty()) {
            list_state.animateScrollToItem(agent.messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        var show_clear_confirm by remember { mutableStateOf(false) }
        var show_history by remember { mutableStateOf(false) }
        var sessions by remember { mutableStateOf(agent.list_sessions()) }
        // 历史会话覆盖层（替换整个面板内容）
        if (show_history) {
            ai_session_history_screen(
                sessions = sessions,
                current_session_id = agent.current_session_id(),
                on_back = { show_history = false },
                on_switch = { new_id ->
                    scope.launch {
                        agent.switch_session(new_id)
                        show_history = false
                    }
                },
                on_new = {
                    agent.new_session()
                    sessions = agent.list_sessions()
                    show_history = false
                },
                on_rename = { id, title ->
                    agent.rename_session(title)
                    sessions = agent.list_sessions()
                },
                on_delete = { id ->
                    agent.delete_session_by_id(id)
                    sessions = agent.list_sessions()
                }
            )
            return@Column
        }
        // 顶部工具条：模型选择 + 设置 + 历史
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 提供商/模型选择器
            ai_model_selector(
                current_provider = current_provider,
                current_model = current_model,
                available_models = available_models,
                configured_providers = configured_providers,
                on_session_model_change = on_session_model_change,
                on_open_settings = on_open_settings,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = on_open_settings, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "AI 设置", tint = colors.top_button_icon, modifier = Modifier.size(18.dp))
            }
            // 历史：点击打开历史列表；长按清空当前对话
            Box {
                IconButton(
                    onClick = {
                        sessions = agent.list_sessions()
                        show_history = true
                    },
                    modifier = Modifier.size(32.dp).combinedClickable(
                        onClick = {
                            sessions = agent.list_sessions()
                            show_history = true
                        },
                        onLongClick = { show_clear_confirm = true }
                    )
                ) {
                    Icon(Icons.Default.History, contentDescription = "历史会话", tint = colors.top_button_icon, modifier = Modifier.size(18.dp))
                }
            }
        }

        HorizontalDivider(color = colors.input_border.copy(alpha = 0.3f))

        // 消息流
        if (agent.messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = colors.subtitle.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                    Text("问点什么呢？", fontSize = 14.sp, color = colors.subtitle)
                    Text("可以问我 Go 编程问题、解释代码、修复错误", fontSize = 11.sp, color = colors.subtitle.copy(alpha = 0.7f))
                    // 快捷建议 chips
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        suggestion_prompts.take(6).forEach { prompt ->
                            ai_suggestion_chip(prompt) { input = prompt }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = list_state,
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                itemsIndexed(agent.messages, key = { index, msg -> "$index-${msg.timestamp}-${msg.role.name}" }) { index, msg ->
                    ai_message_bubble(
                        message = msg,
                        show_thinking = show_thinking,
                        on_copy = { copy_text_to_clipboard(context, msg.text) },
                        on_share = { share_text(context, msg.text) },
                        on_delete = { agent.delete_message(index) },
                        on_regenerate = if (msg.role == com.jmwl.gostudio.ai.ai_message_role.ASSISTANT) {
                            { agent.regenerate_last() }
                        } else null,
                        on_edit = if (msg.role == com.jmwl.gostudio.ai.ai_message_role.USER) {
                            { new_text -> agent.edit_and_resend_user(index, new_text) }
                        } else null
                    )
                }
            }
        }

        // 清空确认弹窗
        if (show_clear_confirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { show_clear_confirm = false },
                title = { Text("清空对话", color = colors.dialog_text) },
                text = { Text("确定要清空当前对话吗？此操作不可撤销。", color = colors.dialog_text) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        show_clear_confirm = false
                        agent.clear_messages()
                    }) {
                        Text("清空", color = colors.danger)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { show_clear_confirm = false }) {
                        Text("取消", color = colors.subtitle)
                    }
                }
            )
        }

        // 输入区（imePadding 让键盘不遮挡）
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).imePadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 输入框 + 自动补全浮层（Box 锚点）
            Box(modifier = Modifier.weight(1f)) {
                var cursor_pos by remember { mutableStateOf(0) }
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        cursor_pos = it.length
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入问题…  用 @ 引用文件，/ 调用命令", fontSize = 12.sp, color = colors.input_hint) },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.input_text,
                        unfocusedTextColor = colors.input_text,
                        focusedBorderColor = colors.title_highlight,
                        unfocusedBorderColor = colors.input_border,
                        cursorColor = colors.title_highlight,
                        focusedContainerColor = colors.card_bg,
                        unfocusedContainerColor = colors.card_bg
                    )
                )
                // 自动补全浮层（锚定在输入框上方）
                ai_input_completion_overlay(
                    text = input,
                    cursor = cursor_pos,
                    project_dir = project_dir,
                    global_prompts_dir = global_prompts_dir,
                    project_prompts_dir = project_prompts_dir,
                    on_select = { insert, trigger_start, _ ->
                        // 替换从 trigger_start 到末尾的部分
                        input = input.substring(0, trigger_start) + insert
                        cursor_pos = input.length
                    }
                )
            }
            if (is_running) {
                FilledIconButton(
                    onClick = { agent.cancel() },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.danger)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "停止", tint = colors.dialog_clone_text, modifier = Modifier.size(20.dp))
                }
            } else {
                FilledIconButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotEmpty()) {
                            agent.send_user_message(text)
                            input = ""
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    enabled = input.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.title_highlight,
                        disabledContainerColor = colors.title_highlight.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送", tint = colors.dialog_clone_text, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** 复制文本到系统剪贴板 */
internal fun copy_text_to_clipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AI 消息", text))
}

/** 默认的快捷建议 prompts */
val default_suggestion_prompts = listOf(
    "解释当前文件的作用",
    "帮我找一下这段代码的 bug",
    "写一个单元测试",
    "优化这段代码的性能",
    "这段代码怎么用？给个示例",
    "解释一下这个错误"
)

/** 快捷建议 chip */
@Composable
internal fun ai_suggestion_chip(text: String, on_click: () -> Unit) {
    val colors = app_theme_provider.colors
    androidx.compose.material3.AssistChip(
        onClick = on_click,
        label = { Text(text, fontSize = 11.sp, color = colors.card_text_title) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = colors.card_bg,
            labelColor = colors.card_text_title
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.input_border.copy(alpha = 0.3f))
    )
}
