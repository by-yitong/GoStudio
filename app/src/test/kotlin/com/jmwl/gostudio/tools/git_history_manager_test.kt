package com.jmwl.gostudio.tools

import com.google.common.truth.Truth.assertThat
import com.jmwl.gostudio.toolchain.parse_commit_files
import com.jmwl.gostudio.toolchain.parse_conflict_paths
import com.jmwl.gostudio.toolchain.parse_log_lines
import com.jmwl.gostudio.toolchain.parse_stash_list
import org.junit.Test

/**
 * git_history_manager 解析层回归：
 * run_git 输出经 normalize_output（去前缀、丢空行、整体 trim）后进解析，
 * 这里锁住 log / diff-tree / stash list / porcelain 冲突提取的关键路径
 * （中文作者与路径、空 body、body 多行、多父合并提交、引号路径）。
 */
class git_history_manager_test {

    /** %x1f 字段分隔符。 */
    private val sep = 31.toChar().toString()

    private val hash_a = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0"
    private val hash_b = "0fedcba9876543210fedcba9876543210fedcba9"
    private val parent_1 = "1111111111111111111111111111111111111111"
    private val parent_2 = "2222222222222222222222222222222222222222"

    private fun log_line(
        hash: String, short: String, author: String, ts: String,
        subject: String, parents: String, body: String = ""
    ): String = listOf(hash, short, author, ts, subject, parents, body).joinToString(sep)

    @Test
    fun `parse log reads entries with chinese author and subject`() {
        val out = listOf(
            log_line(hash_a, "a1b2c3d", "张三", "1695000000", "修复启动崩溃", "", ""),
            // 无 body 提交：尾部分隔符被 normalize_output 整体 trim 掉，只剩 6 个字段
            log_line(hash_b, "0fedcba", "John Doe", "1694000000", "initial commit", parent_1)
        ).joinToString("\n")
        val entries = parse_log_lines(out)
        assertThat(entries).hasSize(2)
        val first = entries[0]
        assertThat(first.full_hash).isEqualTo(hash_a)
        assertThat(first.short_hash).isEqualTo("a1b2c3d")
        assertThat(first.author).isEqualTo("张三")
        assertThat(first.timestamp).isEqualTo(1695000000L)
        assertThat(first.subject).isEqualTo("修复启动崩溃")
        assertThat(first.parents).isEmpty()
        assertThat(first.body).isEmpty()
        assertThat(entries[1].parents).containsExactly(parent_1)
        assertThat(entries[1].body).isEmpty()
    }

    @Test
    fun `parse log merges multiline body and two parents for merge commit`() {
        val out = listOf(
            log_line(hash_a, "a1b2c3d", "李四", "1695000100", "合并特性分支", "$parent_1 $parent_2", "正文第一段"),
            // body 续行：不以 hash+分隔符开头，并入上一条的 body
            "正文第二段（续行没有分隔符）",
            log_line(hash_b, "0fedcba", "王五", "1694000000", "初始提交", "")
        ).joinToString("\n")
        val entries = parse_log_lines(out)
        assertThat(entries).hasSize(2)
        assertThat(entries[0].parents).containsExactly(parent_1, parent_2).inOrder()
        assertThat(entries[0].body).isEqualTo("正文第一段\n正文第二段（续行没有分隔符）")
        assertThat(entries[0].subject).isEqualTo("合并特性分支")
        assertThat(entries[1].body).isEmpty()
    }

    @Test
    fun `parse log keeps single line body`() {
        val out = listOf(
            log_line(hash_a, "a1b2c3d", "张三", "1695000000", "发布 1.0", parent_1, "release notes"),
            log_line(hash_b, "0fedcba", "李四", "1694000000", "准备发布", hash_a)
        ).joinToString("\n")
        assertThat(parse_log_lines(out)[0].body).isEqualTo("release notes")
    }

    @Test
    fun `parse log returns empty on blank output`() {
        assertThat(parse_log_lines("")).isEmpty()
        assertThat(parse_log_lines("\n   \n")).isEmpty()
    }

    @Test
    fun `parse commit files handles add modify delete mix`() {
        val out = listOf(
            "M\tsrc/main.kt",
            "A\t新文件.kt",
            "D\told/file.go"
        ).joinToString("\n")
        val files = parse_commit_files(out)
        assertThat(files).hasSize(3)
        assertThat(files[0].change).isEqualTo('M')
        assertThat(files[0].path).isEqualTo("src/main.kt")
        assertThat(files[1].change).isEqualTo('A')
        assertThat(files[1].path).isEqualTo("新文件.kt")
        assertThat(files[2].change).isEqualTo('D')
        assertThat(files[2].path).isEqualTo("old/file.go")
    }

    @Test
    fun `parse commit files unquotes quoted chinese path`() {
        val files = parse_commit_files("A\t\"路径/带 空格.kt\"")
        assertThat(files).hasSize(1)
        assertThat(files[0].change).isEqualTo('A')
        assertThat(files[0].path).isEqualTo("路径/带 空格.kt")
    }

    @Test
    fun `parse commit files returns empty on blank output`() {
        assertThat(parse_commit_files("")).isEmpty()
    }

    @Test
    fun `parse stash list extracts index from stash ref`() {
        val out = listOf(
            listOf("stash@{0}", "1a2b3c4", "3 minutes ago", "WIP on main: 1a2b3c4 修复登录").joinToString(sep),
            listOf("stash@{1}", "9f8e7d6", "2 days ago", "On dev: 特性开发").joinToString(sep)
        ).joinToString("\n")
        val stashes = parse_stash_list(out)
        assertThat(stashes).hasSize(2)
        assertThat(stashes[0].index).isEqualTo(0)
        assertThat(stashes[0].hash).isEqualTo("1a2b3c4")
        assertThat(stashes[0].relative_time).isEqualTo("3 minutes ago")
        assertThat(stashes[0].message).isEqualTo("WIP on main: 1a2b3c4 修复登录")
        assertThat(stashes[1].index).isEqualTo(1)
        assertThat(stashes[1].message).isEqualTo("On dev: 特性开发")
    }

    @Test
    fun `parse stash list returns empty on blank output`() {
        assertThat(parse_stash_list("")).isEmpty()
    }

    @Test
    fun `parse conflict paths collects unmerged pairs only`() {
        val out = listOf(
            "UU conflict/both.txt",
            "AA added-by-both.kt",
            "DD deleted-by-both.go",
            "AU them-added.txt",
            "UA us-added.txt",
            "DU them-deleted.txt",
            "UD us-deleted.txt",
            " M modified-only.txt",
            "M  staged-only.txt",
            "D  index-deleted.txt",
            " D worktree-deleted.txt",
            "R  renamed-old.txt -> renamed-new.txt",
            "?? untracked.txt"
        ).joinToString("\n")
        assertThat(parse_conflict_paths(out)).containsExactly(
            "conflict/both.txt",
            "added-by-both.kt",
            "deleted-by-both.go",
            "them-added.txt",
            "us-added.txt",
            "them-deleted.txt",
            "us-deleted.txt"
        ).inOrder()
    }

    @Test
    fun `parse conflict paths unquotes chinese path and handles empty output`() {
        assertThat(parse_conflict_paths("UU \"中 文/冲突.kt\"")).containsExactly("中 文/冲突.kt")
        assertThat(parse_conflict_paths("")).isEmpty()
    }
}
