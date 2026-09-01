package com.jmwl.gostudio.toolchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** git 变更条目：path 相对项目根。 */
data class git_change_entry(
    val path: String,
    /** 已暂存状态码（空格 = 无）。 */
    val staged_status: Char,
    /** 工作区状态码（空格 = 无）。'?' = 未跟踪。 */
    val worktree_status: Char
) {
    val is_untracked: Boolean get() = worktree_status == '?'
    val is_staged: Boolean get() = staged_status != ' ' && staged_status != '?'
    val is_modified: Boolean get() = worktree_status != ' ' && worktree_status != '?'
}

data class git_status_result(
    val branch: String,
    val entries: List<git_change_entry>,
    val ahead_count: Int = 0,
    val behind_count: Int = 0
)

/**
 * 源代码管理：在 proot 内执行 git 命令（status/diff/add/commit/init）。
 * 全部走 --porcelain 机读格式 + core.quotepath=false，避免中文路径被转义。
 */
object git_manager {

    private const val COMMAND_TIMEOUT_MS = 15_000L
    private const val LOG_TAG = "git_manager"

    /** git 是否可用（rootfs 内已安装）。 */
    fun is_git_available(): Boolean = toolchain_manager.is_git_installed()

    /**
     * proot_shell_runner 会给 stdout/stderr 加 “[OUT] -- ” / “[ERR] -- ” 前缀。
     * git 面板需要原始 git 输出；这里统一去掉前缀，避免把仓库误判成“非 git 仓库”。
     */
    private fun normalize_output(output: String): String = output
        .lines()
        .mapNotNull { raw ->
            val line = raw.trimStart('\r')
            when {
                line.startsWith("[OUT] -- ") -> line.removePrefix("[OUT] -- ")
                line.startsWith("[ERR] -- ") -> line.removePrefix("[ERR] -- ")
                line.startsWith("[OUT] ") -> line.removePrefix("[OUT] ")
                line.startsWith("[ERR] ") -> line.removePrefix("[ERR] ")
                else -> line
            }.takeUnless { it.isBlank() || it == "Welcome To GoStudio Terminal" }
        }
        .joinToString("\n")
        .trim()

    private fun log_command(args: String, ok: Boolean, output: String) {
        val summary = output.lineSequence().take(20).joinToString("\n")
        if (ok) {
            android.util.Log.i(LOG_TAG, "git $args => exit 0\n$summary")
        } else {
            android.util.Log.w(LOG_TAG, "git $args => failed\n$summary")
        }
    }

    private suspend fun run_git(
        project_root: String,
        args: String,
        timeout_ms: Long = COMMAND_TIMEOUT_MS
    ): Pair<Boolean, String> {
        val output = StringBuilder()
        val environment = toolchain_manager.project_environment(project_root).environment
        val command = "git -c core.quotepath=false -c color.ui=never $args"
        val exit_ok = withTimeoutOrNull(timeout_ms) {
            proot_manager.execute_command_with_environment(
                command = command,
                working_dir = project_root,
                extra_environment = environment,
                on_log = { line -> output.appendLine(line) }
            )
        } ?: false
        val clean_output = normalize_output(output.toString())
        log_command(args, exit_ok, clean_output)
        return exit_ok to clean_output
    }

    /** 项目根是否已是 git 仓库。 */
    suspend fun is_repository(project_root: String): Boolean {
        val (ok, out) = run_git(project_root, "rev-parse --is-inside-work-tree", timeout_ms = 8_000L)
        return ok && out.lineSequence().any { it.trim().equals("true", ignoreCase = true) }
    }

    suspend fun init_repository(project_root: String): Boolean {
        val (init_ok, init_out) = run_git(project_root, "init")
        if (!init_ok) return false
        return ensure_local_identity(project_root, init_out)
    }

    /** 本地仓库没有身份配置时提交会失败；默认补一个仅作用于当前仓库的身份。 */
    private suspend fun ensure_local_identity(
        project_root: String,
        previous_output: String = ""
    ): Boolean {
        val (name_ok, name) = run_git(project_root, "config --default ${shell_quote("")} --get user.name", timeout_ms = 8_000L)
        val (email_ok, email) = run_git(project_root, "config --default ${shell_quote("")} --get user.email", timeout_ms = 8_000L)
        if (name_ok && name.isNotBlank() && email_ok && email.isNotBlank()) return true

        if (!name_ok || name.isBlank()) {
            val ok = run_git(project_root, "config user.name GoStudio", timeout_ms = 8_000L).first
            if (!ok) return false
        }
        if (!email_ok || email.isBlank()) {
            val ok = run_git(project_root, "config user.email gostudio@local", timeout_ms = 8_000L).first
            if (!ok) return false
        }
        android.util.Log.d(LOG_TAG, "git identity configured for $project_root; init=$previous_output")
        return true
    }

