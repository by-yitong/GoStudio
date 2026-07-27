package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_client
import com.jmwl.gostudio.ai.ai_provider
import com.jmwl.gostudio.ai.ai_settings_state
import com.jmwl.gostudio.ai.switch_provider
import com.jmwl.gostudio.ui.toast.app_toast
import com.jmwl.gostudio.ui.theme.app_colors
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AI 设置主页：菜单式 4 入口（模型配置 / AI 行为 / MCP 配置 / Skill 配置）。
 * 点进入各自子页。子页用 AnimatedVisibility 滑入覆盖。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ai_settings_screen(
    initial: ai_settings_state,
    on_back: () -> Unit,
    on_save: (ai_settings_state) -> Unit,
    project_dir: java.io.File? = null
) {
    val colors = app_theme_provider.colors
    var settings by remember { mutableStateOf(initial) }
    // 子页导航：null=主页，否则为子页标识
    var sub_page by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // 顶部圆形返回按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(35.dp),
                    shape = CircleShape,
                    color = colors.top_button_bg,
                    onClick = on_back
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = colors.top_button_icon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(35.dp))
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 大标题
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = "AI",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "设置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.subtitle
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4 个菜单入口
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                ai_navigation_card(
                    icon = Icons.Default.SmartToy,
                    title = "模型配置",
                    description = "${settings.provider.display_name} · ${settings.model}",
                    colors = colors,
                    is_top = true,
                    is_bottom = false,
                    trailing = { ai_chevron(colors) },
                    on_click = { sub_page = "model" }
                )
                ai_group_divider()
                ai_navigation_card(
                    icon = Icons.Default.Psychology,
                    title = "AI 行为",
                    description = "思考深度、提示词、语气、思考过程",
                    colors = colors,
                    is_top = false,
                    is_bottom = false,
                    trailing = { ai_chevron(colors) },
                    on_click = { sub_page = "behavior" }
                )
                ai_group_divider()
                ai_navigation_card(
                    icon = Icons.Default.Hub,
                    title = "MCP 配置",
                    description = "外部工具服务器",
                    colors = colors,
                    is_top = false,
                    is_bottom = false,
                    trailing = { ai_chevron(colors) },
                    on_click = { sub_page = "mcp" }
                )
                ai_group_divider()
                ai_navigation_card(
                    icon = Icons.Default.AutoAwesome,
                    title = "Skill 配置",
                    description = "技能管理与创建",
                    colors = colors,
                    is_top = false,
                    is_bottom = true,
                    trailing = { ai_chevron(colors) },
                    on_click = { sub_page = "skill" }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // 子页覆盖层
        sub_page?.let { page ->
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) +
                    androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) +
                    androidx.compose.animation.fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.editor_bg)
                ) {
                    when (page) {
                        "model" -> ai_model_settings_screen(
                            initial = settings,
                            on_back = { sub_page = null },
                            on_save = { new_settings ->
                                settings = new_settings
                                on_save(new_settings)
                                sub_page = null
                            }
                        )
                        "behavior" -> ai_behavior_settings_screen(
                            initial = settings,
                            on_back = { sub_page = null },
                            on_save = { new_settings ->
                                settings = new_settings
                                on_save(new_settings)
                                sub_page = null
                            }
                        )
                        "mcp" -> ai_mcp_settings_screen(
                            project_dir = project_dir,
                            on_back = { sub_page = null }
                        )
                        "skill" -> ai_skill_settings_screen(
                            project_dir = project_dir,
                            on_back = { sub_page = null }
                        )
                    }
                }
            }
        }
    }
}

/** 菜单卡片的尾部箭头 */
@Composable
private fun ai_chevron(colors: app_colors) {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "进入",
        tint = colors.card_chevron,
        modifier = Modifier.size(18.dp)
    )
}

