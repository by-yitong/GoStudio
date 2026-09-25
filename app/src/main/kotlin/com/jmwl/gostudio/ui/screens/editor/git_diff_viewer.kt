package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * git diff 着色展示（自 editor_git_panel 的 diff_content 抽出，面板与全屏历史页共用）：
 * +绿 / -红 / @@青 / 头部灰，等宽字体纵向滚动。
 */
@Composable
internal fun git_diff_viewer(diff_text: String, modifier: Modifier = Modifier) {
    val colors = app_theme_provider.colors
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        diff_text.lines().forEach { line ->
            val tint = when {
                line.startsWith("+") -> colors.success
                line.startsWith("-") -> colors.danger
                line.startsWith("@@") -> colors.editor_icon
                line.startsWith("diff ") || line.startsWith("index ") -> colors.editor_hint
                else -> colors.editor_text
            }
            Text(
                text = line.ifBlank { " " },
                color = tint,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
