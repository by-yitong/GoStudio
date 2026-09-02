package com.jmwl.gostudio.ui.dialogs.main

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.update.app_update_controller
import com.jmwl.gostudio.update.app_update_step
import com.jmwl.gostudio.ui.screens.ai.ai_markdown_text
import com.jmwl.gostudio.ui.theme.app_theme_provider

private fun format_mb(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 100) "%.0f MB".format(mb) else "%.1f MB".format(mb)
}

/**
 * 应用更新弹窗：发现新版本 → 下载进度 → 唤起安装。
 * step 为 Idle 时不显示任何内容。
 */
@Composable
fun app_update_dialog(
    controller: app_update_controller,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val step = controller.step

    // 下载失败等错误提示
    LaunchedEffect(controller.on_error_message) {
        controller.on_error_message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            controller.clear_error()
        }
    }

    // 下载完成后自动唤起一次系统安装器
    var install_triggered by remember { mutableStateOf(false) }
    LaunchedEffect(step) {
        if (step is app_update_step.Downloaded) {
            if (!install_triggered) {
                install_triggered = true
                controller.install(step.file)
            }
        } else {
            install_triggered = false
        }
    }

    when (val current = step) {
        is app_update_step.Available -> {
            AlertDialog(
                onDismissRequest = on_dismiss,
                containerColor = colors.dialog_bg,
                title = {
                    Text(
                        text = "发现新版本 ${current.info.tag}",
                        color = colors.dialog_text,
                        fontSize = 17.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 260.dp)
                    ) {
                        Text(
                            text = current.info.title,
                            color = colors.dialog_text,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (current.info.size_bytes > 0) {
                            Text(
                                text = "安装包大小：${format_mb(current.info.size_bytes)}",
                                color = colors.subtitle,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        if (current.info.release_notes.isNotBlank()) {
                            // Release body 是 Markdown，用现有渲染器展示（标题/列表/链接/行内格式）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                ai_markdown_text(
                                    text = current.info.release_notes,
                                    color = colors.dialog_text
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { controller.start_download(current.info) }) {
                        Text("下载更新", color = colors.title_highlight)
                    }
                },
                dismissButton = {
                    TextButton(onClick = on_dismiss) {
                        Text("以后再说", color = colors.subtitle)
                    }
                }
            )
        }

        is app_update_step.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* 下载中不允许点外部关闭 */ },
                containerColor = colors.dialog_bg,
                title = {
                    Text(
                        text = "正在下载更新",
                        color = colors.dialog_text,
                        fontSize = 17.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinearProgressIndicator(
                            progress = { (current.percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.title_highlight,
                            trackColor = colors.card_bg
                        )
                        Text(
                            text = "${current.percent}% · ${format_mb(current.received)} / ${format_mb(current.total)}",
                            color = colors.subtitle,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { controller.cancel_download() }) {
                        Text("取消", color = colors.danger)
                    }
                }
            )
        }

        is app_update_step.Downloaded -> {
            AlertDialog(
                onDismissRequest = on_dismiss,
                containerColor = colors.dialog_bg,
                title = {
                    Text(
                        text = "下载完成",
                        color = colors.dialog_text,
                        fontSize = 17.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "若安装界面没有自动弹出，点击「安装」开始安装 ${current.info.tag}。",
                        color = colors.subtitle,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { controller.install(current.file) }) {
                        Text("安装", color = colors.title_highlight)
                    }
                },
                dismissButton = {
                    TextButton(onClick = on_dismiss) {
                        Text("稍后", color = colors.subtitle)
                    }
                }
            )
        }

        app_update_step.Idle -> Unit
    }
}

/** 检查中占位弹窗（手动检查时使用） */
@Composable
fun app_update_checking_dialog() {
    val colors = app_theme_provider.colors
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.dialog_bg,
        text = {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = colors.title_highlight,
                    modifier = Modifier.padding(2.dp)
                )
                Text("正在检查更新…", color = colors.dialog_text, fontSize = 14.sp)
            }
        },
        confirmButton = {}
    )
}
