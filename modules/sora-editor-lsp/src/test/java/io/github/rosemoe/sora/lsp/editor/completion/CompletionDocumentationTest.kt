package io.github.rosemoe.sora.lsp.editor.completion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CompletionDocumentationTest {

    @Test
    fun `null or blank documentation has no summary`() {
        assertThat(completionDocumentationSummary(null)).isNull()
        assertThat(completionDocumentationSummary("   ")).isNull()
        assertThat(completionDocumentationSummary("\n\n")).isNull()
    }

    @Test
    fun `first paragraph is joined into one line`() {
        val documentation = """
            Println formats using the default formats for its operands and writes the resulting
            string to standard output.

            Spaces are always added between operands.
        """.trimIndent()

        val expected =
            "Println formats using the default formats for its operands and writes the resulting string to standard output."

        assertThat(completionDocumentationSummary(documentation, maxLength = 200))
            .isEqualTo(expected)
        assertThat(completionDocumentationSummary(documentation)!!.endsWith("…")).isTrue()
    }

    @Test
    fun `leading code block is skipped`() {
        val documentation = """
            ```go
            func Example() {}
            ```

            Example does something.
        """.trimIndent()

        assertThat(completionDocumentationSummary(documentation)).isEqualTo("Example does something.")
    }

    @Test
    fun `markdown decorations are stripped`() {
        val documentation =
            "Returns the `value` of x. See [the docs](https://example.com) for details."

        assertThat(completionDocumentationSummary(documentation))
            .isEqualTo("Returns the value of x. See the docs for details.")
    }

    @Test
    fun `long summaries are cut at a word boundary`() {
        val summary = completionDocumentationSummary("word ".repeat(40), maxLength = 20)

        assertThat(summary).isNotNull()
        assertThat(summary!!.length).isAtMost(21)
        assertThat(summary.endsWith("…")).isTrue()
    }

    @Test
    fun `documentation with only a code block has no summary`() {
        val documentation = """
            ```go
            func OnlyCode() {}
            ```
        """.trimIndent()

        assertThat(completionDocumentationSummary(documentation)).isNull()
    }

    @Test
    fun `leading symbol name is stripped`() {
        val documentation = "Command runs the command specified by name."

        assertThat(completionDocumentationSummary(documentation, symbolName = "Command"))
            .isEqualTo("runs the command specified by name.")
    }

    @Test
    fun `leading symbol name with chinese connector is stripped`() {
        assertThat(
            completionDocumentationSummary("SayHello是向某人打招呼的函数", symbolName = "SayHello")
        ).isEqualTo("向某人打招呼的函数")
    }

    @Test
    fun `summary equal to symbol name only has no result`() {
        assertThat(completionDocumentationSummary("Command", symbolName = "Command")).isNull()
    }
}
