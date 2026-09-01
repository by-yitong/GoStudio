package com.jmwl.gostudio.lsp.gopls.translation

import com.google.gson.annotations.SerializedName
import io.github.rosemoe.sora.lsp.editor.documentation.LspDocumentationTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.MarkedString
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections

data class gopls_translation_settings(
    val enabled: Boolean = false,
    val endpoint: String = "",
    val backend_api_key: String = "",
    val target_language: String = "zh-CN",
    val timeout_millis: Int = 4000
)

private data class translation_request(
    val text: String,
    @SerializedName("source_language") val source_language: String = "en",
    @SerializedName("target_language") val target_language: String,
    val kind: String
)

private data class translation_response(
    @SerializedName("translated_text") val translated_text: String = ""
)

/**
 * Calls a GoStudio translation backend and rewrites only LSP documentation content.
 * Network failures are swallowed so hover/signature help still shows the original text.
 */
class gopls_documentation_translator(
    settings: gopls_translation_settings
) : LspDocumentationTranslator {

    @Volatile
    private var current_settings: gopls_translation_settings = settings

    override val isLoadingEnabled: Boolean
        get() = is_enabled()

    fun update_settings(settings: gopls_translation_settings) {
        current_settings = settings
    }

    private val cache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > 256
            }
        }
    )

    override suspend fun translateHover(hover: Hover?): Hover? {
        if (!is_enabled() || hover?.contents == null) return hover

        val contents = hover.contents
        if (contents.isRight) {
            translate_markup(contents.right, "gopls-hover")
        } else {
            val items = contents.left.orEmpty().toMutableList()
            items.forEachIndexed { index, item ->
                if (item.isLeft) {
                    val original = item.left ?: return@forEachIndexed
                    val translated = translate_if_needed(original, "gopls-hover")
                    if (translated != null) items[index] = Either.forLeft(translated)
                } else {
                    translate_marked_string(item.right, "gopls-hover")
                }
            }
            hover.contents = Either.forLeft(items)
        }
        return hover
    }

    override suspend fun translateSignatureHelp(signatureHelp: SignatureHelp?): SignatureHelp? {
        if (!is_enabled() || signatureHelp == null) return signatureHelp

        signatureHelp.signatures.orEmpty().forEach { signature ->
            val documentation = signature.documentation ?: return@forEach
            signature.documentation = translate_signature_documentation(
                documentation,
                "gopls-signature"
            )

            signature.parameters.orEmpty().forEach { parameter ->
                val parameter_documentation = parameter.documentation ?: return@forEach
                parameter.documentation = translate_signature_documentation(
                    parameter_documentation,
                    "gopls-parameter"
                )
            }
        }
        return signatureHelp
    }

    private suspend fun translate_signature_documentation(
        documentation: Either<String, MarkupContent>,
        kind: String
    ): Either<String, MarkupContent> {
        if (documentation.isLeft) {
            val value = documentation.left ?: return documentation
            val translated = translate_if_needed(value, kind) ?: return documentation
            return Either.forLeft(translated)
        }

        translate_markup(documentation.right, kind)
        return documentation
    }

    private suspend fun translate_marked_string(markedString: MarkedString?, kind: String) {
        val markup = markedString ?: return
        if (markup.language.isNullOrBlank()) {
            val translated = translate_if_needed(markup.value, kind)
            if (translated != null) markup.value = translated
        }
    }

    private suspend fun translate_markup(markup: MarkupContent?, kind: String) {
        val value = markup?.value ?: return
        val translated = translate_if_needed(value, kind)
        if (translated != null) markup.value = translated
    }

    private suspend fun translate_if_needed(value: String, kind: String): String? {
        val trimmed = value.trim()
        if (!has_translatable_prose(trimmed) || looks_like_target_language(trimmed)) return null
        val key = cache_key(kind, value)
        cache[key]?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching { request_translation(value, kind) }.getOrNull()
        }?.also { translated ->
            if (translated.isNotBlank()) cache[key] = translated else translated
        }?.takeIf { it.isNotBlank() }
    }

    private fun is_enabled(): Boolean {
        if (!current_settings.enabled) return false
        val endpoint = current_settings.endpoint.trim().trimEnd('/')
        if (endpoint.isBlank()) return false
        val uri = runCatching { URI(endpoint) }.getOrNull() ?: return false
        return uri.scheme == "http" || uri.scheme == "https"
    }

    private fun request_translation(text: String, kind: String): String {
        val endpoint = current_settings.endpoint.trim().trimEnd('/') + "/v1/translate"
        val connection = URI(endpoint).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = current_settings.timeout_millis.coerceIn(500, 15_000)
            connection.readTimeout = current_settings.timeout_millis.coerceIn(500, 15_000)
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            if (current_settings.backend_api_key.isNotBlank()) {
                connection.setRequestProperty("X-GoStudio-Translation-Key", current_settings.backend_api_key)
            }

            val payload = gson.toJson(
                translation_request(
                    text = text,
                    target_language = current_settings.target_language.ifBlank { "zh-CN" },
                    kind = kind
                )
            ).toByteArray(StandardCharsets.UTF_8)
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { output -> output.write(payload) }

            val status = connection.responseCode
            val response = read_stream(
                if (status in 200..299) connection.inputStream else connection.errorStream
            )
            if (status !in 200..299) {
                throw IllegalStateException("translation backend returned HTTP $status")
            }
            val decoded = gson.fromJson(response, translation_response::class.java)
            require(!decoded.translated_text.isBlank()) { "translation backend returned empty text" }
            return decoded.translated_text
        } finally {
            connection.disconnect()
        }
    }

    private fun read_stream(stream: java.io.InputStream?): String {
        if (stream == null) return ""
        return stream.use { input ->
            val buffer = ByteArray(4096)
            val output = ByteArrayOutputStream()
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                if (output.size() > 128 * 1024) throw IllegalStateException("translation response is too large")
            }
            output.toString("UTF-8")
        }
    }

    private fun cache_key(kind: String, text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((current_settings.endpoint + '\u0000' + current_settings.target_language + '\u0000' + kind + '\u0000' + text).toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun has_translatable_prose(markdown: String): Boolean {
        val without_fenced_code = markdown.replace(Regex("```.*?```", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("~~~.*?~~~", RegexOption.DOT_MATCHES_ALL), "")
        return without_fenced_code.any { it.isLetter() }
    }

    private fun looks_like_target_language(text: String): Boolean {
        val target_chars = text.count { char -> Character.UnicodeScript.of(char.code) == Character.UnicodeScript.HAN }
        val letters = text.count { char -> char.isLetter() }
        return letters > 0 && target_chars.toFloat() / letters >= 0.30f
    }

    private companion object {
        private val gson = com.google.gson.Gson()
    }
}
