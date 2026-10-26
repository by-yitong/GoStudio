package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.editor.model.default_symbol_bar_snippets
import com.jmwl.gostudio.editor.model.default_symbol_bar_symbols
import com.jmwl.gostudio.editor.model.editor_settings_state
import com.jmwl.gostudio.editor.settings.load_editor_settings
import com.jmwl.gostudio.editor.settings.save_editor_settings
import com.jmwl.gostudio.ui.components.sub_page_top_bar
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.toast.app_toast

/** 符号最长 8 字符、片段显示名最长 10 字符，防止撑爆符号栏 */
private const val max_symbol_length = 8
private const val max_snippet_label_length = 10

/**
 * 编辑器设置 → 符号栏子页：
 * 第一行「符号」与第二行「快捷片段」的增/删/排序，即改即存，
 * 保存后回到编辑器由 onResume 重载设置自动生效。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editor_symbol_settings_screen(on_back: () -> Unit) {
    val context = LocalContext.current
    val colors = app_theme_provider.colors
    var settings by remember { mutableStateOf(load_editor_settings(context)) }

    fun update_settings(next_settings: editor_settings_state) {
        settings = next_settings
        save_editor_settings(context, next_settings)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = { sub_page_top_bar("符号栏设置", on_back) }
    ) { padding_values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding_values)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── 分组一：第一行 · 符号 ──
            symbol_group_header(
                title = "第一行 · 符号",
                on_reset = {
                    update_settings(settings.copy(symbol_bar_symbols = default_symbol_bar_symbols))
                }
            )
            var new_symbol by remember { mutableStateOf("") }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card_bg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (settings.symbol_bar_symbols.isEmpty()) {
                        Text(
                            text = "暂无符号，删除后可在下方重新添加",
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = colors.card_text_subtitle,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    settings.symbol_bar_symbols.forEachIndexed { index, symbol ->
                        editor_symbol_setting_row(
                            symbol = symbol,
                            can_move_up = index > 0,
                            can_move_down = index < settings.symbol_bar_symbols.lastIndex,
                            on_move_up = {
                                update_settings(
                                    settings.copy(
                                        symbol_bar_symbols = moved(settings.symbol_bar_symbols, index, index - 1)
                                    )
                                )
                            },
                            on_move_down = {
                                update_settings(
                                    settings.copy(
                                        symbol_bar_symbols = moved(settings.symbol_bar_symbols, index, index + 1)
                                    )
                                )
                            },
                            on_delete = {
                                update_settings(
                                    settings.copy(
                                        symbol_bar_symbols = settings.symbol_bar_symbols.filterIndexed { i, _ -> i != index }
                                    )
                                )
                            }
                        )
                    }
                    editor_setting_add_row(
                        value = new_symbol,
                        on_value_change = { new_symbol = it },
                        placeholder = "新符号，如 %%",
                        on_add = {
                            val trimmed = new_symbol.trim()
                            when {
                                trimmed.isEmpty() -> app_toast.show(context, "符号不能为空", app_toast.LENGTH_SHORT)
                                trimmed.length > max_symbol_length ->
                                    app_toast.show(context, "符号最多 $max_symbol_length 个字符", app_toast.LENGTH_SHORT)
                                else -> {
                                    update_settings(settings.copy(symbol_bar_symbols = settings.symbol_bar_symbols + trimmed))
                                    new_symbol = ""
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── 分组二：第二行 · 快捷片段 ──
            symbol_group_header(
                title = "第二行 · 快捷片段",
                on_reset = {
                    update_settings(settings.copy(symbol_bar_snippets = default_symbol_bar_snippets))
                }
            )
            var new_label by remember { mutableStateOf("") }
            var new_code by remember { mutableStateOf("") }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card_bg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (settings.symbol_bar_snippets.isEmpty()) {
                        Text(
                            text = "暂无片段，展开符号栏第二行将只剩方向键",
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            color = colors.card_text_subtitle,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    settings.symbol_bar_snippets.forEachIndexed { index, (label, code) ->
                        editor_snippet_setting_row(
                            label = label,
                            code = code,
                            can_move_up = index > 0,
                            can_move_down = index < settings.symbol_bar_snippets.lastIndex,
                            on_move_up = {
                                update_settings(
                                    settings.copy(
                                        symbol_bar_snippets = moved(settings.symbol_bar_snippets, index, index - 1)
                                    )
                                )
                            },
                            on_move_down = {
                                update_settings(
                                    settings.copy(
                                        symbol_bar_snippets = moved(settings.symbol_bar_snippets, index, index + 1)
                                    )
                                )
                            },
                            on_delete = {
                                update_settings(
                                    settings.copy(
                                        symbol_bar_snippets = settings.symbol_bar_snippets.filterIndexed { i, _ -> i != index }
                                    )
                                )
                            }
                        )
                    }
                    editor_snippet_add_area(
                        label = new_label,
                        on_label_change = { new_label = it },
                        code = new_code,
                        on_code_change = { new_code = it },
                        on_add = {
                            val trimmed_label = new_label.trim()
                            when {
                                trimmed_label.isEmpty() || new_code.isEmpty() ->
                                    app_toast.show(context, "显示名与插入代码不能为空", app_toast.LENGTH_SHORT)
                                trimmed_label.length > max_snippet_label_length ->
                                    app_toast.show(context, "显示名最多 $max_snippet_label_length 个字符", app_toast.LENGTH_SHORT)
                                else -> {
                                    // 代码中的换行原样保留（trimEnd 只去掉末尾多余空白行）
                                    update_settings(
                                        settings.copy(
                                            symbol_bar_snippets = settings.symbol_bar_snippets +
                                                (trimmed_label to new_code.trimEnd())
                                        )
                                    )
                                    new_label = ""
                                    new_code = ""
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/** 分组标题行：左侧小标题 + 右侧「恢复默认」文字按钮。 */
@Composable
private fun symbol_group_header(title: String, on_reset: () -> Unit) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colors.title_highlight,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = on_reset,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(text = "恢复默认", fontSize = 10.sp, color = colors.card_text_subtitle)
        }
    }
}

