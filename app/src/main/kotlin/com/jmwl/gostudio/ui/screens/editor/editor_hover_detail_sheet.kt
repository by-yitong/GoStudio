package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.lsp.gopls.gopls_hover_detail
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * 符号详情底部弹层：长按工具栏「详情」触发，展示 gopls hover 的
 * 函数签名（等宽字体）+ 文档说明，样式对齐 editor_diagnostic_sheet。
 * 下拉或点 × 关闭；查询中显示 loading，无结果显示提示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun editor_hover_detail_sheet(
    detail: gopls_hover_detail?,
    loading: Boolean,
    error: String?,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        sheetState = sheet_state,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            // 标题行：图标 + 符号名（取不到用通用标题） + 关闭
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = colors.title_highlight,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = detail?.title ?: "符号详情",
                    color = colors.title_highlight,
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

            when {
                loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    CircularProgressIndicator(
                        color = colors.title_highlight,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("正在查询详情...", color = colors.editor_hint, fontSize = 13.sp)
                }
                error != null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.editor_hint,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(error, color = colors.editor_hint, fontSize = 13.sp)
                }
                else -> {
                    // 完整签名 + 文档（可滚动、可选中文本）
                    SelectionContainer {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            detail?.signature?.let { signature ->
                                Text(
                                    text = signature,
                                    color = colors.dialog_text,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.editor_bg)
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                            detail?.documentation?.let { documentation ->
                                if (detail.signature != null) Spacer(Modifier.height(10.dp))
                                Text(
                                    text = documentation,
                                    color = colors.dialog_text,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
