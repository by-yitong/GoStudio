package com.jmwl.gostudio.toolchain

/** git 命令机读输出的字段分隔符（%x1f）。 */
internal const val GIT_FIELD_SEPARATOR = '\u001f'

/** 提交历史条目；parents 为父提交 hash 列表（合并提交 >1 个）。 */
data class git_commit_entry(
    val full_hash: String,
    val short_hash: String,
    val author: String,
    val timestamp: Long,
    val subject: String,
    val parents: List<String>,
    val body: String
)

/** 提交内文件变更；change ∈ A/M/D。 */
data class git_commit_file(val change: Char, val path: String)

/** 贮藏条目；index 对应 stash@{index}。 */
data class git_stash_entry(
    val index: Int,
    val hash: String,
    val relative_time: String,
    val message: String
)

/** pull 结果；失败时 conflict_paths 为冲突文件路径（无冲突则为空）。 */
data class git_pull_result(
    val ok: Boolean,
    val conflict_paths: List<String>,
    val message: String
)

/**
 * 提交历史 / fetch / pull（允许合并）/ 贮藏：全部复用 git_manager.run_git 走 proot。
 * 解析逻辑全部是不依赖 Android 的 internal 纯函数，供 JUnit 单测覆盖。
 */
object git_history_manager {

    private const val LOG_TAG = "git_history_manager"

    suspend fun log(project_root: String, skip: Int = 0, limit: Int = 100): List<git_commit_entry> {
        val (ok, out) = git_manager.run_git(
            project_root,
            "log --pretty=format:%H%x1f%h%x1f%an%x1f%at%x1f%s%x1f%P%x1f%b --skip $skip -n $limit",
            timeout_ms = 20_000L
        )
        return if (!ok) emptyList() else parse_log_lines(out)
    }

    suspend fun commit_files(project_root: String, hash: String): List<git_commit_file> {
        val (ok, out) = git_manager.run_git(
            project_root,
            "diff-tree --no-commit-id -r --root --name-status --no-renames ${shell_quote(hash.trim())}",
            timeout_ms = 20_000L
        )
        return if (!ok) emptyList() else parse_commit_files(out)
    }

    /**
     * 提交内单文件 diff；输出限制在 ~200KB，避免巨型 diff 撑爆 UI（与 git_manager.diff 一致）。
     */
    suspend fun commit_file_diff(project_root: String, hash: String, path: String): String {
        val (ok, out) = git_manager.run_git(
            project_root,
            "diff-tree -p --root --unified=3 ${shell_quote(hash.trim())} -- ${shell_quote(path)}",
            timeout_ms = 20_000L
        )
        if (!ok) return out.ifBlank { "获取 diff 失败" }
        return if (out.length > 200_000) out.take(200_000) + "\n... (diff 过长已截断)" else out.trimEnd()
    }

    suspend fun fetch(project_root: String): Boolean =
        git_manager.run_git(project_root, "fetch --prune", timeout_ms = 60_000L).first

    /** pull --no-rebase（允许合并）；失败后用 status --porcelain 收集冲突文件。 */
    suspend fun pull_merge(project_root: String): git_pull_result {
        val (ok, out) = git_manager.run_git(
            project_root,
            "pull --no-rebase",
            timeout_ms = 120_000L
        )
        if (ok) return git_pull_result(true, emptyList(), out)
        val (_, status_out) = git_manager.run_git(
            project_root,
            "status --porcelain=v1",
            timeout_ms = 15_000L
        )
        val conflicts = parse_conflict_paths(status_out)
        android.util.Log.w(LOG_TAG, "git pull failed: ${out.lineSequence().take(5).joinToString(" / ")}")
        return git_pull_result(false, conflicts, out)
    }

    suspend fun stash_list(project_root: String): List<git_stash_entry> {
        val (ok, out) = git_manager.run_git(
            project_root,
            "stash list --format=%gd%x1f%h%x1f%ar%x1f%s",
            timeout_ms = 30_000L
        )
        return if (!ok) emptyList() else parse_stash_list(out)
    }

    suspend fun stash_push(project_root: String, message: String): Boolean =
        git_manager.run_git(
            project_root,
            "stash push -m ${shell_quote(message)}",
            timeout_ms = 30_000L
        ).first

