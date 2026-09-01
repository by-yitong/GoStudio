package com.jmwl.gostudio.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider

/**
 * 二级页统一顶栏：标题与返回键同行（标题不再压在返回按钮下方）。
 * 透明容器，自动处理状态栏 inset，供各二级页面 Scaffold 的 topBar 使用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun sub_page_top_bar(
    title: String,
    on_back: () -> Unit
) {
    val colors = app_theme_provider.colors
    TopAppBar(
        title = {
            Text(
                title,
                color = colors.title_large,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = on_back) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.top_button_icon
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}
