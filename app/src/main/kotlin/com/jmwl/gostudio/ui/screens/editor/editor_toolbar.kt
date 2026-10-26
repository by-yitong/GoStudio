package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * 顶栏正下方的编辑工具栏：撤销 · 重做 · 搜索 · 保存 · 格式化 · 参数（展开/折叠参数）。
 * 收编原右上角浮动条（22dp 热区过小且遮挡代码），现为六个纯文字按钮（11sp）：
 * 高 36dp、宽度随内容（左右 8dp 内边距撑开热区），整行 44dp。
 */
@Composable
fun editor_toolbar(
    can_undo: Boolean,
    can_redo: Boolean,
    has_changes: Boolean,
    can_format: Boolean,
    /** 光标是否选中内容：决定格式化按钮文案显示「格式化选区」还是「格式化」（在叶子组合里读取） */
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            editor_toolbar_text_button(
                label = "撤销",
                enabled = can_undo,
                on_click = on_undo
            )
            editor_toolbar_text_button(
                label = "重做",
                enabled = can_redo,
                on_click = on_redo
            )
            editor_toolbar_text_button(
                label = "搜索",
                enabled = true,
                // 搜索激活时换主题强调色（teal），未激活用常规文字色
                tint = if (search_active) colors.search_button_active else null,
                on_click = on_toggle_search
            )
            editor_toolbar_text_button(
                label = "保存",
                enabled = has_changes,
                on_click = on_save
            )
            editor_toolbar_text_button(
                label = if (format_selection) "格式化选区" else "格式化",
                enabled = can_format,
                on_click = on_format
            )
            // 展开/折叠参数：光标在函数调用括号内时，参数一行一个展开（Go 多行风格），再点折叠回单行
            editor_toolbar_text_button(
                label = "参数",
                enabled = can_format,
                on_click = on_toggle_call_args
            )
        }
    }
}

/** 工具栏文字按钮：11sp 纯文字，启用用文字色、禁用用 hint 色，tint 可覆盖（搜索激活态）。 */
@Composable
private fun editor_toolbar_text_button(
    label: String,
    enabled: Boolean,
    on_click: () -> Unit,
    tint: Color? = null
) {
    val colors = app_theme_provider.colors
    editor_toolbar_tap_target(enabled = enabled, on_click = on_click) {
        Text(
            text = label,
            color = tint ?: if (enabled) colors.editor_text else colors.editor_hint,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

/** 工具栏点击位：高 36dp、宽度随内容（左右 8dp 内边距撑开热区）、纯文字无底色，禁用态不可点击。 */
@Composable
private fun editor_toolbar_tap_target(
    enabled: Boolean,
    on_click: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clickable(enabled = enabled, onClick = on_click)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
