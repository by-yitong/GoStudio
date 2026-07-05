package com.xc.code.ui.screens.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.xc.code.ui.theme.app_theme_provider

@Composable
fun empty_editor_placeholder(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = colors.editor_hint,
            fontSize = 14.sp
        )
    }
}
