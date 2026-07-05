package com.xc.code.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.project.project_ide_config
import com.xc.code.project.project_manager
import com.xc.code.toolchain.toolchain_manager
import com.xc.code.ui.theme.app_theme_provider

@Composable
fun editor_project_config_panel(
    project_root_path: String,
    on_apply: (project_ide_config, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val ndk_options = remember { toolchain_manager.available_ndk_versions() }
    val cmake_options = remember { toolchain_manager.available_cmake_versions() }
    val abi_options = listOf("x86_64", "arm64-v8a", "x86", "armeabi-v7a")
    val common_platform_options = listOf("android-21", "android-23", "android-24", "android-26", "android-28", "android-30", "android-33", "android-35")
    val cpp_standard_options = listOf("11", "14", "17", "20", "23", "26")
    val build_type_options = listOf("Debug", "Release")

    var saved_config by remember(project_root_path) { mutableStateOf(project_manager.read_project_ide_config(project_root_path)) }
    var config by remember(project_root_path) { mutableStateOf(saved_config) }
    val has_changes = config != saved_config
    var expanded_ndk by remember { mutableStateOf(false) }
    var expanded_cmake by remember { mutableStateOf(false) }
    var expanded_abi by remember { mutableStateOf(false) }
    var expanded_platform by remember { mutableStateOf(false) }
    var expanded_cpp by remember { mutableStateOf(false) }
    var expanded_build_type by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "项目配置",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colors.title_highlight,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        project_config_group_title("工具链")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            project_config_option_card(
                icon = Icons.Default.Android,
                title = "NDK",
                value = config.ndk_version.ifBlank { "未配置" },
                expanded = expanded_ndk,
                options = ndk_options,
                shape = project_config_item_shape(is_top = true, is_bottom = false),
                on_expanded_change = { expanded_ndk = it },
                on_select = { config = config.copy(ndk_version = it) }
            )
            project_config_option_card(
                icon = Icons.Default.Build,
                title = "CMake",
                value = config.cmake_version.ifBlank { "未配置" },
                expanded = expanded_cmake,
                options = cmake_options,
                shape = project_config_item_shape(is_top = false, is_bottom = true),
                on_expanded_change = { expanded_cmake = it },
                on_select = { config = config.copy(cmake_version = it) }
            )
        }

        project_config_group_title("构建")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            project_config_option_card(
                icon = Icons.Default.Android,
                title = "构建 ABI",
                value = config.build.abi,
                expanded = expanded_abi,
                options = abi_options,
                shape = project_config_item_shape(is_top = true, is_bottom = false),
                on_expanded_change = { expanded_abi = it },
                on_select = { config = config.copy(build = config.build.copy(abi = it)) }
            )
            project_config_option_card(
                icon = Icons.Default.Tune,
                title = "Platform",
                value = config.build.platform,
                expanded = expanded_platform,
                options = common_platform_options,
                shape = project_config_item_shape(is_top = false, is_bottom = false),
                on_expanded_change = { expanded_platform = it },
                on_select = { config = config.copy(build = config.build.copy(platform = it)) }
            )
            project_config_option_card(
                icon = Icons.Default.Code,
                title = "C++ 标准",
                value = "C++${config.build.cpp_standard}",
                expanded = expanded_cpp,
                options = cpp_standard_options.map { "C++$it" },
                shape = project_config_item_shape(is_top = false, is_bottom = false),
                on_expanded_change = { expanded_cpp = it },
                on_select = { config = config.copy(build = config.build.copy(cpp_standard = it.removePrefix("C++"))) }
            )
            project_config_option_card(
                icon = Icons.Default.Build,
                title = "构建类型",
                value = config.build.build_type,
                expanded = expanded_build_type,
                options = build_type_options,
                shape = project_config_item_shape(is_top = false, is_bottom = false),
                on_expanded_change = { expanded_build_type = it },
                on_select = { config = config.copy(build = config.build.copy(build_type = it)) }
            )
            project_config_parallel_jobs_card(
                value = config.build.parallel_jobs,
                on_change = { jobs -> config = config.copy(build = config.build.copy(parallel_jobs = jobs)) }
            )
        }

        project_config_group_title("CMake 参数")
        project_config_extra_cmake_args_card(
            value = config.build.extra_cmake_args,
            on_change = { args -> config = config.copy(build = config.build.copy(extra_cmake_args = args)) }
        )

        Spacer(modifier = Modifier.height(6.dp))

        project_config_apply_card(
            enabled = has_changes,
            on_click = { on_apply(config) { saved_config = config } }
        )
    }
}

