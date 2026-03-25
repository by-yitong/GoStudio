package com.termux.app.gostudio.lsp

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * LSP 服务封装
 */
class LspService(
    private val goplsPath: String
) {
    companion object {
        private const val TAG = "LspService"
    }

    private var client: LspClient? = null
    private val documentVersions = ConcurrentHashMap<String, Int>()
    private val openedDocuments = ConcurrentHashMap<String, String>()

    var onDiagnostics: ((String, List<Diagnostic>) -> Unit)? = null

    fun start(): Boolean {
        if (client?.isAlive() == true) return true
        client = LspClient(goplsPath)
        client?.setNotificationHandler { method, params ->
            if (method == "textDocument/publishDiagnostics" && params != null) {
                val diagParams = PublishDiagnosticsParams.fromJson(params)
                onDiagnostics?.invoke(diagParams.uri, diagParams.diagnostics)
            }
        }
        return client?.start() ?: false
    }

    fun initialize(rootUri: String, callback: (Boolean) -> Unit) {
        val c = client ?: run { callback(false); return }

        val capabilities = JSONObject().apply {
            put("textDocument", JSONObject().apply {
                put("completion", JSONObject().apply {
                    put("completionItem", JSONObject().apply {
                        put("snippetSupport", false)
                        put("commitCharactersSupport", false)
                    })
                    put("contextSupport", false)
                })
                put("hover", JSONObject().apply {
                    put("contentFormat", JSONArray().apply { put("plaintext") })
                    put("support", true)
                })
                put("synchronization", JSONObject().apply {
                    put("dynamicRegistration", false)
                    put("willSave", false)
                    put("willSaveWaitUntil", false)
                    put("didSave", true)
                })
            })
        }

        val params = InitializeParams(
            processId = android.os.Process.myPid().toLong(),
            rootUri = rootUri,
            capabilities = capabilities
        )

        c.sendRequestAsync("initialize", params.toJson()) { response ->
            if (response.error != null) {
                Log.e(TAG, "LSP initialize 失败: ${response.error.message}")
                callback(false)
            } else {
                c.sendNotification("initialized", JSONObject())
                callback(true)
            }
        }
    }

    fun didOpen(fileUri: String, content: String, languageId: String = "go") {
        val c = client ?: return
        val version = documentVersions.compute(fileUri) { _, v -> (v ?: 0) + 1 }!!
        openedDocuments[fileUri] = content
        val params = DidOpenTextDocumentParams(TextDocumentItem(fileUri, languageId, version, content))
        c.sendNotification("textDocument/didOpen", params.toJson())
    }

    fun didChange(fileUri: String, fullContent: String) {
        val c = client ?: return
        val version = documentVersions.compute(fileUri) { _, v -> (v ?: 0) + 1 }!!
        openedDocuments[fileUri] = fullContent
        val changes = JSONArray().apply { put(TextDocumentContentChangeEvent(fullContent).toJson()) }
        val params = DidChangeTextDocumentParams(VersionedTextDocumentIdentifier(fileUri, version), changes)
        c.sendNotification("textDocument/didChange", params.toJson())
    }

    fun getCompletions(fileUri: String, line: Int, column: Int, callback: (List<CompletionItem>) -> Unit) {
        val c = client ?: run { callback(emptyList()); return }
        val params = CompletionParams(TextDocumentIdentifier(fileUri), Position(line, column))
        c.sendRequestAsync("textDocument/completion", params.toJson()) { response ->
            if (response.error != null) {
                callback(emptyList())
            } else {
                callback(CompletionList.parse(response.result))
            }
        }
    }

    fun didSave(fileUri: String) {
        val c = client ?: return
        val params = JSONObject().apply { put("textDocument", TextDocumentIdentifier(fileUri).toJson()) }
        c.sendNotification("textDocument/didSave", params)
    }

    fun didClose(fileUri: String) {
        val c = client ?: return
        val params = JSONObject().apply { put("textDocument", TextDocumentIdentifier(fileUri).toJson()) }
        c.sendNotification("textDocument/didClose", params)
        openedDocuments.remove(fileUri)
        documentVersions.remove(fileUri)
    }

    fun getDefinition(fileUri: String, line: Int, column: Int, callback: (List<Location>) -> Unit) {
        val c = client ?: run { Log.w(TAG, "getDefinition: client is null"); callback(emptyList()); return }
        Log.d(TAG, "getDefinition: $fileUri:$line:$column")
        val params = JSONObject().apply {
            put("textDocument", JSONObject().apply { put("uri", fileUri) })
            put("position", JSONObject().apply { put("line", line); put("character", column) })
        }
        c.sendRequestAsync("textDocument/definition", params) { response ->
            Log.d(TAG, "getDefinition response: ${response.result}")
            if (response.error != null) {
                Log.w(TAG, "getDefinition error: ${response.error}")
                callback(emptyList())
            } else {
                val result = response.result
                val locations = mutableListOf<Location>()
                if (result is JSONArray) {
                    for (i in 0 until result.length()) {
                        locations.add(Location.parse(result.getJSONObject(i)))
                    }
                } else if (result is JSONObject) {
                    locations.add(Location.parse(result))
                }
                callback(locations)
            }
        }
    }

    fun shutdown() {
        try { client?.shutdown() } catch (e: Exception) { Log.w(TAG, "LSP shutdown 异常", e) }
        client = null
        documentVersions.clear()
        openedDocuments.clear()
    }

    fun isAlive(): Boolean = client?.isAlive() ?: false
}
