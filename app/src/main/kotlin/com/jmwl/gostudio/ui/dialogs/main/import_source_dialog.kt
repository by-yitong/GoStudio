package com.jmwl.gostudio.ui.dialogs.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.*

/** 导入方式选择：项目目录 或 ZIP 压缩包。 */
@Composable
fun import_source_dialog(
    on_dismiss: () -> Unit,
    on_pick_dir: () -> Unit,
    on_pick_zip: () -> Unit
) {
    val colors = app_theme_provider.colors

    AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        shape = MaterialTheme.shapes.large,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = colors.dialog_icon)
                Text("导入项目", color = colors.dialog_text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                import_source_row(
                    icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = colors.dialog_icon) },
                    title = "导入项目目录",
                    subtitle = "从文件管理器选择项目根目录（需含 go.mod）",
                    onClick = on_pick_dir
                )
                import_source_row(
                    icon = { Icon(Icons.Default.Archive, contentDescription = null, tint = colors.dialog_icon) },
                    title = "导入 ZIP 压缩包",
                    subtitle = "选择项目的 .zip 文件，自动解压并识别根目录",
                    onClick = on_pick_zip
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = on_dismiss) { Text("取消") }
        }
    )
}

@Composable
private fun import_source_row(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = app_theme_provider.colors
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.dialog_card_bg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Column {
                Text(title, color = colors.dialog_text, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = colors.dialog_hint, lineHeight = 15.sp)
            }
        }
    }
}
