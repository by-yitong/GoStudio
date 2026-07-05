package com.xc.code.ui.dialogs.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.utils.uri_utils
import com.xc.code.ui.theme.*

@Composable
fun open_project_dialog(
    on_dismiss: () -> Unit,
    on_open: (String) -> Unit
) {
    val context = LocalContext.current
    var project_path by remember { mutableStateOf("") }
    val is_open_enabled = project_path.isNotBlank()
    val colors = app_theme_provider.colors
    
    val folder_launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val path = uri_utils.get_path_from_uri(context, it)
                project_path = path
            }
        }
    }
    
    val text_field_colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.dialog_input_border,
        unfocusedBorderColor = colors.dialog_input_hint.copy(alpha = 0.5f),
        focusedTextColor = colors.dialog_input_text,
        unfocusedTextColor = colors.dialog_input_text,
        cursorColor = colors.dialog_input_border,
        focusedLeadingIconColor = colors.dialog_input_icon,
        unfocusedLeadingIconColor = colors.dialog_input_icon_hint,
        focusedLabelColor = colors.dialog_input_border,
        unfocusedLabelColor = colors.dialog_input_hint,
        focusedContainerColor = colors.dialog_input_bg,
        unfocusedContainerColor = colors.dialog_input_bg
    )
    
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
                    Icons.Default.FolderOpen,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = project_path,
                    onValueChange = { project_path = it },
                    label = { Text("项目路径", color = colors.dialog_input_hint) },
                    placeholder = { Text("/storage/emulated/0/XCodeProjects", color = colors.dialog_input_hint) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                folder_launcher.launch(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "选择文件夹",
                                tint = colors.dialog_input_icon_hint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = text_field_colors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (is_open_enabled) {
                        on_open(project_path)
                        on_dismiss()
                    }
                },
                enabled = is_open_enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dialog_clone_bg,
                    contentColor = colors.dialog_clone_text,
                    disabledContainerColor = colors.dialog_hint.copy(alpha = 0.3f),
                    disabledContentColor = colors.dialog_hint
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(100.dp)
            ) {
                Text("打开", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = on_dismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.dialog_cancel
                )
            ) {
                Text("取消", fontSize = 14.sp)
            }
        }
    )
}