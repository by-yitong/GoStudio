package com.jmwl.gostudio.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.learn.learn_content
import com.jmwl.gostudio.learn.learn_progress
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * 课程轨道页（参考 CodeAssist LessonTrackScreen）：
 * 轨道标题 + 课程卡片列表（完成状态 / 预计时长 / 步数进度）。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun learn_track_screen(
    track_id: String,
    on_back: () -> Unit,
    on_open_lesson: (lesson_id: String, step_index: Int) -> Unit
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val track = remember(track_id) { learn_content.tracks.firstOrNull { it.id == track_id } } ?: run {
        on_back()
        return
    }
    val accent = Color(track.accent_color.toInt())

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(track.title, color = colors.title_large, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(track.subtitle, color = colors.subtitle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = on_back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.top_button_icon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(track.lessons, key = { it.id }) { lesson ->
                val done = remember(lesson.id) { learn_progress.completed_steps(context, lesson.id) }
                val completed_count = lesson.steps.count { it.id in done }
                val lesson_done = completed_count >= lesson.steps.size

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.card_bg)
                        .clickable { on_open_lesson(lesson.id, 0) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (lesson_done) colors.success_bg else accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (lesson_done) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "已完成",
                                tint = colors.success,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = "${track.lessons.indexOf(lesson) + 1}",
                                color = accent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = lesson.title,
                            color = colors.card_text_title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = lesson.summary,
                            color = colors.card_text_subtitle,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = colors.card_chevron,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${lesson.est_minutes} 分钟 · ${completed_count}/${lesson.steps.size} 步",
                                color = colors.card_chevron,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
