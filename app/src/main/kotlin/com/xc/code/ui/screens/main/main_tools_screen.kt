package com.xc.code.ui.screens.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.app_colors
import com.xc.code.ui.theme.app_theme_provider

private data class main_tool_download_item(
    val version: String,
    val install_key: String,
    val url: String,
    val sha256: String = ""
)

private data class main_basic_tool_item(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private enum class main_tools_page {
    CMAKE,
    NDK
}

data class main_tools_install_status(
    val cmake_installed: Boolean = false,
    val ndk_installed: Boolean = false,
    val installed_cmake_versions: Set<String> = emptySet(),
    val installed_ndk_versions: Set<String> = emptySet()
)

private val basic_tool_items = listOf(
    main_basic_tool_item(
        id = "cmake",
        title = "CMake",
        description = "检查 CMake/Ninja 是否可用，用于初始化构建目录",
        icon = Icons.Default.Build
    )
)

private val cmake_download_items = listOf(
    main_tool_download_item(
        version = "CMake 4.3.0",
        install_key = "4.3.0",
        url = "https://github.com/Kitware/CMake/releases/download/v4.3.0/cmake-4.3.0-linux-aarch64.tar.gz",
        sha256 = "26fe3011f497eb9398115dcabcc094685e634b1841f7c01dc01c5a89b8b0ea0d"
    ),
    main_tool_download_item(
        version = "CMake 4.2.1",
        install_key = "4.2.1",
        url = "https://github.com/Kitware/CMake/releases/download/v4.2.1/cmake-4.2.1-linux-aarch64.tar.gz",
        sha256 = "3e178207a2c42af4cd4883127f8800b6faf99f3f5187dccc68bfb2cc7808f5f7"
    ),
    main_tool_download_item(
        version = "CMake 3.31.10",
        install_key = "3.31.10",
        url = "https://github.com/Kitware/CMake/releases/download/v3.31.10/cmake-3.31.10-linux-aarch64.tar.gz",
        sha256 = "a343c6294f770742904e6a6792e0956b5ff8212abfb63cac99237de2e210fa0f"
    )
)

private fun cmake_tool_items(installed_versions: Set<String>): List<main_tool_download_item> {
    val download_keys = cmake_download_items.map { it.install_key }.toSet()
    val installed_items = installed_versions
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() && it !in download_keys }
        .distinct()
        .sortedDescending()
        .map { version ->
            main_tool_download_item(
                version = "CMake $version",
                install_key = version,
                url = ""
            )
        }
        .toList()
    return cmake_download_items + installed_items
}

private val ndk_download_items = listOf(
    main_tool_download_item(
        version = "Android NDK R29",
        install_key = "android-ndk-r29",
        url = "https://github.com/SodaMilk233/OLLVM_NDK/releases/download/android-ndk-r29/android-ndk-r29.tar.xz",
        sha256 = "bd59e5b7895a069a13d77f19b024c9546c53e834a51d161c35e369a7c477ba1a"
    )
)

