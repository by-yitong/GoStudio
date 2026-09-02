package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jmwl.gostudio.ui.theme.app_theme_provider

/** 项目配置页「编译」弹窗的状态：Activity 持有并写入，配置面板负责渲染。 */
internal class editor_compile_dialog_state {
    var visible by mutableStateOf(false)
    var running by mutableStateOf(false)
    val lines = mutableStateListOf<editor_output_line>()

    fun show() {
        lines.clear()
        running = true
        visible = true
    }

    fun hide() {
        visible = false
    }

    fun finish() {
        running = false
    }

    fun append(text: String, level: editor_output_line_level = editor_output_line_level.NORMAL) {
        text.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .forEach { lines.add(editor_output_line(it, level)) }
        if (lines.size > 500) {
            lines.subList(0, lines.size - 500).clear()
        }
    }
}

/** 编译日志弹窗：滚动展示 go build 输出，运行中可停止，结束后关闭。 */
@Composable
internal fun editor_compile_log_dialog(
    state: editor_compile_dialog_state,
    on_cancel: () -> Unit = {}
) {
    if (!state.visible) return
    val colors = app_theme_provider.colors
    val list_state = rememberLazyListState()
    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) list_state.scrollToItem(state.lines.size - 1)
    }

    Dialog(onDismissRequest = { if (!state.running) state.hide() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.editor_panel_overlay
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = colors.title_highlight
                        )
                    }
                    Text(
                        text = if (state.running) "正在编译..." else "编译结束",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.editor_text,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    state = list_state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 340.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.editor_bg)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(state.lines.size) { index ->
                        val line = state.lines[index]
                        Text(
                            text = line.text,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp,
                            color = when (line.level) {
                                editor_output_line_level.ERROR -> colors.danger
                                editor_output_line_level.WARNING -> colors.warning
                                editor_output_line_level.SUCCESS -> colors.success
                                editor_output_line_level.INFO -> colors.info
                                else -> colors.editor_text
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (state.running) {
                    Button(
                        onClick = on_cancel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger_bg, contentColor = colors.danger)
                    ) {
                        Text("停止编译")
                    }
                } else {
                    Button(
                        onClick = { state.hide() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.editor_button_bg,
                            contentColor = colors.editor_text
                        )
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}
