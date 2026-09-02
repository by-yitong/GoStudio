package com.jmwl.gostudio.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.plugins.marketplace_entry
import com.jmwl.gostudio.plugins.plugin_manager
import com.jmwl.gostudio.plugins.plugin_marketplace
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.launch

/** 已安装 / 可更新 / 未安装 */
private enum class entry_state { INSTALLED, UPDATABLE, NOT_INSTALLED }

/**
 * 插件市场：从 gostudio-plugins 仓库浏览并一键安装插件。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun plugin_market_screen(on_back: () -> Unit) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var entries by remember { mutableStateOf<List<marketplace_entry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var installing_id by remember { mutableStateOf<String?>(null) }
    var installed_versions by remember { mutableStateOf(plugin_manager.all().associate { it.id to it.manifest.version }) }

    fun reload() {
        scope.launch {
            plugin_marketplace.fetch_index().fold(
                onSuccess = {
                    entries = it
                    error = null
                },
                onFailure = { error = it.message ?: "网络错误" }
            )
        }
    }

    // 进入页面自动拉取
    androidx.compose.runtime.LaunchedEffect(Unit) { reload() }

    fun state_of(entry: marketplace_entry): entry_state {
        val installed = installed_versions[entry.id] ?: return entry_state.NOT_INSTALLED
        return if (installed != entry.version) entry_state.UPDATABLE else entry_state.INSTALLED
    }

    fun install(entry: marketplace_entry) {
        installing_id = entry.id
        scope.launch {
            plugin_marketplace.install(context, entry).fold(
                onSuccess = { manifest ->
                    installed_versions = plugin_manager.all().associate { it.id to it.manifest.version }
                    snackbar.showSnackbar("已安装 ${manifest.name} v${manifest.version}")
                },
                onFailure = { e ->
                    snackbar.showSnackbar("安装失败：${e.message ?: "未知错误"}")
                }
            )
            installing_id = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("插件市场", color = colors.title_large) },
                navigationIcon = {
                    IconButton(onClick = on_back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = colors.top_button_icon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        val list = entries
        when {
            error != null -> market_error_state(
                message = error!!,
                on_retry = { reload() },
                modifier = Modifier.padding(padding)
            )
            list == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.title_highlight)
            }
            list.isEmpty() -> market_error_state(
                message = "市场暂无插件",
                on_retry = { reload() },
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                list.forEach { entry ->
                    market_entry_card(
                        entry = entry,
                        state = state_of(entry),
                        installing = installing_id == entry.id,
                        on_install = { install(entry) }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun market_entry_card(
    entry: marketplace_entry,
    state: entry_state,
    installing: Boolean,
    on_install: () -> Unit
) {
    val colors = app_theme_provider.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card_bg)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Storefront, null, tint = colors.title_highlight, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.card_text_title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append("v${entry.version}")
                        if (entry.author.isNotBlank()) append(" · ${entry.author}")
                        if (entry.size > 0) append(" · ${entry.size / 1024} KB")
                    },
                    fontSize = 11.sp,
                    color = colors.card_text_subtitle
                )
            }
            when (state) {
                entry_state.INSTALLED -> Text(
                    "已安装",
                    fontSize = 12.sp,
                    color = colors.card_text_subtitle
                )
                else -> OutlinedButton(
                    onClick = on_install,
                    enabled = !installing,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    if (installing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = colors.title_highlight
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudDownload,
                            null,
                            Modifier.size(15.dp),
                            tint = colors.title_highlight
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (state == entry_state.UPDATABLE) "更新" else "安装",
                            fontSize = 12.sp,
                            color = colors.title_highlight
                        )
                    }
                }
            }
        }
        if (entry.description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                entry.description,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.card_text_subtitle
            )
        }
    }
}

@Composable
private fun market_error_state(message: String, on_retry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = app_theme_provider.colors
    Box(modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, fontSize = 14.sp, color = colors.subtitle, modifier = Modifier.padding(bottom = 12.dp))
            TextButton(onClick = on_retry) { Text("重试", color = colors.title_highlight) }
        }
    }
}
