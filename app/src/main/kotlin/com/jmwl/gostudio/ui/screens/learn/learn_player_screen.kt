package com.jmwl.gostudio.ui.screens.learn

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jmwl.gostudio.editor.settings.apply_editor_colors
import com.jmwl.gostudio.editor.settings.apply_editor_behavior_settings
import com.jmwl.gostudio.editor.settings.load_editor_settings
import com.jmwl.gostudio.ui.components.entrance_pop
import com.jmwl.gostudio.ui.components.entrance_slide_up
import com.jmwl.gostudio.gostudio_application
import com.jmwl.gostudio.learn.exercise_result
import com.jmwl.gostudio.learn.learn_content
import com.jmwl.gostudio.learn.learn_progress
import com.jmwl.gostudio.learn.normalize_lesson_code
import com.jmwl.gostudio.learn.learn_runner
import com.jmwl.gostudio.learn.learn_step
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.theme.motion
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 课程步骤播放器（移植自 CodeAssist LessonPlayerScreen，Go 版）：
 * 顶栏进度 + 方向性步骤切换动画 + 三种步骤（概念 / 交互练习 / 测验）。
 * 下一步按步骤类型门控：概念恒可、练习判题通过（或看了答案）、测验答对。
 */
@Composable
fun learn_player_screen(
    lesson_id: String,
    initial_step: Int,
    on_exit: () -> Unit
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val found = remember(lesson_id) { learn_content.find_lesson(lesson_id) }
    val lesson = found?.second ?: run { on_exit(); return }
    val steps = lesson.steps

    var step_index by remember { mutableStateOf(initial_step.coerceIn(0, (steps.size - 1).coerceAtLeast(0))) }
    // 各交互步骤是否已解锁下一步（判题通过或查看答案）
    val solved_steps = remember { mutableStateMapOf<String, Boolean>() }
    // 各测验是否已答对
    val answered_quizzes = remember { mutableStateMapOf<String, Boolean>() }

    // 进入/切步都记录断点
    LaunchedEffect(lesson_id, step_index) {
        learn_progress.record_resume(context, lesson_id, step_index)
    }
    // 播放器打开即后台预热 scratch（编译缓存），首次判题不至于冷启动
    LaunchedEffect(Unit) {
        if (!warmed_up && learn_runner.is_go_available()) {
            warmed_up = true
            learn_runner.warm_up()
        }
    }

    val current_step = steps.getOrNull(step_index)
    val is_last = step_index >= steps.size - 1
    val can_advance = when (val step = current_step) {
        is learn_step.concept -> true
        is learn_step.interactive -> solved_steps[step.id] == true
        is learn_step.quiz -> answered_quizzes[step.id] == true
        null -> false
    }
    val completed = learn_progress.completed_steps(context, lesson_id)
    val progress = if (steps.isEmpty()) 0f else completed.size.toFloat() / steps.size

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.editor_bg)
            .statusBarsPadding()
    ) {
        // 顶栏：关闭 + 课名 + 步骤计数 + 进度条
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = on_exit) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.top_button_icon)
            }
            Text(
                text = lesson.title,
                color = colors.title_large,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${step_index + 1}/${steps.size}",
                color = colors.subtitle,
                fontSize = 12.sp
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = colors.title_highlight,
            trackColor = colors.card_icon_bg
        )

        // 步骤内容：方向性滑动（前进从右进，后退从左进）
        AnimatedContent(
            targetState = step_index,
            transitionSpec = {
                val forward = targetState >= initialState
                val enter = fadeIn(tween(motion.BASE, easing = motion.soft)) +
                    slideInHorizontally(tween(motion.BASE, easing = motion.quiet)) { w -> (if (forward) w else -w) / 5 }
                val exit = fadeOut(tween(motion.FAST)) +
                    slideOutHorizontally(tween(motion.BASE, easing = motion.quiet)) { w -> (if (forward) -w else w) / 5 }
                enter togetherWith exit
            },
            label = "lessonStep",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { index ->
            when (val step = steps.getOrNull(index)) {
                is learn_step.concept -> concept_step(step)
                is learn_step.interactive -> interactive_step(
                    step = step,
                    on_solved = { solved_steps[step.id] = true }
                )
                is learn_step.quiz -> quiz_step(step) { correct ->
                    answered_quizzes[step.id] = correct
                }
                null -> {}
            }
        }

        // 底部：上一步 / 下一步（完成）
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step_index > 0) {
                player_button("上一步", primary = false) { step_index-- }
            }
            Spacer(Modifier.weight(1f))
            player_button(
                text = if (is_last) "完成" else "下一步",
                primary = true,
                enabled = can_advance
            ) {
                current_step?.let { step ->
                    learn_progress.mark_step_complete(context, lesson_id, step.id)
                    if (step is learn_step.interactive) solved_steps[step.id] = true
                }
                if (is_last) on_exit() else step_index++
            }
        }
    }
}

