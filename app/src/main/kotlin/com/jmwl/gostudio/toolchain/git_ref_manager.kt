package com.jmwl.gostudio.toolchain

/** 引用类型：本地分支 / 远程分支 / 标签。 */
enum class git_ref_kind { LOCAL, REMOTE, TAG }

/** 单条引用；upstream/ahead/behind 仅本地跟踪分支有值，其余为空/0。 */
data class git_ref_entry(
    val kind: git_ref_kind,
    val short_name: String,
    val full_ref: String,
    val hash: String,
    val is_head: Boolean,
    val upstream: String,
    val ahead: Int,
    val behind: Int
)

/**
 * 分支 / 标签管理：for-each-ref 一次取全量引用，分支删除/重命名、远程检出、tag 增删。
 * 复用 git_manager.run_git 走 proot；解析为不依赖 Android 的 internal 纯函数。
 */
object git_ref_manager {

    suspend fun all_refs(project_root: String): List<git_ref_entry> {
        val (ok, out) = git_manager.run_git(
            project_root,
            "for-each-ref --format=%(refname:short)%x1f%(objectname)%x1f%(HEAD)%x1f%(refname)%x1f%(upstream:short)%x1f%(upstream:track) refs/heads refs/remotes refs/tags",
            timeout_ms = 15_000L
        )
        return if (!ok) emptyList() else parse_for_each_ref(out)
    }

    /** 删除本地分支；force=true 时用 -D（未合并分支需用户二次确认后传 true）。 */
    suspend fun delete_branch(project_root: String, name: String, force: Boolean = false): Boolean =
        name.isNotBlank() && git_manager.run_git(
            project_root,
            "branch ${if (force) "-D" else "-d"} ${shell_quote(name.trim())}",
            timeout_ms = 15_000L
        ).first

    suspend fun rename_branch(project_root: String, old_name: String, new_name: String): Boolean =
        old_name.isNotBlank() && new_name.isNotBlank() && git_manager.run_git(
            project_root,
            "branch -m ${shell_quote(old_name.trim())} ${shell_quote(new_name.trim())}",
            timeout_ms = 15_000L
        ).first

    /** 检出远程分支为本地跟踪分支；同名本地分支已存在时回退为 checkout 本地名。 */
    suspend fun checkout_remote(project_root: String, remote_short_name: String): Boolean {
        val remote_ref = remote_short_name.trim()
        if (remote_ref.isBlank()) return false
        val (ok, out) = git_manager.run_git(
            project_root,
            "checkout -t ${shell_quote(remote_ref)}",
            timeout_ms = 30_000L
        )
        if (ok) return true
        if (out.contains("already exists")) {
            val local_name = remote_ref.substringAfter('/')
            return git_manager.run_git(
                project_root,
                "checkout ${shell_quote(local_name)}",
                timeout_ms = 30_000L
            ).first
        }
        return false
    }

    suspend fun create_tag(project_root: String, name: String): Boolean =
        name.isNotBlank() && git_manager.run_git(
            project_root,
            "tag ${shell_quote(name.trim())}",
            timeout_ms = 15_000L
        ).first

    suspend fun delete_tag(project_root: String, name: String): Boolean =
        name.isNotBlank() && git_manager.run_git(
            project_root,
            "tag -d ${shell_quote(name.trim())}",
            timeout_ms = 15_000L
        ).first
}

/**
 * 解析 `for-each-ref --format=%(refname:short)%x1f%(objectname)%x1f%(HEAD)%x1f%(refname)%x1f%(upstream:short)%x1f%(upstream:track)`。
 * kind 由 full_ref 前缀判断；is_head = %(HEAD) 列为 "*"。
 */
internal fun parse_for_each_ref(output: String): List<git_ref_entry> =
    output.lines().mapNotNull { raw ->
        val line = raw.trimEnd('\r')
        if (line.isBlank()) return@mapNotNull null
        val f = line.split(GIT_FIELD_SEPARATOR)
        if (f.size < 6) return@mapNotNull null
        val full_ref = f[3]
        val kind = when {
            full_ref.startsWith("refs/heads/") -> git_ref_kind.LOCAL
            full_ref.startsWith("refs/remotes/") -> git_ref_kind.REMOTE
            full_ref.startsWith("refs/tags/") -> git_ref_kind.TAG
            else -> return@mapNotNull null
        }
        val (ahead, behind) = parse_upstream_track(f[5])
        git_ref_entry(
            kind = kind,
            short_name = f[0],
            full_ref = full_ref,
            hash = f[1],
            is_head = f[2] == "*",
            upstream = f[4],
            ahead = ahead,
            behind = behind
        )
    }

/** 解析 `%(upstream:track)`（形如 `[ahead 2]` / `[behind 3]` / `[ahead 1, behind 2]` / 空 / `[gone]`）。 */
internal fun parse_upstream_track(track: String): Pair<Int, Int> {
    val ahead = Regex("""\bahead (\d+)""").find(track)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    val behind = Regex("""\bbehind (\d+)""").find(track)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    return ahead to behind
}