@Composable
fun main_tools_screen(
    on_back: () -> Unit,
    on_install_cmake: (String, String, String) -> Unit = { _, _, _ -> },
    on_custom_install_cmake: () -> Unit = {},
    on_uninstall_cmake: (String) -> Unit = {},
    on_install_ndk: (String, String, String) -> Unit = { _, _, _ -> },
    on_custom_install_ndk: () -> Unit = {},
    on_uninstall_ndk: (String) -> Unit = {},
    install_status: main_tools_install_status = main_tools_install_status()
) {
    val colors = app_theme_provider.colors
    var selected_page by remember { mutableStateOf(main_tools_page.CMAKE) }
    var selected_cmake by remember { mutableStateOf(cmake_download_items.first()) }
    var selected_ndk by remember { mutableStateOf(ndk_download_items.first()) }
    val cmake_items = remember(install_status.installed_cmake_versions) {
        cmake_tool_items(install_status.installed_cmake_versions)
    }

    LaunchedEffect(cmake_items) {
        if (cmake_items.none { it.install_key == selected_cmake.install_key }) {
            selected_cmake = cmake_items.first()
        }
    }

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
                    text = "开发工具",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_large
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CMake 构建组件",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "管理 CMake 和 Android NDK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.subtitle
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                main_tools_page_switch(
                    selected_page = selected_page,
                    on_select = { page -> selected_page = page }
                )

                when (selected_page) {
                    main_tools_page.CMAKE -> {
                        main_tools_install_card(
                            icon = Icons.Default.Build,
                            title = "CMake",
                            description = "用于生成 Ninja 构建目录和 compile_commands.json",
                            selected_item = selected_cmake,
                            items = cmake_items,
                            installed = selected_cmake.install_key in install_status.installed_cmake_versions,
                            on_select = { selected_cmake = it },
                            on_install = { on_install_cmake(selected_cmake.install_key, selected_cmake.url, selected_cmake.sha256) },
                            on_custom_install = on_custom_install_cmake,
                            on_uninstall = { on_uninstall_cmake(selected_cmake.install_key) }
                        )

                        main_tools_info_card(
                            icon = Icons.Default.Settings,
                            title = "CMake 环境",
                            lines = listOf(
                                "安装到 /home/xcode/cmake/<版本>",
                                "Ninja 从 APK 内置 libninja_exec.so 复制到 CMake bin"
                            )
                        )
                    }

                    main_tools_page.NDK -> {
                        main_tools_install_card(
                            icon = Icons.Default.Android,
                            title = "Android NDK",
                            description = "用于 Android ABI 交叉编译和工具链配置",
                            selected_item = selected_ndk,
                            items = ndk_download_items,
                            installed = selected_ndk.install_key in install_status.installed_ndk_versions,
                            on_select = { selected_ndk = it },
                            on_install = { on_install_ndk(selected_ndk.install_key, selected_ndk.url, selected_ndk.sha256) },
                            on_custom_install = on_custom_install_ndk,
                            on_uninstall = { on_uninstall_ndk(selected_ndk.install_key) }
                        )

                        main_tools_info_card(
                            icon = Icons.Default.Settings,
                            title = "NDK 环境",
                            lines = listOf(
                                "使用 Ubuntu 环境 wget 下载并解压安装",
                                "缓存位于 /home/xcode/cache，安装到 /home/xcode/ndk"
                            )
                        )
                    }

                }
            }
        }
    }
}

@Composable
private fun main_tools_page_switch(
    selected_page: main_tools_page,
    on_select: (main_tools_page) -> Unit
) {
    val colors = app_theme_provider.colors
    val pages = listOf(
        main_tools_page.CMAKE to Pair("CMake", Icons.Default.Build),
        main_tools_page.NDK to Pair("NDK", Icons.Default.Android)
    )
    val selected_index = pages.indexOfFirst { item -> item.first == selected_page }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.top_button_bg)
            .padding(4.dp)
    ) {
        val item_width = maxWidth / pages.size
        val indicator_offset by animateDpAsState(
            targetValue = item_width * selected_index,
            animationSpec = tween(durationMillis = 220),
            label = "main_tools_page_indicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicator_offset)
                .width(item_width)
                .fillMaxHeight()
                .clip(RoundedCornerShape(9.dp))
                .background(colors.card_bg)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEach { item ->
                val page = item.first
                val title = item.second.first
                val icon = item.second.second
                main_tools_page_button(
                    title = title,
                    icon = icon,
                    selected = selected_page == page,
                    modifier = Modifier.weight(1f),
                    on_click = { on_select(page) }
                )
            }
        }
    }
}

@Composable
private fun main_tools_page_button(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val background = if (is_pressed) colors.card_pressed.copy(alpha = 0.35f) else Color.Transparent
    val content_color = if (selected) colors.card_text_title else colors.card_text_subtitle

    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .clickable(
                interactionSource = interaction_source,
                indication = null
            ) { on_click() }
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = content_color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = content_color,
            maxLines = 1
        )
    }
}

@Composable
private fun main_tools_basic_card(
    selected_tool: main_basic_tool_item,
    tools: List<main_basic_tool_item>,
    installed: Boolean,
    on_select: (main_basic_tool_item) -> Unit,
    on_install: () -> Unit,
    on_custom_install: () -> Unit,
    on_uninstall: () -> Unit
) {
    val colors = app_theme_provider.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card_bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                main_tools_icon_box(icon = selected_tool.icon, title = selected_tool.title, colors = colors)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = selected_tool.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.card_text_title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selected_tool.description,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = colors.card_text_subtitle
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                tools.forEachIndexed { index, item ->
                    main_tools_basic_tool_row(
                        item = item,
                        selected = item == selected_tool,
                        colors = colors,
                        is_top = index == 0,
                        is_bottom = index == tools.lastIndex,
                        on_click = { on_select(item) }
                    )
                    if (index < tools.lastIndex) {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }

            main_tools_action_button(
                installed = installed,
                install_text = "安装",
                uninstall_text = "卸载",
                on_install = on_install,
                on_custom_install = on_custom_install,
                on_uninstall = on_uninstall
            )
        }
    }
}

