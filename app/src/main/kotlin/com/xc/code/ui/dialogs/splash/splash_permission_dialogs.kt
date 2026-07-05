package com.xc.code.ui.dialogs.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun manage_storage_dialog(
    on_confirm: () -> Unit,
    on_deny: () -> Unit
) {
    Dialog(
        onDismissRequest = on_deny,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        permission_dialog_card(
            title = "需要所有文件访问权限",
            message = "Android 11 及以上版本需要授予「所有文件访问权限」才能正常读写文件",
            cancel_text = "退出",
            confirm_text = "去设置",
            on_cancel = on_deny,
            on_confirm = on_confirm
        )
    }
}

@Composable
fun permission_rationale_dialog(
    on_confirm: () -> Unit,
    on_deny: () -> Unit
) {
    Dialog(
        onDismissRequest = on_deny,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        permission_dialog_card(
            title = "需要存储权限",
            message = "为了打开、编辑和保存代码文件，需要访问存储权限",
            cancel_text = "拒绝",
            confirm_text = "同意",
            on_cancel = on_deny,
            on_confirm = on_confirm
        )
    }
}

@Composable
fun permission_denied_dialog(
    on_retry: () -> Unit,
    on_exit: () -> Unit
) {
    Dialog(
        onDismissRequest = on_exit,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        permission_dialog_card(
            title = "权限被拒绝",
            message = "应用需要存储权限才能正常运行，请在设置中手动开启",
            cancel_text = "退出",
            confirm_text = "重试",
            on_cancel = on_exit,
            on_confirm = on_retry
        )
    }
}

@Composable
private fun permission_dialog_card(
    title: String,
    message: String,
    cancel_text: String,
    confirm_text: String,
    on_cancel: () -> Unit,
    on_confirm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = on_cancel) {
                    Text(cancel_text, color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = on_confirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004DEA))
                ) {
                    Text(confirm_text, color = Color.White)
                }
            }
        }
    }
}
