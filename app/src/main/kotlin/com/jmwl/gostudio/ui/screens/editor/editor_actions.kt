package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
 * 键盘上方符号栏：默认单行（// 注释 + 常用符号），上拉展开第二行（快捷代码片段）、下拉收起。
 * 符号与片段为纯文本 token，不加按钮底色包裹。
 */
@Composable
fun editor_symbol_bar(
    on_insert: (String) -> Unit
) {

    val colors = app_theme_provider.colors
    var expanded by remember { mutableStateOf(false) }
    var drag_accumulator by remember { mutableStateOf(0f) }
    val bar_height by animateDpAsState(
        targetValue = if (expanded) 88.dp else 44.dp,
        animationSpec = tween(180)
    )
    val symbols = remember {
        listOf(
            "TAB", ":=", ".", ",", ";", ":", "(", ")", "{", "}",
            "[", "]", "\"\"", "''", "<-", "...", "=", "==", "!=",
            "!", "&&", "||", "func", "if", "for", "range", "return"
        )
    }
    // 快捷代码片段：label 显示在栏上，点击插入完整代码（多行含 \n\t）
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bar_height)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { drag_accumulator = 0f },
                        onVerticalDrag = { _, drag_amount ->
                            drag_accumulator += drag_amount
                            when {
                                drag_accumulator < -22f -> {
                                    expanded = true
                                    drag_accumulator = 0f
                                }
                                drag_accumulator > 22f -> {
                                    expanded = false
                                    drag_accumulator = 0f
                                }
                            }
                        },
                        onDragEnd = { drag_accumulator = 0f },
                        onDragCancel = { drag_accumulator = 0f }
                    )
                }
        ) {
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    editor_symbols_row(
                        symbols = symbols,
                        on_insert = on_insert
                    )
                    editor_snippets_row(
                        snippets = snippets,
                        on_insert = on_insert
                    )
                }
            } else {
                editor_symbols_row(
                    symbols = symbols,
                    on_insert = on_insert,
                    vertical_padding = 5.dp
                )
            }
        }
    }
}

@Composable
private fun editor_symbols_row(
    symbols: List<String>,
    on_insert: (String) -> Unit,
    vertical_padding: Dp = 0.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = vertical_padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        symbols.forEach { symbol ->
            editor_symbol_token(
                symbol = symbol,
                on_insert = on_insert
            )
        }
    }
}

@Composable
private fun editor_snippets_row(
    snippets: List<Pair<String, String>>,
    on_insert: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        snippets.forEach { (label, code) ->
            editor_snippet_token(
                label = label,
                code = code,
                on_insert = on_insert
            )
        }
    }
}

/** 纯文本符号 token：不加按钮底色，整行高 34dp 可点。 */
@Composable
private fun editor_symbol_token(
    symbol: String,
    on_insert: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Box(
        modifier = Modifier
            .height(34.dp)
            .clickable { on_insert(symbol) }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            color = colors.editor_text,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** 快捷代码片段 token：label 上屏（hint 色与符号区分），点击插入完整代码。 */
@Composable
private fun editor_snippet_token(
    label: String,
    code: String,
    on_insert: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Box(
        modifier = Modifier
            .height(34.dp)
            .semantics { contentDescription = "插入 $label 对应代码" }
            .clickable { on_insert(code) }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = colors.editor_hint,
            fontSize = 12.5.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

