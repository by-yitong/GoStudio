package com.jmwl.gostudio.ui.screens.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.plugins.plugin_instance
import com.jmwl.gostudio.plugins.plugin_manager
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 插件管理页。
 *
 * 数据包插件（不执行代码）：一个含 manifest.json 的目录，
 * 通过 ZIP 导入安装。当前支持 skills 能力目录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun plugins_screen(on_back: () -> Unit, on_browse: () -> Unit) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var plugins by remember { mutableStateOf(plugin_manager.all()) }
    var pending_delete by remember { mutableStateOf<plugin_instance?>(null) }

    // 插件列表变更时刷新（安装/卸载/开关），离开页面注销
    DisposableEffect(Unit) {
        val listener = { plugins = plugin_manager.all() }
        plugin_manager.add_listener(listener)
        onDispose { plugin_manager.remove_listener(listener) }
    }

    fun refresh() {
        plugins = plugin_manager.all()
    }

    // SAF 选择 ZIP 安装
    val zip_picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        plugin_manager.install(context, stream).getOrThrow()
                    } ?: throw IllegalStateException("无法读取所选文件")
                }
            }
            result.fold(
                onSuccess = { manifest ->
                    snackbar.showSnackbar("已安装 ${manifest.name} v${manifest.version}")
                },
                onFailure = { error ->
                    snackbar.showSnackbar("安装失败：${error.message ?: "未知错误"}")
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("插件", color = colors.title_large) },
                navigationIcon = {
                    IconButton(onClick = on_back) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = colors.top_button_icon
                        )
                    }
                },
                actions = {
                    TextButton(onClick = on_browse) {
                        Text("浏览", color = colors.title_highlight, fontSize = 14.sp)
                    }
                    TextButton(onClick = { zip_picker.launch(arrayOf("application/zip", "application/octet-stream")) }) {
                        Icon(
                            Icons.Default.InstallMobile,
                            contentDescription = null,
                            tint = colors.title_highlight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("安装", color = colors.title_highlight, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (plugins.isEmpty()) {
            plugins_empty_state(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                plugins.forEach { plugin ->
                    plugin_card(
                        plugin = plugin,
                        on_toggle = { enabled ->
                            plugin_manager.set_enabled(plugin.id, enabled)
                            refresh()
                        },
                        on_delete = { pending_delete = plugin }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // 删除确认
    pending_delete?.let { plugin ->
        AlertDialog(
            onDismissRequest = { pending_delete = null },
            title = { Text("卸载插件") },
            text = { Text("确定卸载「${plugin.manifest.name}」？插件目录将被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    if (plugin_manager.uninstall(plugin.id)) {
                        scope.launch { snackbar.showSnackbar("已卸载 ${plugin.manifest.name}") }
                    }
                    pending_delete = null
                    refresh()
                }) { Text("卸载", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pending_delete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun plugin_card(
    plugin: plugin_instance,
    on_toggle: (Boolean) -> Unit,
    on_delete: () -> Unit
) {
    val colors = app_theme_provider.colors
    val caps = plugin.capabilities()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card_bg)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Extension,
                    contentDescription = null,
                    tint = colors.title_highlight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plugin.manifest.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.card_text_title
                )
                Text(
                    "v${plugin.manifest.version}" +
                        (if (plugin.manifest.author.isNotBlank()) " · ${plugin.manifest.author}" else ""),
                    fontSize = 11.sp,
                    color = colors.card_text_subtitle
                )
            }
            Switch(
                checked = plugin.enabled,
                onCheckedChange = on_toggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.title_highlight,
                    checkedThumbColor = Color.White
                )
            )
        }

        if (plugin.manifest.description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                plugin.manifest.description,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.card_text_subtitle
            )
        }

        if (caps.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                caps.forEach { cap ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.card_icon_bg.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(cap, fontSize = 10.sp, color = colors.card_text_subtitle)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = on_delete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "卸载",
                    tint = colors.card_text_subtitle.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun plugins_empty_state(modifier: Modifier = Modifier) {
    val colors = app_theme_provider.colors
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Extension,
                contentDescription = null,
                tint = colors.card_text_subtitle.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("暂无插件", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.subtitle)
            Spacer(Modifier.height(6.dp))
            Text(
                "点击右上角「安装」导入插件 ZIP。\n\n插件是一个含 manifest.json 的目录，\n可包含 skills/ 等 capability 目录。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = colors.card_text_subtitle,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
