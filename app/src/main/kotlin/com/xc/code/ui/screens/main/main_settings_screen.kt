package com.xc.code.ui.screens.main

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun main_settings_screen(
    on_back: () -> Unit,
    on_theme_click: () -> Unit = {},
    on_editor_click: () -> Unit = {},
    on_about_click: () -> Unit = {}
) {
    val colors = app_theme_provider.colors
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { padding_values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding_values)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(35.dp),
                    shape = CircleShape,
                    color = colors.top_button_bg,
                    onClick = on_back
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = colors.top_button_icon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.size(35.dp))
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = "自定义你的",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_large
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "使用体验",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "应用设置与偏好",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.subtitle
                )
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = "设置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    main_settings_card_item(
                        icon = Icons.Default.BrightnessMedium,
                        title = "主题",
                        subtitle = "切换深色/浅色模式",
                        colors = colors,
                        onClick = on_theme_click,
                        is_top = true,
                        is_bottom = false
                    )
                    
                    Spacer(modifier = Modifier.height(1.dp))
                    
                    main_settings_card_item(
                        icon = Icons.Default.Code,
                        title = "编辑器",
                        subtitle = "字体大小、缩进、自动保存等",
                        colors = colors,
                        onClick = on_editor_click,
                        is_top = false,
                        is_bottom = false
                    )
                    
                    Spacer(modifier = Modifier.height(1.dp))
                    
                    main_settings_card_item(
                        icon = Icons.Default.Info,
                        title = "关于",
                        subtitle = "版本信息、开源许可",
                        colors = colors,
                        onClick = on_about_click,
                        is_top = false,
                        is_bottom = true
                    )
                }
            }
        }
    }
}

@Composable
private fun main_settings_card_item(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: app_colors,
    onClick: () -> Unit,
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
            ) { onClick() }
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
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
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
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = colors.card_chevron,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}