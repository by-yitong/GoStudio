package com.xc.code.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.R
import com.xc.code.ui.theme.*

data class recent_project(
    val name: String,
    val path: String,
    val cmake_version: String,
    val ndk_version: String,
    val last_opened: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun main_screen(
    on_new_project: () -> Unit = {},
    on_open_project: () -> Unit = {},
    on_tools: () -> Unit = {},
    on_plugins: () -> Unit = {},
    on_settings: () -> Unit = {},
    on_terminal: () -> Unit = {},
    on_ai: () -> Unit = {},
    recent_projects: List<recent_project> = emptyList(),
    on_project_click: (recent_project) -> Unit = {},
    on_project_remove: (recent_project) -> Unit = {}
) {
    val colors = app_theme_provider.colors
    var search_text by remember { mutableStateOf("") }
    val focus_requester = remember { FocusRequester() }
    var show_search by remember { mutableStateOf(false) }
    
    val filtered_projects = if (search_text.isNotBlank()) {
        recent_projects.filter {
            it.name.contains(search_text, ignoreCase = true) ||
            it.path.contains(search_text, ignoreCase = true)
        }
    } else {
        recent_projects
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { padding_values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(
                    bottom = padding_values.calculateBottomPadding()
                )
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(
                padding_values.calculateTopPadding() + 30.dp
            ))
            
            top_bar(
                colors = colors,
                on_ai_click = on_ai,
                on_terminal_click = on_terminal,
                on_settings_click = on_settings
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "打开或创建",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_large
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "您的下一个项目",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "XCode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.subtitle
                )
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Text(
                text = "快捷操作",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.section_title
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                action_card_item(
                    icon = Icons.Default.Add,
                    title = "新建项目",
                    subtitle = "创建新的项目",
                    colors = colors,
                    on_click = on_new_project,
                    is_top = true,
                    is_bottom = false
                )
                
                Spacer(modifier = Modifier.height(1.dp))
                
                action_card_item(
                    icon = Icons.Default.FolderOpen,
                    title = "打开项目",
                    subtitle = "打开已有项目",
                    colors = colors,
                    on_click = on_open_project,
                    is_top = false,
                    is_bottom = false
                )
                
                Spacer(modifier = Modifier.height(1.dp))
                
                action_card_item(
                    icon = Icons.Default.Extension,
                    title = "插件管理",
                    subtitle = "管理 XCode 插件",
                    colors = colors,
                    on_click = on_plugins,
                    is_top = false,
                    is_bottom = false
                )
                
                Spacer(modifier = Modifier.height(1.dp))
                
                action_card_item(
                    icon = Icons.Default.Build,
                    title = "开发工具",
                    subtitle = "管理 NDK Cmake 组件",
                    colors = colors,
                    on_click = on_tools,
                    is_top = false,
                    is_bottom = true
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = show_search,
                        transitionSpec = {
                            fadeIn().togetherWith(fadeOut()) using SizeTransform(clip = false)
                        },
                        modifier = Modifier.weight(1f)
                    ) { is_searching ->
                        if (is_searching) {
                            OutlinedTextField(
                                value = search_text,
                                onValueChange = { search_text = it },
                                placeholder = { Text("搜索最近项目...", color = colors.input_hint) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focus_requester)
                                    .onGloballyPositioned {
                                        focus_requester.requestFocus()
                                    },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.input_border,
                                    unfocusedBorderColor = colors.input_hint.copy(alpha = 0.5f),
                                    focusedTextColor = colors.input_text,
                                    unfocusedTextColor = colors.input_text,
                                    cursorColor = colors.input_border,
                                    focusedLabelColor = colors.input_border,
                                    unfocusedLabelColor = colors.input_hint,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        } else {
                            Text(
                                text = "最近项目",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.section_title
                            )
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.size(35.dp),
                        shape = CircleShape,
                        color = if (show_search) colors.search_button_bg_active else colors.top_button_bg,
                        onClick = {
                            show_search = !show_search
                            if (!show_search) search_text = ""
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (show_search) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (show_search) "取消搜索" else "搜索",
                                tint = if (show_search) colors.search_button_active else colors.top_button_icon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (filtered_projects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card_bg)
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (search_text.isNotBlank()) "未找到相关项目" else "暂无最近项目",
                            color = colors.input_hint,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filtered_projects.forEachIndexed { index, project ->
                            recent_project_card(
                                project = project,
                                colors = colors,
                                on_click = { on_project_click(project) },
                                on_remove = { on_project_remove(project) },
                                is_top = index == 0,
                                is_bottom = index == filtered_projects.size - 1
                            )
                            if (index != filtered_projects.size - 1) Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun top_bar(
    colors: app_colors,
    on_ai_click: () -> Unit = {},
    on_terminal_click: () -> Unit = {},
    on_settings_click: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_xcode_logo),
            contentDescription = "logo",
            modifier = Modifier.size(44.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            top_bar_icon_button(
                icon = Icons.Outlined.AutoAwesome,
                description = "agent",
                bg = colors.top_button_bg,
                tint = colors.top_button_icon,
                onClick = on_ai_click
            )
            
            top_bar_icon_button(
                icon = Icons.Default.Terminal,
                description = "terminal",
                bg = colors.top_button_bg,
                tint = colors.top_button_icon,
                onClick = on_terminal_click
            )
            
            top_bar_icon_button(
                icon = Icons.Default.Settings,
                description = "settings",
                bg = colors.top_button_bg,
                tint = colors.top_button_icon,
                onClick = on_settings_click
            )
        }
    }
}

@Composable
private fun top_bar_icon_button(
    icon: ImageVector,
    description: String,
    bg: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(35.dp),
        shape = CircleShape,
        color = bg,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun action_card_item(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: app_colors,
    on_click: () -> Unit = {},
    is_top: Boolean = false,
    is_bottom: Boolean = false
) {
    val corner_radius = 12.dp
    val shape = when {
        is_top && is_bottom -> RoundedCornerShape(corner_radius)
        is_top -> RoundedCornerShape(topStart = corner_radius, topEnd = corner_radius, bottomStart = 0.dp, bottomEnd = 0.dp)
        is_bottom -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = corner_radius, bottomEnd = corner_radius)
        else -> RoundedCornerShape(0.dp)
    }
    
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    
    val background_color = when {
        is_pressed -> colors.card_pressed
        else -> colors.card_bg
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background_color)
            .clickable(
                interactionSource = interaction_source,
                indication = ripple(bounded = true)
            ) { on_click() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = colors.card_icon_bg,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.card_text_title
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.card_text_subtitle
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = colors.card_chevron,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun recent_project_card(
    project: recent_project,
    colors: app_colors,
    on_click: () -> Unit = {},
    on_remove: () -> Unit = {},
    is_top: Boolean = false,
    is_bottom: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    
    val background_color = when {
        is_pressed -> colors.card_pressed
        else -> colors.card_bg
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background_color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction_source,
                    indication = ripple(bounded = true)
                ) { on_click() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .padding(end = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.card_text_title
                )
                Text(
                    text = project.path,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.card_text_subtitle,
                    maxLines = 1
                )
                
                HorizontalDivider(
                    color = colors.input_hint.copy(alpha = 0.3f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = colors.card_text_subtitle,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CMake ${project.cmake_version}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Light,
                            color = colors.card_text_subtitle
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Android,
                            contentDescription = null,
                            tint = colors.card_text_subtitle,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NDK ${project.ndk_version}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Light,
                            color = colors.card_text_subtitle
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = colors.card_text_subtitle,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = project.last_opened,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Light,
                            color = colors.card_text_subtitle
                        )
                    }
                }
            }
        }
        
        IconButton(
            onClick = on_remove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(28.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "移除项目",
                tint = colors.input_hint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}