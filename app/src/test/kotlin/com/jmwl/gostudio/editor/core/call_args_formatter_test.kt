package com.jmwl.gostudio.editor.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 参数展开/折叠纯文本变换回归：
 * 扫描器要正确跳过 Go 的字符串/rune/注释，展开折叠要能无损往返，
 * 任何回退都会直接改坏用户代码，这里锁住各关键路径。
 */
class call_args_formatter_test {

    private val formatter = call_args_formatter

    /** 模拟编辑器侧的一次整体替换：把光标所在括号对换成变换结果。 */
    private fun toggle(content: String, cursor: Int): String? {
        val range = formatter.find_enclosing_call(content, cursor) ?: return null
        val transform = formatter.format_call_args(content, cursor) ?: return null
        return content.substring(0, range.open_offset) +
            transform.new_call_text +
            content.substring(range.close_offset + 1)
    }

    // ---------- find_enclosing_call ----------

    @Test
    fun `finds simple paren pair around cursor`() {
        assertThat(formatter.find_enclosing_call("foo(a, b)", 5))
            .isEqualTo(call_args_range(open_offset = 3, close_offset = 8))
    }

    @Test
    fun `cursor right after open paren is inside`() {
        assertThat(formatter.find_enclosing_call("foo(a, b)", 4))
            .isEqualTo(call_args_range(3, 8))
    }

    @Test
    fun `cursor right before close paren is inside`() {
        assertThat(formatter.find_enclosing_call("foo(a)", 5))
            .isEqualTo(call_args_range(3, 5))
    }

    @Test
    fun `cursor outside parens returns null`() {
        assertThat(formatter.find_enclosing_call("foo(a, b)", 2)).isNull()
        assertThat(formatter.find_enclosing_call("foo(a, b)", 0)).isNull()
    }

    @Test
    fun `nested call returns innermost pair`() {
        val content = "foo(bar(x), y)"
        // bar 的括号对是 (7, 9)
        assertThat(formatter.find_enclosing_call(content, 8))
            .isEqualTo(call_args_range(7, 9))
        // y 处取外层 foo 的括号对
        assertThat(formatter.find_enclosing_call(content, 12))
            .isEqualTo(call_args_range(3, 13))
    }

    @Test
    fun `unmatched paren returns null`() {
        assertThat(formatter.find_enclosing_call("foo(a, b", 5)).isNull()
        assertThat(formatter.find_enclosing_call("foo(a, b)", 99)).isNull()
    }

    @Test
    fun `parens inside double quoted string are ignored`() {
        // 字符串 "(" 里的括号不算：真实括号对是 (1, 5)
        assertThat(formatter.find_enclosing_call("f(\"(\")", 3))
            .isEqualTo(call_args_range(1, 5))
    }

    @Test
    fun `parens inside comments are ignored`() {
        assertThat(formatter.find_enclosing_call("f(a, // )\n b)", 8))
            .isEqualTo(call_args_range(1, 12))
        assertThat(formatter.find_enclosing_call("f(a /* ( */)", 7))
            .isEqualTo(call_args_range(1, 11))
    }

    // ---------- split_top_level_args ----------

    @Test
    fun `splits plain args`() {
        assertThat(formatter.split_top_level_args("a, b, c")).containsExactly("a", " b", " c").inOrder()
    }

    @Test
    fun `empty arg region returns empty list`() {
        assertThat(formatter.split_top_level_args("")).isEmpty()
        assertThat(formatter.split_top_level_args("   ")).isEmpty()
    }

    @Test
    fun `comma inside nested call does not split`() {
        assertThat(formatter.split_top_level_args("bar(x, y), z")).hasSize(2)
    }

    @Test
    fun `comma inside double quoted string does not split`() {
        assertThat(formatter.split_top_level_args("\"a,b\", c")).hasSize(2)
        // \" 转义不结束字符串
        assertThat(formatter.split_top_level_args("\"a\\\",b\", c")).hasSize(2)
    }

    @Test
    fun `comma inside raw string does not split`() {
        assertThat(formatter.split_top_level_args("`a,b`, c")).hasSize(2)
    }

    @Test
    fun `comma inside rune does not split`() {
        assertThat(formatter.split_top_level_args("','  , c")).hasSize(2)
    }

    @Test
    fun `comma inside line comment does not split`() {
        val parts = formatter.split_top_level_args("a, // ,\n b")
        assertThat(parts).hasSize(2)
        assertThat(parts[0]).isEqualTo("a")
    }

    @Test
    fun `comma inside block comment does not split`() {
        assertThat(formatter.split_top_level_args("a /* , */, b")).hasSize(2)
    }

    @Test
    fun `comma inside composite literal brackets does not split`() {
        // []int{1, 2} 里的逗号在 []/{} 深度内
        assertThat(formatter.split_top_level_args("[]int{1, 2}, b")).hasSize(2)
    }

