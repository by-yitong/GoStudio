package com.xc.code.ui.screens.main

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
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
import com.xc.code.BuildConfig
import com.xc.code.R
import com.xc.code.ui.theme.app_colors
import com.xc.code.ui.theme.app_theme_provider

@Composable
fun main_about_screen(
    on_back: () -> Unit
) {
    val colors = app_theme_provider.colors
    val version_text = "v${BuildConfig.VERSION_NAME}-arm64-v8a"
    val android_text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = colors.top_button_bg,
            onClick = on_back
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.top_button_icon,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(34.dp))

        Text(
            text = "关于",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = colors.title_highlight
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "设置",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = colors.subtitle
        )

        Spacer(modifier = Modifier.height(42.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(18.dp),
                color = colors.card_bg.copy(alpha = 0.58f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_xcode_logo),
                        contentDescription = "XCode",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "XCode",
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.card_text_title,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.card_bg.copy(alpha = 0.78f)
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
                text = "by 小陈在肝码",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.title_highlight
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        about_section_title("设备信息", colors)
        about_info_group(
            rows = listOf(
                "设备" to Build.BRAND + " " + Build.MODEL,
                "型号" to Build.DEVICE,
                "系统" to android_text
            ),
            colors = colors
        )

        Spacer(modifier = Modifier.height(24.dp))

        about_section_title("链接", colors)
        about_link_group(
            colors = colors,
            rows = listOf(
                about_link_item(Icons.Default.Code, "源代码", "https://github.com", "https://github.com"),
                about_link_item(Icons.Default.Forum, "官方群", "https://github.com", "https://github.com"),
                about_link_item(Icons.Default.Campaign, "官方频道", "https://github.com", "https://github.com")
            )
        )

        Spacer(modifier = Modifier.height(38.dp))
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
        color = colors.title_highlight,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun about_info_group(
    rows: List<Pair<String, String>>,
    colors: app_colors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        rows.forEachIndexed { index, row ->
            about_info_row(
                label = row.first,
                value = row.second,
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
private fun about_info_row(
    label: String,
    value: String,
    colors: app_colors,
    shape: RoundedCornerShape
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card_bg.copy(alpha = 0.72f))
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = colors.card_text_subtitle,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.card_text_title,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.25f)
        )
    }
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
            .clip(RoundedCornerShape(16.dp))
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
            .background(colors.card_bg.copy(alpha = 0.72f))
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
        is_top && is_bottom -> RoundedCornerShape(16.dp)
        is_top -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        is_bottom -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(0.dp)
    }
}
