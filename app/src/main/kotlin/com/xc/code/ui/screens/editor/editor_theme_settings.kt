package com.xc.code.ui.screens.editor

import com.xc.code.ui.toast.app_toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.editor.theme.editor_theme_color_item
import com.xc.code.editor.theme.editor_theme_token_color_item
import com.xc.code.editor.theme.editor_theme_preview_palette
import com.xc.code.editor.theme.editor_theme_manager
import com.xc.code.ui.dialogs.editor.editor_theme_color_dialog
import com.xc.code.ui.dialogs.editor.editor_theme_reset_dialog
import com.xc.code.ui.theme.app_theme_provider

@Composable
fun editor_theme_settings_screen(
    on_back: () -> Unit
) {
    val colors = app_theme_provider.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { padding_values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding_values)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(35.dp),
                    shape = CircleShape,
                    color = colors.top_button_bg,
                    onClick = on_back
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = colors.top_button_icon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(35.dp))
            }

            Spacer(modifier = Modifier.height(30.dp))

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = "编辑器主题",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "颜色设置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.subtitle
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            editor_theme_color_section(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
internal fun editor_theme_color_section(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val colors = app_theme_provider.colors
    var color_items by remember { mutableStateOf(editor_theme_manager.load_color_items(context)) }
    var token_color_items by remember { mutableStateOf(editor_theme_manager.load_token_color_items(context)) }
    var preview_palette by remember { mutableStateOf(editor_theme_manager.load_preview_palette(context)) }
    var editing_color_item by remember { mutableStateOf<editor_theme_color_item?>(null) }
    var editing_token_item by remember { mutableStateOf<editor_theme_token_color_item?>(null) }
    var reset_requested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        editor_theme_manager.version.collect {
            color_items = editor_theme_manager.load_color_items(context)
            token_color_items = editor_theme_manager.load_token_color_items(context)
            preview_palette = editor_theme_manager.load_preview_palette(context)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = if (compact) 8.dp else 0.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "编辑器颜色",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.title_highlight,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = { reset_requested = true }
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "恢复默认颜色",
                    tint = colors.card_text_subtitle,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "恢复默认",
                    fontSize = 10.sp,
                    color = colors.card_text_subtitle
                )
            }
        }

        editor_theme_preview_card(preview_palette = preview_palette)

        Spacer(modifier = Modifier.height(8.dp))

        editor_theme_subtitle("编辑器界面颜色")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            color_items.forEachIndexed { index, item ->
                editor_theme_color_card(
                    title = item.title,
                    description = item.description,
                    value = item.value,
                    shape = editor_theme_group_shape(
                        is_top = index == 0,
                        is_bottom = index == color_items.lastIndex
                    ),
                    on_click = { editing_color_item = item }
                )
                if (index < color_items.lastIndex) {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        editor_theme_subtitle("语法高亮颜色")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            token_color_items.forEachIndexed { index, item ->
                editor_theme_color_card(
                    title = item.title,
                    description = item.scope,
                    value = item.value,
                    shape = editor_theme_group_shape(
                        is_top = index == 0,
                        is_bottom = index == token_color_items.lastIndex
                    ),
                    on_click = { editing_token_item = item }
                )
                if (index < token_color_items.lastIndex) {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }

    editing_color_item?.let { item ->
        editor_theme_color_dialog(
            title = item.title,
            key = item.key,
            value = item.value,
            on_dismiss = { editing_color_item = null },
            on_save = { value ->
                val saved = editor_theme_manager.update_color(context, item.key, value)
                if (saved) {
                    color_items = editor_theme_manager.load_color_items(context)
                    token_color_items = editor_theme_manager.load_token_color_items(context)
                    preview_palette = editor_theme_manager.load_preview_palette(context)
                    editing_color_item = null
                    app_toast.show(context, "颜色已保存", app_toast.LENGTH_SHORT)
                } else {
                    app_toast.show(context, "颜色格式错误", app_toast.LENGTH_SHORT)
                }
            }
        )
    }

    editing_token_item?.let { item ->
        editor_theme_color_dialog(
            title = item.title,
            key = item.scope,
            value = item.value,
            on_dismiss = { editing_token_item = null },
            on_save = { value ->
                val saved = editor_theme_manager.update_token_color(context, item.index, value)
                if (saved) {
                    token_color_items = editor_theme_manager.load_token_color_items(context)
                    preview_palette = editor_theme_manager.load_preview_palette(context)
                    editing_token_item = null
                    app_toast.show(context, "颜色已保存", app_toast.LENGTH_SHORT)
                } else {
                    app_toast.show(context, "颜色格式错误", app_toast.LENGTH_SHORT)
                }
            }
        )
    }

    if (reset_requested) {
        editor_theme_reset_dialog(
            on_dismiss = { reset_requested = false },
            on_confirm = {
                reset_requested = false
                val reset = editor_theme_manager.reset_to_default(context)
                if (reset) {
                    color_items = editor_theme_manager.load_color_items(context)
                    token_color_items = editor_theme_manager.load_token_color_items(context)
                    preview_palette = editor_theme_manager.load_preview_palette(context)
                    app_toast.show(context, "编辑器颜色已恢复默认", app_toast.LENGTH_SHORT)
                } else {
                    app_toast.show(context, "恢复默认失败", app_toast.LENGTH_SHORT)
                }
            }
        )
    }
}

@Composable
private fun editor_theme_preview_card(preview_palette: editor_theme_preview_palette) {
    val background = editor_theme_color(preview_palette.background)
    val foreground = editor_theme_color(preview_palette.foreground)
    val cursor = editor_theme_color(preview_palette.cursor)
    val scroll_state = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 184.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "主题预览",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = foreground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "左右滑动切换 C++ / CMake",
                    fontSize = 9.sp,
                    color = foreground.copy(alpha = 0.62f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(cursor.copy(alpha = 0.75f))
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val page_width = this.maxWidth
                Row(
                    modifier = Modifier
                        .horizontalScroll(scroll_state)
                ) {
                    editor_theme_cpp_preview_page(
                        preview_palette = preview_palette,
                        modifier = Modifier.width(page_width)
                    )
                    editor_theme_cmake_preview_page(
                        preview_palette = preview_palette,
                        modifier = Modifier.width(page_width)
                    )
                }
            }
        }
    }
}