    @Test
    fun `trailing comma piece is dropped`() {
        assertThat(formatter.split_top_level_args("\n\ta,\n\tb,\n"))
            .containsExactly("\n\ta", "\n\tb")
            .inOrder()
    }

    // ---------- 展开 ----------

    @Test
    fun `expand single line call`() {
        val transform = formatter.format_call_args("foo(a, b)", 5)!!
        assertThat(transform.new_call_text).isEqualTo("(\n\ta,\n\tb,\n)")
        assertThat(transform.new_cursor).isEqualTo(9)
    }

    @Test
    fun `expand uses call line indent`() {
        val content = "\t\tfoo(a, b)"
        val transform = formatter.format_call_args(content, 7)!!
        assertThat(transform.new_call_text).isEqualTo("(\n\t\t\ta,\n\t\t\tb,\n\t\t)")
    }

    @Test
    fun `expand single arg`() {
        val transform = formatter.format_call_args("foo(a)", 5)!!
        assertThat(transform.new_call_text).isEqualTo("(\n\ta,\n)")
    }

    @Test
    fun `expand keeps string arg with commas as one arg`() {
        val transform = formatter.format_call_args("foo(\"a,b\", c)", 5)!!
        assertThat(transform.new_call_text).isEqualTo("(\n\t\"a,b\",\n\tc,\n)")
    }

    @Test
    fun `expand nested call stays on one arg line`() {
        val transform = formatter.format_call_args("foo(bar(x, y), z)", 5)!!
        assertThat(transform.new_call_text).isEqualTo("(\n\tbar(x, y),\n\tz,\n)")
    }

    // ---------- 折叠 ----------

    @Test
    fun `collapse multiline args back to single line`() {
        val transform = formatter.format_call_args("foo(\n\ta,\n\tb,\n)", 4)!!
        assertThat(transform.new_call_text).isEqualTo("(a, b)")
        assertThat(transform.new_cursor).isEqualTo(5)
    }

    @Test
    fun `collapse keeps nested multiline arg structure`() {
        val content = "foo(\n\tbar(\n\t\t1,\n\t),\n\t2,\n)"
        val transform = formatter.format_call_args(content, 6)!!
        assertThat(transform.new_call_text).isEqualTo("(bar(\n\t\t1,\n\t), 2)")
    }

    @Test
    fun `collapse with indented close paren`() {
        val content = "\tfoo(\n\t\ta,\n\t\tb,\n\t)"
        val transform = formatter.format_call_args(content, 8)!!
        assertThat(transform.new_call_text).isEqualTo("(a, b)")
    }

    // ---------- 智能切换与往返 ----------

    @Test
    fun `toggle picks innermost call when cursor nested`() {
        val content = "foo(bar(x), y)"
        // 光标在 x 上：只动 bar 的参数
        assertThat(toggle(content, 8)).isEqualTo("foo(bar(\n\tx,\n), y)")
    }

    @Test
    fun `expanded nested inner arg is collapsed by outer toggle`() {
        // 外层 foo 已展开，光标在 y 上：折叠外层，内层 bar 多行结构原样保留
        val content = "foo(\n\tbar(\n\t\t1,\n\t),\n\t2,\n)"
        assertThat(toggle(content, 18)).isEqualTo("foo(bar(\n\t\t1,\n\t), 2)")
    }

    @Test
    fun `expand collapse round trips twice`() {
        val original = "result := foo(a, b)"
        val expanded = toggle(original, 15)!!
        assertThat(expanded).isEqualTo("result := foo(\n\ta,\n\tb,\n)")

        val collapsed = toggle(expanded, 17)!!
        assertThat(collapsed).isEqualTo(original)

        val expanded_again = toggle(collapsed, 15)!!
        val collapsed_again = toggle(expanded_again, 17)!!
        assertThat(expanded_again).isEqualTo(expanded)
        assertThat(collapsed_again).isEqualTo(original)
    }

    // ---------- 边界 ----------

    @Test
    fun `empty args expand to indented blank line and fold back`() {
        val expanded = formatter.format_call_args("http.Post()", 11)!!
        assertThat(expanded.new_call_text).isEqualTo("(\n\t\n)")
        assertThat(expanded.new_cursor).isEqualTo("(\n\t".length)

        val folded = formatter.format_call_args("http.Post(\n\t\n)", 12)!!
        assertThat(folded.new_call_text).isEqualTo("()")
    }

    @Test
    fun `whitespace only args also expand`() {
        val expanded = formatter.format_call_args("foo(   )", 4)!!
        assertThat(expanded.new_call_text).isEqualTo("(\n\t\n)")
    }

    @Test
    fun `no enclosing call returns null`() {
        assertThat(formatter.format_call_args("x := 1", 3)).isNull()
        assertThat(formatter.format_call_args("foo(a)", 2)).isNull()
    }
}
