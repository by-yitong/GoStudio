package com.jmwl.gostudio.ui.screens.ai

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_message
import com.jmwl.gostudio.ai.ai_message_role
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.toast.app_toast

/**
 * 消息操作菜单（长按触发）。根据消息角色提供不同操作：
 * - 所有：复制、分享、删除
 * - assistant：重新生成
 * - user：编辑并重发
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ai_message_with_actions(
    message: ai_message,
    on_copy: (String) -> Unit,
    on_share: (String) -> Unit,
    on_delete: () -> Unit,
    on_regenerate: () -> Unit = {},
    on_edit: ((String) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var menu_open by remember { mutableStateOf(false) }
    var edit_dialog_open by remember { mutableStateOf(false) }
    var edit_text by remember { mutableStateOf(message.text) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu_open = true
                }
            )
    ) {
        content()

        DropdownMenu(
            expanded = menu_open,
            onDismissRequest = { menu_open = false }
        ) {
            // 复制
            DropdownMenuItem(
                text = { Text("复制", fontSize = 13.sp, color = colors.dialog_text) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.subtitle, modifier = Modifier.padding(end = 8.dp).then(Modifier)) },
                onClick = {
                    menu_open = false
                    on_copy(message.text)
                    app_toast.show(context, "已复制", app_toast.LENGTH_SHORT)
                }
            )
            // 分享
            DropdownMenuItem(
                text = { Text("分享", fontSize = 13.sp, color = colors.dialog_text) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = colors.subtitle) },
                onClick = {
                    menu_open = false
                    on_share(message.text)
                }
            )
            // assistant 专属：重新生成
            if (message.role == ai_message_role.ASSISTANT && !message.is_error && !message.streaming) {
                DropdownMenuItem(
                    text = { Text("重新生成", fontSize = 13.sp, color = colors.dialog_text) },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.subtitle) },
                    onClick = {
                        menu_open = false
                        on_regenerate()
                    }
                )
            }
            // user 专属：编辑并重发
            if (message.role == ai_message_role.USER && on_edit != null) {
                DropdownMenuItem(
                    text = { Text("编辑并重发", fontSize = 13.sp, color = colors.dialog_text) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.subtitle) },
                    onClick = {
                        menu_open = false
                        edit_text = message.text
                        edit_dialog_open = true
                    }
                )
            }
            // 删除
            DropdownMenuItem(
                text = { Text("删除", fontSize = 13.sp, color = colors.danger) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.danger) },
                onClick = {
                    menu_open = false
                    on_delete()
                }
            )
        }
    }

    // 编辑对话框
    if (edit_dialog_open && on_edit != null) {
        ai_edit_message_dialog(
            initial_text = edit_text,
            on_confirm = { new_text ->
                edit_dialog_open = false
                on_edit(new_text)
            },
            on_dismiss = { edit_dialog_open = false }
        )
    }
}

/** 编辑消息的小对话框 */
@Composable
private fun ai_edit_message_dialog(
    initial_text: String,
    on_confirm: (String) -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    var text by remember(initial_text) { mutableStateOf(initial_text) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = on_dismiss,
        title = { Text("编辑消息", color = colors.dialog_text) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                minLines = 2,
                maxLines = 6
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { on_confirm(text) }) {
                Text("重发", color = colors.title_highlight)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = on_dismiss) {
                Text("取消", color = colors.subtitle)
            }
        }
    )
}

/** 把文本分享出去（系统分享面板） */
fun share_text(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
