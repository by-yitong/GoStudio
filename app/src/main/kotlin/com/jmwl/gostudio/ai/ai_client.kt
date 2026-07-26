package com.jmwl.gostudio.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * AI 流式响应回调。
 * - on_text: 收到一段文本增量（可能很短，如单个字/词），UI 累加显示
 * - on_done: 一轮回复完成，[tool_calls] 为模型要求调用的工具（可能为空）
 */
interface ai_stream_callback {
    fun on_text(delta: String)
    fun on_done(tool_calls: List<ai_tool_call>)
    fun on_error(message: String)
}

/**
 * OpenAI 兼容 API 客户端（智谱/DeepSeek/Kimi/OpenAI/xAI 都用这个）。
 * Anthropic 用不同格式，第一版暂不支持（会提示用户用兼容端点）。
 *
 * 流式 SSE：POST {base_url}/chat/completions 带 stream:true，
 * 响应逐行 `data: {json}`，`data: [DONE]` 结束。
 */
class ai_client(
    private val settings: ai_settings_state
) {
    private val gson = Gson()
    private val json_media_type = "application/json; charset=utf-8".toMediaType()

    private val http_client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // 流式响应可能持续很久
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 发起一次流式对话请求。
     * @param messages 完整消息历史（含 system）
     * @param tools 工具定义（OpenAI tools 数组），空则不带 tools 字段
     * @param callback 流式回调（on_text 在 IO 线程触发，UI 层自行切主线程）
     */
    suspend fun stream_chat(
        messages: List<ai_message>,
        tools: List<Map<String, Any>>,
        callback: ai_stream_callback
    ) {
        if (!settings.is_configured()) {
            callback.on_error("AI 未配置：请在设置里填写 base_url、model、api_key")
            return
        }
        if (settings.provider == ai_provider.ANTHROPIC) {
            // 第一版：Anthropic 用官方 Messages API，格式与 OpenAI 不同，暂走不了。
            // 提示用户改用第三方 OpenAI 兼容中转，或后续版本适配。
            callback.on_error("Anthropic 原生格式暂不支持，请用 OpenAI 兼容端点（如中转服务）或换其他提供商")
            return
        }

        val url = settings.base_url.trimEnd('/') + "/chat/completions"
        val body = build_request_body(messages, tools)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${settings.api_key}")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(body.toRequestBody(json_media_type))
            .build()

        try {
            http_client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()?.take(500) ?: ""
                    callback.on_error("请求失败 ${response.code}: ${errBody.ifBlank { response.message }}")
                    return
                }
                parse_sse_stream(response.body?.byteStream() ?: return, callback)
            }
        } catch (e: Exception) {
            callback.on_error("网络错误: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun build_request_body(messages: List<ai_message>, tools: List<Map<String, Any>>): String {
        val payload = linkedMapOf<String, Any>(
            "model" to settings.model,
            "messages" to messages.map { it.to_api_map() },
            "stream" to true
        )
        if (tools.isNotEmpty()) {
            payload["tools"] = tools
        }
        return gson.toJson(payload)
    }

    /**
     * 解析 SSE 流：逐行读 `data: {...}`，累积 content delta 和 tool_calls delta。
     * OpenAI 流式协议要点：
     * - 每个 chunk 的 choices[0].delta.content 是文本增量
     * - choices[0].delta.tool_calls[i] 是工具调用增量（按 index 拼接 arguments）
     * - `data: [DONE]` 表示流结束
     */
    private fun parse_sse_stream(stream: java.io.InputStream, callback: ai_stream_callback) {
        // tool_calls 累积器：index -> (id, name, arguments_builder)
        val tool_acc = mutableMapOf<Int, Triple<String, String, StringBuilder>>()
        val reader = stream.bufferedReader(Charsets.UTF_8)

        while (true) {
            val line = reader.readLine() ?: break
            if (!line.startsWith("data:")) continue
            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") break
            if (data.isEmpty()) continue

            val chunk = runCatching { JsonParser.parseString(data).asJsonObject }.getOrElse { continue }
            val choice = chunk.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject ?: continue
            // 流式错误（部分服务把错误塞在 chunk 里）
            chunk.get("error")?.asJsonObject?.let { err ->
                callback.on_error("API 错误: ${err.get("message")?.asString ?: data}")
                return
            }
            val delta = choice.getAsJsonObject("delta") ?: continue

            // 文本增量
            delta.get("content")?.takeIf { !it.isJsonNull }?.asString?.let { text ->
                if (text.isNotEmpty()) callback.on_text(text)
            }

            // 工具调用增量
            delta.getAsJsonArray("tool_calls")?.forEach { tcElem ->
                val tc = tcElem.asJsonObject
                val index = tc.get("index")?.asInt ?: 0
                val fn = tc.getAsJsonObject("function") ?: return@forEach
                val existing = tool_acc[index]
                val id = tc.get("id")?.takeIf { !it.isJsonNull }?.asString ?: existing?.first ?: ""
                val name = fn.get("name")?.takeIf { !it.isJsonNull }?.asString ?: existing?.second ?: ""
                val argsPart = fn.get("arguments")?.takeIf { !it.isJsonNull }?.asString ?: ""
                val argsBuilder = existing?.third ?: StringBuilder()
                argsBuilder.append(argsPart)
                tool_acc[index] = Triple(id, name, argsBuilder)
            }
        }

        // 组装最终 tool_calls（按 index 排序）
        val tool_calls = tool_acc.toSortedMap().values.map { (id, name, args) ->
            ai_tool_call(id = id, name = name, arguments_json = args.toString())
        }
        callback.on_done(tool_calls)
    }
}