@Composable
private fun editor_theme_cpp_preview_page(
    preview_palette: editor_theme_preview_palette,
    modifier: Modifier = Modifier
) {
    editor_theme_preview_code_page(
        title = "C++",
        preview_palette = preview_palette,
        modifier = modifier
    ) { line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor ->
        editor_theme_preview_line("1", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("#include", preview_palette.keyword)
            append_token(" <", preview_palette.punctuation)
            append_token("vector", preview_palette.type)
            append_token(">", preview_palette.punctuation)
        }
        editor_theme_preview_line("2", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("#define", preview_palette.keyword)
            append_plain(" ", preview_palette.foreground)
            append_token("MAX_COUNT", preview_palette.constant)
            append_plain(" ", preview_palette.foreground)
            append_token("128", preview_palette.number)
        }
        editor_theme_preview_line("3", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("namespace", preview_palette.keyword)
            append_plain(" ", preview_palette.foreground)
            append_token("demo", preview_palette.type)
            append_plain(" ", preview_palette.foreground)
            append_token("{", preview_palette.punctuation)
        }
        editor_theme_preview_line("4", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("class", preview_palette.keyword)
            append_plain(" ", preview_palette.foreground)
            append_token("Texture", preview_palette.type)
            append_plain(" ", preview_palette.foreground)
            append_token("{", preview_palette.punctuation)
        }
        editor_theme_preview_line("5", true, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_plain("  ", preview_palette.foreground)
            append_token("void", preview_palette.type)
            append_plain(" ", preview_palette.foreground)
            append_token("load", preview_palette.function)
            append_token("(", preview_palette.punctuation)
            append_token("const", preview_palette.keyword)
            append_plain(" ", preview_palette.foreground)
            append_token("std", preview_palette.type)
            append_token("::", preview_palette.operator)
            append_token("string", preview_palette.type)
            append_token("& path", preview_palette.foreground)
            append_token(")", preview_palette.punctuation)
        }
        editor_theme_preview_line("6", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_plain("  ", preview_palette.foreground)
            append_token("auto", preview_palette.keyword)
            append_plain(" count ", preview_palette.foreground)
            append_token("=", preview_palette.operator)
            append_plain(" ", preview_palette.foreground)
            append_token("MAX_COUNT", preview_palette.constant)
            append_plain(" ", preview_palette.foreground)
            append_token("+", preview_palette.operator)
            append_plain(" ", preview_palette.foreground)
            append_token("1", preview_palette.number)
            append_token(";", preview_palette.punctuation)
        }
        editor_theme_preview_line("7", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_plain("  ", preview_palette.foreground)
            append_token("// Load texture file", preview_palette.comment)
        }
    }
}

