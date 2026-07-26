package com.jmwl.gostudio.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/**
 * 会话持久化：每个项目一个 JSONL 文件，每行一条消息。
 *
 * 存储位置：`<app home>/.ai/sessions/<session_id>.jsonl`
 * session_id 通常用项目名或 "global"（主界面通用问答）。
 *
 * 重启 app 后调 [load_session] 恢复历史。
 */
class ai_session_store(private val sessions_dir: File) {
    private val gson = Gson()

    init { sessions_dir.mkdirs() }

    private fun session_file(session_id: String): File = File(sessions_dir, "$session_id.jsonl")

    /** 保存整个对话历史（覆盖写） */
    fun save_session(session_id: String, messages: List<ai_message>) {
        val file = session_file(session_id)
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            // 只持久化有意义的消息（跳过 streaming 中的占位、空 assistant）
            for (msg in messages) {
                if (msg.role == ai_message_role.SYSTEM) continue
                if (msg.streaming) continue
                if (msg.role == ai_message_role.ASSISTANT && !msg.has_visible_text && msg.tool_calls.isEmpty()) continue
                val obj = message_to_json(msg)
                writer.write(obj.toString())
                writer.newLine()
            }
        }
    }

    /** 加载历史会话 */
    fun load_session(session_id: String): List<ai_message> {
        val file = session_file(session_id)
        if (!file.isFile) return emptyList()
        return runCatching {
            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.mapNotNull { line ->
                    runCatching { json_to_message(JsonParser.parseString(line).asJsonObject) }.getOrNull()
                }.toList()
            }
        }.getOrDefault(emptyList())
    }

    /** 列出所有会话（按修改时间倒序） */
    fun list_sessions(): List<Pair<String, Long>> {
        return sessions_dir.listFiles { f -> f.isFile && f.name.endsWith(".jsonl") }
            ?.map { it.nameWithoutExtension to it.lastModified() }
            ?.sortedByDescending { it.second }
            ?: emptyList()
    }

    /** 删除某会话 */
    fun delete_session(session_id: String) {
        session_file(session_id).delete()
    }

    private fun message_to_json(msg: ai_message): JsonObject {
        val obj = JsonObject()
        obj.addProperty("role", msg.role.name)
        obj.addProperty("text", msg.text)
        if (msg.tool_calls.isNotEmpty()) {
            obj.add("tool_calls", gson.toJsonTree(msg.tool_calls.map { tc ->
                mapOf("id" to tc.id, "name" to tc.name, "arguments_json" to tc.arguments_json)
            }))
        }
        if (msg.tool_call_id.isNotEmpty()) obj.addProperty("tool_call_id", msg.tool_call_id)
        if (msg.is_error) obj.addProperty("is_error", true)
        return obj
    }

    private fun json_to_message(obj: JsonObject): ai_message {
        val role = runCatching { ai_message_role.valueOf(obj.get("role").asString) }.getOrDefault(ai_message_role.USER)
        val text = obj.get("text")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val tool_calls = obj.getAsJsonArray("tool_calls")?.map { tc ->
            val tcObj = tc.asJsonObject
            ai_tool_call(
                id = tcObj.get("id").asString,
                name = tcObj.get("name").asString,
                arguments_json = tcObj.get("arguments_json").asString
            )
        } ?: emptyList()
        val tool_call_id = obj.get("tool_call_id")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val is_error = obj.get("is_error")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
        return ai_message(
            role = role, text = text, tool_calls = tool_calls,
            tool_call_id = tool_call_id, is_error = is_error
        )
    }
}

/**
 * 上下文压缩（Compaction，参考 pi）。
 *
 * 当消息历史过长时，把旧消息（保留最近若干条）摘要成一条 summary 消息，
 * 避免每轮请求都带着完整历史导致 token 爆炸。
 *
 * 简化实现：不是真正调用模型做摘要（那需要额外请求），而是把旧消息的关键信息
 * （工具调用记录、关键结论）压缩成结构化文本。真正的模型摘要可作为后续增强。
 */
object ai_compaction {
    /**
     * 压缩消息历史。
     * @param messages 完整历史（不含 system）
     * @param max_chars 字符上限
     * @param keep_recent 保留最近多少条不压缩
     * @return 压缩后的消息列表（前面是 summary，后面是 recent 原文）
     */
    fun compact(messages: List<ai_message>, max_chars: Int, keep_recent: Int = 10): List<ai_message> {
        val total = messages.sumOf { it.text.length + it.tool_calls.sumOf { tc -> tc.arguments_json.length } }
        if (total <= max_chars) return messages
        if (messages.size <= keep_recent) return messages

        val toCompact = messages.dropLast(keep_recent)
        val recent = messages.takeLast(keep_recent)

        val summary = build_summary(toCompact)
        return listOf(summary) + recent
    }

    /** 把一批旧消息压缩成结构化摘要 */
    private fun build_summary(messages: List<ai_message>): ai_message {
        val sb = StringBuilder()
        sb.appendLine("（以下是之前对话的摘要，详细内容已省略以节省空间）")
        for (msg in messages) {
            when (msg.role) {
                ai_message_role.USER -> {
                    sb.appendLine("[用户] ${msg.text.take(200)}")
                }
                ai_message_role.ASSISTANT -> {
                    if (msg.has_visible_text) sb.appendLine("[助手] ${msg.text.take(300)}")
                    for (tc in msg.tool_calls) {
                        val result = msg.tool_executions.firstOrNull { it.call.id == tc.id }
                        sb.appendLine("  └ 调用工具 ${tc.name}(${tc.arguments_json.take(80)}) → ${result?.to_result_content()?.take(150) ?: "(未记录)"}")
                    }
                }
                ai_message_role.TOOL -> {
                    // 工具结果已在 assistant 消息的 tool_executions 里摘要，跳过
                }
                else -> {}
            }
        }
        return ai_message(role = ai_message_role.USER, text = sb.toString())
    }
}
