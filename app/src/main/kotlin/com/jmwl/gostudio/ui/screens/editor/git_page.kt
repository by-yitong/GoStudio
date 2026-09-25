package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider

/** 全屏 Git 页的双 Tab：提交历史 / 分支。 */
internal enum class git_page_tab { HISTORY, BRANCHES }

/**
 * 全屏 Git 页壳（editor_screen 仿 show_ai_page 模式的覆盖层）：
 * 顶栏返回 + 当前 Tab 标题 + 刷新（bump refresh_tick）；胶囊双 Tab 切换；内容区渲染各 Tab。
 */
@Composable
internal fun editor_git_page(
    project_root_path: String,
    initial_tab: git_page_tab,
    on_close: () -> Unit
) {
    val colors = app_theme_provider.colors
    var selected_tab by remember { mutableStateOf(initial_tab) }
    var refresh_tick by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.editor_bg)
            .statusBarsPadding()
    ) {
        // 顶栏：返回 + 当前 Tab 标题 + 刷新
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = on_close, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.editor_icon,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = if (selected_tab == git_page_tab.HISTORY) "提交历史" else "分支",
                color = colors.editor_text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            IconButton(onClick = { refresh_tick++ }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = colors.editor_hint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 胶囊双 Tab（参照 editor_git_panel.git_panel_action 的胶囊风格）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            git_page_tab_button(
                label = "提交历史",
                active = selected_tab == git_page_tab.HISTORY,
                modifier = Modifier.weight(1f)
            ) { selected_tab = git_page_tab.HISTORY }
            git_page_tab_button(
                label = "分支",
                active = selected_tab == git_page_tab.BRANCHES,
                modifier = Modifier.weight(1f)
            ) { selected_tab = git_page_tab.BRANCHES }
        }

        // 内容区
        Box(modifier = Modifier.weight(1f)) {
            when (selected_tab) {
                git_page_tab.HISTORY -> git_log_tab(project_root_path, refresh_tick)
                git_page_tab.BRANCHES -> git_branches_tab(
                    project_root_path = project_root_path,
                    refresh_tick = refresh_tick,
                    on_state_changed = { refresh_tick++ }
                )
            }
        }
    }
}

/** 胶囊 Tab 按钮：选中态 editor_sidebar_selected_bg 填充，非选中 editor_button_bg。 */
@Composable
private fun git_page_tab_button(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) colors.editor_sidebar_selected_bg else colors.editor_button_bg)
            .clickable(onClick = on_click)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) colors.editor_text else colors.editor_hint,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
