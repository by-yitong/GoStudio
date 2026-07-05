package com.xc.code.editor.core

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher

class editor_search_controller(
    private var editor: CodeEditor,
    private val can_search: () -> Boolean,
    private val can_replace: () -> Boolean
) {
    fun set_editor(editor: CodeEditor) {
        if (this.editor == editor) return
        clear_search()
        this.editor = editor
    }

    fun update_search(
        query: String,
        match_case: Boolean,
        whole_word: Boolean,
        regex: Boolean
    ): editor_search_update_result {
        if (query.isBlank() || !can_search()) {
            clear_search()
            return editor_search_update_result()
        }

        val type = when {
            regex -> EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION
            whole_word -> EditorSearcher.SearchOptions.TYPE_WHOLE_WORD
            else -> EditorSearcher.SearchOptions.TYPE_NORMAL
        }

        return runCatching {
            editor.searcher.setEnsureOccurrenceVisible(true)
            editor.searcher.search(
                query,
                EditorSearcher.SearchOptions(type, !match_case)
            )
            editor_search_update_result(has_match = editor.searcher.gotoNext())
        }.getOrElse { error ->
            clear_search()
            editor_search_update_result(
                status_text = if (regex) {
                    "正则无效: ${error.message.orEmpty()}"
                } else {
                    "搜索失败: ${error.message.orEmpty()}"
                }
            )
        }
    }

    fun goto_search_result(forward: Boolean): String? {
        return runCatching {
            if (!editor.searcher.hasQuery()) return null
            val moved = if (forward) {
                editor.searcher.gotoNext()
            } else {
                editor.searcher.gotoPrevious()
            }
            if (moved) null else "未找到匹配"
        }.getOrElse { error ->
            "搜索失败: ${error.message.orEmpty()}"
        }
    }

    fun replace_current_match(replacement: String): editor_search_edit_result {
        if (!can_replace()) return editor_search_edit_result()

        return runCatching {
            editor.searcher.replaceCurrentMatch(replacement)
            editor_search_edit_result(changed = true)
        }.getOrElse { error ->
            editor_search_edit_result(status_text = "替换失败: ${error.message.orEmpty()}")
        }
    }

    fun replace_all_matches(replacement: String): editor_search_edit_result {
        if (!can_replace()) return editor_search_edit_result()

        return runCatching {
            var changed = false
            editor.searcher.replaceAll(replacement) {
                changed = true
            }
            editor_search_edit_result(changed = changed)
        }.getOrElse { error ->
            editor_search_edit_result(status_text = "替换失败: ${error.message.orEmpty()}")
        }
    }

    fun clear_search() {
        runCatching {
            if (editor.searcher.hasQuery()) {
                editor.searcher.stopSearch()
            }
        }
    }
}

data class editor_search_update_result(
    val has_match: Boolean = false,
    val status_text: String? = null
)

data class editor_search_edit_result(
    val changed: Boolean = false,
    val status_text: String? = null
)
