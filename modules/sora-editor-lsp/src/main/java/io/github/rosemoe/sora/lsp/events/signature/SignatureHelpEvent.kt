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

package io.github.rosemoe.sora.lsp.events.signature

import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.text.CharPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureHelpParams
import java.util.concurrent.CompletableFuture

class SignatureHelpEvent : AsyncEventListener() {
    override val eventName: String = EventType.signatureHelp

    var future: CompletableFuture<Void>? = null

    override val isAsync = true

    override suspend fun doHandleAsync(context: EventContext) = withContext(Dispatchers.IO) {
        val editor = context.get<LspEditor>("lsp-editor")
        val position = context.getByClass<CharPosition>() ?: return@withContext

        val requestManager = editor.requestManager

        val signatureHelpParams = SignatureHelpParams(
            editor.uri.createTextDocumentIdentifier(),
            position.asLspPosition()
        )

        // VS Code 式"不轻易放弃"：包冷启动（如 net/http 首次触达）时 gopls 响应慢或为空，
        // 首次尝试失败后延迟重试一次，期间不硬性放弃，让晚到的答案仍能弹出。
        var signatureHelp: SignatureHelp? = null
        var attempt = 0
        while (attempt < 2 && signatureHelp?.signatures.isNullOrEmpty()) {
            attempt++
            val future = requestManager.signatureHelp(signatureHelpParams) ?: return@withContext
            this@SignatureHelpEvent.future = future.thenAccept { }
            try {
                withTimeout(Timeout[Timeouts.SIGNATURE].toLong()) {
                    signatureHelp = future.await()
                }
            } catch (e: Exception) {
                // 超时/失败：空结果走重试，第二次仍失败则放弃
            }
            if (attempt == 1 && signatureHelp?.signatures.isNullOrEmpty()) {
                delay(2500)
            }
        }

        if (signatureHelp?.signatures.isNullOrEmpty()) {
            editor.showSignatureHelp(null)
            return@withContext
        }

        val translatedSignatureHelp = editor.project.documentationTranslator.translateSignatureHelp(signatureHelp)
        editor.showSignatureHelp(translatedSignatureHelp)
    }

    override fun dispose() {
        future?.cancel(true)
        future = null
    }

}

val EventType.signatureHelp: String
    get() = "textDocument/signatureHelp"