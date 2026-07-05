package com.xc.code.ui.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.app_theme_provider

@Composable
fun editor_search_panel(
    query: String,
    replacement: String,
    expanded: Boolean,
    match_case: Boolean,
    whole_word: Boolean,
    regex: Boolean,
    has_match: Boolean,
    replace_enabled: Boolean,
    on_query_change: (String) -> Unit,
    on_replacement_change: (String) -> Unit,
    on_expanded_change: (Boolean) -> Unit,
    on_match_case_change: (Boolean) -> Unit,
    on_whole_word_change: (Boolean) -> Unit,
    on_regex_change: (Boolean) -> Unit,
    on_previous: () -> Unit,
    on_next: () -> Unit,
    on_replace_current: () -> Unit,
    on_replace_all: () -> Unit,
    on_close: () -> Unit,
    on_drag: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val has_query = query.isNotBlank()
    val can_replace = has_match && replace_enabled
    var replace_all_mode by remember { mutableStateOf(false) }
    val query_focus_requester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        query_focus_requester.requestFocus()
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .widthIn(min = 248.dp, max = 360.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag_amount ->
                            change.consume()
                            on_drag(drag_amount)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(colors.editor_hint)
                        )
                    }
                }
            }

            search_box_row(
                value = query,
                placeholder = "输入搜索内容...",
                expanded = expanded,
                on_value_change = on_query_change,
                on_expanded_change = on_expanded_change,
                focus_requester = query_focus_requester,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                    search_option_button(
                        label = "Aa",
                        selected = match_case,
                        on_click = { on_match_case_change(!match_case) }
                    )
                    search_option_button(
                        label = "Ab",
                        selected = whole_word,
                        on_click = { on_whole_word_change(!whole_word) }
                    )
                    search_option_button(
                        label = ".*",
                        selected = regex,
                        on_click = { on_regex_change(!regex) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = on_previous, enabled = has_query, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "上一个",
                            tint = if (has_query) colors.editor_text else colors.editor_hint,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = on_next, enabled = has_query, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "下一个",
                            tint = if (has_query) colors.editor_text else colors.editor_hint,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                IconButton(onClick = on_close, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭搜索",
                        tint = colors.editor_text,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        search_text_field(
                            value = replacement,
                            placeholder = "输入替换内容...",
                            on_value_change = on_replacement_change,
                            trailing_content = {
                                search_replace_mode_button(
                                    selected = replace_all_mode,
                                    content_description = if (replace_all_mode) "当前替换全部，点击切换为单次替换" else "当前单次替换，点击切换为替换全部",
                                    on_click = { replace_all_mode = !replace_all_mode }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        search_replace_button(
                            icon = Icons.Default.FindReplace,
                            enabled = can_replace,
                            content_description = if (replace_all_mode) "执行替换全部" else "执行单次替换",
                            on_click = {
                                if (replace_all_mode) {
                                    on_replace_all()
                                } else {
                                    on_replace_current()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun search_box_row(
    value: String,
    placeholder: String,
    expanded: Boolean,
    on_value_change: (String) -> Unit,
    on_expanded_change: (Boolean) -> Unit,
    focus_requester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            IconButton(onClick = { on_expanded_change(!expanded) }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起替换" else "展开替换",
                    tint = colors.editor_hint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = colors.editor_button_bg,
                border = if (focused) BorderStroke(1.dp, colors.editor_icon) else null
            ) {
                search_plain_text_field(
                    value = value,
                    placeholder = placeholder,
                    on_value_change = on_value_change,
                    on_focus_change = { focused = it },
                    focus_requester = focus_requester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(horizontal = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun search_text_field(
    value: String,
    placeholder: String,
    on_value_change: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailing_content: (@Composable () -> Unit)? = null
) {
    val colors = app_theme_provider.colors
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = colors.editor_button_bg,
        border = if (focused) BorderStroke(1.dp, colors.editor_icon) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .padding(start = 10.dp, end = if (trailing_content == null) 10.dp else 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            search_plain_text_field(
                value = value,
                placeholder = placeholder,
                on_value_change = on_value_change,
                on_focus_change = { focused = it },
                modifier = Modifier.weight(1f)
            )
            trailing_content?.invoke()
        }
    }
}

@Composable
private fun search_plain_text_field(
    value: String,
    placeholder: String,
    on_value_change: (String) -> Unit,
    on_focus_change: (Boolean) -> Unit,
    focus_requester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    Box(
        modifier = modifier.heightIn(min = 30.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = colors.editor_hint,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicTextField(
            value = value,
            onValueChange = on_value_change,
            singleLine = true,
            textStyle = TextStyle(color = colors.editor_text, fontSize = 12.sp),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focus_requester != null) Modifier.focusRequester(focus_requester) else Modifier)
                .onFocusChanged { on_focus_change(it.isFocused) }
        )
    }
}

@Composable
private fun search_replace_mode_button(
    selected: Boolean,
    content_description: String,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = on_click
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = content_description,
            tint = if (selected) colors.editor_icon else colors.editor_hint,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun search_option_button(
    label: String,
    selected: Boolean,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Surface(
        modifier = Modifier
            .size(width = 34.dp, height = 28.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = on_click
            ),
        shape = RoundedCornerShape(7.dp),
        color = if (selected) colors.editor_sidebar_selected_bg else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) colors.editor_icon else colors.editor_hint,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun search_replace_button(
    icon: ImageVector,
    enabled: Boolean,
    content_description: String,
    on_click: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    Box(
        modifier = modifier
            .size(width = 34.dp, height = 28.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = on_click
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = content_description,
            tint = if (enabled) colors.editor_icon else colors.editor_hint,
            modifier = Modifier.size(18.dp)
        )
    }
}
