package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_agent_loop
import com.jmwl.gostudio.ai.ai_provider
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * AI 聊天全屏页（主界面 "agent" 路由用）。
 * 不绑定具体项目，做通用 Go 编程问答。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ai_chat_screen(
    agent: ai_agent_loop,
    on_back: () -> Unit,
    on_open_settings: () -> Unit,
    current_provider: ai_provider = ai_provider.ZHIPU,
    current_model: String = "",
    available_models: Map<ai_provider, List<String>> = emptyMap(),
    configured_providers: Set<ai_provider> = emptySet(),
    on_session_model_change: (ai_provider, String) -> Unit = { _, _ -> }
) {
    val colors = app_theme_provider.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 助手", color = colors.title_large) },
                navigationIcon = {
                    IconButton(onClick = on_back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.top_button_icon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = colors.gradient_start
    ) { padding ->
        ai_chat_panel(
            agent = agent,
            on_open_settings = on_open_settings,
            current_provider = current_provider,
            current_model = current_model,
            available_models = available_models,
            configured_providers = configured_providers,
            on_session_model_change = on_session_model_change,
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
