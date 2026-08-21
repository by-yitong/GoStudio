package com.jmwl.gostudio.ui.screens.ai

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_session_meta
import com.jmwl.gostudio.ui.theme.app_theme_provider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 会话历史列表（滑入覆盖层）。展示所有历史会话，支持切换/新建/重命名/删除。
 */
@Composable
fun ai_session_history_screen(
    sessions: List<ai_session_meta>,
    current_session_id: String,
    on_back: () -> Unit,
    on_switch: (String) -> Unit,
    on_new: () -> Unit,
    on_rename: (String, String) -> Unit,
    on_delete: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    var rename_target by remember { mutableStateOf<ai_session_meta?>(null) }
    var delete_target by remember { mutableStateOf<ai_session_meta?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = on_back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.top_button_icon)
            }
            Text(
                text = "历史会话",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.title_large,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = on_new) {
                Icon(Icons.Default.Add, contentDescription = "新建会话", tint = colors.title_highlight)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = colors.subtitle.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                    Text("还没有历史会话", fontSize = 13.sp, color = colors.subtitle)
                    Text("点击右上角 + 开始新对话", fontSize = 11.sp, color = colors.subtitle.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sessions, key = { it.id }) { meta ->
                    ai_session_card(
                        meta = meta,
                        is_current = meta.id == current_session_id,
                        on_click = { on_switch(meta.id) },
                        on_rename = { rename_target = meta },
                        on_delete = { delete_target = meta }
                    )
                }
            }
        }
    }

    // 重命名对话框
    rename_target?.let { meta ->
        var new_title by remember(meta.id) { mutableStateOf(meta.title) }
        AlertDialog(
            onDismissRequest = { rename_target = null },
            title = { Text("重命名会话", color = colors.dialog_text) },
            text = {
                OutlinedTextField(
                    value = new_title,
                    onValueChange = { new_title = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    on_rename(meta.id, new_title.trim().ifBlank { meta.title })
                    rename_target = null
                }) { Text("保存", color = colors.title_highlight) }
            },
            dismissButton = {
                TextButton(onClick = { rename_target = null }) { Text("取消", color = colors.subtitle) }
            }
        )
    }

    // 删除确认
    delete_target?.let { meta ->
        AlertDialog(
            onDismissRequest = { delete_target = null },
            title = { Text("删除会话", color = colors.dialog_text) },
            text = { Text("确定要删除「${meta.title}」吗？", color = colors.dialog_text) },
            confirmButton = {
                TextButton(onClick = {
                    on_delete(meta.id)
                    delete_target = null
                }) { Text("删除", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { delete_target = null }) { Text("取消", color = colors.subtitle) }
            }
        )
    }
}

/** 单个会话卡片 */
@Composable
private fun ai_session_card(
    meta: ai_session_meta,
    is_current: Boolean,
    on_click: () -> Unit,
    on_rename: () -> Unit,
    on_delete: () -> Unit
) {
    val colors = app_theme_provider.colors
    val date_fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { on_click() },
        shape = RoundedCornerShape(10.dp),
        color = if (is_current) colors.title_highlight.copy(alpha = 0.1f) else colors.card_bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.title,
                    fontSize = 14.sp,
                    fontWeight = if (is_current) FontWeight.Bold else FontWeight.Medium,
                    color = colors.card_text_title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${meta.message_count} 条消息 · ${date_fmt.format(Date(meta.mtime))}",
                    fontSize = 10.5.sp,
                    color = colors.subtitle
                )
            }
            IconButton(onClick = on_rename, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "重命名", tint = colors.subtitle, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = on_delete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = colors.danger, modifier = Modifier.size(16.dp))
            }
        }
    }
}
