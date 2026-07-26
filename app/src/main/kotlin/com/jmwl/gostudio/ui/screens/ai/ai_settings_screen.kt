package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_provider
import com.jmwl.gostudio.ai.ai_settings_state
import com.jmwl.gostudio.ai.default_state_for_provider
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * AI 设置页：提供商选择 + base_url/model + API key + 工具开关。
 *
 * @param initial 当前设置（load_ai_settings 读出的）
 * @param on_save 保存回调（参数为新设置）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ai_settings_screen(
    initial: ai_settings_state,
    on_back: () -> Unit,
    on_save: (ai_settings_state) -> Unit
) {
    val colors = app_theme_provider.colors
    var settings by remember { mutableStateOf(initial) }
    var key_visible by remember { mutableStateOf(false) }
    var provider_menu_open by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 设置", color = colors.title_large) },
                navigationIcon = {
                    IconButton(onClick = on_back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.top_button_icon)
                    }
                },
                actions = {
                    TextButton(onClick = { on_save(settings) }) {
                        Text("保存", color = colors.title_highlight, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = colors.gradient_start.copy(alpha = 0.03f)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 提供商选择
            section_title("AI 提供商")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colors.card_bg
            ) {
                Box {
                    OutlinedTextField(
                        value = settings.provider.display_name,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable { provider_menu_open = true } as Modifier,
                        label = { Text("提供商", color = colors.input_hint) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.subtitle) },
                        colors = field_colors(colors)
                    )
                    DropdownMenu(expanded = provider_menu_open, onDismissRequest = { provider_menu_open = false }) {
                        ai_provider.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.display_name, color = colors.dialog_text) },
                                onClick = {
                                    if (p != ai_provider.CUSTOM) settings = default_state_for_provider(p)
                                    else settings = settings.copy(provider = p)
                                    provider_menu_open = false
                                }
                            )
                        }
                    }
                }
            }

            // base_url
            section_title("接口地址 (Base URL)")
            outlined_field(
                value = settings.base_url,
                on_change = { settings = settings.copy(base_url = it) },
                placeholder = "https://api.example.com/v1",
                colors = colors
            )
            Text(
                "OpenAI 兼容格式。${if (settings.provider == ai_provider.ANTHROPIC) "⚠️ Anthropic 原生格式暂不支持，请用兼容中转。" else ""}",
                fontSize = 10.sp, color = colors.subtitle
            )

            // model
            section_title("模型名称")
            outlined_field(
                value = settings.model,
                on_change = { settings = settings.copy(model = it) },
                placeholder = settings.provider.default_model.ifBlank { "如 gpt-4o、deepseek-chat" },
                colors = colors
            )

            // API key
            section_title("API Key")
            OutlinedTextField(
                value = settings.api_key,
                onValueChange = { settings = settings.copy(api_key = it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-...", color = colors.input_hint, fontSize = 13.sp) },
                singleLine = true,
                visualTransformation = if (key_visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { key_visible = !key_visible }) {
                        Icon(
                            if (key_visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (key_visible) "隐藏" else "显示",
                            tint = colors.subtitle, modifier = Modifier.size(18.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = field_colors(colors)
            )
            Text("密钥已加密存储在设备本地", fontSize = 10.sp, color = colors.success)

            // 工具开关
            section_title("Agent 能力")
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = colors.card_bg) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    switch_row("启用工具调用（让 AI 能读写文件、执行命令）", settings.enable_tools, colors) {
                        settings = settings.copy(enable_tools = it)
                    }
                    HorizontalDivider(color = colors.input_border.copy(alpha = 0.2f))
                    switch_row("允许执行命令 (bash/go build 等)", settings.enable_bash, colors) {
                        settings = settings.copy(enable_bash = it)
                    }
                    HorizontalDivider(color = colors.input_border.copy(alpha = 0.2f))
                    switch_row("允许写入/修改文件", settings.enable_write, colors) {
                        settings = settings.copy(enable_write = it)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun section_title(text: String) {
    val colors = app_theme_provider.colors
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.title_highlight)
}

@Composable
private fun outlined_field(value: String, on_change: (String) -> Unit, placeholder: String, colors: com.jmwl.gostudio.ui.theme.app_colors) {
    OutlinedTextField(
        value = value,
        onValueChange = on_change,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = colors.input_hint, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = field_colors(colors)
    )
}

@Composable
private fun switch_row(label: String, checked: Boolean, colors: com.jmwl.gostudio.ui.theme.app_colors, on_change: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = colors.card_text_title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = on_change, colors = SwitchDefaults.colors(
            checkedThumbColor = colors.title_highlight,
            checkedTrackColor = colors.title_highlight.copy(alpha = 0.3f)
        ))
    }
}

@Composable
private fun field_colors(colors: com.jmwl.gostudio.ui.theme.app_colors) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.input_text,
    unfocusedTextColor = colors.input_text,
    focusedBorderColor = colors.title_highlight,
    unfocusedBorderColor = colors.input_border,
    cursorColor = colors.title_highlight,
    focusedContainerColor = colors.card_bg,
    unfocusedContainerColor = colors.card_bg
)
