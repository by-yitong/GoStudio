package com.jmwl.gostudio.lsp.gopls

import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * 跳转定义的简单结果（剥离 lsp4j 类型，避免 app 层依赖 lsp4j）。
 *
 * @param file_path 目标文件 host 路径（已去掉 file:// 前缀）
 * @param line 0-based 行号
 * @param column 0-based 列号
 */
data class gopls_definition_location(
    val file_path: String,
    val line: Int,
    val column: Int
)

/**
 * 请求 gopls 的 textDocument/definition，返回第一个定义位置。
 *
 * gopls 经 proot 绑定运行，返回的 uri 即 host 路径，无需额外转换。
 * 超时（8s）或无结果返回 null。
 */
suspend fun gopls_lsp_project.request_definition(
    file: File,
    editor: CodeEditor,
    cursor_line: Int,
    cursor_column: Int
): gopls_definition_location? = withTimeoutOrNull(8000L) {
    val lsp_editor = get_or_create_editor(file, editor)
    val requestManager = lsp_editor.requestManager ?: return@withTimeoutOrNull null
    val position = editor.text.getIndexer()
        .getCharPosition(cursor_line, cursor_column)
        .asLspPosition()
    val identifier = lsp_editor.uri.createTextDocumentIdentifier()
    val params = org.eclipse.lsp4j.DefinitionParams(identifier, position)
    val future = requestManager.definition(params) ?: return@withTimeoutOrNull null
    val result = future.await() ?: return@withTimeoutOrNull null

    when {
        result.left != null && result.left.isNotEmpty() -> {
            val loc = result.left.first()
            gopls_definition_location(
                file_path = loc.uri.removePrefix("file://"),
                line = loc.range.start.line,
                column = loc.range.start.character
            )
        }
        result.right != null && result.right.isNotEmpty() -> {
            val link = result.right.first()
            gopls_definition_location(
                file_path = link.targetUri.removePrefix("file://"),
                line = link.targetSelectionRange.start.line,
                column = link.targetSelectionRange.start.character
            )
        }
        else -> null
    }
}