@Composable
private fun editor_theme_cmake_preview_page(
    preview_palette: editor_theme_preview_palette,
    modifier: Modifier = Modifier
) {
    editor_theme_preview_code_page(
        title = "CMake",
        preview_palette = preview_palette,
        modifier = modifier
    ) { line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor ->
        editor_theme_preview_line("1", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("cmake_minimum_required", preview_palette.function)
            append_token("(", preview_palette.punctuation)
            append_token("VERSION", preview_palette.keyword)
            append_plain(" ", preview_palette.foreground)
            append_token("3.15", preview_palette.number)
            append_token(")", preview_palette.punctuation)
        }
        editor_theme_preview_line("2", true, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("project", preview_palette.function)
            append_token("(", preview_palette.punctuation)
            append_token("demo", preview_palette.string)
            append_plain(" ", preview_palette.foreground)
            append_token("CXX", preview_palette.constant)
            append_token(")", preview_palette.punctuation)
        }
        editor_theme_preview_line("3", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("set", preview_palette.function)
            append_token("(", preview_palette.punctuation)
            append_token("CMAKE_CXX_STANDARD", preview_palette.constant)
            append_plain(" ", preview_palette.foreground)
            append_token("20", preview_palette.number)
            append_token(")", preview_palette.punctuation)
        }
        editor_theme_preview_line("4", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("add_library", preview_palette.function)
            append_token("(", preview_palette.punctuation)
            append_token("demo", preview_palette.string)
            append_plain(" ", preview_palette.foreground)
            append_token("SHARED", preview_palette.keyword)
            append_plain(" ", preview_palette.foreground)
            append_token("src/demo.cpp", preview_palette.string)
            append_token(")", preview_palette.punctuation)
        }
        editor_theme_preview_line("5", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("target_include_directories", preview_palette.function)
            append_token("(", preview_palette.punctuation)
            append_token("demo", preview_palette.string)
            append_plain(" ", preview_palette.foreground)
            append_token("PUBLIC", preview_palette.keyword)
            append_plain(" ", preview_palette.foreground)
            append_token("include", preview_palette.string)
            append_token(")", preview_palette.punctuation)
        }
        editor_theme_preview_line("6", false, line_number_color, active_line_number_color, line_highlight, selection, block_line, indent_line, cursor) {
            append_token("# Android CMake project", preview_palette.comment)
        }
    }
}

@Composable
private fun editor_theme_preview_code_page(
    title: String,
    preview_palette: editor_theme_preview_palette,
    modifier: Modifier = Modifier,
    content: @Composable (Color, Color, Color, Color, Color, Color, Color) -> Unit
) {
    val line_highlight = editor_theme_color(preview_palette.line_highlight)
    val selection = editor_theme_color(preview_palette.selection)
    val line_number = editor_theme_color(preview_palette.line_number)
    val active_line_number = editor_theme_color(preview_palette.active_line_number)
    val cursor = editor_theme_color(preview_palette.cursor)
    val indent_line = editor_theme_color(preview_palette.indent_line)
    val block_line = editor_theme_color(preview_palette.block_line)

    Column(
        modifier = modifier.padding(end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = editor_theme_color(preview_palette.foreground).copy(alpha = 0.72f),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        content(line_number, active_line_number, line_highlight, selection, block_line, indent_line, cursor)
    }
}

@Composable
private fun editor_theme_preview_line(
    number: String,
    active: Boolean,
    line_number_color: Color,
    active_line_number_color: Color,
    line_highlight: Color,
    selection: Color,
    block_line: Color,
    indent_line: Color,
    cursor: Color,
    content: androidx.compose.ui.text.AnnotatedString.Builder.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) line_highlight else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = if (active) active_line_number_color else line_number_color,
            modifier = Modifier.width(20.dp)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(15.dp)
                .background(if (active) block_line else indent_line)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .background(if (active) selection.copy(alpha = 0.32f) else Color.Transparent)
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = buildAnnotatedString(content),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontFamily = FontFamily.Monospace
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(width = 1.dp, height = 15.dp)
                        .background(cursor)
                )
            }
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.append_token(text: String, color: String) {
    withStyle(SpanStyle(color = editor_theme_color(color))) {
        append(text)
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.append_plain(text: String, color: String) {
    withStyle(SpanStyle(color = editor_theme_color(color))) {
        append(text)
    }
}

@Composable
private fun editor_theme_subtitle(title: String) {
    val colors = app_theme_provider.colors
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.card_text_subtitle,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

private fun editor_theme_color(value: String): Color {
    return editor_theme_manager.parse_color_argb(value)?.let { Color(it) } ?: Color.Unspecified
}

@Composable
private fun editor_theme_color_card(
    title: String,
    description: String,
    value: String,
    shape: RoundedCornerShape,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = if (is_pressed) colors.card_pressed else colors.card_bg
    val preview_color = editor_theme_manager.parse_color_argb(value)?.let { Color(it) } ?: Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = background_color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction_source,
                    indication = ripple(bounded = true),
                    onClick = on_click
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = title,
                    tint = colors.card_icon_bg,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.card_text_title
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.card_text_subtitle
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = value.ifBlank { "未设置" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.card_text_subtitle
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(preview_color)
                )
            }
        }
    }
}

private fun editor_theme_group_shape(
    is_top: Boolean,
    is_bottom: Boolean,
    radius: androidx.compose.ui.unit.Dp = 12.dp
): RoundedCornerShape {
    return when {
        is_top && is_bottom -> RoundedCornerShape(radius)
        is_top -> RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = 0.dp, bottomEnd = 0.dp)
        is_bottom -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(0.dp)
    }
}
