package com.jmwl.gostudio.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.components.sub_page_top_bar
import com.jmwl.gostudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun main_theme_settings_screen(
    current_theme: app_theme_type,
    current_preset: app_theme_preset,
    custom_accent: Int,
    scale_value: Float,
    on_theme_change: (app_theme_type) -> Unit,
    on_preset_change: (app_theme_preset) -> Unit,
    on_custom_accent_change: (Int) -> Unit,
    on_scale_change: (Float) -> Unit,
    on_back: () -> Unit
) {
    val colors = app_theme_provider.colors
    var expanded by remember { mutableStateOf(false) }
    var preset_expanded by remember { mutableStateOf(false) }
    var custom_color_text by remember(custom_accent) {
        mutableStateOf(format_app_theme_color(custom_accent))
    }
    val parsed_custom_color = parse_app_theme_color(custom_color_text)
    val custom_is_current = current_preset == app_theme_preset.CUSTOM

    val theme_icon = when (current_theme) {
        app_theme_type.LIGHT -> Icons.Default.WbSunny
        app_theme_type.DARK -> Icons.Default.NightsStay
        app_theme_type.SYSTEM -> Icons.Default.Smartphone
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = { sub_page_top_bar("主题设置", on_back) }
    ) { padding_values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding_values)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                section_label("外观", colors.title_highlight)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card_bg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        val interaction_source = remember { MutableInteractionSource() }
                        val is_pressed by interaction_source.collectIsPressedAsState()
                        val header_bg = if (is_pressed) colors.card_pressed else colors.card_bg

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(header_bg)
                                .clickable(
                                    interactionSource = interaction_source,
                                    indication = ripple(bounded = true)
                                ) { expanded = !expanded }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    theme_icon,
                                    contentDescription = "主题模式",
                                    tint = colors.card_icon_bg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "主题模式",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.card_text_title
                                )
                                Text(
                                    text = when (current_theme) {
                                        app_theme_type.DARK -> "深色模式"
                                        app_theme_type.LIGHT -> "浅色模式"
                                        app_theme_type.SYSTEM -> "跟随系统"
                    },
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    color = colors.card_text_subtitle
                                )
                            }

                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "收起" else "展开",
                                tint = colors.card_chevron,
                                modifier = Modifier.size(20.dp).rotate(if (expanded) 180f else 0f)
                            )
                        }

                        if (expanded) {
                            Column {
                                theme_option_item("浅色模式", current_theme == app_theme_type.LIGHT, colors) {
                                    on_theme_change(app_theme_type.LIGHT)
                                    expanded = false
                                }
                                theme_option_item("深色模式", current_theme == app_theme_type.DARK, colors) {
                                    on_theme_change(app_theme_type.DARK)
                                    expanded = false
                                }
                                theme_option_item("跟随系统", current_theme == app_theme_type.SYSTEM, colors) {
                                    on_theme_change(app_theme_type.SYSTEM)
                                    expanded = false
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card_bg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        val preset_interaction = remember { MutableInteractionSource() }
                        val preset_pressed by preset_interaction.collectIsPressedAsState()
                        val preset_header_bg = if (preset_pressed) colors.card_pressed else colors.card_bg

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(preset_header_bg)
                                .clickable(
                                    interactionSource = preset_interaction,
                                    indication = ripple(bounded = true)
                                ) { preset_expanded = !preset_expanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "主题预设",
                                    tint = colors.card_icon_bg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "主题预设",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.card_text_title
                                )
                                Text(
                                    text = if (custom_is_current) {
                                        "自定义"
                                    } else {
                                        current_preset.display_name
                                    },
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    color = colors.card_text_subtitle
                                )
                            }

                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = if (preset_expanded) "收起主题预设" else "展开主题预设",
                                tint = colors.card_chevron,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(if (preset_expanded) 180f else 0f)
                            )
                        }

                        if (preset_expanded) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                app_theme_preset.values()
                                    .filter { it != app_theme_preset.CUSTOM }
                                    .chunked(2)
                                    .forEach { row_items ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            row_items.forEach { preset ->
                                                theme_preset_card(
                                                    preset = preset,
                                                    is_selected = current_preset == preset,
                                                    colors = colors,
                                                    modifier = Modifier.weight(1f)
                                                ) { on_preset_change(preset) }
                                            }
                                            if (row_items.size == 1) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }

                                Text(
                                    text = "点击主题立即生效；深浅模式仍按上方设置切换。",
                                    fontSize = 10.sp,
                                    color = colors.card_text_subtitle
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card_bg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(custom_accent).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ColorLens,
                                    contentDescription = "自定义颜色",
                                    tint = Color(custom_accent),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "自定义主题",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.card_text_title
                                )
                                Text(
                                    text = if (custom_is_current) "已启用自定义种子色" else "输入或选择一个种子色",
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    color = colors.card_text_subtitle
                                )
                            }
                            if (custom_is_current) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "已选中",
                                    tint = colors.title_highlight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = custom_color_text,
                                onValueChange = { custom_color_text = it.take(9) },
                                label = { Text("种子色") },
                                placeholder = { Text("#RRGGBB") },
                                singleLine = true,
                                isError = custom_color_text.isNotBlank() && parsed_custom_color == null,
                                supportingText = {
                                    Text(
                                        if (parsed_custom_color == null) "支持 #RGB、#RRGGBB、#AARRGGBB" else "将自动适配深浅模式",
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                enabled = parsed_custom_color != null,
                                onClick = { parsed_custom_color?.let { on_custom_accent_change(it.toArgb()) } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.card_icon_bg,
                                    contentColor = colors.dialog_clone_text
                                )
                            ) { Text("应用") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                0xFF7C9EFF,
                                0xFFFF6B6B,
                                0xFFFFB84D,
                                0xFF5DD39E,
                                0xFF54C7EC,
                                0xFFB983FF
                            ).forEach { color_value ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(color_value))
                                        .border(
                                            width = if (custom_is_current && custom_accent == color_value.toInt()) 2.dp else 1.dp,
                                            color = if (custom_is_current && custom_accent == color_value.toInt()) {
                                                colors.title_highlight
                                            } else {
                                                colors.card_chevron.copy(alpha = 0.45f)
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable { on_custom_accent_change(color_value.toInt()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (custom_is_current && custom_accent == color_value.toInt()) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "已选择",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                section_label("界面", colors.title_highlight)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card_bg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = "应用缩放",
                                    tint = colors.card_icon_bg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "应用缩放",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.card_text_title
                                )
                                Text(
                                    text = "当前缩放: ${String.format("%.2f", scale_value)}x",
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    color = colors.card_text_subtitle
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Slider(
                                value = scale_value,
                                onValueChange = on_scale_change,
                                valueRange = 0.50f..1.50f,
                                steps = 19,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.card_icon_bg,
                                    activeTrackColor = colors.card_icon_bg,
                                    inactiveTrackColor = colors.card_text_subtitle.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun section_label(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

@Composable
private fun theme_preset_card(
    preset: app_theme_preset,
    is_selected: Boolean,
    colors: app_colors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val selected_background = lerp(colors.card_bg, colors.card_icon_bg, 0.16f)
    val background = when {
        is_pressed && is_selected -> lerp(selected_background, colors.card_pressed, 0.65f)
        is_pressed -> colors.card_pressed
        is_selected -> selected_background
        else -> colors.card_bg
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction_source,
                indication = ripple(bounded = true)
            ) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(preset.dark_accent),
                contentAlignment = Alignment.Center
            ) {
                if (is_selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = Color(0xFF15161A),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = preset.display_name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (is_selected) colors.title_highlight else colors.card_text_title
                )
                Text(
                    text = "点击切换",
                    fontSize = 9.sp,
                    color = colors.card_text_subtitle
                )
            }
        }
    }
}

@Composable
private fun theme_option_item(
    title: String,
    is_selected: Boolean,
    colors: app_colors,
    onClick: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (is_pressed) colors.card_pressed else colors.card_bg)
            .clickable(
                interactionSource = interaction_source,
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(44.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            color = if (is_selected) colors.title_highlight else colors.card_text_title,
            modifier = Modifier.weight(1f)
        )
        if (is_selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选中",
                tint = colors.title_highlight,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
