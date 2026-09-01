package com.jmwl.gostudio.ui.screens.learn

import android.os.Bundle
import com.jmwl.gostudio.editor.core.go_completion_keywords
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference

/**
 * 学习页使用轻量语义补全：精确匹配 Go 关键字，并为 fmt 等常用包提供成员提示。
 * 这里不接入 gopls，避免练习编辑器依赖完整项目环境。
 */
class learn_go_language : TextMateLanguage(
    checkNotNull(GrammarRegistry.getInstance().findGrammar("source.go")),
    GrammarRegistry.getInstance().findLanguageConfiguration("source.go"),
    GrammarRegistry.getInstance(),
    ThemeRegistry.getInstance(),
    true
) {

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val line_before = content.getLine(position.line).take(position.column)
        val dot_match = member_regex.find(line_before)
        if (dot_match != null) {
            val receiver = dot_match.groupValues[1]
            val prefix = dot_match.groupValues[2]
            val members = package_members[receiver].orEmpty()
            publisher.addItems(
                members.filter { it.label.startsWith(prefix, ignoreCase = true) }
            )
            return
        }

        val prefix = CompletionHelper.computePrefix(content, position) { char ->
            char.isLetterOrDigit() || char == '_'
        }
        if (prefix.isEmpty()) {
            return
        }

        val identifiers = mutableSetOf<String>().apply {
            for (line_index in 0 until content.lineCount) {
                identifier_regex.findAll(content.getLine(line_index)).forEach {
                    add(it.value)
                }
            }
        }

        val keyword_items = go_completion_keywords
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .map { keyword ->
                SimpleCompletionItem(keyword, "Keyword", prefix.length, keyword)
                    .kind(CompletionItemKind.Keyword)
            }
        val identifier_items = identifiers
            .filter { it != prefix && it.startsWith(prefix, ignoreCase = true) }
            .sorted()
            .take(30)
            .map { identifier ->
                SimpleCompletionItem(identifier, "Identifier", prefix.length, identifier)
                    .kind(CompletionItemKind.Identifier)
            }

        publisher.addItems(identifier_items + keyword_items)
    }

    private companion object {
        val member_regex = Regex("""([A-Za-z_][A-Za-z0-9_]*)\.([A-Za-z_][A-Za-z0-9_]*)?$""")
        val identifier_regex = Regex("""[A-Za-z_][A-Za-z0-9_]*""")

        val package_members = mapOf(
            "fmt" to listOf(
                member("Print", "func(a ...any) (n int, err error)"),
                member("Printf", "func(format string, a ...any) (n int, err error)"),
                member("Println", "func(a ...any) (n int, err error)"),
                member("Sprint", "func(a ...any) string"),
                member("Sprintf", "func(format string, a ...any) string"),
                member("Sprintln", "func(a ...any) string"),
                member("Errorf", "func(format string, a ...any) error"),
                member("Scan", "func(a ...any) (n int, err error)"),
                member("Scanf", "func(format string, a ...any) (n int, err error)"),
                member("Scanln", "func(a ...any) (n int, err error)")
            )
        )

        private fun member(label: String, detail: String) =
            SimpleCompletionItem(label, detail, 0, label)
                .kind(CompletionItemKind.Function)
    }
}
