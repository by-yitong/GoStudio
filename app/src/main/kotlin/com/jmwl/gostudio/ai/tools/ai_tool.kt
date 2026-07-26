package com.jmwl.gostudio.ai.tools

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AI 工具接口（内置工具 read/write/bash 等和 MCP 工具都实现这个接口）。
 *
 * 模型通过 OpenAI tool calling 调用工具时，[execute] 被调用，参数从模型返回的 JSON 解析。
 */
interface ai_tool {
    /** 工具名（模型调用时用，必须唯一） */
    val name: String
    /** 给模型看的工具描述（决定模型什么时候调它） */
    val description: String
    /**
     * 参数的 JSON Schema（OpenAI tools.function.parameters 格式）。
     * Gson JsonObject，序列化时直接 toString()。
     */
    val parameters_schema: JsonObject

    /**
     * 执行工具。
     * @param params 模型传来的参数（已解析成 JsonObject）
     * @return 工具结果文本（会作为 tool 消息内容回填给模型）
     */
    suspend fun execute(params: JsonObject): String
}

/**
 * 解析 JsonObject 里的字符串字段，缺失返回默认值。
 * 模型偶尔会把参数传成 null 或漏字段，这里统一兜底。
 */
fun JsonObject.string_or(key: String, default: String = ""): String {
    val elem = get(key) ?: return default
    if (elem.isJsonNull) return default
    return if (elem.isJsonPrimitive) elem.asString else elem.toString()
}

fun JsonObject.int_or(key: String, default: Int): Int {
    val elem = get(key) ?: return default
    return if (elem.isJsonPrimitive && elem.asJsonPrimitive.isNumber) elem.asInt else default
}

/** 把字符串解析成 JsonObject（模型返回的 arguments 是 JSON 字符串） */
fun parse_json_object(json: String): JsonObject {
    return runCatching { JsonParser.parseString(json).asJsonObject }.getOrElse { JsonObject() }
}

/**
 * 工具注册表：管理所有可用工具（内置 + MCP 动态注册）。
 */
class ai_tool_registry {
    private val tools = linkedMapOf<String, ai_tool>()

    fun register(tool: ai_tool) { tools[tool.name] = tool }
    fun unregister(name: String) { tools.remove(name) }
    fun get(name: String): ai_tool? = tools[name]
    fun all(): List<ai_tool> = tools.values.toList()

    /**
     * 生成发给 OpenAI API 的 tools 数组（每个工具转成 function 描述）。
     * 返回 null 表示没有可用工具（API 请求里不带 tools 字段）。
     */
    fun to_api_tools(): List<Map<String, Any>> {
        return all().map { tool ->
            mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to tool.parameters_schema.toString()
                )
            )
        }
    }
}

/** 安全执行工具（带超时），失败返回错误信息而非抛异常 */
suspend fun ai_tool.execute_safely(params: JsonObject, timeout_ms: Long = 30_000L): String {
    return withTimeoutOrNull(timeout_ms) {
        withContext(Dispatchers.IO) {
            try {
                execute(params)
            } catch (e: Exception) {
                "工具 $name 执行异常: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    } ?: "工具 $name 执行超时（${timeout_ms}ms）"
}