// ==================== 子页：模型配置 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ai_model_settings_screen(
    initial: ai_settings_state,
    on_back: () -> Unit,
    on_save: (ai_settings_state) -> Unit
) {
    val colors = app_theme_provider.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(initial) }
    var key_visible by remember { mutableStateOf(false) }
    var provider_menu_open by remember { mutableStateOf(false) }
    var model_menu_open by remember { mutableStateOf(false) }
    var fetching_models by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // 顶部栏
            ai_sub_page_header(colors = colors, title = "模型配置", on_back = on_back)

            Spacer(modifier = Modifier.height(24.dp))

            // 提供商
            ai_group_title(colors = colors, title = "提供商")
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))
            ) {
                ai_navigation_card(
                    icon = Icons.Default.SmartToy,
                    title = "AI 提供商",
                    description = settings.provider.display_name,
                    colors = colors,
                    is_top = true, is_bottom = true,
                    trailing = {
                        Box {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择", tint = colors.card_chevron, modifier = Modifier.size(20.dp))
                            DropdownMenu(expanded = provider_menu_open, onDismissRequest = { provider_menu_open = false }) {
                                ai_provider.entries.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.display_name, color = colors.dialog_text) },
                                        onClick = { settings = switch_provider(settings, p); provider_menu_open = false }
                                    )
                                }
                            }
                        }
                    },
                    on_click = { provider_menu_open = true }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 接口
            ai_group_title(colors = colors, title = "接口")
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))
            ) {
                ai_input_card(icon = Icons.Default.Cloud, title = "Base URL", colors = colors, is_top = true, is_bottom = false) {
                    OutlinedTextField(
                        value = settings.base_url,
                        onValueChange = { settings = settings.copy(base_url = it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://api.example.com/v1", color = colors.input_hint, fontSize = 13.sp) },
                        singleLine = true, shape = RoundedCornerShape(0.dp), colors = field_colors(colors)
                    )
                }
                ai_group_divider()
                ai_input_card(icon = Icons.Default.ModelTraining, title = "模型", colors = colors, is_top = false, is_bottom = true) {
                    OutlinedTextField(
                        value = settings.model,
                        onValueChange = { settings = settings.copy(model = it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(settings.provider.default_model.ifBlank { "如 glm-4.6、deepseek-chat" }, color = colors.input_hint, fontSize = 13.sp) },
                        singleLine = true, shape = RoundedCornerShape(0.dp),
                        trailingIcon = {
                            if (settings.provider.supports_model_list) {
                                Box {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (fetching_models) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.title_highlight)
                                        } else {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择模型", tint = colors.subtitle,
                                                modifier = Modifier.size(22.dp).clip(CircleShape).clickable { model_menu_open = true })
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Refresh, contentDescription = "获取模型列表", tint = colors.title_highlight,
                                            modifier = Modifier.size(18.dp).clip(CircleShape).clickable {
                                                if (settings.api_key.isBlank()) { app_toast.show(context, "请先填写 API Key", app_toast.LENGTH_SHORT); return@clickable }
                                                fetching_models = true
                                                scope.launch {
                                                    val result = withContext(Dispatchers.IO) { runCatching { ai_client(settings).fetch_models() } }
                                                    fetching_models = false
                                                    result.onSuccess { models ->
                                                        if (models.isEmpty()) { app_toast.show(context, "未获取到模型", app_toast.LENGTH_SHORT) }
                                                        else {
                                                            settings = settings.copy(custom_models = settings.custom_models + (settings.base_url to models))
                                                            model_menu_open = true
                                                            app_toast.show(context, "已获取 ${models.size} 个模型", app_toast.LENGTH_SHORT)
                                                        }
                                                    }.onFailure { e -> app_toast.show(context, "获取失败: ${e.message ?: ""}", app_toast.LENGTH_LONG) }
                                                }
                                            })
                                    }
                                    val deduped = ((settings.custom_models[settings.base_url] ?: emptyList()) + settings.provider.default_models).distinct()
                                    DropdownMenu(expanded = model_menu_open, onDismissRequest = { model_menu_open = false }) {
                                        if (deduped.isEmpty()) {
                                            DropdownMenuItem(text = { Text("无候选，点刷新获取", color = colors.subtitle) }, onClick = { model_menu_open = false })
                                        } else {
                                            deduped.forEach { m ->
                                                DropdownMenuItem(text = { Text(m, color = colors.dialog_text) }, onClick = { settings = settings.copy(model = m); model_menu_open = false })
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = field_colors(colors)
                    )
                }
            }
            ai_hint_text(colors = colors, text = if (settings.provider == ai_provider.ANTHROPIC) "OpenAI 兼容格式。⚠️ Anthropic 原生格式暂不支持，请用兼容中转。" else "OpenAI 兼容格式。")

            Spacer(modifier = Modifier.height(14.dp))

            // 认证
            ai_group_title(colors = colors, title = "认证")
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))
            ) {
                ai_input_card(icon = Icons.Default.Key, title = "API Key", colors = colors, is_top = true, is_bottom = true) {
                    OutlinedTextField(
                        value = settings.api_key,
                        onValueChange = { new_key ->
                            settings = settings.copy(api_key = new_key, api_keys = settings.api_keys + (settings.provider to new_key))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("sk-...", color = colors.input_hint, fontSize = 13.sp) },
                        singleLine = true,
                        visualTransformation = if (key_visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).clickable { key_visible = !key_visible }, contentAlignment = Alignment.Center) {
                                Icon(if (key_visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (key_visible) "隐藏" else "显示", tint = colors.subtitle, modifier = Modifier.size(16.dp))
                            }
                        },
                        shape = RoundedCornerShape(0.dp), colors = field_colors(colors)
                    )
                }
            }
            ai_hint_text(colors = colors, text = "密钥已加密存储在设备本地", success = true)

            Spacer(modifier = Modifier.height(14.dp))

            // Agent 能力
            ai_group_title(colors = colors, title = "Agent 能力")
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))
            ) {
                ai_switch_card(icon = Icons.Default.AutoAwesome, title = "工具调用", description = "让 AI 能读写文件、执行命令", checked = settings.enable_tools, colors = colors, is_top = true, is_bottom = false) { settings = settings.copy(enable_tools = it) }
                ai_group_divider()
                ai_switch_card(icon = Icons.Default.Terminal, title = "执行命令", description = "允许 bash / go build 等命令", checked = settings.enable_bash, colors = colors, is_top = false, is_bottom = false) { settings = settings.copy(enable_bash = it) }
                ai_group_divider()
                ai_switch_card(icon = Icons.Default.Edit, title = "修改文件", description = "允许写入 / 修改项目文件", checked = settings.enable_write, colors = colors, is_top = false, is_bottom = true) { settings = settings.copy(enable_write = it) }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }

        // 底部保存
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = colors.editor_bg, shadowElevation = 8.dp) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                ai_save_button(colors = colors) { on_save(settings) }
            }
        }
    }
}