    suspend fun stash_pop(project_root: String, index: Int): Boolean =
        git_manager.run_git(
            project_root,
            "stash pop ${shell_quote("stash@{$index}")}",
            timeout_ms = 30_000L
        ).first

    suspend fun stash_drop(project_root: String, index: Int): Boolean =
        git_manager.run_git(
            project_root,
            "stash drop ${shell_quote("stash@{$index}")}",
            timeout_ms = 30_000L
        ).first
}

/**
 * 解析 `log --pretty=format:%H%x1f%h%x1f%an%x1f%at%x1f%s%x1f%P%x1f%b`。
 * %b（body）可跨行：新记录以「40/64 位 hash + 分隔符」开头识别，
 * 其余行并入上一条记录的 body（normalize_output 已丢空行，这里同样忽略空行）。
 */
internal fun parse_log_lines(output: String): List<git_commit_entry> {
    if (output.isBlank()) return emptyList()
    val record_start = Regex("^[0-9a-f]{40}(?:[0-9a-f]{24})?\u001f")
    val entries = mutableListOf<git_commit_entry>()
    var fields: List<String>? = null
    val body_extra = mutableListOf<String>()

    fun flush() {
        val f = fields ?: return
        fields = null
        if (f.size < 6) {
            body_extra.clear()
            return
        }
        val body = (listOf(f.getOrElse(6) { "" }) + body_extra)
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
        entries += git_commit_entry(
            full_hash = f[0],
            short_hash = f[1],
            author = f[2],
            timestamp = f[3].toLongOrNull() ?: 0L,
            subject = f[4],
            parents = f[5].split(' ').filter { it.isNotBlank() },
            body = body
        )
        body_extra.clear()
    }

    output.lines().forEach { raw ->
        val line = raw.trimEnd('\r')
        if (line.isEmpty()) return@forEach
        if (record_start.containsMatchIn(line)) {
            flush()
            fields = line.split(GIT_FIELD_SEPARATOR)
        } else {
            fields?.let { body_extra += line }
        }
    }
    flush()
    return entries
}

/** 解析 `diff-tree --name-status --no-renames`：每行 `M\tpath`，path 可能带引号。 */
internal fun parse_commit_files(output: String): List<git_commit_file> =
    output.lines().mapNotNull { raw ->
        val line = raw.trimEnd('\r')
        val sep = line.indexOf('\t')
        if (sep <= 0) return@mapNotNull null
        val change = line.take(sep).trim().firstOrNull { it in "AMD" } ?: return@mapNotNull null
        val path = line.substring(sep + 1).trim().removeSurrounding("\"")
        if (path.isEmpty()) return@mapNotNull null
        git_commit_file(change, path)
    }

/** 解析 `stash list --format=%gd%x1f%h%x1f%ar%x1f%s`；index 从 `stash@{N}` 提取。 */
internal fun parse_stash_list(output: String): List<git_stash_entry> =
    output.lines().mapNotNull { raw ->
        val line = raw.trimEnd('\r')
        if (line.isBlank()) return@mapNotNull null
        val f = line.split(GIT_FIELD_SEPARATOR)
        if (f.size < 4) return@mapNotNull null
        val index = Regex("""stash@\{(\d+)\}""").find(f[0])
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return@mapNotNull null
        git_stash_entry(
            index = index,
            hash = f[1],
            relative_time = f[2],
            message = f[3]
        )
    }

/**
 * 解析 `status --porcelain=v1`：收集冲突行（X/Y 两列均 ∈ U/A/D，
 * 即 UU/AA/DD/AU/UA/DU/UD 组合），普通修改与重命名不收集。
 */
internal fun parse_conflict_paths(status_output: String): List<String> {
    val paths = mutableListOf<String>()
    status_output.lines().forEach { raw ->
        val line = raw.trimEnd('\r')
        if (line.length < 4) return@forEach
        val x = line[0]
        val y = line[1]
        if (x in "UAD" && y in "UAD") {
            paths += line.substring(3).trim().removeSurrounding("\"")
        }
    }
    return paths
}
