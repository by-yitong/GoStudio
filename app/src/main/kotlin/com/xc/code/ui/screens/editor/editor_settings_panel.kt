package com.xc.code.ui.screens.editor

import com.xc.code.editor.model.editor_settings_state

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.app_theme_provider
import kotlin.math.roundToInt

@Composable
fun editor_settings_panel(
    settings: editor_settings_state,
    on_settings_change: (editor_settings_state) -> Unit,
    on_import_font: () -> Unit,
    on_open_theme_settings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val switch_groups = editor_settings_switch_groups(settings)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "编辑器",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colors.title_highlight,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            editor_settings_font_card(
                selected_font = settings.font_family,
                shape = editor_settings_group_item_shape(is_top = true, is_bottom = false),
                on_font_change = { font -> on_settings_change(settings.copy(font_family = font)) },
                on_import_font = on_import_font
            )

            editor_settings_group_divider()

            editor_settings_slider_card(
                icon = Icons.Default.Settings,
                title = "字体大小",
                description = "调整代码文字大小",
                value = settings.font_size,
                value_range = 10f..24f,
                steps = 13,
                value_label = "${format_editor_font_size(settings.font_size)}sp",
                shape = editor_settings_group_item_shape(is_top = false, is_bottom = false),
                on_value_change = { value -> on_settings_change(settings.copy(font_size = value.coerceIn(10f, 24f))) }
            )

            editor_settings_group_divider()

            editor_settings_slider_card(
                icon = Icons.Default.Settings,
                title = "制表符大小",
                description = "设置 Tab 占用空格数",
                value = settings.tab_size.toFloat(),
                value_range = 2f..8f,
                steps = 5,
                value_label = settings.tab_size.toString(),
                shape = editor_settings_group_item_shape(is_top = false, is_bottom = true),
                on_value_change = { value -> on_settings_change(settings.copy(tab_size = value.roundToInt().coerceIn(2, 8))) }
            )
        }

        editor_settings_group_title("主题")
        editor_settings_navigation_card(
            icon = Icons.Default.Palette,
            title = "编辑器主题",
            description = "调整编辑器与高亮颜色",
            on_click = on_open_theme_settings
        )

        switch_groups.forEach { group ->
            editor_settings_group_title(group.title)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                group.items.forEachIndexed { index, item ->
                    editor_settings_switch_card(
                        icon = Icons.Default.Settings,
                        title = item.title,
                        description = item.description,
                        checked = item.checked,
                        shape = editor_settings_group_item_shape(
                            is_top = index == 0,
                            is_bottom = index == group.items.lastIndex
                        ),
                        on_checked_change = { enabled -> on_settings_change(item.update(enabled)) }
                    )
                    if (index < group.items.lastIndex) {
                        editor_settings_group_divider()
                    }
                }
            }
        }
    }
}


@Composable
private fun editor_settings_group_divider() {
    Spacer(modifier = Modifier.height(1.dp))
}

private fun editor_settings_group_item_shape(
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

@Composable
private fun editor_settings_group_title(title: String) {
    val colors = app_theme_provider.colors
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = colors.title_highlight,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun editor_settings_font_card(
    selected_font: String,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    on_font_change: (String) -> Unit,
    on_import_font: () -> Unit
) {
    val fonts = listOf(
        "jetbrains_mono" to "JetBrains Mono",
        "roboto" to "Roboto",
        "imported" to "导入字体"
    )
    val selected_label = fonts.firstOrNull { it.first == selected_font }?.second ?: "JetBrains Mono"
    var expanded by remember { mutableStateOf(false) }
    editor_settings_expandable_options_card(
        icon = Icons.Default.Settings,
        title = "编辑器字体",
        description = "当前字体: $selected_label",
        expanded = expanded,
        options = fonts,
        selected_value = selected_font,
        shape = shape,
        on_expanded_change = { expanded = it },
        on_value_change = on_font_change,
        on_import_font = on_import_font
    )
}

@Composable
private fun editor_settings_expandable_options_card(
    icon: ImageVector,
    title: String,
    description: String,
    expanded: Boolean,
    options: List<Pair<String, String>>,
    selected_value: String,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    on_expanded_change: (Boolean) -> Unit,
    on_value_change: (String) -> Unit,
    on_import_font: () -> Unit
) {
    val colors = app_theme_provider.colors
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = if (is_pressed) colors.card_pressed else colors.card_bg

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = background_color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interaction_source,
                        indication = ripple(bounded = true)
                    ) { on_expanded_change(!expanded) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                editor_settings_icon(icon = icon, title = title)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
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
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = colors.card_chevron,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                options.forEach { option ->
                    editor_settings_option_row(
                        title = option.second,
                        selected = option.first == selected_value,
                        icon = if (option.first == "imported") Icons.Default.UploadFile else null,
                        on_click = {
                            if (option.first == "imported") {
                                on_import_font()
                            } else {
                                on_value_change(option.first)
                                on_expanded_change(false)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun editor_settings_slider_card(
    icon: ImageVector,
    title: String,
    description: String,
    value: Float,
    value_range: ClosedFloatingPointRange<Float>,
    steps: Int,
    value_label: String,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    on_value_change: (Float) -> Unit
) {
    val colors = app_theme_provider.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = colors.card_bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                editor_settings_icon(icon = icon, title = title)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
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
                Text(
                    text = value_label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.card_text_title
                )
            }

            Slider(
                value = value,
                onValueChange = on_value_change,
                valueRange = value_range,
                steps = steps,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.card_icon_bg,
                    activeTrackColor = colors.card_icon_bg,
                    inactiveTrackColor = colors.card_text_subtitle.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun editor_settings_option_row(
    title: String,
    selected: Boolean,
    icon: ImageVector? = null,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = when {
        is_pressed -> colors.card_pressed
        else -> colors.card_bg
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background_color)
            .clickable(
                interactionSource = interaction_source,
                indication = ripple(bounded = true),
                onClick = on_click
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(38.dp))
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.card_text_subtitle,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            color = if (selected) colors.title_highlight else colors.card_text_title,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Default.Done,
                contentDescription = "已选中",
                tint = colors.title_highlight,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun editor_settings_navigation_card(
    icon: ImageVector,
    title: String,
    description: String,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = if (is_pressed) colors.card_pressed else colors.card_bg

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            editor_settings_icon(icon = icon, title = title)

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
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

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "进入",
                tint = colors.card_chevron,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun editor_settings_switch_card(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    on_checked_change: (Boolean) -> Unit
) {
    val colors = app_theme_provider.colors
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background_color = if (is_pressed) colors.card_pressed else colors.card_bg

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
                    indication = ripple(bounded = true)
                ) { on_checked_change(!checked) }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            editor_settings_icon(icon = icon, title = title)

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
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

            editor_settings_small_switch(checked = checked)
        }
    }
}

@Composable
private fun editor_settings_icon(icon: ImageVector, title: String) {
    val colors = app_theme_provider.colors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(colors.card_icon_bg.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = colors.card_icon_bg,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun editor_settings_small_switch(checked: Boolean) {
    val colors = app_theme_provider.colors
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (checked) colors.card_icon_bg.copy(alpha = 0.36f)
                else colors.card_text_subtitle.copy(alpha = 0.18f)
            )
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(14.dp)
                .clip(CircleShape)
                .background(if (checked) colors.card_icon_bg else colors.card_text_subtitle)
        )
    }
}

private fun format_editor_font_size(value: Float): String {
    val int_value = value.toInt()
    return if (value == int_value.toFloat()) int_value.toString() else String.format("%.1f", value)
}