@Composable
private fun project_config_group_title(title: String) {
    val colors = app_theme_provider.colors
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = colors.title_highlight,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun project_config_option_card(
    icon: ImageVector,
    title: String,
    value: String,
    expanded: Boolean,
    options: List<String>,
    shape: RoundedCornerShape,
    on_expanded_change: (Boolean) -> Unit,
    on_select: (String) -> Unit
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
                project_config_icon(icon)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.card_text_title)
                    Text(value, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Light, color = colors.card_text_subtitle)
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
                    project_config_option_row(
                        title = option,
                        selected = option == value,
                        on_click = {
                            on_select(option)
                            on_expanded_change(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun project_config_option_row(
    title: String,
    selected: Boolean,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = on_click)
            .padding(start = 46.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = if (selected) colors.card_icon_bg else colors.card_text_title,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.card_icon_bg, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun project_config_parallel_jobs_card(
    value: Int,
    on_change: (Int) -> Unit
) {
    val colors = app_theme_provider.colors
    val label = if (value <= 0) "自动" else value.toString()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = project_config_item_shape(is_top = false, is_bottom = false),
        colors = CardDefaults.cardColors(containerColor = colors.card_bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                project_config_icon(Icons.Default.Tune)
                Column(modifier = Modifier.weight(1f)) {
                    Text("并行编译", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.card_text_title)
                    Text(label, fontSize = 10.sp, lineHeight = 12.sp, color = colors.card_text_subtitle)
                }
            }
            Slider(
                value = value.coerceIn(0, 8).toFloat(),
                onValueChange = { on_change(it.roundToInt().coerceIn(0, 8)) },
                valueRange = 0f..8f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = colors.title_highlight,
                    activeTrackColor = colors.title_highlight,
                    inactiveTrackColor = colors.top_button_bg,
                    activeTickColor = colors.card_bg,
                    inactiveTickColor = colors.card_text_subtitle.copy(alpha = 0.35f)
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun project_config_extra_cmake_args_card(
    value: String,
    on_change: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    var input by remember(value) { mutableStateOf("") }
    val args = remember(value) { split_cmake_args(value) }

    fun update_args(next_args: List<cmake_arg_chip>) {
        on_change(next_args.joinToString(" ") { if (it.enabled) it.text else "#${it.text}" })
    }

    fun commit_input() {
        val next = input.trim().removePrefix("#")
        if (next.isBlank()) return
        update_args(args + cmake_arg_chip(next, enabled = true))
        input = ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card_bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                project_config_icon(Icons.Default.Code)
                Column(modifier = Modifier.weight(1f)) {
                    Text("额外 CMake 参数", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.card_text_title)
                    Text("追加到 CMake 初始化命令", fontSize = 10.sp, lineHeight = 12.sp, color = colors.card_text_subtitle)
                }
            }

            if (args.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    args.forEachIndexed { index, arg ->
                        project_config_arg_chip(
                            text = arg.text,
                            enabled = arg.enabled,
                            on_toggle = {
                                update_args(args.mapIndexed { item_index, item ->
                                    if (item_index == index) item.copy(enabled = !item.enabled) else item
                                })
                            },
                            on_remove = { update_args(args.filterIndexed { item_index, _ -> item_index != index }) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = colors.top_button_bg,
                    border = BorderStroke(0.6.dp, colors.card_text_subtitle.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (input.isEmpty()) {
                            Text(
                                text = "-DXXX=ON",
                                color = colors.card_text_subtitle.copy(alpha = 0.62f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            singleLine = true,
                            textStyle = TextStyle(color = colors.card_text_title, fontSize = 10.sp),
                            cursorBrush = SolidColor(colors.title_highlight),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = if (input.isNotBlank()) colors.search_button_bg_active else colors.card_text_subtitle.copy(alpha = 0.12f),
                    onClick = { if (input.isNotBlank()) commit_input() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "添加",
                            tint = if (input.isNotBlank()) colors.title_highlight else colors.card_text_subtitle.copy(alpha = 0.45f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun project_config_arg_chip(
    text: String,
    enabled: Boolean,
    on_toggle: () -> Unit,
    on_remove: () -> Unit
) {
    val colors = app_theme_provider.colors
    val background = if (enabled) colors.search_button_bg_active else colors.card_text_subtitle.copy(alpha = 0.12f)
    val content_color = if (enabled) colors.title_highlight else colors.card_text_subtitle.copy(alpha = 0.68f)
    Box(
        modifier = Modifier.padding(top = 2.dp, end = 2.dp)
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = on_toggle),
            shape = RoundedCornerShape(18.dp),
            color = background,
            tonalElevation = 0.dp
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                color = content_color,
                modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 5.dp, bottom = 5.dp)
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .size(14.dp),
            shape = RoundedCornerShape(7.dp),
            color = colors.card_bg,
            onClick = on_remove
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = "移除", tint = colors.card_text_subtitle, modifier = Modifier.size(9.dp))
            }
        }
    }
}

private data class cmake_arg_chip(
    val text: String,
    val enabled: Boolean
)

private fun split_cmake_args(value: String): List<cmake_arg_chip> {
    return value.split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { raw ->
            val enabled = !raw.startsWith("#")
            cmake_arg_chip(text = raw.removePrefix("#"), enabled = enabled)
        }
}

@Composable
private fun project_config_apply_card(
    enabled: Boolean,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    val content_color = if (enabled) colors.card_text_title else colors.card_text_subtitle.copy(alpha = 0.55f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card_bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = on_click)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            project_config_icon(Icons.Default.Check)
            Text(
                text = if (enabled) "应用项目配置" else "项目配置无变化",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = content_color,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun project_config_icon(icon: ImageVector) {
    val colors = app_theme_provider.colors
    Surface(
        modifier = Modifier.size(28.dp),
        shape = RoundedCornerShape(8.dp),
        color = colors.card_icon_bg.copy(alpha = 0.14f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.editor_icon, modifier = Modifier.size(16.dp))
        }
    }
}

private fun project_config_item_shape(
    is_top: Boolean,
    is_bottom: Boolean
): RoundedCornerShape {
    return when {
        is_top && is_bottom -> RoundedCornerShape(12.dp)
        is_top -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        is_bottom -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> RoundedCornerShape(0.dp)
    }
}