/** 进程级预热标记（一次安装生命周期内只预热一次）。 */
private var warmed_up = false

// ---- 概念步骤 ----

@Composable
private fun concept_step(step: learn_step.concept) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(step.title, color = app_theme_provider.colors.title_large, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        learn_blocks(step.blocks)
        Spacer(Modifier.height(24.dp))
    }
}

// ---- 交互练习步骤 ----

@Composable
private fun interactive_step(
    step: learn_step.interactive,
    on_solved: () -> Unit
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember(step.id) { mutableStateOf<exercise_result?>(null) }
    var running by remember(step.id) { mutableStateOf(false) }
    var hints_shown by remember(step.id) { mutableStateOf(0) }
    var show_solution by remember(step.id) { mutableStateOf(false) }

    // 内嵌练习编辑器：每个交互步骤独立创建，离开时释放
    val step_editor = remember(step.id) { create_lesson_editor(context, step.starter_code) }
    DisposableEffect(step.id) {
        onDispose {
            runCatching { step_editor.release() }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(step.title, color = colors.title_large, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (step.blocks.isNotEmpty()) learn_blocks(step.blocks)

        if (!learn_runner.is_go_available()) {
            callout_box(colors.warning, "Go 工具链未安装：请先到主界面「开发工具」安装 Go，再回来做练习。")
        } else {
            // 练习编辑器（固定高度，内部滚动）
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.editor_bg)
                    .border(1.dp, colors.editor_divider, RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { step_editor },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 操作行：提示 / 答案 / 运行并检查
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (step.hints.isNotEmpty() && hints_shown < step.hints.size) {
                    text_action("需要提示 (${hints_shown + 1}/${step.hints.size})") { hints_shown++ }
                }
                if (step.solution.isNotBlank() && !show_solution) {
                    text_action("查看答案") { show_solution = true }
                }
                Spacer(Modifier.weight(1f))
                player_button(
                    text = if (running) "运行中..." else "运行并检查",
                    primary = true,
                    enabled = !running
                ) {
                    if (!running) {
                        running = true
                        val code = step_editor.text.toString()
                        scope.launch {
                            val r = try {
                                learn_runner.check(code, step.check)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                exercise_result(false, false, "", "检查失败: ${e.message}")
                            }
                            result = r
                            running = false
                            if (r.passed) on_solved()
                        }
                    }
                }
            }

            // 判题结果面板（key 重挂载重放入场动画）
            result?.let { r ->
                key(r) { result_panel(r, Modifier.entrance_pop()) }
            }

            // 逐条揭示的提示卡
            for (i in 0 until hints_shown.coerceAtMost(step.hints.size)) {
                hint_card(i + 1, step.hints[i])
            }

            // 答案（可一键复制进编辑器）
            if (show_solution) {
                Column(
                    Modifier.entrance_slide_up(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("参考答案", color = colors.title_large, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    learn_code_sample(step.solution)
                    text_action("复制到编辑器") {
                        step_editor.setText(normalize_lesson_code(step.solution))
                        on_solved()
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun create_lesson_editor(context: Context, starter_code: String): CodeEditor {
    return CodeEditor(context).apply {
        val language = runCatching { learn_go_language() }
            .getOrNull()
            ?: gostudio_application.instance.create_textmate_language("main.go")
        language?.let(::setEditorLanguage)
        apply_editor_colors(context, this)
        setText(normalize_lesson_code(starter_code))
        apply_editor_behavior_settings(
            context = context,
            editor = this,
            settings = load_editor_settings(context),
            file_path = "main.go",
            current_language = language
        )
        setUndoEnabled(true)
    }
}

@Composable
private fun result_panel(result: exercise_result, modifier: Modifier = Modifier) {
    val colors = app_theme_provider.colors
    val (tint, icon_desc) = when {
        result.passed -> colors.success to "通过"
        !result.compiled -> colors.danger to "编译失败"
        else -> colors.warning to "未通过"
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$icon_desc · ${result.message}",
            color = colors.card_text_title,
            fontSize = 14.sp
        )
        if (result.output.isNotBlank()) {
            Text("程序输出", color = colors.subtitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            SelectionContainer {
                Text(
                    text = result.output,
                    color = colors.editor_text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun hint_card(number: Int, hint: String) {
    val colors = app_theme_provider.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.info_bg.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "提示 $number",
            color = colors.info,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = inline_markup(hint),
            color = colors.card_text_subtitle,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun callout_box(tint: Color, message: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Text(message, color = tint, fontSize = 13.sp)
    }
}

// ---- 测验步骤 ----

@Composable
private fun quiz_step(step: learn_step.quiz, on_answered: (Boolean) -> Unit) {
    val colors = app_theme_provider.colors
    var selected by remember(step.id) { mutableStateOf(-1) }
    var submitted by remember(step.id) { mutableStateOf(false) }
    val correct = selected == step.correct_index

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(step.title, color = colors.title_large, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(step.prompt, color = colors.card_text_subtitle, fontSize = 15.sp)

        step.options.forEachIndexed { i, option ->
            val reveal_correct = submitted && i == step.correct_index
            val reveal_wrong = submitted && selected == i && i != step.correct_index
            val border_color = when {
                reveal_correct -> colors.success
                reveal_wrong -> colors.danger
                selected == i -> colors.title_highlight
                else -> colors.input_border
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            reveal_correct -> colors.success_bg.copy(alpha = 0.4f)
                            reveal_wrong -> colors.danger_bg.copy(alpha = 0.4f)
                            selected == i -> colors.card_icon_bg.copy(alpha = 0.5f)
                            else -> colors.card_bg
                        }
                    )
                    .border(1.dp, border_color, RoundedCornerShape(12.dp))
                    .clickable(enabled = !submitted || !correct) {
                        if (!(submitted && correct)) {
                            selected = i
                            submitted = false
                        }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = when {
                        reveal_wrong -> Icons.Default.Close
                        reveal_correct || selected == i -> Icons.Default.Check
                        else -> Icons.Default.Check
                    },
                    contentDescription = null,
                    tint = when {
                        reveal_correct -> colors.success
                        reveal_wrong -> colors.danger
                        selected == i -> colors.title_highlight
                        else -> colors.input_border
                    },
                    modifier = Modifier.size(16.dp)
                )
                Text(option, color = colors.card_text_title, fontSize = 14.sp)
            }
        }

        if (submitted) {
            Text(
                text = if (correct) "回答正确！" else "再想想～",
                color = if (correct) colors.success else colors.warning,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (step.explanation.isNotBlank() && correct) {
                Text(
                    text = inline_markup(step.explanation),
                    color = colors.subtitle,
                    fontSize = 13.sp
                )
            }
            if (!correct) {
                player_button("重新作答", primary = false) { submitted = false }
            }
        } else {
            player_button("提交", primary = true, enabled = selected >= 0) {
                submitted = true
                on_answered(selected == step.correct_index)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---- 通用按钮 ----

@Composable
private fun player_button(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    on_click: () -> Unit
) {
    val colors = app_theme_provider.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !primary -> colors.top_button_bg
                    enabled -> colors.title_highlight
                    else -> colors.card_icon_bg
                }
            )
            .clickable(interaction, indication = null, enabled = enabled, onClick = on_click)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = text,
            color = if (primary && enabled) Color.White else colors.card_text_title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun text_action(text: String, on_click: () -> Unit) {
    val colors = app_theme_provider.colors
    Text(
        text = text,
        color = colors.title_highlight,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = on_click)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}
