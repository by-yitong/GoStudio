package com.jmwl.gostudio.ui.screens.install

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.delay

@Composable
fun install_screen(
    logs: List<String>,
    is_downloading: Boolean,
    is_extracting: Boolean,
    is_configuring: Boolean,
    current_progress: Float,
    elapsed_seconds: Long = 0,
    on_export_logs: () -> Unit
) {
    val list_state = rememberLazyListState()
    val colors = app_theme_provider.colors

    LaunchedEffect(logs.size, logs.lastOrNull()) {
        delay(50)
        if (logs.isNotEmpty()) {
            list_state.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GoStudio",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.title_highlight
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Alpine 基础环境安装",
            fontSize = 14.sp,
            color = colors.subtitle
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.editor_bg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.card_bg)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "安装日志",
                            fontSize = 11.sp,
                            color = colors.card_text_subtitle,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = format_elapsed(elapsed_seconds),
                            fontSize = 11.sp,
                            color = colors.card_text_subtitle.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = on_export_logs, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "导出",
                            tint = colors.card_chevron,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                LazyColumn(
                    state = list_state,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs) { log ->
                        val color = when {
                            log.startsWith("解压:") || log.startsWith("下载进度:") -> colors.subtitle
                            log.contains("失败") || log.contains("错误") -> colors.danger
                            log.contains("完成") || log.contains("通过") || log.contains("成功") -> colors.success
                            else -> colors.editor_text
                        }
                        Text(log, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (is_downloading || is_extracting || is_configuring) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { current_progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colors.title_highlight,
                trackColor = colors.title_highlight.copy(alpha = 0.2f)
            )
        }
    }
}

/** 秒数 → "mm:ss"，超过 1 小时 → "h:mm:ss"。 */
private fun format_elapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
