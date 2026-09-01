package com.jmwl.gostudio.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * 全局动效令牌（移植自 CodeAssist 的 Motion）。
 * 所有页面过渡、抽屉、弹层的时长与缓动统一从这里取值，
 * 避免各处硬编码 tween 导致节奏不一致。
 */
object motion {
    /** 平静减速：页面级滑动/位移的默认缓动。 */
    val quiet: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** 液态回弹：小元素入场（灯泡、结果卡）用。 */
    val springy: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** 柔和进出：淡入淡出的默认缓动。 */
    val soft: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** 时长（毫秒）：快（微交互）/ 基准（页面、抽屉）/ 慢（大位移收尾）。 */
    const val FAST = 160
    const val BASE = 260
    const val SLOW = 420

    /** 按压反馈缩放比例。 */
    const val PRESS_SCALE = 0.96f
}