// ==================== 子页：AI 行为 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ai_behavior_settings_screen(
    initial: ai_settings_state,
    on_back: () -> Unit,
    on_save: (ai_settings_state) -> Unit
) {
    val colors = app_theme_provider.colors
    var settings by remember { mutableStateOf(initial) }
    val tones = listOf("friendly" to "友好", "professional" to "专业", "concise" to "简洁")
    val depths = listOf(0 to "标准", 1 to "深入", 2 to "极致")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ai_sub_page_header(colors = colors, title = "AI 行为", on_back = on_back)
            Spacer(modifier = Modifier.height(24.dp))

            // 思考深度
            ai_group_title(colors = colors, title = "思考深度")
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                depths.forEachIndexed { index, (value, label) ->
                    if (index > 0) ai_group_divider()
                    ai_radio_card(
                        icon = Icons.Default.Psychology,
                        title = label,
                        description = when (value) { 0 -> "平衡速度与质量（10 轮）"; 1 -> "更深入的多步推理（20 轮）"; else -> "最大推理深度（40 轮）" },
                        selected = settings.thinking_depth == value,
                        colors = colors,
                        is_top = index == 0,
                        is_bottom = index == depths.lastIndex
                    ) {
                        settings = settings.copy(
                            thinking_depth = value,
                            max_agent_iterations = when (value) { 0 -> 10; 1 -> 20; else -> 40 }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 交流语气
            ai_group_title(colors = colors, title = "交流语气")
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                tones.forEachIndexed { index, (value, label) ->
                    if (index > 0) ai_group_divider()
                    ai_radio_card(
                        icon = Icons.Default.RecordVoiceOver,
                        title = label,
                        description = when (value) { "friendly" -> "亲切、易懂、带鼓励"; "professional" -> "严谨、专业、重准确"; else -> "精炼、直击要点" },
                        selected = settings.conversation_tone == value,
                        colors = colors,
                        is_top = index == 0,
                        is_bottom = index == tones.lastIndex
                    ) { settings = settings.copy(conversation_tone = value) }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 自定义提示词
            ai_group_title(colors = colors, title = "自定义提示词")
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                ai_input_card(icon = Icons.Default.Edit, title = "追加系统提示", colors = colors, is_top = true, is_bottom = true) {
                    OutlinedTextField(
                        value = settings.custom_system_prompt,
                        onValueChange = { settings = settings.copy(custom_system_prompt = it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("追加到默认系统提示之后，例如：始终先解释再改代码", color = colors.input_hint, fontSize = 13.sp) },
                        minLines = 3, maxLines = 6,
                        shape = RoundedCornerShape(0.dp), colors = field_colors(colors)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 思考过程
            ai_group_title(colors = colors, title = "思考过程")
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                ai_switch_card(icon = Icons.Default.Visibility, title = "显示思考过程", description = "展示 AI 读文件、执行命令等中间步骤", checked = settings.show_thinking_process, colors = colors, is_top = true, is_bottom = false) { settings = settings.copy(show_thinking_process = it) }
                ai_group_divider()
                ai_switch_card(icon = Icons.Default.UnfoldMore, title = "自动展开", description = "思考过程默认展开（否则折叠只显示标题）", checked = settings.auto_expand_thinking, colors = colors, is_top = false, is_bottom = true) { settings = settings.copy(auto_expand_thinking = it) }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }

        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = colors.editor_bg, shadowElevation = 8.dp) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                ai_save_button(colors = colors) { on_save(settings) }
            }
        }
    }
}

// ==================== 子页占位：MCP / Skill（阶段 2 实现） ====================

// ==================== 子页：MCP 配置（表单 + 原始 JSON 双模式） ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ai_mcp_settings_screen(project_dir: java.io.File?, on_back: () -> Unit) {
    val colors = app_theme_provider.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    // 复用 mcp_manager 读配置（无项目时 project_dir=null，只管全局）
    val manager = remember(project_dir) {
        com.jmwl.gostudio.ai.mcp.ai_mcp_manager(project_dir)
    }
    var global_configs by remember { mutableStateOf(manager.list_global_configs()) }
    var project_configs by remember { mutableStateOf(manager.list_project_configs()) }
    var show_raw by remember { mutableStateOf(false) }
    var raw_global by remember { mutableStateOf("") }
    var raw_project by remember { mutableStateOf("") }
    var show_add_dialog by remember { mutableStateOf(false) }
    var add_target_global by remember { mutableStateOf(true) }

    fun reload() {
        global_configs = manager.list_global_configs()
        project_configs = manager.list_project_configs()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ai_sub_page_header(colors = colors, title = "MCP 配置", on_back = on_back)
            Spacer(modifier = Modifier.height(16.dp))

            // 模式切换：表单 / 原始 JSON
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ai_mode_tab("表单", !show_raw, colors) { show_raw = false }
                ai_mode_tab("原始 JSON", show_raw, colors) {
                    raw_global = runCatching { manager.global_config_file().readText() }.getOrDefault("{\n  \"mcpServers\": {}\n}")
                    raw_project = manager.project_config_file()?.let { runCatching { it.readText() }.getOrDefault("{\n  \"mcpServers\": {}\n}") } ?: ""
                    show_raw = true
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!show_raw) {
                // ===== 表单模式 =====
                // 全局服务器
                ai_group_title(colors = colors, title = "全局服务器（~/.ai/mcp.json）")
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                    if (global_configs.isEmpty()) {
                        ai_empty_hint(colors, "暂无全局服务器")
                    } else {
                        global_configs.forEachIndexed { index, cfg ->
                            if (index > 0) ai_group_divider()
                            ai_mcp_server_card(cfg, colors, is_top = index == 0, is_bottom = index == global_configs.lastIndex) {
                                manager.save_configs(manager.global_config_file(), global_configs.filter { it.name != cfg.name })
                                reload()
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
                    ai_add_button(colors) { add_target_global = true; show_add_dialog = true }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 项目服务器
                ai_group_title(colors = colors, title = if (project_dir != null) "项目服务器（.ai/mcp.json）" else "项目服务器（需在项目中配置）")
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                    if (project_configs.isEmpty()) {
                        ai_empty_hint(colors, "暂无项目服务器")
                    } else {
                        project_configs.forEachIndexed { index, cfg ->
                            if (index > 0) ai_group_divider()
                            ai_mcp_server_card(cfg, colors, is_top = index == 0, is_bottom = index == project_configs.lastIndex) {
                                manager.project_config_file()?.let { f ->
                                    manager.save_configs(f, project_configs.filter { it.name != cfg.name })
                                    reload()
                                }
                            }
                        }
                    }
                }
                if (project_dir != null) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
                        ai_add_button(colors) { add_target_global = false; show_add_dialog = true }
                    }
                }
            } else {
                // ===== 原始 JSON 模式 =====
                ai_group_title(colors = colors, title = "全局 mcp.json")
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                    OutlinedTextField(
                        value = raw_global,
                        onValueChange = { raw_global = it },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        minLines = 6, maxLines = 12,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        shape = RoundedCornerShape(8.dp), colors = field_colors(colors)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                    ai_save_button(colors = colors, label = "保存全局") {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                runCatching { manager.global_config_file().writeText(raw_global) }
                            }
                            reload()
                            app_toast.show(context, "已保存", app_toast.LENGTH_SHORT)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (project_dir != null) {
                    ai_group_title(colors = colors, title = "项目 mcp.json")
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                        OutlinedTextField(
                            value = raw_project,
                            onValueChange = { raw_project = it },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            minLines = 6, maxLines = 12,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            shape = RoundedCornerShape(8.dp), colors = field_colors(colors)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                        ai_save_button(colors = colors, label = "保存项目") {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    manager.project_config_file()?.let { runCatching { it.writeText(raw_project) } }
                                }
                                reload()
                                app_toast.show(context, "已保存", app_toast.LENGTH_SHORT)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 添加 server 对话框
    if (show_add_dialog) {
        ai_mcp_add_dialog(
            colors = colors,
            is_global = add_target_global,
            on_dismiss = { show_add_dialog = false },
            on_confirm = { name, command, args, env ->
                val cfg = com.jmwl.gostudio.ai.mcp.ai_mcp_manager.mcp_server_config(name, command, args, env)
                if (add_target_global) {
                    manager.save_configs(manager.global_config_file(), global_configs + cfg)
                } else {
                    manager.project_config_file()?.let { manager.save_configs(it, project_configs + cfg) }
                }
                reload()
                show_add_dialog = false
            }
        )
    }
}

/** MCP server 卡片（名称 + command + 删除） */
@Composable
private fun ai_mcp_server_card(
    cfg: com.jmwl.gostudio.ai.mcp.ai_mcp_manager.mcp_server_config,
    colors: app_colors,
    is_top: Boolean,
    is_bottom: Boolean,
    on_delete: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.fillMaxWidth().background(colors.card_bg)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ai_icon_chip(Icons.Default.Hub, colors)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(cfg.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.card_text_title)
            Text(cfg.command + if (cfg.args.isNotEmpty()) " " + cfg.args.joinToString(" ") else "",
                fontSize = 10.sp, color = colors.card_text_subtitle, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Surface(
            modifier = Modifier.size(28.dp), shape = CircleShape,
            color = colors.danger_bg.copy(alpha = 0.3f), onClick = on_delete
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = colors.danger, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** 添加 MCP server 对话框 */
@Composable
private fun ai_mcp_add_dialog(
    colors: app_colors,
    is_global: Boolean,
    on_dismiss: () -> Unit,
    on_confirm: (String, String, List<String>, Map<String, String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var args_text by remember { mutableStateOf("") }
    var env_text by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        title = { Text(if (is_global) "添加全局服务器" else "添加项目服务器", color = colors.dialog_text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, colors = field_colors(colors))
                OutlinedTextField(value = command, onValueChange = { command = it }, label = { Text("命令 (command)") }, singleLine = true, colors = field_colors(colors))
                OutlinedTextField(value = args_text, onValueChange = { args_text = it }, label = { Text("参数 (空格分隔)") }, singleLine = true, colors = field_colors(colors))
                OutlinedTextField(value = env_text, onValueChange = { env_text = it }, label = { Text("环境变量 (KEY=value,逗号分隔)") }, singleLine = true, colors = field_colors(colors))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && command.isNotBlank()) {
                    val args = args_text.split(" ").filter { it.isNotBlank() }
                    val env = env_text.split(",").mapNotNull {
                        val idx = it.indexOf('=')
                        if (idx > 0) it.substring(0, idx).trim() to it.substring(idx + 1).trim() else null
                    }.toMap()
                    on_confirm(name.trim(), command.trim(), args, env)
                }
            }) { Text("添加", color = colors.title_highlight) }
        },
        dismissButton = { TextButton(onClick = on_dismiss) { Text("取消", color = colors.subtitle) } }
    )
}

// ==================== 子页：Skill 配置 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ai_skill_settings_screen(project_dir: java.io.File?, on_back: () -> Unit) {
    val colors = app_theme_provider.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val paths = remember { com.jmwl.gostudio.toolchain.toolchain_runtime_provider.paths() }
    val manager = remember(project_dir) {
        com.jmwl.gostudio.ai.skills.ai_skill_manager(
            File(paths.home_dir, ".ai/skills"),
            project_dir?.let { File(it, ".ai/skills") }
        ).also { it.discover() }
    }
    var skills by remember { mutableStateOf(manager.all()) }
    var show_create_dialog by remember { mutableStateOf(false) }
    var viewing_skill by remember { mutableStateOf<com.jmwl.gostudio.ai.skills.ai_skill?>(null) }

    fun reload() { skills = manager.all() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ai_sub_page_header(colors = colors, title = "Skill 配置", on_back = on_back)
            Spacer(modifier = Modifier.height(20.dp))

            // 创建按钮 + AI 创建提示
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ai_add_button(colors, label = "创建技能") { show_create_dialog = true }
            }
            ai_hint_text(colors = colors, text = "💡 也可在对话里让 AI 自动创建技能（描述需求，如\"创建一个 code-review 技能\"）")

            Spacer(modifier = Modifier.height(16.dp))

            ai_group_title(colors = colors, title = "已安装技能（${skills.size}）")
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(12.dp))) {
                if (skills.isEmpty()) {
                    ai_empty_hint(colors, "暂无技能")
                } else {
                    skills.forEachIndexed { index, skill ->
                        if (index > 0) ai_group_divider()
                        ai_skill_card(skill, colors, is_top = index == 0, is_bottom = index == skills.lastIndex,
                            on_view = { viewing_skill = skill },
                            on_delete = {
                                if (manager.delete_skill(skill.name)) { reload(); app_toast.show(context, "已删除 ${skill.name}", app_toast.LENGTH_SHORT) }
                                else app_toast.show(context, "内置技能不可删除", app_toast.LENGTH_SHORT)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 创建技能对话框
    if (show_create_dialog) {
        ai_skill_create_dialog(
            colors = colors,
            on_dismiss = { show_create_dialog = false },
            on_create = { name, desc, content ->
                if (manager.create_skill(name, desc, content)) {
                    reload()
                    app_toast.show(context, "技能 $name 已创建", app_toast.LENGTH_SHORT)
                } else {
                    app_toast.show(context, "创建失败", app_toast.LENGTH_SHORT)
                }
                show_create_dialog = false
            }
        )
    }

    // 查看技能内容
    viewing_skill?.let { skill ->
        val content = remember(skill) { manager.read_skill_content(skill.name) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewing_skill = null },
            containerColor = colors.dialog_bg,
            title = { Text(skill.name, color = colors.dialog_text, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(skill.description, fontSize = 12.sp, color = colors.subtitle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(content ?: "(无法读取)", fontSize = 11.sp, color = colors.card_text_title,
                        modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 300.dp))
                }
            },
            confirmButton = { TextButton(onClick = { viewing_skill = null }) { Text("关闭", color = colors.title_highlight) } }
        )
    }
}

/** Skill 卡片 */
@Composable
private fun ai_skill_card(
    skill: com.jmwl.gostudio.ai.skills.ai_skill,
    colors: app_colors,
    is_top: Boolean, is_bottom: Boolean,
    on_view: () -> Unit,
    on_delete: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val source_label = when (skill.source) {
        com.jmwl.gostudio.ai.skills.skill_source.BUILT_IN -> "内置"
        com.jmwl.gostudio.ai.skills.skill_source.GLOBAL -> "全局"
        com.jmwl.gostudio.ai.skills.skill_source.PROJECT -> "项目"
    }
    val can_delete = skill.source != com.jmwl.gostudio.ai.skills.skill_source.BUILT_IN
    Row(
        modifier = Modifier.fillMaxWidth().background(colors.card_bg)
            .clickable(interactionSource = interaction_source, indication = null, onClick = on_view)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ai_icon_chip(Icons.Default.AutoAwesome, colors)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(skill.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.card_text_title)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(shape = RoundedCornerShape(4.dp), color = colors.title_highlight.copy(alpha = 0.15f)) {
                    Text(source_label, fontSize = 9.sp, color = colors.title_highlight, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
            Text(skill.description, fontSize = 10.sp, color = colors.card_text_subtitle, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (can_delete) {
            Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = colors.danger_bg.copy(alpha = 0.3f), onClick = on_delete) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = colors.danger, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/** 创建技能对话框 */
@Composable
private fun ai_skill_create_dialog(
    colors: app_colors,
    on_dismiss: () -> Unit,
    on_create: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        title = { Text("创建技能", color = colors.dialog_text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称（英文）") }, singleLine = true, colors = field_colors(colors))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("描述（何时触发）") }, singleLine = true, colors = field_colors(colors))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("指令正文") }, minLines = 4, maxLines = 8, colors = field_colors(colors))
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank() && content.isNotBlank()) on_create(name.trim(), desc.trim(), content) }) {
                Text("创建", color = colors.title_highlight)
            }
        },
        dismissButton = { TextButton(onClick = on_dismiss) { Text("取消", color = colors.subtitle) } }
    )
}

// ==================== 小工具组件 ====================

@Composable
private fun ai_mode_tab(label: String, active: Boolean, colors: app_colors, on_click: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (active) colors.title_highlight else colors.card_bg,
        onClick = on_click
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) androidx.compose.ui.graphics.Color.White else colors.subtitle,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
    }
}

@Composable
private fun ai_empty_hint(colors: app_colors, text: String) {
    Box(modifier = Modifier.fillMaxWidth().background(colors.card_bg).padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 12.sp, color = colors.subtitle)
    }
}

@Composable
private fun ai_add_button(colors: app_colors, label: String = "添加", on_click: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.title_highlight.copy(alpha = 0.12f),
        onClick = on_click
    ) {
        Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = null, tint = colors.title_highlight, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = colors.title_highlight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==================== 共用子页头部 ====================

@Composable
private fun ai_sub_page_header(colors: app_colors, title: String, on_back: () -> Unit) {
    Spacer(modifier = Modifier.height(30.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(35.dp), shape = CircleShape, color = colors.top_button_bg, onClick = on_back) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.top_button_icon, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.size(35.dp))
    }
    Spacer(modifier = Modifier.height(30.dp))
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.title_highlight)
    }
}

/** 单选卡片（用于思考深度/语气选择） */
@Composable
private fun ai_radio_card(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    colors: app_colors,
    is_top: Boolean,
    is_bottom: Boolean,
    on_select: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = if (is_pressed) colors.card_pressed else colors.card_bg

    Row(
        modifier = Modifier.fillMaxWidth().background(background_color)
            .clickable(interactionSource = interaction_source, indication = null, onClick = on_select)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ai_icon_chip(icon, colors)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.card_text_title)
            Text(description, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Light, color = colors.card_text_subtitle)
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "已选中", tint = colors.title_highlight, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------- 内部组件（与 editor_settings_panel 风格统一）----------

@Composable
private fun ai_group_title(colors: app_colors, title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = colors.title_highlight,
        modifier = Modifier.padding(start = 22.dp, bottom = 10.dp)
    )
}

@Composable
private fun ai_group_divider() {
    Spacer(modifier = Modifier.height(1.dp))
}

private fun ai_item_shape(is_top: Boolean, is_bottom: Boolean, radius: Dp = 12.dp): RoundedCornerShape {
    return when {
        is_top && is_bottom -> RoundedCornerShape(radius)
        is_top -> RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = 0.dp, bottomEnd = 0.dp)
        is_bottom -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(0.dp)
    }
}

/** 图标圆角徽章（与 editor_settings_icon 一致） */
@Composable
private fun ai_icon_chip(icon: ImageVector, colors: app_colors) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(colors.card_icon_bg.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.card_icon_bg,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** 带图标 + 标题 + 副标题 + 尾部控件的导航卡片（用于提供商） */
@Composable
private fun ai_navigation_card(
    icon: ImageVector,
    title: String,
    description: String,
    colors: app_colors,
    is_top: Boolean,
    is_bottom: Boolean,
    trailing: @Composable () -> Unit,
    on_click: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = if (is_pressed) colors.card_pressed else colors.card_bg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background_color)
            .clickable(
                interactionSource = interaction_source,
                indication = null,
                onClick = on_click
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ai_icon_chip(icon, colors)
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.card_text_title
            )
            Text(
                text = description,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Light,
                color = colors.card_text_subtitle
            )
        }
        trailing()
    }
}

/** 带图标 + 标题 + 输入框的卡片（用于 base_url / model / key） */
@Composable
private fun ai_input_card(
    icon: ImageVector,
    title: String,
    colors: app_colors,
    is_top: Boolean,
    is_bottom: Boolean,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card_bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ai_icon_chip(icon, colors)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.card_text_title
            )
        }
        content()
    }
}

