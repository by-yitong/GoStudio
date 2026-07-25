package com.jmwl.gostudio.lsp.gopls

import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspLanguage
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

/**
 * gopls 项目封装（参照 clangd_lsp_project）。
 *
 * 每个项目一个 [LspProject]，每个打开的 .go 文件一个 [LspEditor]。
 * gopls 进程在最后一个文件关闭时自动停止，打开新文件时重启。
 * wrapperLanguage 保留 TextMate 语法高亮（gopls 只负责语义：补全/诊断/跳转/格式化）。
 */
class gopls_lsp_project(
    val project_dir: File,
    private val config_factory: (working_dir: String) -> gopls_lsp_config,
    private val disabled_features: Set<LspFeature> = emptySet()
) {
    val project: LspProject = LspProject(project_dir.absolutePath).apply {
        addServerDefinitions(create_gopls_language_server_definitions(config_factory, disabled_features))
        init()
    }

    fun get_or_create_editor(file: File, editor: CodeEditor): LspEditor {
        val lsp_editor = project.getOrCreateEditor(file.absolutePath)
        val current_language = editor.editorLanguage
        lsp_editor.wrapperLanguage = if (current_language is LspLanguage) {
            current_language.wrapperLanguage
        } else {
            current_language
        }
        lsp_editor.editor = editor
        return lsp_editor
    }

    suspend fun connect(file: File, editor: CodeEditor): Boolean {
        return get_or_create_editor(file, editor).connect(throwException = false)
    }

    fun close_file(file: File) {
        project.getEditor(file.absolutePath)?.dispose()
        project.removeEditor(file.absolutePath)
    }

    fun dispose() {
        project.dispose()
    }
}
