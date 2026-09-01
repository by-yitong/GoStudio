package com.jmwl.gostudio.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.learn.learn_block
import com.jmwl.gostudio.learn.normalize_lesson_code
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * 课程内容块渲染（参考 CodeAssist LessonBlocks）：
 * 讲解文本（内联 **粗体** / `代码`）、只读代码块、提示框（tip/note）。
 */
@Composable
internal fun learn_blocks(blocks: List<learn_block>, modifier: Modifier = Modifier) {
    val colors = app_theme_provider.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block) {
                is learn_block.text -> Text(
                    text = inline_markup(block.md),
                    color = colors.card_text_subtitle,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )
                is learn_block.code -> learn_code_sample(block.src)
                is learn_block.callout -> learn_callout(block.kind, block.text)
            }
        }
    }
}

/**
 * 极简内联标记：**粗体** 与 `等宽代码`。
 * 不引入 markdown 依赖，课程文本只需要这两种强调。
 */
internal fun inline_markup(md: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < md.length) {
        when {
            md.startsWith("**", i) -> {
                val end = md.indexOf("**", i + 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(md.substring(i + 2, end)) }
                    i = end + 2
                } else {
                    append(md[i]); i++
                }
            }
            md[i] == '`' -> {
                val end = md.indexOf('`', i + 1)
                if (end > 0) {
                    withStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    ) { append(md.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append(md[i]); i++
                }
            }
            else -> {
                append(md[i]); i++
            }
        }
    }
}

/** 只读代码块：等宽 + 编辑器底色，横向可滚动。 */
@Composable
internal fun learn_code_sample(src: String, modifier: Modifier = Modifier) {
    val colors = app_theme_provider.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.editor_bg)
            .horizontalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(
            text = normalize_lesson_code(src).trimEnd(),
            color = colors.editor_text,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun learn_callout(kind: String, text: String) {
    val colors = app_theme_provider.colors
    val (tint, bg) = when (kind) {
        "warn" -> colors.warning to colors.warning_bg
        "note" -> colors.info to colors.info_bg
        else -> colors.success to colors.success_bg
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = inline_markup(text),
            color = tint,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}
