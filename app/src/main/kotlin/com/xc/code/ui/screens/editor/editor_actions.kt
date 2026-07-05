package com.xc.code.ui.screens.editor

import com.xc.code.editor.core.R

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.app_theme_provider

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

@Composable
fun editor_floating_actions(
    can_undo: Boolean,
    can_redo: Boolean,
    has_changes: Boolean,
    can_format: Boolean,
    format_selection: Boolean,
    show_format: Boolean,
    show_save: Boolean,
    on_undo: () -> Unit,
    on_redo: () -> Unit,
    on_format: () -> Unit,
    on_save: () -> Unit,
    modifier: Modifier = Modifier
) {

    val colors = app_theme_provider.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = colors.editor_panel_overlay
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                compact_floating_icon_button(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    content_description = "撤销",
                    enabled = can_undo,
                    tint = if (can_undo) colors.editor_text else colors.editor_hint,
                    on_click = on_undo
                )
                compact_floating_icon_button(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    content_description = "重做",
                    enabled = can_redo,
                    tint = if (can_redo) colors.editor_text else colors.editor_hint,
                    on_click = on_redo
                )
                if (show_save) {
                    compact_floating_icon_button(
                        icon = Icons.Default.Save,
                        content_description = "保存",
                        enabled = has_changes,
                        tint = if (has_changes) colors.editor_icon else colors.editor_hint,
                        on_click = on_save
                    )
                }
                if (show_format) {
                    compact_floating_painter_button(
                        icon_res = if (format_selection) R.drawable.ic_editor_format_selection else R.drawable.ic_editor_format,
                        content_description = if (format_selection) "格式化选区" else "格式化全文",
                        enabled = can_format,
                        tint = if (can_format) colors.editor_text else colors.editor_hint,
                        on_click = on_format
                    )
                }
            }
        }

    }
}

@Composable
private fun compact_floating_painter_button(
    icon_res: Int,
    content_description: String,
    enabled: Boolean,
    tint: Color,
    on_click: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(5.dp))
            .clickable(enabled = enabled, onClick = on_click),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon_res),
            contentDescription = content_description,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun compact_floating_icon_button(
    icon: ImageVector,
    content_description: String,
    enabled: Boolean,
    tint: Color,
    on_click: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(5.dp))
            .clickable(enabled = enabled, onClick = on_click),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = content_description,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun editor_symbol_bar(
    on_insert: (String) -> Unit
) {

    val colors = app_theme_provider.colors
    var expanded by remember { mutableStateOf(false) }
    var drag_accumulator by remember { mutableStateOf(0f) }
    val bar_height by animateDpAsState(
        targetValue = if (expanded) 92.dp else 44.dp,
        animationSpec = tween(180)
    )
    val symbols = remember {
        listOf("TAB", "()", "{}", "[]", "\"\"", "''", "<>", ";", ":", "=", "+", "-", "*", "/", "|", "&", "!", "?", ",", ".")
    }
    val first_row_symbols = remember(symbols) { symbols.take(10) }
    val second_row_symbols = remember(symbols) { symbols.drop(10) }

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
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(width = 44.dp, height = 34.dp),
                            shape = editor_symbol_button_shape(symbol_button_position.start),
                            color = colors.editor_button_bg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "符号栏",
                                    tint = colors.editor_icon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        first_row_symbols.forEach { symbol ->
                            editor_symbol_button(
                                symbol = symbol,
                                position = symbol_button_position.middle,
                                on_insert = on_insert
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        second_row_symbols.forEachIndexed { index, symbol ->
                            editor_symbol_button(
                                symbol = symbol,
                                position = if (index == second_row_symbols.lastIndex) symbol_button_position.end else symbol_button_position.middle,
                                on_insert = on_insert
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    symbols.forEachIndexed { index, symbol ->
                        editor_symbol_button(
                            symbol = symbol,
                            position = symbol_button_position.for_index(index, symbols.lastIndex),
                            on_insert = on_insert
                        )
                    }
                }
            }
        }
    }
}

private enum class symbol_button_position {
    start,
    middle,
    end,
    single;

    companion object {
        fun for_index(index: Int, last_index: Int): symbol_button_position {
            return when {
                last_index <= 0 -> single
                index == 0 -> start
                index == last_index -> end
                else -> middle
            }
        }
    }
}

private fun editor_symbol_button_shape(position: symbol_button_position): RoundedCornerShape {
    return when (position) {
        symbol_button_position.start -> RoundedCornerShape(
            topStart = 16.dp,
            bottomStart = 16.dp,
            topEnd = 7.dp,
            bottomEnd = 7.dp
        )
        symbol_button_position.end -> RoundedCornerShape(
            topStart = 7.dp,
            bottomStart = 7.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp
        )
        symbol_button_position.single -> RoundedCornerShape(16.dp)
        symbol_button_position.middle -> RoundedCornerShape(7.dp)
    }
}

@Composable
private fun editor_symbol_button(
    symbol: String,
    position: symbol_button_position,
    on_insert: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Surface(
        modifier = Modifier.size(width = if (symbol == "TAB") 44.dp else 38.dp, height = 34.dp),
        shape = editor_symbol_button_shape(position),
        color = colors.editor_button_bg,
        onClick = { on_insert(symbol) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, color = colors.editor_text, fontSize = 12.5.sp)
        }
    }
}
