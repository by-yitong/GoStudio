package com.xc.code.ui.dialogs.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xc.code.ui.toast.app_toast
import com.xc.code.ui.theme.app_theme_provider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun install_progress_dialog(
    title: String,
    task: suspend (on_log: (String) -> Unit, on_progress: (Int) -> Unit) -> Boolean,
    on_dismiss: () -> Unit,
    on_success: () -> Unit = {},
    on_minimize: () -> Unit = {},
    visible: Boolean = true,
    auto_close_delay: Long = 2000
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = app_theme_provider.colors
    val list_state = rememberLazyListState()
    
    var is_running by remember { mutableStateOf(true) }
    var current_progress by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var show_close_button by remember { mutableStateOf(false) }
    
    fun add_log(text: String) {
        logs = if (text.startsWith("\r") && logs.isNotEmpty()) {
            logs.dropLast(1) + text.removePrefix("\r")
        } else {
            logs + text
        }
        scope.launch {
            delay(50)
            if (logs.isNotEmpty()) {
                list_state.animateScrollToItem(logs.size - 1)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        val success = task(
            { line -> add_log(line) },
            { progress -> current_progress = progress }
        )
        
        is_running = false
        show_close_button = true
        
        if (success) {
            add_log("")
            add_log("操作成功！")
            delay(auto_close_delay)
            on_success()
        } else {
            add_log("")
            add_log("操作失败")
        }
    }
    
    fun export_logs() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = java.io.File(java.io.File(context.filesDir, "home/xcode"), "cache/log_$timestamp.txt")
            file.writeText(logs.joinToString("\n"))
            app_toast.show(context, "已导出到: ${file.absolutePath}", app_toast.LENGTH_LONG)
        } catch (e: Exception) {
            app_toast.show(context, "导出失败: ${e.message}", app_toast.LENGTH_SHORT)
        }
    }
    
    if (!visible) return

    Dialog(
        onDismissRequest = { if (!is_running && show_close_button) on_dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !is_running && show_close_button,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(390.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.dialog_bg)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.card_bg)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Circle, null, tint = colors.danger, modifier = Modifier.size(9.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Circle, null, tint = colors.warning, modifier = Modifier.size(9.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Circle, null, tint = colors.success, modifier = Modifier.size(9.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            color = colors.card_text_subtitle,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Row {
                        if (is_running) {
                            IconButton(onClick = on_minimize, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Remove, "后台", tint = colors.card_text_subtitle, modifier = Modifier.size(15.dp))
                            }
                        }
                        IconButton(onClick = { export_logs() }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Download, "导出", tint = colors.card_text_subtitle, modifier = Modifier.size(15.dp))
                        }
                        
                        if (show_close_button) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = on_dismiss, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, "关闭", tint = colors.card_text_subtitle, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card_bg)
                    ) {
                        LazyColumn(
                            state = list_state,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(logs) { log ->
                                val color = when {
                                    log.contains("失败") -> colors.danger
                                    log.contains("成功") -> colors.success
                                    else -> colors.card_text_title
                                }
                                Text(
                                    text = log,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = color
                                )
                            }
                            
                            if (logs.isEmpty()) {
                                item {
                                    Text(
                                        text = "正在准备...",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.card_text_subtitle
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (is_running) {
                    LinearProgressIndicator(
                        progress = { current_progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 12.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = colors.dialog_clone_bg,
                        trackColor = colors.dialog_hint.copy(alpha = 0.3f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}