/** 带图标 + 标题 + 副标题 + 开关的卡片（与 editor_settings_switch_card 一致） */
@Composable
private fun ai_switch_card(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    colors: app_colors,
    is_top: Boolean,
    is_bottom: Boolean,
    on_checked_change: (Boolean) -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = if (is_pressed) colors.card_pressed else colors.card_bg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background_color)
            .clickable(
                interactionSource = interaction_source,
                indication = null
            ) { on_checked_change(!checked) }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ai_icon_chip(icon, colors)
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.card_text_title
            )
            Text(
                text = description,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Light,
                color = colors.card_text_subtitle
            )
        }
        ai_small_switch(checked = checked, colors = colors)
    }
}

/** 小开关（与 editor_settings_small_switch 一致） */
@Composable
private fun ai_small_switch(checked: Boolean, colors: app_colors) {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (checked) colors.card_icon_bg.copy(alpha = 0.36f)
                else colors.card_text_subtitle.copy(alpha = 0.18f)
            )
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(14.dp)
                .clip(CircleShape)
                .background(if (checked) colors.card_icon_bg else colors.card_text_subtitle)
        )
    }
}

@Composable
private fun ai_hint_text(colors: app_colors, text: String, success: Boolean = false) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = if (success) colors.success else colors.subtitle,
        modifier = Modifier.padding(start = 22.dp, top = 8.dp, end = 22.dp)
    )
}

@Composable
private fun ai_save_button(colors: app_colors, label: String = "保存设置", on_click: () -> Unit) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = if (is_pressed) colors.title_highlight.copy(alpha = 0.85f) else colors.title_highlight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background_color)
            .clickable(
                interactionSource = interaction_source,
                indication = null,
                onClick = on_click
            )
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun field_colors(colors: app_colors) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.input_text,
    unfocusedTextColor = colors.input_text,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    cursorColor = colors.title_highlight,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
