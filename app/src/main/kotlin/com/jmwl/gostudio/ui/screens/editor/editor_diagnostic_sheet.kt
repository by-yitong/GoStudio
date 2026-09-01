package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.lsp.gopls.gopls_code_action
import com.jmwl.gostudio.lsp.gopls.gopls_diagnostic
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * 诊断详情底部弹层（移植自 CodeAssist DiagnosticSheet）：
 * 完整可滚动的错误消息 + 该行可用的大触摸目标快速修复列表。
 * 点遮罩或 × 关闭；点修复由外部应用并关闭。
 */
@Composable
internal fun editor_diagnostic_sheet(
    diagnostic: gopls_diagnostic,
    actions: List<gopls_code_action>,
    actions_loading: Boolean,
    on_apply_action: (gopls_code_action) -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val (tint, label) = when (diagnostic.severity) {
        1 -> colors.danger to "错误"
        2 -> colors.warning to "警告"
        else -> colors.info to "信息"
    }
    val sheet_shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) { detectTapGestures { on_dismiss() } },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(sheet_shape)
                .background(colors.dialog_bg)
                .pointerInput(Unit) { detectTapGestures { } } // 面板内吞掉点击，避免误关
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 标题行：severity + 行号 + 关闭
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$label · 行 ${diagnostic.line + 1}",
                    color = tint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = on_dismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = colors.editor_hint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // 完整消息（可选中文本）
            SelectionContainer {
                Text(
                    text = diagnostic.message,
                    color = colors.dialog_text,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            // 快速修复
            Spacer(Modifier.height(12.dp))
            when {
                actions_loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    CircularProgressIndicator(
                        color = colors.title_highlight,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("正在查询快速修复...", color = colors.editor_hint, fontSize = 13.sp)
                }
                actions.isEmpty() -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = colors.editor_hint,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("此问题没有可用的快速修复", color = colors.editor_hint, fontSize = 13.sp)
                }
                else -> {
                    Text(
                        text = "快速修复",
                        color = colors.editor_hint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    actions.forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { on_apply_action(action) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = colors.title_highlight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = action.title,
                                color = colors.dialog_text,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
