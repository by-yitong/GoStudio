/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

package io.github.rosemoe.sora.lsp.events.document

import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.utils.createDidChangeTextDocumentParams
import io.github.rosemoe.sora.lsp.utils.createTextDocumentContentChangeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import java.util.concurrent.CompletableFuture

class DocumentChangeEvent : AsyncEventListener() {
    override val eventName = EventType.documentChange

    var future: CompletableFuture<Void>? = null

    override suspend fun doHandleAsync(context: EventContext) {
        val editor = context.get<LspEditor>("lsp-editor")
        val event = context.getByClass<ContentChangeEvent>() ?: return

        val params = createDidChangeTextDocumentParams(editor, event)

        editor.requestManager.let { requestManager ->
            future = CompletableFuture.runAsync {
                requestManager.didChange(
                    params
                )
            }

            withContext(Dispatchers.IO) {
                future?.get()
            }

        }
    }

    override fun dispose() {
        future?.cancel(true)
        future = null
    }

    private fun createFullTextDocumentContentChangeEvent(editor: LspEditor): List<TextDocumentContentChangeEvent> {
        return listOf(
            editor.uri.createTextDocumentContentChangeEvent(editor.editorContent)
        )
    }

    private fun createDidChangeTextDocumentParams(
        editor: LspEditor,
        @Suppress("UNUSED_PARAMETER") data: ContentChangeEvent
    ): DidChangeTextDocumentParams {
        // 始终全量同步：增量同步的删除区间换算在删行时会与 gopls 侧文本错位，
        // 导致诊断行号整体偏移（删掉错误代码后错误提示串到上一行，重开文件才恢复）。
        // 手机场景文件小，全量同步代价可忽略，换取一致性。
        return editor.uri.createDidChangeTextDocumentParams(
            createFullTextDocumentContentChangeEvent(editor)
        )
    }
}

val EventType.documentChange: String
    get() = "textDocument/didChange"