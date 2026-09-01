package com.jmwl.gostudio.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.jmwl.gostudio.ui.components.press_scale
import com.jmwl.gostudio.ui.components.entrance_slide_up
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.R
import com.jmwl.gostudio.ui.theme.*
import com.jmwl.gostudio.ui.theme.motion

data class recent_project(
    val name: String,
    val path: String,
    val go_version: String,
    val last_opened: String
)

/** 首页底部导航的三个页签（参考 CodeAssist HomeTab）。 */
internal enum class main_home_tab(val title: String) {
    PROJECTS("项目"),
    LEARN("学习"),
    SETTINGS("设置")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun main_screen(
    on_new_project: () -> Unit = {},
    on_open_project: () -> Unit = {},
    on_tools: () -> Unit = {},
    on_plugins: () -> Unit = {},
    on_terminal: () -> Unit = {},
    on_ai: () -> Unit = {},
    on_learn_open_track: (String) -> Unit = {},
    on_learn_resume: (String, Int) -> Unit = { _, _ -> },
    on_theme_click: () -> Unit = {},
    on_editor_theme_click: () -> Unit = {},
    on_editor_click: () -> Unit = {},
    on_ai_settings_click: () -> Unit = {},
    on_about_click: () -> Unit = {},
    recent_projects: List<recent_project> = emptyList(),
    on_project_click: (recent_project) -> Unit = {},
    on_project_remove: (recent_project) -> Unit = {}
) {
    val colors = app_theme_provider.colors
    // rememberSaveable：从二级页返回时恢复页签，不再跳回「项目」
    var selected_tab by rememberSaveable { mutableStateOf(main_home_tab.PROJECTS) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = colors.card_bg,
                tonalElevation = 0.dp
            ) {
                main_home_tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected_tab == tab,
                        onClick = { selected_tab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    main_home_tab.PROJECTS -> Icons.Default.Folder
                                    main_home_tab.LEARN -> Icons.Default.School
                                    main_home_tab.SETTINGS -> Icons.Default.Settings
                                },
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.title_highlight,
                            selectedTextColor = colors.title_highlight,
                            unselectedIconColor = colors.subtitle,
                            unselectedTextColor = colors.subtitle,
                            indicatorColor = colors.title_highlight.copy(alpha = 0.14f)
                        )
                    )
                }
            }
        }
    ) { padding_values ->
        // 页签交叉淡入（CodeAssist HomeScreen 原样：Crossfade + Motion.BASE/soft）
        Crossfade(
            targetState = selected_tab,
            animationSpec = tween(motion.BASE, easing = motion.soft),
            label = "homeTab",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding_values.calculateBottomPadding())
        ) { tab ->
            when (tab) {
                main_home_tab.PROJECTS -> projects_page(
                    on_new_project = on_new_project,
                    on_open_project = on_open_project,
                    on_terminal = on_terminal,
                    recent_projects = recent_projects,
                    on_project_click = on_project_click,
                    on_project_remove = on_project_remove
                )
                main_home_tab.LEARN -> Box(Modifier.statusBarsPadding()) {
                    learn_tab_page(
                        on_open_track = on_learn_open_track,
                        on_resume = on_learn_resume
                    )
                }
                main_home_tab.SETTINGS -> Box(Modifier.statusBarsPadding()) {
                    settings_tab_page(
                        on_theme_click = on_theme_click,
                        on_editor_theme_click = on_editor_theme_click,
                        on_editor_click = on_editor_click,
                        on_ai_settings_click = on_ai_settings_click,
                        on_about_click = on_about_click,
                        on_tools_click = on_tools,
                        on_plugins_click = on_plugins
                    )
                }
            }
        }
    }
}

