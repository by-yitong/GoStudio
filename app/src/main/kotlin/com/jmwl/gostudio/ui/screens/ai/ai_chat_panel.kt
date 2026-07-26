package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_agent_loop
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.launch

/**
 * AI 聊天面板（消息流 + 输入框）。同时用于：
 * - 编辑器侧边栏「助手」抽屉（嵌入版）
 * - 主界面 agent 路由（全屏版，外层包 Scaffold）
 *
 * @param agent agent loop 实例
 * @param on_open_settings 打开 AI 设置页的回调
 */
@Composable
fun ai_chat_panel(
    agent: ai_agent_loop,
    on_open_settings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    var input by rememberSaveable { mutableStateOf("") }
    val is_running by agent.is_running.collectAsState()
    val list_state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 新消息时自动滚到底
    LaunchedEffect(agent.messages.size) {
        if (agent.messages.isNotEmpty()) {
            list_state.animateScrollToItem(agent.messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部工具条：标题 + 设置 + 清空
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = colors.title_highlight, modifier = Modifier.size(18.dp))
            Text("AI 助手", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.title_large, modifier = Modifier.weight(1f))
            IconButton(onClick = on_open_settings, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "AI 设置", tint = colors.top_button_icon, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { agent.clear_messages() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "清空对话", tint = colors.top_button_icon, modifier = Modifier.size(18.dp))
            }
        }

        HorizontalDivider(color = colors.input_border.copy(alpha = 0.3f))

        // 消息流
        if (agent.messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = colors.subtitle.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                    Text("问点什么呢？", fontSize = 13.sp, color = colors.subtitle)
                    Text("可以问我 Go 编程问题、解释代码、修复错误", fontSize = 11.sp, color = colors.subtitle.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = list_state,
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(agent.messages, key = { it.timestamp.toString() + it.role.name + it.text.hashCode() }) { msg ->
                    ai_message_bubble(msg)
                }
            }
        }

        // 输入区
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入问题…", fontSize = 13.sp, color = colors.input_hint) },
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
            if (is_running) {
                FilledIconButton(
                    onClick = { agent.cancel() },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.danger)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "停止", tint = Color.White, modifier = Modifier.size(20.dp))
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
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
