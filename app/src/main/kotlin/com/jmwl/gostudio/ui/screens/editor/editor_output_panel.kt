package com.jmwl.gostudio.ui.screens.editor

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jmwl.gostudio.ui.theme.app_theme_provider
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal enum class editor_output_tab(val title: String) {
    Output("构建输出"),
    Log("IDE日志")
}

internal enum class editor_output_line_level {
    NORMAL,
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

internal data class editor_output_line(
    val text: String,
    val level: editor_output_line_level = editor_output_line_level.NORMAL
)

private const val editor_output_max_lines = 1000
private val editor_log_time_formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

internal class editor_output_panel_state {
    var selected_tab by mutableStateOf(editor_output_tab.Output)
    var task_running by mutableStateOf(false)
    var task_stopping by mutableStateOf(false)
    var output_revision by mutableStateOf(0)
    val output_lines = mutableStateListOf<editor_output_line>()
    val log_lines = mutableStateListOf<editor_output_line>()

    fun append_output(text: String, level: editor_output_line_level = editor_output_line_level.NORMAL) {
        append_lines(output_lines, text, level)
    }

    fun append_log(text: String, level: editor_output_line_level = editor_output_line_level.INFO) {
        val time = LocalTime.now().format(editor_log_time_formatter)
        append_lines(log_lines, "[$time] $text", level)
    }

    fun clear_output() {
        output_lines.clear()
        output_revision++
    }

    fun clear_selected_tab() {
        when (selected_tab) {
            editor_output_tab.Output -> {
                output_lines.clear()
                output_revision++
            }
            editor_output_tab.Log -> {
                log_lines.clear()
                output_revision++
            }
        }
    }

    fun selected_tab_text(): String {
        val lines = when (selected_tab) {
            editor_output_tab.Output -> output_lines
            editor_output_tab.Log -> log_lines
        }
        return lines.joinToString("\n") { line -> line.text }
    }

    private fun append_lines(
        target: MutableList<editor_output_line>,
        text: String,
        level: editor_output_line_level
    ) {
        text.lineSequence()
            .map { line -> line.trimEnd() }
            .filter { line -> line.isNotBlank() }
            .forEach { line ->
                target.add(editor_output_line(line, level))
            }
        if (target.size > editor_output_max_lines) {
            target.subList(0, target.size - editor_output_max_lines).clear()
        }
        output_revision++
    }
}

/**
 * 左侧抽屉「日志」工具面板：构建输出 / IDE 日志两个子页签，
 * 右侧提供分享与清空入口。
 */
@Composable
internal fun editor_log_panel(
    state: editor_output_panel_state,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.editor_bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.editor_button_bg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                editor_output_tab.entries.forEach { tab ->
                    val selected = state.selected_tab == tab
                    Text(
                        text = tab.title,
                        color = if (selected) colors.editor_tab_selected_text else colors.editor_hint,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) colors.editor_tab_selected_bg else Color.Transparent)
                            .clickable {
                                if (!selected) state.selected_tab = tab
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable {
                            share_editor_output_text(
                                context = context,
                                title = state.selected_tab.title,
                                text = state.selected_tab_text()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "分享当前内容",
                        tint = colors.editor_hint,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { state.clear_selected_tab() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "清理当前内容",
                        tint = colors.editor_hint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        val lines = when (state.selected_tab) {
            editor_output_tab.Output -> state.output_lines
            editor_output_tab.Log -> state.log_lines
        }
        if (lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                editor_output_empty_state(tab = state.selected_tab)
            }
        } else {
            editor_output_line_list(
                lines = lines,
                revision = state.output_revision,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun editor_output_line_list(
    lines: List<editor_output_line>,
    revision: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = app_theme_provider.colors
    var rendered_line_count by remember { mutableStateOf(0) }
    var rendered_revision by remember { mutableStateOf(-1) }
    val output_editor = remember {
        CodeEditor(context).apply {
            isEditable = false
            setUndoEnabled(false)
            setLineNumberEnabled(true)
            setWordwrap(false, false)
            setScrollBarEnabled(false)
            setVerticalScrollBarEnabled(false)
            setHorizontalScrollBarEnabled(false)
            setTextSize(11f)
            setLineInfoTextSize(10f)
            setLineSpacing(2f, 1.08f)
            setHighlightCurrentLine(false)
            setHighlightCurrentBlock(false)
            setHighlightBracketPair(false)
            setBlockLineEnabled(false)
            props.stickyScroll = false
        }
    }

    LaunchedEffect(colors) {
        val background = colors.editor_bg.toArgb()
        val line_number = colors.editor_hint.toArgb()
        output_editor.setBackgroundColor(background)
        output_editor.colorScheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, background)
        output_editor.colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, colors.editor_text.toArgb())
        output_editor.colorScheme.setColor(EditorColorScheme.CURRENT_LINE, Color.Transparent.toArgb())
        output_editor.colorScheme.setColor(EditorColorScheme.LINE_DIVIDER, Color.Transparent.toArgb())
        output_editor.colorScheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, background)
        output_editor.colorScheme.setColor(EditorColorScheme.LINE_NUMBER, line_number)
        output_editor.colorScheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, line_number)
        output_editor.invalidate()
    }

    LaunchedEffect(revision, lines.size) {
        if (rendered_revision == revision && rendered_line_count == lines.size) return@LaunchedEffect
        if (lines.size < rendered_line_count || rendered_revision < 0) {
            output_editor.setText("")
            rendered_line_count = 0
        }
        if (lines.size > rendered_line_count) {
            val append_text = lines.drop(rendered_line_count)
                .joinToString("\n", postfix = "\n") { line -> line.text }
            if (rendered_line_count == 0 || output_editor.text.length == 0) {
                output_editor.setText(append_text)
            } else {
                val last_line = output_editor.text.lineCount.coerceAtLeast(1) - 1
                val last_column = output_editor.text.getColumnCount(last_line)
                output_editor.text.insert(last_line, last_column, append_text)
            }
            rendered_line_count = lines.size
        }
        rendered_revision = revision
        val last_line = output_editor.text.lineCount.coerceAtLeast(1) - 1
        val last_column = output_editor.text.getColumnCount(last_line)
        delay(16)
        output_editor.post {
            output_editor.setSelection(last_line, last_column, true)
        }
    }

    DisposableEffect(output_editor) {
        onDispose { output_editor.release() }
    }

    AndroidView(
        factory = {
            output_editor.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun editor_output_empty_state(
    tab: editor_output_tab,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val title = when (tab) {
        editor_output_tab.Output -> "暂无构建输出"
        editor_output_tab.Log -> "暂无 IDE 日志"
    }
    val subtitle = when (tab) {
        editor_output_tab.Output -> "点击编译后，构建输出会显示在这里"
        editor_output_tab.Log -> "项目识别与 IDE 状态会显示在这里"
    }
    val icon = when (tab) {
        editor_output_tab.Output -> Icons.Default.Terminal
        editor_output_tab.Log -> Icons.AutoMirrored.Filled.ListAlt
    }
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.editor_icon,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            color = colors.editor_text,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            color = colors.editor_hint,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun share_editor_output_text(
    context: Context,
    title: String,
    text: String
) {
    if (text.isBlank()) return
    val send_intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(send_intent, "分享$title"))
    }
}
