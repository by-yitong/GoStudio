package com.xc.code.ui.screens.editor

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.xc.code.project_file_tree.project_file_tree_colors

@Composable
fun file_tree_child_count(count: Int, colors: project_file_tree_colors) {
    Text(
        text = count.toString(),
        color = colors.editor_hint,
        fontSize = 10.sp
    )
}
