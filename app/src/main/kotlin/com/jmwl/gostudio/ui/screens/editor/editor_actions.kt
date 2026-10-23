package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider

@Composable
fun cursor_chip(
    line: Int,
    column: Int,
    modifier: Modifier = Modifier
) {

    val colors = app_theme_provider.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = colors.editor_panel_overlay
    ) {
        Text(
            text = "Ln $line, Col $column",
            color = colors.editor_hint,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * 键盘上方符号栏：固定两行——第一行 // 注释 + 常用符号，第二行快捷代码片段。
 * 原来的上拉展开手势已去掉，高度恒定（两行 34dp + 行距 6dp + 竖向 padding 6dp）。
 */
@Composable
fun editor_symbol_bar(
    on_insert: (String) -> Unit,
    on_toggle_comment: () -> Unit
) {

    val colors = app_theme_provider.colors
    val symbols = remember {
        listOf(
            "TAB", ":=", ".", ",", ";", ":", "(", ")", "{", "}",
            "[", "<-", "...", "=", "==", "!=", "!", "&&", "||",
            "func", "if", "for", "range", "return"
        )
    }
    // 快捷代码片段：label 显示在按钮上，点击插入完整代码（多行含 \n\t）
    val snippets = remember {
        listOf(
            "err ret" to "if err != nil {\n\treturn\n}",
            "ret err" to "if err != nil {\n\treturn err\n}",
            "range" to "for _, v := range ",
            "func" to "func name() {\n\n}",
            "defer" to "defer func() {\n\t\n}()",
            "go" to "go func() {\n\t\n}()",
            "make" to "make([]T, 0)",
            "map" to "map[string]T{}",
            "sprintf" to "fmt.Sprintf(\"%v\", )",
            "println" to "fmt.Println()"
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = colors.editor_bg,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 第一行：// 注释 + 常用符号
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                editor_symbol_comment_button(on_click = on_toggle_comment)
                symbols.forEach { symbol ->
                    editor_symbol_button(
                        symbol = symbol,
                        on_insert = on_insert
                    )
                }
            }
            // 第二行：快捷代码片段
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                snippets.forEach { (label, code) ->
                    editor_snippet_button(
                        label = label,
                        code = code,
                        on_insert = on_insert
                    )
                }
            }
        }
    }
}

@Composable
private fun editor_symbol_button(
    symbol: String,
    on_insert: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Surface(
        modifier = Modifier
            .height(34.dp)
            .clickable { on_insert(symbol) },
        shape = RoundedCornerShape(8.dp),
        color = colors.editor_button_bg
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                symbol,
                color = colors.editor_text,
                fontSize = 12.5.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** 快捷代码片段按钮：label 上屏显示，点击插入完整代码；样式与符号 chip 一致。 */
@Composable
private fun editor_snippet_button(
    label: String,
    code: String,
    on_insert: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Surface(
        modifier = Modifier
            .height(34.dp)
            .semantics { contentDescription = "插入 $label 对应代码" }
            .clickable { on_insert(code) },
        shape = RoundedCornerShape(8.dp),
        color = colors.editor_button_bg
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = colors.editor_text,
                fontSize = 12.5.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** 注释切换按钮：给当前行（或选区各行）加/去 // 注释。 */
@Composable
private fun editor_symbol_comment_button(
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Surface(
        modifier = Modifier
            .size(width = 44.dp, height = 34.dp)
            .clickable { on_click() },
        shape = RoundedCornerShape(8.dp),
        color = colors.editor_button_bg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "//",
                color = colors.editor_text,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** 跳转到定义的胶囊按钮，由编辑器底部中间展示。 */
@Composable
fun goto_definition_chip(
    running: Boolean,
    on_click: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val text_measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val label = "跳转定义"
    val label_width = text_measurer.measure(
        text = label,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    ).size.width
    val chip_width = with(density) { (label_width + 50.dp.toPx()).toDp() }

    Surface(
        modifier = modifier
            .size(width = chip_width, height = 32.dp)
            .clickable(enabled = !running) { on_click() },
        shape = RoundedCornerShape(16.dp),
        color = colors.editor_panel_overlay
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (running) "…" else "⇲",
                color = colors.title_highlight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                color = colors.title_highlight,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
