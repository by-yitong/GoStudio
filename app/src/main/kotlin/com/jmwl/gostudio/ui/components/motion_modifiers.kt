package com.jmwl.gostudio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.jmwl.gostudio.ui.theme.motion

/**
 * 按压缩放反馈（0.96 倍 + 淡入淡出），配合无 ripple 的 clickable 使用。
 * 移植自 CodeAssist 的 pressScale。
 */
fun Modifier.press_scale(interaction: MutableInteractionSource): Modifier = Modifier.composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) motion.PRESS_SCALE else 1f,
        animationSpec = tween(motion.FAST, easing = motion.soft),
        label = "press_scale"
    )
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** 一次性入场：从下方 48px 滑入 + 淡入；[delay_ms] 支持列表逐项错峰（stagger）。 */
fun Modifier.entrance_slide_up(delay_ms: Int = 0): Modifier = Modifier.composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delay_ms > 0) kotlinx.coroutines.delay(delay_ms.toLong())
        progress.animateTo(1f, tween(motion.BASE, easing = motion.quiet))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 48f
    }
}

/** 一次性入场：弹簧放大（0.85 → 1）。用于结果面板、灯泡等需要吸引注意的元素。 */
fun Modifier.entrance_pop(): Modifier = Modifier.composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(motion.BASE, easing = motion.springy))
    }
    graphicsLayer {
        val s = 0.85f + 0.15f * progress.value
        scaleX = s
        scaleY = s
        alpha = progress.value
    }
}