/** 符号条目行：等宽预览 + 上移/下移/删除。 */
@Composable
private fun editor_symbol_setting_row(
    symbol: String,
    can_move_up: Boolean,
    can_move_down: Boolean,
    on_move_up: () -> Unit,
    on_move_down: () -> Unit,
    on_delete: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.card_text_title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        editor_order_buttons(
            can_move_up = can_move_up,
            can_move_down = can_move_down,
            on_move_up = on_move_up,
            on_move_down = on_move_down,
            on_delete = on_delete
        )
    }
    editor_setting_row_divider()
}

/** 快捷片段条目行：两行式 label + 代码首行预览（ellipsis）+ 上移/下移/删除。 */
@Composable
private fun editor_snippet_setting_row(
    label: String,
    code: String,
    can_move_up: Boolean,
    can_move_down: Boolean,
    on_move_up: () -> Unit,
    on_move_down: () -> Unit,
    on_delete: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.card_text_title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = code.substringBefore('\n'),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                color = colors.card_text_subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        editor_order_buttons(
            can_move_up = can_move_up,
            can_move_down = can_move_down,
            on_move_up = on_move_up,
            on_move_down = on_move_down,
            on_delete = on_delete
        )
    }
    editor_setting_row_divider()
}

/** 上移/下移/删除三个紧凑图标按钮（首个禁用↑、末个禁用↓）。 */
@Composable
private fun editor_order_buttons(
    can_move_up: Boolean,
    can_move_down: Boolean,
    on_move_up: () -> Unit,
    on_move_down: () -> Unit,
    on_delete: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = on_move_up, enabled = can_move_up, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "上移",
                tint = if (can_move_up) colors.card_text_title else colors.card_text_subtitle.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = on_move_down, enabled = can_move_down, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "下移",
                tint = if (can_move_down) colors.card_text_title else colors.card_text_subtitle.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = on_delete, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "删除",
                tint = colors.card_text_subtitle,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 条目行之间的细分隔线。 */
@Composable
private fun editor_setting_row_divider() {
    val colors = app_theme_provider.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.card_text_subtitle.copy(alpha = 0.12f))
    )
}

/** 符号添加行：单行输入 + 「添加」按钮。 */
@Composable
private fun editor_setting_add_row(
    value: String,
    on_value_change: (String) -> Unit,
    placeholder: String,
    on_add: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = on_value_change,
            modifier = Modifier.weight(1f),
            placeholder = { Text(text = placeholder, fontSize = 12.sp) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 12.sp, color = colors.card_text_title)
        )
        TextButton(onClick = on_add) {
            Text(text = "添加", fontSize = 12.sp)
        }
    }
}

/** 快捷片段添加区：显示名单行输入 + 插入代码多行输入 + 「添加」按钮。 */
@Composable
private fun editor_snippet_add_area(
    label: String,
    on_label_change: (String) -> Unit,
    code: String,
    on_code_change: (String) -> Unit,
    on_add: () -> Unit
) {
    val colors = app_theme_provider.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = on_label_change,
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "显示名，如 sw", fontSize = 12.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = colors.card_text_title)
            )
            TextButton(onClick = on_add) {
                Text(text = "添加", fontSize = 12.sp)
            }
        }
        OutlinedTextField(
            value = code,
            onValueChange = on_code_change,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "插入代码，支持换行", fontSize = 12.sp) },
            minLines = 2,
            textStyle = TextStyle(fontSize = 12.sp, color = colors.card_text_title)
        )
    }
}

/** 把列表 from 位置的元素移动到 to 位置（上移/下移排序用）。 */
private fun <T> moved(list: List<T>, from: Int, to: Int): List<T> {
    val next = list.toMutableList()
    val item = next.removeAt(from)
    next.add(to, item)
    return next
}
