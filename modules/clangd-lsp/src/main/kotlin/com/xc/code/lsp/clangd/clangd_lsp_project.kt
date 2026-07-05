package com.xc.code.lsp.clangd

import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspLanguage
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class clangd_lsp_project(
    val project_dir: File,
    private val config_factory: (working_dir: String) -> clangd_lsp_config,
    private val disabled_features: Set<LspFeature> = emptySet()
) {
    val project: LspProject = LspProject(project_dir.absolutePath).apply {
        addServerDefinitions(create_clangd_language_server_definitions(config_factory, disabled_features))
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
