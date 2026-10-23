package com.jmwl.gostudio.ui.screens.editor

import com.jmwl.gostudio.editor.core.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * 顶栏正下方的编辑工具栏：撤销 · 重做 · 搜索 · 保存 · 格式化 · 展开/折叠参数。
 * 收编原右上角浮动条（22dp 热区过小且遮挡代码），按钮统一 44x36dp 热区。
 */
@Composable
fun editor_toolbar(
    can_undo: Boolean,
    can_redo: Boolean,
    has_changes: Boolean,
    can_format: Boolean,
    /** 光标是否选中内容：决定格式化图标用「格式化选区」还是「格式化全文」（在叶子组合里读取） */
    format_selection_provider: () -> Boolean = { false },
    search_active: Boolean,
    on_undo: () -> Unit,
    on_redo: () -> Unit,
    on_save: () -> Unit,
    on_format: () -> Unit,
    on_toggle_call_args: () -> Unit,
    on_toggle_search: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val format_selection = format_selection_provider()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.editor_bg,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            editor_toolbar_icon_button(
                icon = Icons.AutoMirrored.Filled.Undo,
                content_description = "撤销",
                enabled = can_undo,
                on_click = on_undo
            )
            editor_toolbar_icon_button(
                icon = Icons.AutoMirrored.Filled.Redo,
                content_description = "重做",
                enabled = can_redo,
                on_click = on_redo
            )
            editor_toolbar_icon_button(
                icon = Icons.Default.Search,
                content_description = "搜索",
                enabled = true,
                // 搜索激活时换主题强调色（teal），未激活用常规文字色
                tint = if (search_active) colors.search_button_active else null,
                on_click = on_toggle_search
            )
            editor_toolbar_icon_button(
                icon = Icons.Default.Save,
                content_description = "保存",
                enabled = has_changes,
                on_click = on_save
            )
            editor_toolbar_painter_button(
                icon_res = if (format_selection) R.drawable.ic_editor_format_selection else R.drawable.ic_editor_format,
                content_description = "格式化",
                enabled = can_format,
                on_click = on_format
            )
            // 展开/折叠参数：光标在函数调用括号内时，参数一行一个展开（Go 多行风格），再点折叠回单行
            editor_toolbar_icon_button(
                icon = Icons.Default.UnfoldMore,
                content_description = "展开折叠参数",
                enabled = can_format,
                on_click = on_toggle_call_args
            )
        }
    }
}

@Composable
private fun editor_toolbar_icon_button(
    icon: ImageVector,
    content_description: String,
    enabled: Boolean,
    on_click: () -> Unit,
    tint: Color? = null
) {
    val colors = app_theme_provider.colors
    editor_toolbar_tap_target(enabled = enabled, on_click = on_click) {
        Icon(
            imageVector = icon,
            contentDescription = content_description,
            tint = tint ?: if (enabled) colors.editor_text else colors.editor_hint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun editor_toolbar_painter_button(
    icon_res: Int,
    content_description: String,
    enabled: Boolean,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    editor_toolbar_tap_target(enabled = enabled, on_click = on_click) {
        Icon(
            painter = painterResource(icon_res),
            contentDescription = content_description,
            tint = if (enabled) colors.editor_text else colors.editor_hint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 工具栏点击位：44x36dp 热区、纯图标无底色，禁用态不可点击。 */
@Composable
private fun editor_toolbar_tap_target(
    enabled: Boolean,
    on_click: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 36.dp)
            .clickable(enabled = enabled, onClick = on_click),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
