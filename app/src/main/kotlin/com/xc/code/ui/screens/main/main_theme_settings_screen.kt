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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun main_theme_settings_screen(
    current_theme: app_theme_type,
    scale_value: Float,
    on_theme_change: (app_theme_type) -> Unit,
    on_scale_change: (Float) -> Unit,
    on_back: () -> Unit
) {
    val colors = app_theme_provider.colors
    var expanded by remember { mutableStateOf(false) }
    
    val theme_icon = when (current_theme) {
        app_theme_type.LIGHT -> Icons.Default.WbSunny
        app_theme_type.DARK -> Icons.Default.NightsStay
        app_theme_type.SYSTEM -> Icons.Default.Smartphone
    }
    
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
                    text = "主题",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "设置",
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
                    text = "外观",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card_bg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        val interaction_source = remember { MutableInteractionSource() }
                        val is_pressed by interaction_source.collectIsPressedAsState()
                        val header_bg = if (is_pressed) colors.card_pressed else colors.card_bg
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(header_bg)
                                .clickable(
                                    interactionSource = interaction_source,
                                    indication = ripple(bounded = true)
                                ) { expanded = !expanded }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    theme_icon,
                                    contentDescription = "主题",
                                    tint = colors.card_icon_bg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "主题模式",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.card_text_title
                                )
                                Text(
                                    text = when (current_theme) {
                                        app_theme_type.DARK -> "深色模式"
                                        app_theme_type.LIGHT -> "浅色模式"
                                        app_theme_type.SYSTEM -> "跟随系统"
                                    },
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    color = colors.card_text_subtitle
                                )
                            }
                            
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "收起" else "展开",
                                tint = colors.card_chevron,
                                modifier = Modifier.size(20.dp).rotate(if (expanded) 180f else 0f)
                            )
                        }
                        
                        if (expanded) {
                            Column {
                                theme_option_item(
                                    title = "浅色模式",
                                    is_selected = current_theme == app_theme_type.LIGHT,
                                    colors = colors,
                                    onClick = {
                                        on_theme_change(app_theme_type.LIGHT)
                                        expanded = false
                                    }
                                )
                                
                                theme_option_item(
                                    title = "深色模式",
                                    is_selected = current_theme == app_theme_type.DARK,
                                    colors = colors,
                                    onClick = {
                                        on_theme_change(app_theme_type.DARK)
                                        expanded = false
                                    }
                                )
                                
                                theme_option_item(
                                    title = "跟随系统",
                                    is_selected = current_theme == app_theme_type.SYSTEM,
                                    colors = colors,
                                    onClick = {
                                        on_theme_change(app_theme_type.SYSTEM)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(1.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card_bg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.card_icon_bg.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = "应用缩放",
                                    tint = colors.card_icon_bg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "应用缩放",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.card_text_title
                                )
                                Text(
                                    text = "当前缩放: ${String.format("%.2f", scale_value)}x",
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    color = colors.card_text_subtitle
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Slider(
                                value = scale_value,
                                onValueChange = { 
                                    on_scale_change(it)
                                },
                                valueRange = 0.50f..1.50f,
                                steps = 19,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.card_icon_bg,
                                    activeTrackColor = colors.card_icon_bg,
                                    inactiveTrackColor = colors.card_text_subtitle.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun theme_option_item(
    title: String,
    is_selected: Boolean,
    colors: app_colors,
    onClick: () -> Unit
) {
    val interaction_source = remember { MutableInteractionSource() }
    val is_pressed by interaction_source.collectIsPressedAsState()
    
    val background_color = when {
        is_pressed -> colors.card_pressed
        else -> colors.card_bg
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background_color)
            .clickable(
                interactionSource = interaction_source,
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(44.dp))
        
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            color = if (is_selected) colors.title_highlight else colors.card_text_title,
            modifier = Modifier.weight(1f)
        )
        
        if (is_selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选中",
                tint = colors.title_highlight,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}