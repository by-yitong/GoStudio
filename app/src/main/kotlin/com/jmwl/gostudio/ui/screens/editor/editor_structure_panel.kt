package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.editor.core.editor_outline_kind
import com.jmwl.gostudio.editor.core.editor_outline_symbol
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.theme.motion

/**
 * 代码结构面板（移植自 CodeAssist StructureOutline）：
 * 当前文件的声明符号列表（函数/类型/常量/变量/字段），支持名称过滤，
 * 点击跳转光标到对应行。symbols 由 activity 侧解析（gopls 或正则兜底）后传入。
 */
@Composable
internal fun editor_structure_panel(
    file_name: String?,
    symbols: List<editor_outline_symbol>,
    on_navigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    var filter by remember(file_name) { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        Text(
            text = file_name ?: "未打开文件",
            color = colors.editor_hint,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )

        if (file_name == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("打开文件后显示代码结构", color = colors.editor_hint, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            return@Column
        }

        // 名称过滤框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(colors.editor_button_bg, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            if (filter.isEmpty()) {
                Text("过滤符号...", color = colors.editor_hint, fontSize = 14.sp)
            }
            BasicTextField(
                value = filter,
                onValueChange = { filter = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = colors.editor_text,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(colors.editor_icon),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(6.dp))

        val shown = remember(symbols, filter) {
            if (filter.isBlank()) symbols
            else symbols.filter { it.name.contains(filter, ignoreCase = true) }
        }

        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (symbols.isEmpty()) "未找到符号" else "无匹配结果",
                    color = colors.editor_hint,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(shown, key = { "${it.line}-${it.name}" }) { sym ->
                    structure_row(sym) { on_navigate(sym.line) }
                }
            }
        }
    }
}

@Composable
private fun structure_row(sym: editor_outline_symbol, on_click: () -> Unit) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = on_click)
            .padding(start = (12 + sym.depth * 16).dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        kind_badge(sym.kind)
        Text(
            text = sym.name,
            color = colors.editor_text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        if (sym.detail.isNotBlank()) {
            Text(
                text = sym.detail,
                color = colors.editor_hint,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 符号种类字母徽标（IntelliJ 式 C/I/M/F 标记）。 */
@Composable
private fun kind_badge(kind: editor_outline_kind) {
    val colors = app_theme_provider.colors
    val (letter, tint) = when (kind) {
        editor_outline_kind.FUNCTION -> "F" to colors.success
        editor_outline_kind.METHOD -> "M" to colors.success
        editor_outline_kind.STRUCT -> "S" to colors.editor_icon
        editor_outline_kind.INTERFACE -> "I" to colors.editor_icon
        editor_outline_kind.TYPE_ALIAS -> "T" to colors.editor_icon
        editor_outline_kind.CONST -> "C" to colors.warning
        editor_outline_kind.VAR -> "V" to colors.editor_hint
        editor_outline_kind.FIELD -> "f" to colors.editor_hint
    }
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(tint.copy(alpha = 0.16f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