    /** 本地分支名，当前分支排在首位。 */
    suspend fun branches(project_root: String): List<String> {
        val (ok, out) = run_git(project_root, "for-each-ref --format=${shell_quote("%(refname:short)")} refs/heads")
        return if (!ok) {
            emptyList()
        } else {
            out.lines().map { it.trim() }.filter { it.isNotEmpty() && it != "Welcome To GoStudio Terminal" }
        }
    }

    suspend fun checkout(project_root: String, branch: String): Boolean =
        run_git(project_root, "checkout ${shell_quote(branch)}").first

    suspend fun create_branch(project_root: String, branch: String): Boolean =
        branch.isNotBlank() && run_git(project_root, "checkout -b ${shell_quote(branch.trim())}").first

    suspend fun pull(project_root: String): Boolean =
        run_git(project_root, "pull --ff-only", timeout_ms = 60_000L).first

    suspend fun push(project_root: String): Boolean =
        run_git(project_root, "push", timeout_ms = 60_000L).first

    /** 解析 `git status --porcelain=v1 -b`。 */
    suspend fun status(project_root: String): git_status_result {
        val (ok, out) = run_git(project_root, "status --porcelain=v1 -b")
        if (!ok) return git_status_result("", emptyList())
        var branch = ""
        var ahead_count = 0
        var behind_count = 0
        val entries = mutableListOf<git_change_entry>()
        out.lines().forEach { raw ->
            val line = raw.trimEnd('\r')
            if (line.isBlank()) return@forEach
            if (line.startsWith("##")) {
                val header = line.removePrefix("##").trim()
                ahead_count = Regex("""\[.*?\bahead (\d+)""").find(header)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?: 0
                behind_count = Regex("""\[.*?\bbehind (\d+)""").find(header)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?: 0
                branch = header
                    .substringBefore("...")
                    .substringBefore('[')
                    .trim()
                    .removePrefix("No commits yet -")
                    .trim()
                return@forEach
            }
            if (line.length < 4) return@forEach
            entries += git_change_entry(
                staged_status = line[0],
                worktree_status = line[1],
                path = line.substring(3).trim().removeSurrounding("\"")
            )
        }
        return git_status_result(branch, entries, ahead_count, behind_count)
    }

    /**
     * 查看文件改动：[cached]=true 看已暂存的 diff。
     * 输出限制在 ~200KB，避免巨型 diff 撑爆 UI。
     */
    suspend fun diff(project_root: String, path: String, cached: Boolean = false): String {
        val cached_flag = if (cached) " --cached" else ""
        val (ok, out) = run_git(project_root, "diff --unified=3$cached_flag -- ${shell_quote(path)}", timeout_ms = 20_000L)
        if (!ok) return out.ifBlank { "获取 diff 失败" }
        return if (out.length > 200_000) out.take(200_000) + "\n... (diff 过长已截断)" else out.trimEnd()
    }

    /** 全部暂存并提交。 */
    suspend fun commit_all(project_root: String, message: String): Boolean {
        if (message.isBlank()) return false
        if (!ensure_local_identity(project_root)) return false
        val (add_ok, add_out) = run_git(project_root, "add -A")
        if (!add_ok) {
            android.util.Log.w(LOG_TAG, "git add failed: $add_out")
            return false
        }
        val (commit_ok, commit_out) = run_git(
            project_root,
            "commit -m ${shell_quote(message.trim())}",
            timeout_ms = 20_000L
        )
        if (!commit_ok) {
            android.util.Log.w(LOG_TAG, "git commit failed: $commit_out")
            return false
        }
        return true
    }

    suspend fun stage(project_root: String, paths: List<String>): Boolean =
        run_git(project_root, "add -- ${quote_paths(paths)}").first

    suspend fun unstage(project_root: String, paths: List<String>): Boolean {
        val quoted = quote_paths(paths)
        val has_head = run_git(
            project_root,
            "rev-parse --verify --quiet HEAD",
            timeout_ms = 8_000L
        ).first
        val args = if (has_head) "reset HEAD -- $quoted" else "rm --cached -- $quoted"
        return run_git(project_root, args).first
    }

    suspend fun discard(project_root: String, paths: List<String>): Boolean =
        run_git(project_root, "checkout -- ${quote_paths(paths)}").first

    suspend fun stage_all(project_root: String): Boolean =
        run_git(project_root, "add -A").first

    /** 相对项目根的展示路径。 */
    fun relative_display_path(project_root: String, path: String): String =
        path.removePrefix(project_root).trimStart('/')
}

/** 单词级 shell 引号包裹（与 activity 侧 build 命令一致的语义）。 */
internal fun shell_quote(value: String): String =
    if (value.isNotEmpty() && value.none { it in " \t\"'\\$`<>&|;(){}*?[]!~#\n" }) value
    else "'" + value.replace("'", "'\\''") + "'"

private fun quote_paths(paths: List<String>): String =
    paths.filter { it.isNotBlank() }
        .joinToString(" ") { shell_quote(it) }
