package com.jmwl.gostudio.learn

import android.content.Context

/**
 * 学习进度持久化（SharedPreferences，移植自 CodeAssist LearnBackend 的进度部分）：
 * - learn.completed.<lesson_id> = 已完成 step id 集合（逗号分隔）
 * - learn.resume.lesson / learn.resume.step = 断点
 */
object learn_progress {

    private const val PREFS = "learn_progress"
    private const val KEY_RESUME_LESSON = "learn.resume.lesson"
    private const val KEY_RESUME_STEP = "learn.resume.step"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 某课已完成的 step id 集合。 */
    fun completed_steps(context: Context, lesson_id: String): Set<String> =
        prefs(context).getString("learn.completed.$lesson_id", null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()

    fun mark_step_complete(context: Context, lesson_id: String, step_id: String) {
        val current = completed_steps(context, lesson_id).toMutableSet()
        if (current.add(step_id)) {
            prefs(context).edit()
                .putString("learn.completed.$lesson_id", current.joinToString(","))
                .apply()
        }
    }

    /** 记录断点（打开某课时调用）。 */
    fun record_resume(context: Context, lesson_id: String, step_index: Int) {
        prefs(context).edit()
            .putString(KEY_RESUME_LESSON, lesson_id)
            .putInt(KEY_RESUME_STEP, step_index)
            .apply()
    }

    data class resume_point(
        val lesson_id: String,
        val step_index: Int,
        val lesson_title: String,
        val track_title: String,
        /** 该课完成度 0..1。 */
        val fraction: Float
    )

    fun resume(context: Context): resume_point? {
        val lesson_id = prefs(context).getString(KEY_RESUME_LESSON, null) ?: return null
        val found = learn_content.find_lesson(lesson_id) ?: return null
        val (track, lesson) = found
        val last = (lesson.steps.size - 1).coerceAtLeast(0)
        val step = prefs(context).getInt(KEY_RESUME_STEP, 0).coerceIn(0, last)
        val done = completed_steps(context, lesson_id).count { id -> lesson.steps.any { it.id == id } }
        val fraction = if (lesson.steps.isEmpty()) 0f else done.toFloat() / lesson.steps.size
        return resume_point(lesson_id, step, lesson.title, track.title, fraction)
    }
}
