package com.jmwl.gostudio.ui.screens.main

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
import com.jmwl.gostudio.ui.theme.app_colors
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * Go 工具链安装状态（由 main_activity.refresh_toolchain_status 填充）。
 *
 * Go 工具链组件通过 apt 在 proot Ubuntu rootfs 内安装：
 * - golang: Go 编译器（go build / go run / go install）
 * - gopls:  Go 语言服务器（IDE 补全/诊断/跳转/格式化）
 * - git:    版本控制（go get / go mod 依赖）
 */
data class main_tools_install_status(
    val go_installed: Boolean = false,
    val gopls_installed: Boolean = false,
    val git_installed: Boolean = false,
    val installed_go_version: String = ""
)

@Composable
fun main_tools_screen(
    on_back: () -> Unit,
    on_install_go: () -> Unit = {},
    on_install_gopls: () -> Unit = {},
    on_install_git: () -> Unit = {},
    on_install_garble: () -> Unit = {},
    install_status: main_tools_install_status = main_tools_install_status()
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
                    text = "开发工具",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_large
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Go 工具链",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "管理 Go 编译器、语言服务器与开发工具",
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
                main_tools_group_title("核心组件")

                main_tools_tool_card(
                    icon = Icons.Default.Code,
                    title = "Go 工具链",
                    description = "Go 编译器 + gopls + git（一键安装全部）",
                    status_text = if (install_status.go_installed)
                        if (install_status.installed_go_version.isNotBlank())
                            "已安装 · Go ${install_status.installed_go_version}"
                        else "已安装"
                    else "未安装",
                    installed = install_status.go_installed,
                    install_text = if (install_status.go_installed) "重装 Go 工具链" else "安装 Go 工具链",
                    on_install = on_install_go
                )

                main_tools_group_title("语言服务器")

                main_tools_tool_card(
                    icon = Icons.Default.AutoAwesome,
                    title = "gopls",
                    description = "Go 语言服务器：补全、诊断、跳转、悬浮提示、格式化",
                    status_text = if (install_status.gopls_installed) "已安装" else "未安装",
                    installed = install_status.gopls_installed,
                    install_text = if (install_status.gopls_installed) "重装 gopls" else "安装 gopls",
                    on_install = on_install_gopls
                )

                main_tools_group_title("开发工具")

                main_tools_tool_card(
                    icon = Icons.Default.CallSplit,
                    title = "git",
                    description = "版本控制，go get / go mod 拉取依赖所需",
                    status_text = if (install_status.git_installed) "已安装" else "未安装",
                    installed = install_status.git_installed,
                    install_text = if (install_status.git_installed) "重装 git" else "安装 git",
                    on_install = on_install_git
                )

                main_tools_tool_card(
                    icon = Icons.Default.EnhancedEncryption,
                    title = "garble",
                    description = "Go 代码混淆工具（go install mvdan.cc/garble）",
                    status_text = "可选工具",
                    installed = false,
                    install_text = "安装 garble",
                    on_install = on_install_garble
                )

                main_tools_info_card(
                    icon = Icons.Default.Info,
                    title = "安装说明",
                    lines = listOf(
                        "所有工具通过 apt / go install 安装到 Ubuntu rootfs",
                        "安装路径：/usr/local/go（Go）、/usr/local/go/bin/gopls",
                        "首次安装会测速国内镜像源并配置 apt 源"
                    )
                )
            }
        }
    }
}

/** 单个 Go 工具卡片：图标 + 标题 + 描述 + 状态 + 安装按钮。 */
@Composable
private fun main_tools_tool_card(
    icon: ImageVector,
    title: String,
    description: String,
    status_text: String,
    installed: Boolean,
    install_text: String,
    on_install: () -> Unit
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.card_text_title
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (installed) colors.dialog_clone_bg.copy(alpha = 0.18f) else colors.card_text_subtitle.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = status_text,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (installed) colors.dialog_clone_bg else colors.card_text_subtitle,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = description,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = colors.card_text_subtitle
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = on_install,
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
        }
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
            .clip(RoundedCornerShape(8.dp))
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
