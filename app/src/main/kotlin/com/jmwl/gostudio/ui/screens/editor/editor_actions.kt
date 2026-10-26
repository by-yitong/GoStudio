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
import com.jmwl.gostudio.editor.model.default_symbol_bar_snippets
import com.jmwl.gostudio.editor.model.default_symbol_bar_symbols
import com.jmwl.gostudio.ui.theme.app_theme_provider

/** 符号栏方向键回调用的光标移动方向。 */
internal enum class editor_cursor_move { LEFT, RIGHT, UP, DOWN }

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
 * 键盘上方符号栏：第一行 // 注释与常用符号，上拉展开第二行（方向键 + 快捷代码片段）、下拉收起。
 * 符号、方向键与片段均为纯文本 token，不加按钮底色包裹。
 * 符号与片段列表来自编辑器设置（可自定义），默认值见 editor_settings_state 的 default_* 常量。
 */
@Composable
internal fun editor_symbol_bar(
    on_insert: (String) -> Unit,
    on_move_cursor: (editor_cursor_move) -> Unit,
    symbols: List<String> = default_symbol_bar_symbols,
    snippets: List<Pair<String, String>> = default_symbol_bar_snippets
) {

    val colors = app_theme_provider.colors
    var expanded by remember { mutableStateOf(false) }
    var drag_accumulator by remember { mutableStateOf(0f) }
    val bar_height by animateDpAsState(
        targetValue = if (expanded) 88.dp else 44.dp,
        animationSpec = tween(180)
    )

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
                        on_insert = on_insert,
                        on_move_cursor = on_move_cursor
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
    on_insert: (String) -> Unit,
    on_move_cursor: (editor_cursor_move) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // 第二行最前：固定方向键，先于快捷片段渲染，不受 snippets 列表影响
        editor_cursor_move_token("←", editor_cursor_move.LEFT, on_move_cursor)
        editor_cursor_move_token("→", editor_cursor_move.RIGHT, on_move_cursor)
        editor_cursor_move_token("↑", editor_cursor_move.UP, on_move_cursor)
        editor_cursor_move_token("↓", editor_cursor_move.DOWN, on_move_cursor)
        snippets.forEach { (label, code) ->
            editor_snippet_token(
                label = label,
                code = code,
                on_insert = on_insert
            )
        }
    }
}

/** 光标方向键 token：与符号 token 同样式（纯文本无底色），点击回调移动方向。 */
@Composable
private fun editor_cursor_move_token(
    label: String,
    direction: editor_cursor_move,
    on_move_cursor: (editor_cursor_move) -> Unit
) {
    val colors = app_theme_provider.colors
    Box(
        modifier = Modifier
            .height(34.dp)
            .clickable { on_move_cursor(direction) }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = colors.editor_text,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
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

