package com.xc.code.ui.dialogs.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.editor.model.editor_file_node
import com.xc.code.project_file_tree.project_file_tree_colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editor_file_tree_action_sheet(
    node: editor_file_node,
    colors: project_file_tree_colors,
    on_dismiss: () -> Unit,
    on_rename: () -> Unit,
    on_delete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        contentColor = colors.editor_text,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val node_type_name = if (node.is_directory) "文件夹" else "文件"
            Text(
                text = node.name,
                color = colors.editor_text,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.editor_divider)
            )
            file_tree_action_item(
                icon = Icons.Filled.DriveFileRenameOutline,
                title = "重命名",
                subtitle = "修改${node_type_name}名称",
                tint = colors.editor_icon,
                colors = colors,
                on_click = on_rename
            )
            file_tree_action_item(
                icon = Icons.Filled.Delete,
                title = "删除${node_type_name}",
                subtitle = "删除${node_type_name}后无法恢复",
                tint = colors.danger,
                colors = colors,
                on_click = on_delete
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editor_file_tree_delete_sheet(
    node: editor_file_node,
    colors: project_file_tree_colors,
    on_dismiss: () -> Unit,
    on_confirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        contentColor = colors.editor_text,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("删除 ${node.name}", color = colors.editor_text, fontSize = 15.sp)
            val node_type_name = if (node.is_directory) "文件夹" else "文件"
            Text("此操作会删除这个${node_type_name}。", color = colors.editor_hint, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = on_dismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", color = colors.editor_hint, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.danger_bg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = on_confirm
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("删除", color = colors.danger, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun file_tree_action_item(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    colors: project_file_tree_colors,
    on_click: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = on_click
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.editor_text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = colors.editor_hint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
