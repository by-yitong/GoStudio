package com.termux.app.gostudio.lsp

import org.json.JSONArray
import org.json.JSONObject

// ===== JSON-RPC 基础 =====

data class JsonRpcRequest(
    val id: Int,
    val method: String,
    val params: JSONObject? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("jsonrpc", "2.0")
        json.put("id", id)
        json.put("method", method)
        params?.let { json.put("params", it) }
        return json.toString()
    }
}

data class JsonRpcNotification(
    val method: String,
    val params: JSONObject? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("jsonrpc", "2.0")
        json.put("method", method)
        params?.let { json.put("params", it) }
        return json.toString()
    }
}

data class JsonRpcResponse(
    val id: Int,
    val result: Any? = null,
    val error: JsonRpcError? = null
) {
    data class JsonRpcError(val code: Int, val message: String)

    companion object {
        fun fromJson(json: JSONObject): JsonRpcResponse {
            val result = json.opt("result") // 可以是 JSONObject、JSONArray 或 null
            val errorObj = json.optJSONObject("error")
            val error = if (errorObj != null) {
                JsonRpcError(
                    code = errorObj.getInt("code"),
                    message = errorObj.getString("message")
                )
            } else null
            return JsonRpcResponse(
                id = json.getInt("id"),
                result = result,
                error = error
            )
        }
    }
}

// ===== 位置相关 =====

data class Position(val line: Int, val character: Int) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("line", line)
        put("character", character)
    }

    companion object {
        fun fromJson(json: JSONObject): Position = Position(
            line = json.getInt("line"),
            character = json.getInt("character")
        )
    }
}

data class Range(val start: Position, val end: Position) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("start", start.toJson())
        put("end", end.toJson())
    }

    companion object {
        fun fromJson(json: JSONObject): Range = Range(
            start = Position.fromJson(json.getJSONObject("start")),
            end = Position.fromJson(json.getJSONObject("end"))
        )
    }
}

// ===== 文本文档 =====

data class TextDocumentIdentifier(val uri: String) {
    fun toJson(): JSONObject = JSONObject().apply { put("uri", uri) }
}

data class TextDocumentItem(
    val uri: String,
    val languageId: String,
    val version: Int,
    val text: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("uri", uri)
        put("languageId", languageId)
        put("version", version)
        put("text", text)
    }
}

data class TextDocumentContentChangeEvent(val text: String) {
    fun toJson(): JSONObject = JSONObject().apply { put("text", text) }
}

data class Location(val uri: String, val range: Range) {
    companion object {
        fun parse(json: JSONObject): Location = Location(
            uri = json.getString("uri"),
            range = Range.fromJson(json.getJSONObject("range"))
        )
    }
}

data class VersionedTextDocumentIdentifier(val uri: String, val version: Int) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("uri", uri)
        put("version", version)
    }
}

// ===== Initialize =====

data class InitializeParams(
    val processId: Long? = null,
    val rootUri: String? = null,
    val capabilities: JSONObject
) {
    fun toJson(): JSONObject = JSONObject().apply {
        processId?.let { put("processId", it) }
        rootUri?.let { put("rootUri", it) }
        put("capabilities", capabilities)
        val textDocument = JSONObject().apply {
            put("sync", JSONObject().apply { put("fullSync", true) })
            put("completion", JSONObject().apply {
                put("completionItem", JSONObject().apply { put("snippetSupport", false) })
            })
        }
        put("textDocument", textDocument)
    }
}

// ===== 补全 =====

data class CompletionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("textDocument", textDocument.toJson())
        put("position", position.toJson())
    }
}

data class CompletionItem(
    val label: String,
    val kind: Int = 0,
    val detail: String? = null,
    val insertText: String? = null,
    val documentation: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): CompletionItem = CompletionItem(
            label = json.getString("label"),
            kind = json.optInt("kind", 0),
            detail = json.optString("detail", null),
            insertText = json.optString("insertText", null),
            documentation = json.optString("documentation", null)
        )

        fun kindLabel(kind: Int): String = when (kind) {
            1 -> "文本"; 2 -> "方法"; 3 -> "函数"; 4 -> "构造函数"; 5 -> "字段"
            6 -> "变量"; 7 -> "类"; 8 -> "接口"; 9 -> "模块"; 10 -> "属性"
            11 -> "单元"; 12 -> "值"; 13 -> "枚举"; 14 -> "关键字"; 15 -> "片段"
            16 -> "文件"; 17 -> "引用"; 18 -> "文件夹"; 19 -> "枚举成员"; 20 -> "常量"
            21 -> "结构体"; 22 -> "事件"; 23 -> "运算符"; 24 -> "类型参数"
            else -> "未知"
        }

        fun kindIcon(kind: Int): String = when (kind) {
            3, 2 -> "f "; 6 -> "v "; 20 -> "c "; 21 -> "S "; 8 -> "I "
            14 -> "K "; 7 -> "C "; 5 -> "f "; 10 -> "p "; 22 -> "e "
            else -> "? "
        }
    }
}

data class CompletionList(
    val isIncomplete: Boolean,
    val items: List<CompletionItem>
) {
    companion object {
        fun fromJson(json: JSONObject): CompletionList {
            val itemsArray = json.optJSONArray("items")
            val items = mutableListOf<CompletionItem>()
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    items.add(CompletionItem.fromJson(itemsArray.getJSONObject(i)))
                }
            }
            return CompletionList(
                isIncomplete = json.optBoolean("isIncomplete", false),
                items = items
            )
        }

        fun parse(result: Any?): List<CompletionItem> {
            val json = result as? JSONObject ?: return emptyList()
            if (result == null) return emptyList()
            if (result.has("items")) return fromJson(result).items
            if (result.length() == 0) return emptyList()
            if (result.has("label")) return listOf(CompletionItem.fromJson(result))
            return emptyList()
        }
    }
}

// ===== 诊断 =====

data class Diagnostic(
    val range: Range,
    val severity: Int,
    val message: String,
    val source: String? = null
) {
    enum class Severity { ERROR, WARNING, INFO, HINT }

    val severityLevel: Severity
        get() = when (severity) {
            1 -> Severity.ERROR; 2 -> Severity.WARNING; 3 -> Severity.INFO; 4 -> Severity.HINT
            else -> Severity.ERROR
        }

    companion object {
        fun fromJson(json: JSONObject): Diagnostic = Diagnostic(
            range = Range.fromJson(json.getJSONObject("range")),
            severity = json.optInt("severity", 1),
            message = json.getString("message"),
            source = json.optString("source", null)
        )
    }
}

data class PublishDiagnosticsParams(
    val uri: String,
    val diagnostics: List<Diagnostic>
) {
    companion object {
        fun fromJson(json: JSONObject): PublishDiagnosticsParams {
            val uri = json.getString("uri")
            val diagArray = json.getJSONArray("diagnostics")
            val diagnostics = mutableListOf<Diagnostic>()
            for (i in 0 until diagArray.length()) {
                diagnostics.add(Diagnostic.fromJson(diagArray.getJSONObject(i)))
            }
            return PublishDiagnosticsParams(uri, diagnostics)
        }
    }
}

// ===== DidOpen / DidChange =====

data class DidOpenTextDocumentParams(val textDocument: TextDocumentItem) {
    fun toJson(): JSONObject = JSONObject().apply { put("textDocument", textDocument.toJson()) }
}

data class DidChangeTextDocumentParams(
    val textDocument: VersionedTextDocumentIdentifier,
    val contentChanges: JSONArray
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("textDocument", textDocument.toJson())
        put("contentChanges", contentChanges)
    }
}
