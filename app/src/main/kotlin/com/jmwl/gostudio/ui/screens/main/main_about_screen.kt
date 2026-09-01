package com.jmwl.gostudio.ui.screens.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.BuildConfig
import com.jmwl.gostudio.R
import com.jmwl.gostudio.ui.components.sub_page_top_bar
import com.jmwl.gostudio.ui.theme.app_colors
import com.jmwl.gostudio.ui.theme.app_theme_provider

@Composable
fun main_about_screen(
    on_back: () -> Unit
) {
    val colors = app_theme_provider.colors
    val version_text = "v${BuildConfig.VERSION_NAME}-arm64-v8a"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        sub_page_top_bar("关于", on_back)

        Column(modifier = Modifier.padding(horizontal = 30.dp)) {
            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.card_bg
                ) {
                    // 软件启动图标：背景铺满 + 前景放大 1.35×（抵消前景 PNG 自带安全边距），超出部分由 Surface 圆角裁掉
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_background),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize()
                        )
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = "GoStudio",
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "GoStudio",
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.card_text_title,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.card_bg
                ) {
                    Text(
                        text = version_text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.title_highlight,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "by yitong",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            about_section_title("链接", colors)
            about_link_group(
                colors = colors,
                rows = listOf(
                    about_link_item(
                        Icons.Default.Code,
                        "源代码",
                        "github.com/by-yitong/GoStudio",
                        "https://github.com/by-yitong/GoStudio"
                    ),
                    about_link_item(
                        Icons.Default.BugReport,
                        "问题反馈",
                        "通过 GitHub Issues 提交问题",
                        "https://github.com/by-yitong/GoStudio/issues"
                    ),
                    about_link_item(
                        Icons.Default.SystemUpdateAlt,
                        "更新发布",
                        "GitHub Releases 下载最新版本",
                        "https://github.com/by-yitong/GoStudio/releases"
                    )
                )
            )

            Spacer(modifier = Modifier.height(38.dp))
        }
    }
}

@Composable
private fun about_section_title(
    title: String,
    colors: app_colors
) {
    Text(
        text = title,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
       fontWeight = FontWeight.Bold,
        color = colors.section_title,
       modifier = Modifier
           .fillMaxWidth()
           .padding(bottom = 12.dp),
        textAlign = TextAlign.Start
   )
}

private data class about_link_item(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val url: String
)

@Composable
private fun about_link_group(
    colors: app_colors,
    rows: List<about_link_item>
) {
    Column(
        modifier = Modifier
           .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
   ) {
       rows.forEachIndexed { index, row ->
           about_link_row(
                item = row,
                colors = colors,
                shape = about_group_item_shape(
                    is_top = index == 0,
                    is_bottom = index == rows.lastIndex
                )
            )
            if (index < rows.lastIndex) {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun about_link_row(
    item: about_link_item,
    colors: app_colors,
    shape: RoundedCornerShape
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
           .clip(shape)
            .background(colors.card_bg)
           .clickable {
               runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                }
            }
            .heightIn(min = 54.dp)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card_icon_bg.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = colors.card_icon_bg,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.card_text_title
            )
            Text(
                text = item.subtitle,
                fontSize = 10.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Light,
                color = colors.title_highlight
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = colors.card_text_subtitle.copy(alpha = 0.36f),
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun about_group_item_shape(
    is_top: Boolean,
    is_bottom: Boolean
): RoundedCornerShape {
   return when {
        is_top && is_bottom -> RoundedCornerShape(12.dp)
        is_top -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        is_bottom -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
       else -> RoundedCornerShape(0.dp)
   }
}
