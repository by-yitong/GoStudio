package com.jmwl.gostudio.tools

import com.google.common.truth.Truth.assertThat
import com.jmwl.gostudio.toolchain.git_ref_kind
import com.jmwl.gostudio.toolchain.parse_for_each_ref
import com.jmwl.gostudio.toolchain.parse_upstream_track
import org.junit.Test

/**
 * git_ref_manager 解析层回归：
 * 锁住 for-each-ref 六字段解析（本地/远程/tag 混合、HEAD 标记、
 * ahead/behind 各形态、无 upstream）与 upstream:track 提取。
 */
class git_ref_manager_test {

    /** %x1f 字段分隔符。 */
    private val sep = 31.toChar().toString()

    private val hash_a = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    private val hash_b = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    private val hash_c = "cccccccccccccccccccccccccccccccccccccccc"
    private val hash_d = "dddddddddddddddddddddddddddddddddddddddd"
    private val hash_e = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"

    private fun ref_line(
        short: String, hash: String, head: String, full: String,
        upstream: String, track: String
    ): String = listOf(short, hash, head, full, upstream, track).joinToString(sep)

    @Test
    fun `parse for each ref mixes local remote and tag with head marker`() {
        val out = listOf(
            ref_line("main", hash_a, "*", "refs/heads/main", "origin/main", "[ahead 2]"),
            ref_line("dev", hash_b, "", "refs/heads/dev", "", ""),
            ref_line("origin/main", hash_c, "", "refs/remotes/origin/main", "", ""),
            ref_line("v1.0", hash_d, "", "refs/tags/v1.0", "", ""),
            ref_line("功能/登录", hash_e, "", "refs/heads/功能/登录", "", "")
        ).joinToString("\n")
        val refs = parse_for_each_ref(out)
        assertThat(refs).hasSize(5)
        assertThat(refs.map { it.kind }).containsExactly(
            git_ref_kind.LOCAL,
            git_ref_kind.LOCAL,
            git_ref_kind.REMOTE,
            git_ref_kind.TAG,
            git_ref_kind.LOCAL
        ).inOrder()
        val head_ref = refs[0]
        assertThat(head_ref.is_head).isTrue()
        assertThat(head_ref.full_ref).isEqualTo("refs/heads/main")
        assertThat(head_ref.hash).isEqualTo(hash_a)
        assertThat(head_ref.upstream).isEqualTo("origin/main")
        assertThat(head_ref.ahead).isEqualTo(2)
        assertThat(head_ref.behind).isEqualTo(0)
        assertThat(refs[1].is_head).isFalse()
        assertThat(refs[1].upstream).isEmpty()
        assertThat(refs[1].ahead).isEqualTo(0)
        assertThat(refs[1].behind).isEqualTo(0)
        assertThat(refs[3].short_name).isEqualTo("v1.0")
        assertThat(refs[4].short_name).isEqualTo("功能/登录")
    }

    @Test
    fun `parse for each ref reads behind and mixed ahead behind`() {
        val out = listOf(
            ref_line("release", hash_a, "*", "refs/heads/release", "origin/release", "[behind 3]"),
            ref_line("feature", hash_b, "", "refs/heads/feature", "origin/feature", "[ahead 1, behind 2]")
        ).joinToString("\n")
        val refs = parse_for_each_ref(out)
        assertThat(refs[0].ahead).isEqualTo(0)
        assertThat(refs[0].behind).isEqualTo(3)
        assertThat(refs[1].ahead).isEqualTo(1)
        assertThat(refs[1].behind).isEqualTo(2)
    }

    @Test
    fun `parse for each ref returns empty on blank output`() {
        assertThat(parse_for_each_ref("")).isEmpty()
    }

    @Test
    fun `parse upstream track covers all shapes`() {
        assertThat(parse_upstream_track("ahead 2")).isEqualTo(2 to 0)
        assertThat(parse_upstream_track("[behind 3]")).isEqualTo(0 to 3)
        assertThat(parse_upstream_track("[ahead 1, behind 2]")).isEqualTo(1 to 2)
        assertThat(parse_upstream_track("")).isEqualTo(0 to 0)
        assertThat(parse_upstream_track("[gone]")).isEqualTo(0 to 0)
    }
}