/** 「项目」页 —— 逐段移植 CodeAssist ProjectPickerScreen：折叠大标题 + 卡片流 + 项目卡列表。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun projects_page(
    on_new_project: () -> Unit,
    on_open_project: () -> Unit,
    on_terminal: () -> Unit,
    recent_projects: List<recent_project>,
    on_project_click: (recent_project) -> Unit,
    on_project_remove: (recent_project) -> Unit
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("项目") },
                // 容器色与正文 background 统一，消除顶栏白/灰分界线
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = on_terminal) {
                        Icon(Icons.Default.Terminal, contentDescription = "终端")
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth()
                    .padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 主操作卡打头（NewProjectCard）
                new_project_card(on_click = on_new_project)
                // 导入项目（CodeAssist ImportGradleCard 同款描边卡）
                outlined_action_card(Icons.Default.FolderOpen, "导入项目", "打开手机上已有的项目目录", on_open_project)

                if (recent_projects.isEmpty()) {
                    // 空状态：图标 + 双行引导（替代单行灰字）
                    Column(
                        Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier.size(52.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Folder, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            "还没有项目",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "点上方「新建项目」开始你的第一个 Go 项目",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    section_label("最近项目", count = recent_projects.size)
                    recent_projects.forEachIndexed { i, project ->
                        project_card(
                            project = project,
                            delay_millis = i * 50,
                            on_open = { on_project_click(project) },
                            on_delete = { on_project_remove(project) }
                        )
                    }
                }
            }
        }
    }
}

/** 主操作卡（CodeAssist NewProjectCard 原样）：primaryContainer 填充 + primary 图标块。 */
@Composable
private fun new_project_card(on_click: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .press_scale(interaction)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
            .clickable(interaction, indication = null, onClick = on_click)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimary)
        }
        Column(Modifier.weight(1f)) {
            Text(
                "新建项目",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "创建一个新的 Go 项目",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** 导入项目描边卡（CodeAssist ImportGradleCard 原样）：surface 底 + outlineVariant 描边。 */
@Composable
private fun outlined_action_card(
    icon: ImageVector,
    title: String,
    subtitle: String,
    on_click: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .press_scale(interaction)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(interaction, indication = null, onClick = on_click)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

/** 分组小标题（CodeAssist SectionLabel 原样）：大写字标签 + 计数胶囊。 */
@Composable
private fun section_label(text: String, count: Int? = null) {
    Row(
        Modifier.padding(start = 2.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text.uppercase(),
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (count != null) {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(999.dp))
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            ) {
                Text(
                    count.toString(),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** 项目卡（CodeAssist ProjectCard 原样）：错峰滑入 + 字母头像 + 版本胶囊 + 删除位。 */
@Composable
private fun project_card(
    project: recent_project,
    delay_millis: Int,
    on_open: () -> Unit,
    on_delete: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .entrance_slide_up(delay_millis)
            .fillMaxWidth()
            .press_scale(interaction)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(interaction, indication = null, onClick = on_open)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 字母头像（CA 加载项目图标，GoStudio 用首字母 + primaryContainer）
        Box(
            Modifier.size(54.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                project.name.take(1).uppercase().ifBlank { "G" },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                project.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 上次打开时间独占一行（CA：clock + 相对时间）
            if (project.last_opened.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Text(
                        project.last_opened,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // 类型标签（CA：Android 绿胶囊；GoStudio：Go 青胶囊 + 路径尾段，尾段与项目名重复时省略）
            val path_tail = project.path.substringAfterLast('/')
            if (project.go_version.isNotBlank() || (path_tail.isNotBlank() && path_tail != project.name)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (project.go_version.isNotBlank()) {
                        Row(
                            Modifier
                                .background(Color(0xFF00ACD8).copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Code, null, Modifier.size(12.dp), tint = Color(0xFF0087A8))
                            Text(
                                "Go ${project.go_version}",
                                color = Color(0xFF0087A8),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (path_tail.isNotBlank() && path_tail != project.name) {
                        Text(
                            path_tail,
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        val delete_interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(34.dp)
                .press_scale(delete_interaction)
                .clickable(delete_interaction, indication = null, onClick = on_delete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = "移除项目", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

/** 「学习」页：课程目录（CodeAssist Learn 位）。标题在 catalog 的 header item 里，避免滚动嵌套。 */
@Composable
private fun learn_tab_page(
    on_open_track: (String) -> Unit,
    on_resume: (String, Int) -> Unit
) {
    com.jmwl.gostudio.ui.screens.learn.learn_catalog_content(
        on_open_track = on_open_track,
        on_resume = on_resume,
        modifier = Modifier.fillMaxSize(),
        show_header = true
    )
}

/** 「设置」页：设置列表主体。 */
@Composable
private fun settings_tab_page(
    on_theme_click: () -> Unit,
    on_editor_theme_click: () -> Unit,
    on_editor_click: () -> Unit,
    on_ai_settings_click: () -> Unit,
    on_about_click: () -> Unit,
    on_tools_click: () -> Unit,
    on_plugins_click: () -> Unit
) {
    main_settings_content(
        on_theme_click = on_theme_click,
        on_editor_theme_click = on_editor_theme_click,
        on_editor_click = on_editor_click,
        on_ai_click = on_ai_settings_click,
        on_about_click = on_about_click,
        on_tools_click = on_tools_click,
        on_plugins_click = on_plugins_click
    )
}
