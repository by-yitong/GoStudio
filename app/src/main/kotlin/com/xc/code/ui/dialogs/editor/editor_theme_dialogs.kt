package com.xc.code.ui.dialogs.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.editor.theme.editor_theme_manager
import com.xc.code.ui.theme.app_theme_provider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun editor_theme_color_dialog(
    title: String,
    key: String,
    value: String,
    on_dismiss: () -> Unit,
    on_save: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    var editing_value by remember(key) { mutableStateOf(value) }
    val normalized = editor_theme_manager.normalize_color(editing_value)
    val is_valid = normalized != null
    val preview_color = normalized?.let { editor_theme_manager.parse_color_argb(it) }?.let { Color(it) } ?: Color.Transparent

    AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        title = {
            Text(
                text = title,
                color = colors.dialog_text,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = key,
                    fontSize = 11.sp,
                    color = colors.dialog_hint
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(preview_color)
                    )
                    OutlinedTextField(
                        value = editing_value,
                        onValueChange = { editing_value = it },
                        singleLine = true,
                        label = { Text("颜色值") },
                        placeholder = { Text("#RRGGBB 或 #RRGGBBAA") },
                        isError = editing_value.isNotBlank() && !is_valid,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.dialog_input_border,
                            unfocusedBorderColor = colors.dialog_input_hint.copy(alpha = 0.5f),
                            errorBorderColor = Color(0xFFFF6B6B),
                            focusedTextColor = colors.dialog_input_text,
                            unfocusedTextColor = colors.dialog_input_text,
                            cursorColor = colors.dialog_input_border,
                            focusedLabelColor = colors.dialog_input_border,
                            unfocusedLabelColor = colors.dialog_input_hint,
                            focusedContainerColor = colors.dialog_input_bg,
                            unfocusedContainerColor = colors.dialog_input_bg
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                editor_theme_color_palette(
                    selected = normalized,
                    on_select = { editing_value = it }
                )

                editor_theme_alpha_palette(
                    value = normalized,
                    on_select = { editing_value = it }
                )

                Text(
                    text = "支持 #RRGGBB 和 #RRGGBBAA，透明度写在最后两位。",
                    fontSize = 11.sp,
                    color = colors.dialog_hint
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { normalized?.let(on_save) },
                enabled = is_valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dialog_clone_bg,
                    contentColor = colors.dialog_clone_text,
                    disabledContainerColor = colors.dialog_hint.copy(alpha = 0.3f),
                    disabledContentColor = colors.dialog_hint
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = on_dismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.dialog_cancel)
            ) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun editor_theme_color_palette(
    selected: String?,
    on_select: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("快捷色板", color = colors.dialog_text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            editor_theme_quick_colors.forEach { color_value ->
                val color = editor_theme_manager.parse_color_argb(color_value)?.let { Color(it) } ?: Color.Transparent
                val selected_color = selected?.take(7)?.equals(color_value, ignoreCase = true) == true
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = { on_select(apply_existing_alpha(color_value, selected)) }
                        )
                        .then(
                            if (selected_color) {
                                Modifier.background(Color.White.copy(alpha = 0.18f), CircleShape)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun editor_theme_alpha_palette(
    value: String?,
    on_select: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    if (value == null) return
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("透明度", color = colors.dialog_text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            editor_theme_alpha_options.forEach { (label, alpha) ->
                val updated = apply_alpha(value, alpha)
                FilterChip(
                    selected = value.equals(updated, ignoreCase = true),
                    onClick = { on_select(updated) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colors.dialog_input_bg,
                        labelColor = colors.dialog_input_text,
                        selectedContainerColor = colors.dialog_clone_bg,
                        selectedLabelColor = colors.dialog_clone_text
                    )
                )
            }
        }
    }
}

private fun apply_existing_alpha(color_value: String, current: String?): String {
    val alpha = current?.takeIf { it.length == 9 }?.takeLast(2).orEmpty()
    return if (alpha.isBlank() || alpha.equals("FF", ignoreCase = true)) color_value else color_value + alpha
}

private fun apply_alpha(value: String, alpha: String): String {
    val rgb = value.take(7)
    return if (alpha.equals("FF", ignoreCase = true)) rgb else rgb + alpha
}

@Composable
fun editor_theme_reset_dialog(
    on_dismiss: () -> Unit,
    on_confirm: () -> Unit
) {
    val colors = app_theme_provider.colors
    AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        title = {
            Text(
                text = "恢复默认颜色",
                color = colors.dialog_text,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "会把编辑器颜色恢复为 assets/textmate/themes/xcode.json 的默认值。",
                color = colors.dialog_hint,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = on_confirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dialog_clone_bg,
                    contentColor = colors.dialog_clone_text
                )
            ) {
                Text("恢复")
            }
        },
        dismissButton = {
            TextButton(
                onClick = on_dismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.dialog_cancel)
            ) {
                Text("取消")
            }
        }
    )
}

private val editor_theme_quick_colors = listOf(
    "#E8EDF2",
    "#5A7A9A",
    "#4A9EFF",
    "#7DB7E8",
    "#7FD6C2",
    "#A5C25C",
    "#E5B567",
    "#D19A66",
    "#E35D5D",
    "#C678DD",
    "#2A3A4A",
    "#1A242E",
    "#000000",
    "#FFFFFF"
)

private val editor_theme_alpha_options = listOf(
    "100%" to "FF",
    "80%" to "CC",
    "60%" to "99",
    "40%" to "66",
    "25%" to "40"
)
