package com.xc.code.ui.dialogs.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.app_theme_provider

@Composable
fun editor_create_entry_dialog(
    is_folder: Boolean,
    on_confirm: (String) -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    var name by remember(is_folder) { mutableStateOf("") }
    val title = if (is_folder) "新建文件夹" else "新建文件"
    val placeholder = if (is_folder) "文件夹名称" else "文件名称"

    AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text(
                text = title,
                color = colors.dialog_text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(placeholder, color = colors.dialog_input_hint) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.dialog_input_border,
                    unfocusedBorderColor = colors.dialog_input_hint.copy(alpha = 0.5f),
                    focusedTextColor = colors.dialog_input_text,
                    unfocusedTextColor = colors.dialog_input_text,
                    cursorColor = colors.dialog_input_border,
                    focusedContainerColor = colors.dialog_input_bg,
                    unfocusedContainerColor = colors.dialog_input_bg
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.trim().isNotEmpty(),
                onClick = { on_confirm(name.trim()) }
            ) {
                Text("创建", color = colors.dialog_clone_bg)
            }
        },
        dismissButton = {
            TextButton(onClick = on_dismiss) {
                Text("取消", color = colors.dialog_hint)
            }
        }
    )
}
