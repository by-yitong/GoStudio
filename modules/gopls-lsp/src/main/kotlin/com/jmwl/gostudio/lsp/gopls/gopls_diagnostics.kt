package com.jmwl.gostudio.lsp.gopls

import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.workspace.workSpaceApplyEdit
import io.github.rosemoe.sora.lsp.events.workspace.workSpaceExecuteCommand
import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Range
import java.io.File

/** 单条诊断（剥离 lsp4j 类型；severity 1=错误 2=警告 3=信息 4=提示）。 */
data class gopls_diagnostic(
    val severity: Int,
    val message: String,
    /** 0 起始行。 */
    val line: Int,
    val start_column: Int,
    val end_column: Int
)

/** 一个可执行的快速修复（内部持有 lsp4j CodeAction，app 层只拿标题）。 */
class gopls_code_action internal constructor(internal val action: CodeAction) {
    val title: String get() = action.title
}

/** severity 枚举转 1..4 整数（缺省 3=信息）。 */
private fun diagnostic_severity(d: org.eclipse.lsp4j.Diagnostic): Int =
    d.severity?.value ?: 3

/**
 * 当前文件的诊断快照（来自 gopls publishDiagnostics 推送到容器的最新状态）。
 * 未连接/无文件返回空表。
 */
fun gopls_lsp_project.current_diagnostics(file: File): List<gopls_diagnostic> {
    val lsp_editor = safe_editor(file) ?: return emptyList()
    return runCatching {
        lsp_editor.diagnostics.map { d ->
            gopls_diagnostic(
                severity = diagnostic_severity(d),
                message = if (d.message.isLeft) d.message.left ?: "" else d.message.right?.value ?: "",
                line = d.range.start.line,
                start_column = d.range.start.character,
                end_column = d.range.end.character
            )
        }
    }.getOrDefault(emptyList())
}

/**
 * 请求 [line] 行（0 起始）相关的快速修复（textDocument/codeAction）。
 * 超时 6s 或无结果返回空表。
 */
suspend fun gopls_lsp_project.request_code_actions(
    file: File,
    editor: CodeEditor,
    line: Int
): List<gopls_code_action> = withTimeoutOrNull(6000L) {
    val lsp_editor = safe_editor(file) ?: return@withTimeoutOrNull emptyList()
    val request_manager = lsp_editor.requestManager ?: return@withTimeoutOrNull emptyList()
    val line_count = editor.text.lineCount
    val target_line = line.coerceIn(0, (line_count - 1).coerceAtLeast(0))
    val start = editor.text.getIndexer().getCharPosition(target_line, 0).asLspPosition()
    val end = editor.text.getIndexer()
        .getCharPosition(target_line, editor.text.getColumnCount(target_line))
        .asLspPosition()
    // gopls 的 quickfix 只针对 context.diagnostics 里列出的诊断返回，
    // 所以把该行的诊断带上（容器里已是 lsp4j Diagnostic，直接复用）
    val line_diagnostics = lsp_editor.diagnostics.filter { it.range.start.line == target_line }
    val params = CodeActionParams(
        lsp_editor.uri.createTextDocumentIdentifier(),
        Range(start, end),
        CodeActionContext(line_diagnostics)
    )
    val result = request_manager.codeAction(params)?.await() ?: return@withTimeoutOrNull emptyList()
    result.mapNotNull { either ->
        either.right?.let { gopls_code_action(it) }
    }
}.orEmpty()

/**
 * 应用一个快速修复：带 edit 的走 workspace/applyEdit（sora-editor-lsp 内部
 * 事件把变更写回编辑器），嵌套 command 走 executeCommand。返回是否成功发起。
 */
fun gopls_lsp_project.apply_code_action(file: File, action: gopls_code_action): Boolean {
    val lsp_editor = safe_editor(file) ?: return false
    return runCatching {
        val edit = action.action.edit
        if (edit != null) {
            val params = ApplyWorkspaceEditParams().apply {
                label = action.action.title
                this.edit = edit
            }
            lsp_editor.eventManager.emit(EventType.workSpaceApplyEdit, params)
        }
        val command = action.action.command
        if (command != null) {
            lsp_editor.coroutineScope.launch {
                lsp_editor.eventManager.emitAsync(EventType.workSpaceExecuteCommand) {
                    put("command", command.command)
                    put("args", command.arguments ?: emptyList<String>())
                }
            }
        }
        true
    }.getOrDefault(false)
}

private fun gopls_lsp_project.safe_editor(file: File): LspEditor? =
    runCatching { project.getEditor(file.absolutePath) }.getOrNull()
