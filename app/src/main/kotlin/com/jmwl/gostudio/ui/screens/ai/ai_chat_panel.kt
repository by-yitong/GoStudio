package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        var model_menu_open by remember { mutableStateOf(false) }
        // 顶部工具条：模型选择 + 设置 + 清空
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 提供商/模型选择器
            Box(modifier = Modifier.weight(1f)) {
                val has_any_configured = configured_providers.isNotEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // 没有任何已配置供应商时，点击直接跳设置页
                            if (has_any_configured) model_menu_open = true else on_open_settings()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = colors.title_highlight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (has_any_configured) {
                            Text(
                                text = current_provider.display_name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.title_large,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = current_model,
                                fontSize = 10.sp,
                                color = colors.subtitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "未配置",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.danger,
                                maxLines = 1
                            )
                            Text(
                                text = "点击设置 AI 提供商",
                                fontSize = 10.sp,
                                color = colors.subtitle,
                                maxLines = 1
                            )
                        }
                    }
                    if (has_any_configured) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "切换", tint = colors.subtitle, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Settings, contentDescription = "去设置", tint = colors.subtitle, modifier = Modifier.size(14.dp))
                    }
                }
                DropdownMenu(
                    expanded = model_menu_open,
                    onDismissRequest = { model_menu_open = false }
                ) {
                    // 只显示已配置 key 的供应商
                    val visible_providers = ai_provider.entries.filter { it in configured_providers }
                    if (visible_providers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("暂无已配置的供应商，去设置？", color = colors.subtitle, fontSize = 12.sp) },
                            onClick = { model_menu_open = false; on_open_settings() }
                        )
                    } else {
                        visible_providers.forEach { p ->
                            val models = (available_models[p] ?: emptyList()) + p.default_models
                            val deduped = models.distinct()
                            if (deduped.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            p.display_name,
                                            color = if (p == current_provider) colors.title_highlight else colors.dialog_text,
                                            fontWeight = if (p == current_provider) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        on_session_model_change(p, p.default_model.ifBlank { current_model })
                                        model_menu_open = false
                                    }
                                )
                                deduped.forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    m,
                                                    color = if (p == current_provider && m == current_model) colors.title_highlight else colors.subtitle,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (p == current_provider && m == current_model) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.title_highlight, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            on_session_model_change(p, m)
                                            model_menu_open = false
                                        }
                                    )
                                }
                                HorizontalDivider(color = colors.input_border.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
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
                itemsIndexed(agent.messages, key = { index, msg -> "$index-${msg.timestamp}-${msg.role.name}" }) { _, msg ->
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
