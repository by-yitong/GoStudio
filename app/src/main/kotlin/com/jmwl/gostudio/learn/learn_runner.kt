package com.jmwl.gostudio.learn

import android.content.Context
import com.jmwl.gostudio.toolchain.proot_manager
import com.jmwl.gostudio.toolchain.toolchain_manager
import com.jmwl.gostudio.toolchain.toolchain_runtime_provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** 判题结果。 */
data class exercise_result(
    val passed: Boolean,
    /** 是否编译通过（false = 编译错误）。 */
    val compiled: Boolean,
    val output: String,
    val message: String
)

/**
 * 交互练习执行器（移植自 CodeAssist LearnBackend 的 check 流程，Go 版）：
 * 把学习者代码写入常驻 scratch 目录（GOCACHE 复用，二次运行 ~1s），
 * 在 proot 里 `go run main.go` 捕获 stdout，再按 [exercise_check] 判题：
 * 输出比对 + 反硬编码（require_source 必须真的出现在源码里）。
 */
object learn_runner {

    /** go run 首次冷编译可能较慢，上限给足。 */
    private const val RUN_TIMEOUT_MS = 60_000L
    private const val PREPARE_TIMEOUT_MS = 120_000L

    /** scratch 目录（host 侧路径，guest 内对应 /home/gostudio/learn-scratch）。 */
    val scratch_dir: File
        get() = File(toolchain_runtime_provider.paths().gostudio_dir, "learn-scratch")

    fun is_go_available(): Boolean = toolchain_manager.installed_go() != null

    /**
     * 预热：跑一次空程序，把 go build cache 建起来，
     * 让第一次真正的练习判题只需要增量编译。
     */
    suspend fun warm_up(): Boolean = run_in_scratch(
        """
        package main

        func main() {}
        """.trimIndent()
    ) { _, _ -> true }

    /**
     * 运行学习者代码并判题。
     */
    suspend fun check(code: String, check: exercise_check): exercise_result {
        if (!is_go_available()) {
            return exercise_result(false, false, "", "Go 工具链未安装，请先到「开发工具」页安装")
        }
        var stdout = StringBuilder()
        val exit_ok = run_in_scratch(code) { line, _ -> stdout.appendLine(line) }
        if (!exit_ok) {
            val first_error = stdout.toString().lineSequence().firstOrNull { it.contains("error", ignoreCase = true) }
            return exercise_result(
                passed = false,
                compiled = false,
                output = stdout.toString().trimEnd(),
                message = "代码没有通过编译，看看下面的错误再试一次。" + (first_error?.let { "\n$it" } ?: "")
            )
        }

        val actual = normalize(stdout.toString(), check.case_sensitive)
        val output_ok = when {
            check.expected_output != null -> actual == normalize(check.expected_output, check.case_sensitive)
            check.must_contain.isNotEmpty() -> check.must_contain.all { actual.contains(normalize(it, check.case_sensitive)) }
            else -> true
        }
        val missing = missing_constructs(code, check.require_source)
        val passed = output_ok && missing.isEmpty()
        val message = when {
            passed -> "回答正确，漂亮！"
            !output_ok && check.expected_output != null ->
                "还差一点。期望输出：\n${check.expected_output}"
            !output_ok ->
                "还差一点。输出里应该包含：${check.must_contain.joinToString("、")}"
            else ->
                "输出对了，但练习要求你真的用上 ${missing.joinToString("、") { "`$it`" }} —— 不能只把答案打印出来哦。"
        }
        return exercise_result(passed, true, stdout.toString().trimEnd(), message)
    }

    /**
     * 写入 scratch 的 main.go 并在 proot 内 `go run`。
     * [on_line] 收到 stdout/stderr 的每一行（主线程回调）。
     */
    private suspend fun run_in_scratch(code: String, on_line: (String, Boolean) -> Unit): Boolean {
        val dir = scratch_dir
        return withContext(Dispatchers.IO) {
            dir.mkdirs()
            File(dir, "main.go").writeText(code)
            val guest_dir = toolchain_runtime_provider.paths().host_to_guest_path(dir.absolutePath)
            val environment = toolchain_manager.project_environment(dir.absolutePath).environment
            withTimeoutOrNull(RUN_TIMEOUT_MS) {
                proot_manager.execute_command_with_environment(
                    command = "go run main.go",
                    working_dir = guest_dir,
                    extra_environment = environment,
                    on_log = { line -> on_line(line, false) }
                )
            } ?: run {
                on_line("运行超时（${RUN_TIMEOUT_MS / 1000}s），检查是否有死循环或阻塞", true)
                false
            }
        }
    }

    private fun normalize(s: String, case_sensitive: Boolean): String {
        val t = s.replace("\r\n", "\n").lines().joinToString("\n") { it.trimEnd() }.trim()
        return if (case_sensitive) t else t.lowercase()
    }
}

/**
 * 反硬编码检查（移植自 CodeAssist missingConstructs）：
 * 源码剥掉注释与字符串内容、去空白后，required 里的每个构造都必须出现，
 * 否则学习者可以用 `fmt.Println("5")` 骗过输出比对。
 */
internal fun missing_constructs(code: String, required: List<String>): List<String> {
    if (required.isEmpty()) return emptyList()
    val stripped = strip_comments_and_strings(code).filterNot { it.isWhitespace() }
    return required.filter { pat -> !stripped.contains(pat.filterNot { it.isWhitespace() }) }
}

/** 去掉行注释、块注释以及字符串字面量的内容（保留引号本身）。 */
internal fun strip_comments_and_strings(code: String): String {
    val sb = StringBuilder(code.length)
    var i = 0
    val n = code.length
    while (i < n) {
        val c = code[i]
        when {
            c == '/' && i + 1 < n && code[i + 1] == '/' -> {
                i += 2
                while (i < n && code[i] != '\n') i++
            }
            c == '/' && i + 1 < n && code[i + 1] == '*' -> {
                i += 2
                while (i < n && !(code[i] == '*' && i + 1 < n && code[i + 1] == '/')) i++
                i = (i + 2).coerceAtMost(n)
            }
            c == '"' || c == '`' || c == '\'' -> {
                sb.append(c)
                i++
                while (i < n && code[i] != c && code[i] != '\n') {
                    if (code[i] == '\\') i++
                    i++
                }
                if (i < n && code[i] == c) {
                    sb.append(c)
                    i++
                }
            }
            else -> {
                sb.append(c)
                i++
            }
        }
    }
    return sb.toString()
}