@Composable
private fun main_tools_basic_tool_row(
    item: main_basic_tool_item,
    selected: Boolean,
    colors: app_colors,
    is_top: Boolean,
    is_bottom: Boolean,
    on_click: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val shape = main_tools_group_item_shape(is_top = is_top, is_bottom = is_bottom, radius = 10.dp)
    val background = when {
        selected -> colors.search_button_bg_active
        is_pressed -> colors.card_pressed
        else -> colors.top_button_bg
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = interaction_source,
                indication = ripple(bounded = true)
            ) { on_click() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) colors.dialog_clone_bg else colors.card_text_subtitle.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.dialog_clone_text,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Icon(
            item.icon,
            contentDescription = null,
            tint = if (selected) colors.card_text_title else colors.card_text_subtitle,
            modifier = Modifier.size(15.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.card_text_title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.description,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = colors.card_text_subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun main_tools_install_card(
    icon: ImageVector,
    title: String,
    description: String,
    selected_item: main_tool_download_item,
    items: List<main_tool_download_item>,
    installed: Boolean,
    on_select: (main_tool_download_item) -> Unit,
    on_install: () -> Unit,
    on_custom_install: () -> Unit,
    on_uninstall: () -> Unit
) {
    val colors = app_theme_provider.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card_bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                main_tools_icon_box(icon = icon, title = title, colors = colors)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
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
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                items.forEachIndexed { index, item ->
                    main_tools_version_row(
                        item = item,
                        selected = item == selected_item,
                        colors = colors,
                        is_top = index == 0,
                        is_bottom = index == items.lastIndex,
                        on_click = { on_select(item) }
                    )
                    if (index < items.lastIndex) {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }

            main_tools_action_button(
                installed = installed,
                install_text = "安装 ${selected_item.version}",
                uninstall_text = "卸载 ${selected_item.version}",
                on_install = on_install,
                on_custom_install = on_custom_install,
                on_uninstall = on_uninstall
            )
        }
    }
}


@Composable
private fun main_tools_action_button(
    installed: Boolean,
    install_text: String,
    uninstall_text: String,
    enabled: Boolean = true,
    on_install: () -> Unit,
    on_custom_install: (() -> Unit)? = null,
    on_uninstall: () -> Unit
) {
    val colors = app_theme_provider.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (installed) {
            OutlinedButton(
                onClick = on_uninstall,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.card_text_subtitle
                ),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = uninstall_text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Button(
                onClick = on_install,
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.dialog_clone_bg,
                    contentColor = colors.dialog_clone_text
                ),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = install_text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (on_custom_install != null) {
            OutlinedButton(
                onClick = { on_custom_install.invoke() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.card_text_subtitle
                ),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "自定义安装",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
private fun main_tools_version_row(
    item: main_tool_download_item,
    selected: Boolean,
    colors: app_colors,
    is_top: Boolean,
    is_bottom: Boolean,
    on_click: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    val shape = main_tools_group_item_shape(is_top = is_top, is_bottom = is_bottom, radius = 10.dp)
    val background = when {
        selected -> colors.search_button_bg_active
        is_pressed -> colors.card_pressed
        else -> colors.top_button_bg
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = interaction_source,
                indication = ripple(bounded = true)
            ) { on_click() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) colors.dialog_clone_bg else colors.card_text_subtitle.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.dialog_clone_text,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Text(
            text = item.version,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.card_text_title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "GitHub Release",
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = colors.card_text_subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun main_tools_info_card(
    icon: ImageVector,
    title: String,
    lines: List<String>
) {
    val colors = app_theme_provider.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card_bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            main_tools_icon_box(icon = icon, title = title, colors = colors)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.card_text_title
                )
                lines.forEach { line ->
                    Text(
                        text = line,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = colors.card_text_subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun main_tools_icon_box(
    icon: ImageVector,
    title: String,
    colors: app_colors
) {
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
private fun main_tools_group_title(title: String) {
    val colors = app_theme_provider.colors
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = colors.title_highlight,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

private fun main_tools_group_item_shape(
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