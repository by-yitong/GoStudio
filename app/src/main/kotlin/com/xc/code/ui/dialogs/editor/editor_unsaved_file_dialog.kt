package com.xc.code.ui.dialogs.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.app_theme_provider

@Composable
fun editor_unsaved_file_dialog(
    file_name: String,
    on_save: () -> Unit,
    on_discard: () -> Unit
) {
    val colors = app_theme_provider.colors

    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = colors.dialog_icon,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "保存更改",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.dialog_text
                )
            }
        },
        text = {
            Text(
                text = "${file_name.ifBlank { "当前文件" }} 有未保存更改。",
                color = colors.dialog_hint,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = on_save,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dialog_clone_bg,
                    contentColor = colors.dialog_clone_text
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("保存", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = on_discard,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.dialog_hint)
            ) {
                Text("取消", fontSize = 14.sp)
            }
        }
    )
}

@Composable
fun editor_exit_confirm_dialog(
    on_confirm: () -> Unit,
    on_cancel: () -> Unit
) {
    val colors = app_theme_provider.colors

    AlertDialog(
        onDismissRequest = on_cancel,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = colors.dialog_icon,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "退出编辑器",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.dialog_text
                )
            }
        },
        text = {
            Text(
                text = "确定要退出当前编辑器吗？",
                color = colors.dialog_hint,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = on_confirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dialog_clone_bg,
                    contentColor = colors.dialog_clone_text
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("退出", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = on_cancel,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.dialog_hint)
            ) {
                Text("取消", fontSize = 14.sp)
            }
        }
    )
}
