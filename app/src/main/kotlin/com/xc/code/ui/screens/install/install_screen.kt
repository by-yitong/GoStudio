package com.xc.code.ui.screens.install

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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun install_screen(
    logs: List<String>,
    is_downloading: Boolean,
    is_extracting: Boolean,
    is_configuring: Boolean,
    current_progress: Float,
    on_export_logs: () -> Unit
) {
    val list_state = rememberLazyListState()

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
            text = "XCode",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ubuntu基础环境安装",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A3A))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Circle, null, tint = Color(0xFFFF5F56), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Circle, null, tint = Color(0xFFFFBD2E), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Circle, null, tint = Color(0xFF27C93F), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "usr@xcode:~",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = on_export_logs, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "导出",
                            tint = Color.White.copy(alpha = 0.6f),
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
                            log.startsWith("解压:") || log.startsWith("下载进度:") -> Color.White.copy(alpha = 0.7f)
                            log.contains("失败") || log.contains("错误") -> Color(0xFFFF5555)
                            log.contains("完成") || log.contains("通过") || log.contains("成功") -> Color(0xFF4CAF50)
                            else -> Color.White
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}
