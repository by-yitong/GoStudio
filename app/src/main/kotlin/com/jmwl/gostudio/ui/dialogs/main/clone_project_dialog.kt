package com.jmwl.gostudio.ui.dialogs.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun clone_project_dialog(
    on_dismiss: () -> Unit,
    on_clone: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    var repository_url by remember { mutableStateOf("") }
    val is_valid = remember(repository_url) {
        Regex(
            """^(?:https?://[\w.-]+(?::\d+)?(?:/[\w.-]+)+/?|ssh://git@[\w.-]+(?::\d+)?(?:/[\w.-]+)+|git@[\w.-]+:[\w.-]+(?:/[\w.-]+)*)$""",
            RegexOption.IGNORE_CASE
        ).matches(repository_url.trim())
    }
    val field_colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.dialog_input_border,
        unfocusedBorderColor = colors.dialog_input_border.copy(alpha = 0.45f),
        focusedTextColor = colors.dialog_input_text,
        unfocusedTextColor = colors.dialog_input_text,
        cursorColor = colors.dialog_input_border,
        focusedLabelColor = colors.dialog_input_border,
        unfocusedLabelColor = colors.dialog_hint,
        focusedContainerColor = colors.dialog_input_bg,
        unfocusedContainerColor = colors.dialog_input_bg
    )

    AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        shape = MaterialTheme.shapes.large,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = colors.dialog_icon)
                Text("克隆 Git 项目", color = colors.dialog_text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = repository_url,
                    onValueChange = { repository_url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("仓库地址") },
                    placeholder = { Text("https://github.com/owner/repo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    isError = repository_url.isNotBlank() && !is_valid,
                    colors = field_colors
                )
                Text(
                    if (repository_url.isNotBlank() && !is_valid) "请输入有效的 Git 仓库地址"
                    else "支持 GitHub/Gitee/GitLab 的 HTTPS 与 SSH 地址；仅打开根目录含 go.mod 的项目",
                    fontSize = 12.sp,
                    color = if (repository_url.isNotBlank() && !is_valid) colors.danger else colors.dialog_hint
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = is_valid,
                onClick = {
                    on_clone(repository_url.trim())
                    on_dismiss()
                }
            ) { Text("克隆") }
        },
        dismissButton = {
            TextButton(onClick = on_dismiss) { Text("取消") }
        }
    )
}
