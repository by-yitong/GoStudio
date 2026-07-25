package com.jmwl.gostudio.ui.dialogs.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.project.project_manager
import com.jmwl.gostudio.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun open_project_dialog(
    on_dismiss: () -> Unit,
    on_open: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    val projects = remember { project_manager.list_local_projects() }
    val date_format = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = colors.dialog_icon,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "打开项目",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.dialog_text
                )
            }
        },
        text = {
            if (projects.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "暂无项目",
                        fontSize = 14.sp,
                        color = colors.dialog_hint
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "新建一个 Go 项目开始吧",
                        fontSize = 12.sp,
                        color = colors.dialog_hint
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(projects) { project_dir ->
                        ProjectRow(
                            project_dir = project_dir,
                            date_format = date_format,
                            colors = colors,
                            on_click = {
                                on_open(project_dir.absolutePath)
                                on_dismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = on_dismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.dialog_cancel
                )
            ) {
                Text("关闭", fontSize = 14.sp)
            }
        }
    )
}

@Composable
private fun ProjectRow(
    project_dir: File,
    date_format: SimpleDateFormat,
    colors: app_colors,
    on_click: () -> Unit
) {
    val modified = remember(project_dir) {
        runCatching { date_format.format(Date(project_dir.lastModified())) }.getOrDefault("")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.dialog_input_bg)
            .clickable { on_click() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.dialog_icon.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = colors.dialog_icon,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = project_dir.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.dialog_text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (modified.isNotBlank()) {
                Text(
                    text = modified,
                    fontSize = 10.sp,
                    color = colors.dialog_hint
                )
            }
        }
    }
}
