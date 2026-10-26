package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.lsp.gopls.gopls_diagnostic
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.toast.app_toast

/** severity → 颜色（1=错误 danger，2=警告 warning，3/4=信息 info）。 */
private fun editor_diagnostic_tint(
    diagnostic: gopls_diagnostic,
    colors: com.jmwl.gostudio.ui.theme.app_colors
): androidx.compose.ui.graphics.Color = when (diagnostic.severity) {
    1 -> colors.danger
    2 -> colors.warning
    else -> colors.info
}

/**
 * 左侧抽屉「错误」面板：当前文件的 gopls 诊断列表（按行号升序）。
 * 顶部摘要卡展示文件名与按 severity 的统计，并提供「AI 修复」一键入口；
 * 条目点击跳转该行并打开快捷修复弹层，行尾图标复制「文件:行号: 消息」。
 */
@Composable
internal fun editor_errors_panel(
    file_name: String?,
    diagnostics: List<gopls_diagnostic>,
    on_diagnostic_click: (gopls_diagnostic) -> Unit,
    on_ai_fix_all: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    // 按行号升序展示，方便自上而下逐条处理
    val sorted_diagnostics = remember(diagnostics) { diagnostics.sortedBy { it.line } }
    val error_count = diagnostics.count { it.severity == 1 }
    val warning_count = diagnostics.count { it.severity == 2 }
    val info_count = diagnostics.count { it.severity >= 3 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.editor_bg)
    ) {
        // 顶部摘要卡：文件名 + severity 统计 + AI 修复入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.editor_button_bg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = file_name ?: "未打开文件",
                    color = colors.editor_hint,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("错误 $error_count", color = colors.danger, fontSize = 12.sp, maxLines = 1)
                    Text(" · ", color = colors.editor_hint, fontSize = 12.sp)
                    Text("警告 $warning_count", color = colors.warning, fontSize = 12.sp, maxLines = 1)
                    Text(" · ", color = colors.editor_hint, fontSize = 12.sp)
                    Text("信息 $info_count", color = colors.info, fontSize = 12.sp, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // AI 一键修复：无诊断时禁用（hint 色）
            val ai_enabled = diagnostics.isNotEmpty()
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.editor_bg)
                    .clickable(enabled = ai_enabled) { on_ai_fix_all() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = if (ai_enabled) colors.title_highlight else colors.editor_hint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI 修复",
                    color = if (ai_enabled) colors.editor_text else colors.editor_hint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        if (diagnostics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.editor_hint,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "当前文件暂无诊断",
                        color = colors.editor_hint,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp, end = 12.dp, bottom = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    count = sorted_diagnostics.size,
                    key = { index ->
                        val d = sorted_diagnostics[index]
                        "err-$index-${d.line}-${d.severity}-${d.message}"
                    }
                ) { index ->
                    val diagnostic = sorted_diagnostics[index]
                    val tint = editor_diagnostic_tint(diagnostic, colors)
                    // 紧凑诊断卡：左侧 severity 竖色条 + 消息/行号 + 复制
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.editor_button_bg)
                            .clickable { on_diagnostic_click(diagnostic) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(tint)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = diagnostic.message.lineSequence().firstOrNull() ?: "",
                                color = colors.editor_text,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "行 ${diagnostic.line + 1}",
                                color = colors.editor_hint,
                                fontSize = 10.sp
                            )
                        }
                        // 复制图标独立 clickable，消费自身点击，不触发行点击
                        val display_name = file_name ?: "当前文件"
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    clipboard.setText(
                                        AnnotatedString(
                                            "$display_name:${diagnostic.line + 1}: ${diagnostic.message}"
                                        )
                                    )
                                    app_toast.show(context, "已复制", app_toast.LENGTH_SHORT)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制诊断",
                                tint = colors.editor_hint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
