package com.jmwl.gostudio.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
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
 * 学习主页（参考 CodeAssist LearnScreen）：
 * 顶部「继续学习」卡（断点直达）+ 按 category 分组的课程轨道卡片。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun learn_home_screen(
    on_back: () -> Unit,
    on_open_track: (String) -> Unit,
    on_resume: (lesson_id: String, step_index: Int) -> Unit
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val resume = remember { learn_progress.resume(context) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Go 学习", color = colors.title_large, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = on_back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.top_button_icon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            learn_catalog_content(
                on_open_track = on_open_track,
                on_resume = on_resume,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 学习目录主体：继续学习卡 + 分组轨道卡。
 * 路由版与首页「学习」底部页签共用。
 */
@Composable
fun learn_catalog_content(
    on_open_track: (String) -> Unit,
    on_resume: (lesson_id: String, step_index: Int) -> Unit,
    modifier: Modifier = Modifier,
    show_header: Boolean = false
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val resume = remember { learn_progress.resume(context) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (show_header) {
            item(key = "header") {
                Text(
                    text = "学习",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_large
                )
            }
        }
        if (resume != null) {
            item(key = "resume") {
                resume_card(resume) {
                    on_resume(resume.lesson_id, resume.step_index)
                }
            }
        }

        learn_content.tracks.groupBy { it.category }.forEach { (category, tracks) ->
            item(key = "cat-$category") {
                Text(
                    text = category,
                    color = colors.section_title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }
            items(tracks, key = { it.id }) { track ->
                track_card(track) { on_open_track(track.id) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun resume_card(
    resume: learn_progress.resume_point,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card_bg)
            .clickable(onClick = on_click)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = colors.title_highlight,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "继续学习",
                color = colors.title_highlight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "${resume.track_title} · ${resume.lesson_title}",
            color = colors.card_text_title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { resume.fraction },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colors.title_highlight,
                trackColor = colors.card_icon_bg
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.card_chevron,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun track_card(
    track: com.jmwl.gostudio.learn.learn_track,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    val accent = Color(track.accent_color.toInt())
    val context = LocalContext.current
    // 轨道完成度 = 已完成课程（全部 step 完成）/ 总课程
    val completed_lessons = track.lessons.count { lesson ->
        val done = learn_progress.completed_steps(context, lesson.id)
        lesson.steps.all { it.id in done }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card_bg)
            .border(1.dp, colors.editor_divider, RoundedCornerShape(14.dp))
            .clickable(onClick = on_click)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${track.title.take(1)}",
                color = accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = track.title,
                color = colors.card_text_title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = track.subtitle,
                color = colors.card_text_subtitle,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (completed_lessons > 0) "已完成 $completed_lessons/${track.lessons.size} 课"
                else "${track.lessons.size} 课",
                color = colors.card_chevron,
                fontSize = 11.sp
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.card_chevron,
            modifier = Modifier.size(16.dp)
        )
    }
}
