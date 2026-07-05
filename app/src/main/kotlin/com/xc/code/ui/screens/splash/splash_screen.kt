package com.xc.code.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun splash_content(
    on_ready: () -> Unit
) {
    var current_progress by remember { mutableFloatStateOf(0f) }
    var tip_index by remember { mutableIntStateOf(0) }
    var load_finished by remember { mutableStateOf(false) }

    val tips = listOf(
        "随时随地写代码",
        "轻量级代码编辑器",
        "支持 C/C++ 编译",
        "内置 Linux 终端"
    )

    val infinite_transition = rememberInfiniteTransition()
    val scale by infinite_transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val alpha by infinite_transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            tip_index = (tip_index + 1) % tips.size
        }
    }

    LaunchedEffect(Unit) {
        current_progress = 0f
        load_finished = false

        while (current_progress < 1f) {
            delay(16)
            current_progress += 0.008f
            if (current_progress >= 1f) {
                current_progress = 1f
                load_finished = true
            }
        }
    }

    LaunchedEffect(load_finished) {
        if (load_finished) {
            delay(300)
            on_ready()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFF0F0F0))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "X",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = 4.sp,
                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Code",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = tips[tip_index],
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally() togetherWith
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
                },
                label = "tip"
            ) { tip ->
                Text(text = tip, fontSize = 13.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { current_progress },
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color.Black,
                trackColor = Color.Black.copy(alpha = 0.2f)
            )
        }
    }
}
