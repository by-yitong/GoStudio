package io.github.rosemoe.sora.lsp.editor.documentation

import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.SignatureHelp

/**
 * Translates human-readable documentation returned by a language server.
 * Implementations must never change code snippets, identifiers, or signatures.
 */
interface LspDocumentationTranslator {
    val isLoadingEnabled: Boolean get() = false

    suspend fun translateHover(hover: Hover?): Hover?
    suspend fun translateSignatureHelp(signatureHelp: SignatureHelp?): SignatureHelp?

    companion object {
        val NOOP: LspDocumentationTranslator = object : LspDocumentationTranslator {
            override suspend fun translateHover(hover: Hover?): Hover? = hover
            override suspend fun translateSignatureHelp(signatureHelp: SignatureHelp?): SignatureHelp? = signatureHelp
        }
    }
}
