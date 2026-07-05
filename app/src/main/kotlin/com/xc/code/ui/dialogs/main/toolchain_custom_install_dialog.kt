package com.xc.code.ui.dialogs.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.xc.code.ui.theme.app_theme_provider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun toolchain_custom_install_dialog(
    title: String,
    on_dismiss: () -> Unit,
    on_install: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    var archive_path by remember { mutableStateOf("") }
    val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trimmed_path = archive_path.trim()

    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        sheetState = sheet_state,
        dragHandle = null,
        containerColor = colors.dialog_bg,
        contentColor = colors.dialog_text,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.dialog_text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "选择本地压缩包进行安装",
                        fontSize = 12.sp,
                        color = colors.dialog_hint
                    )
                }
            }

            OutlinedTextField(
                value = archive_path,
                onValueChange = { archive_path = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                singleLine = true,
                label = {
                    Text(
                        text = "压缩包路径",
                        fontSize = 12.sp,
                        color = colors.dialog_input_hint
                    )
                },
                placeholder = {
                    Text(
                        text = "/storage/emulated/0/Download/toolchain.zip",
                        fontSize = 12.sp,
                        color = colors.dialog_input_hint.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (archive_path.isBlank()) colors.dialog_input_icon_hint else colors.dialog_input_icon,
                        modifier = Modifier.size(18.dp)
                    )
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.dialog_input_border,
                    unfocusedBorderColor = colors.dialog_input_hint.copy(alpha = 0.45f),
                    focusedTextColor = colors.dialog_input_text,
                    unfocusedTextColor = colors.dialog_input_text,
                    cursorColor = colors.dialog_input_border,
                    focusedContainerColor = colors.dialog_input_bg,
                    unfocusedContainerColor = colors.dialog_input_bg
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { on_install(trimmed_path) },
                    enabled = trimmed_path.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.dialog_clone_bg,
                        contentColor = colors.dialog_clone_text
                    ),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Icon(
                        Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "安装",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}