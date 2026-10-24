package com.jmwl.gostudio.lsp.gopls

import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.lsp4j.DocumentSymbolParams
import java.io.File

/**
 * VS Code 式预热：对文件请求一次 documentSymbol，强制 gopls 完成该文件所属包
 * 及其依赖树（如 net/http 这类标准库大包）的类型检查。结果丢弃，只为让签名
 * 提示/补全/悬停在包冷启动时不再超时。30s 兜底，失败静默（预热尽力而为）。
 */
suspend fun gopls_lsp_project.warm_up_analysis(file: File) {
    val lsp_editor = runCatching {
        project.getEditor(file.absolutePath)
    }.getOrNull() ?: return
    if (!lsp_editor.isConnected) return
    withTimeoutOrNull(30_000) {
        runCatching {
            lsp_editor.requestManager?.documentSymbol(
                DocumentSymbolParams(lsp_editor.uri.createTextDocumentIdentifier())
            )?.await()
        }
    }
}
