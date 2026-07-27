package com.jmwl.gostudio.ai.mcp

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jmwl.gostudio.ai.tools.ai_tool
import com.jmwl.gostudio.ai.tools.ai_tool_registry
import com.jmwl.gostudio.ai.tools.string_or
import com.jmwl.gostudio.toolchain.runtime.proot_command_builder
import com.jmwl.gostudio.toolchain.runtime.toolchain_runtime_paths
import com.jmwl.gostudio.toolchain.toolchain_runtime_provider
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * MCP（Model Context Protocol）客户端管理器。
 *
 * 让 agent 能调用外部工具服务器。MCP server 是独立进程，通过 stdin/stdout
 * 做 JSON-RPC（LSP 式 Content-Length 分帧）。
 *
 * 在 Android 上，MCP server 通过 proot rootfs 启动（rootfs 内有 node/python 等）。
 *
 * 配置：`<project>/.ai/mcp.json`，格式：
 * ```json
 * {
 *   "mcpServers": {
 *     "server-name": { "command": "node", "args": ["/path/server.js"], "env": {} }
 *   }
 * }
 * ```
 * 或 stdio 命令字符串："command args"
 *
 * 工作流：start() → 对每个 server：拉起进程 → initialize → tools/list → 注册工具
 *        agent 调工具时 → tools/call 转发 → 返回结果
 *        stop() → 杀所有进程
 */
class ai_mcp_manager(
    private val project_dir: File?
) {
    private val gson = Gson()
    private val paths: toolchain_runtime_paths = toolchain_runtime_provider.paths()
    private val request_id_counter = AtomicInteger(0)
    private val servers = ConcurrentHashMap<String, mcp_server_session>()

    /** 配置里的 server 定义 */
    data class mcp_server_config(
        val name: String,
        val command: String,
        val args: List<String> = emptyList(),
        val env: Map<String, String> = emptyMap()
    )

    /**
     * 启动所有配置的 MCP server，把它们的工具注册到 [registry]。
     * @return 成功连接的 server 数量
     */
    fun start(registry: ai_tool_registry): Int {
        val configs = load_configs()
        var connected = 0
        for (config in configs) {
            if (start_server(config, registry)) connected++
        }
        return connected
    }

    /** 读取 MCP 配置（全局 ~/.ai/mcp.json + 项目 .ai/mcp.json 合并，项目优先） */
    private fun load_configs(): List<mcp_server_config> {
        return (parse_config_file(global_config_file()) + (project_config_file()?.let { parse_config_file(it) } ?: emptyList()))
            .distinctBy { it.name }
    }

    /** 全局配置文件：~/.ai/mcp.json */
    fun global_config_file(): File = File(paths.home_dir, ".ai/mcp.json")

    /** 项目配置文件：<project>/.ai/mcp.json（无项目返回 null） */
    fun project_config_file(): File? = project_dir?.let { File(it, ".ai/mcp.json") }

    /** 公开：列出全局配置的 server */
    fun list_global_configs(): List<mcp_server_config> = parse_config_file(global_config_file())

    /** 公开：列出项目配置的 server */
    fun list_project_configs(): List<mcp_server_config> =
        project_config_file()?.let { parse_config_file(it) } ?: emptyList()

    private fun parse_config_file(file: File): List<mcp_server_config> {
        if (!file.isFile) return emptyList()
        return runCatching {
            val root = JsonParser.parseString(file.readText()).asJsonObject
            val servers = root.getAsJsonObject("mcpServers") ?: return emptyList()
            servers.entrySet().map { (name, def) ->
                val obj = def.asJsonObject
                mcp_server_config(
                    name = name,
                    command = obj.get("command")?.asString ?: "",
                    args = obj.getAsJsonArray("args")?.map { it.asString } ?: emptyList(),
                    env = obj.getAsJsonObject("env")?.entrySet()?.associate { it.key to it.value.asString } ?: emptyMap()
                )
            }.filter { it.command.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    /** 保存配置到指定文件 */
    fun save_configs(file: File, configs: List<mcp_server_config>) {
        file.parentFile?.mkdirs()
        val root = JsonObject()
        val serversObj = JsonObject()
        for (c in configs) {
            val def = JsonObject().apply {
                addProperty("command", c.command)
                val argsArr = com.google.gson.JsonArray()
                c.args.forEach { argsArr.add(it) }
                add("args", argsArr)
                val envObj = JsonObject()
                c.env.forEach { (k, v) -> envObj.addProperty(k, v) }
                add("env", envObj)
            }
            serversObj.add(c.name, def)
        }
        root.add("mcpServers", serversObj)
        file.writeText(gson.toJson(root))
    }

    /** 启动单个 MCP server 进程并注册其工具 */
    private fun start_server(config: mcp_server_config, registry: ai_tool_registry): Boolean {
        return runCatching {
            // 通过 proot 启动 server 命令
            val proot_args = proot_command_builder(paths).base_args(
                working_dir = project_dir?.absolutePath ?: "/home",
                extra_mounts = emptyList()
            )
            val full_command = proot_args + listOf(config.command) + config.args
            val process_builder = ProcessBuilder(full_command).apply {
                environment().putAll(config.env)
                redirectErrorStream(false)
            }
            val process = process_builder.start()
            val session = mcp_server_session(config.name, process, gson, request_id_counter)
            servers[config.name] = session

            // initialize 握手
            val initResult = session.request("initialize", JsonObject().apply {
                addProperty("protocolVersion", "2024-11-05")
                add("capabilities", JsonObject())
                add("clientInfo", JsonObject().apply {
                    addProperty("name", "GoStudio")
                    addProperty("version", "1.0")
                })
            })
            // 发 initialized 通知
            session.notify("notifications/initialized", JsonObject())

            // tools/list 拿工具列表
            val toolsResult = session.request("tools/list", JsonObject())
            val tools = toolsResult?.getAsJsonArray("tools") ?: return false
            // 注册每个工具到 registry
            for (toolElem in tools) {
                val toolObj = toolElem.asJsonObject
                val toolName = toolObj.get("name")?.asString ?: continue
                val description = toolObj.get("description")?.asString ?: ""
                val inputSchema = toolObj.getAsJsonObject("inputSchema") ?: JsonObject()
                // 工具名加 server 前缀避免冲突：serverName__toolName
                val qualifiedName = "${config.name}__$toolName"
                registry.register(mcp_remote_tool(qualifiedName, description, inputSchema, session, toolName))
            }
            true
        }.getOrElse {
            false // 启动失败（命令不存在/进程退出），静默跳过
        }
    }

    /** 停止所有 MCP server */
    fun stop() {
        servers.values.forEach { it.close() }
        servers.clear()
    }

    /** 已连接的 server 数 */
    fun server_count(): Int = servers.size
}

/** 单个 MCP server 的会话（进程 + JSON-RPC 通信） */
private class mcp_server_session(
    val name: String,
    private val process: Process,
    private val gson: Gson,
    private val id_counter: AtomicInteger
) {
    private val stdin = process.outputStream
    private val stdout = process.inputStream
    private val lock = Any()

    /** 发 JSON-RPC 请求并等响应（同步） */
    fun request(method: String, params: JsonObject): JsonObject? = synchronized(lock) {
        val id = id_counter.incrementAndGet()
        val requestObj = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", id)
            addProperty("method", method)
            add("params", params)
        }
        write_message(requestObj)
        // 读响应，匹配 id
        var attempts = 0
        while (attempts < 50) {
            val response = read_message() ?: return null
            if (response.get("id")?.asInt == id) {
                return response.getAsJsonObject("result")
            }
            // 不是我们要的 id（可能是通知），继续读
            attempts++
        }
        null
    }

    /** 发通知（不等响应） */
    fun notify(method: String, params: JsonObject) = synchronized(lock) {
        val obj = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            add("params", params)
        }
        write_message(obj)
    }

    /** 调用工具 */
    fun call_tool(name: String, arguments: JsonObject): String = synchronized(lock) {
        val params = JsonObject().apply {
            addProperty("name", name)
            add("arguments", arguments)
        }
        val result = request("tools/call", params) ?: return "MCP 工具调用无响应"
        val content = result.getAsJsonArray("content")
        if (content != null && content.size() > 0) {
            // content 是 [{type:text,text:...}] 数组，拼所有 text
            content.joinToString("") { it.asJsonObject.get("text")?.asString ?: "" }
        } else {
            result.toString()
        }
    }

    /** 写 LSP 式分帧消息（Content-Length 头 + JSON 体） */
    private fun write_message(obj: JsonObject) {
        val json = gson.toJson(obj).toByteArray(Charsets.UTF_8)
        val header = "Content-Length: ${json.size}\r\n\r\n"
        stdin.write(header.toByteArray(Charsets.UTF_8))
        stdin.write(json)
        stdin.flush()
    }

    /** 读一条 LSP 式分帧消息 */
    private fun read_message(): JsonObject? {
        return runCatching {
            // 读 headers
            var contentLength = -1
            while (true) {
                val line = read_line() ?: return null
                if (line.isEmpty()) break // headers 结束
                if (line.startsWith("Content-Length:")) {
                    contentLength = line.removePrefix("Content-Length:").trim().toIntOrNull() ?: -1
                }
            }
            if (contentLength <= 0) return null
            val body = ByteArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val n = stdout.read(body, read, contentLength - read)
                if (n < 0) return null
                read += n
            }
            JsonParser.parseString(String(body, Charsets.UTF_8)).asJsonObject
        }.getOrNull()
    }

    /** 读一行（\r\n 结尾） */
    private fun read_line(): String? {
        val sb = StringBuilder()
        while (true) {
            val b = stdout.read()
            if (b < 0) return null
            if (b == '\r'.code) {
                val next = stdout.read()
                if (next == '\n'.code) return sb.toString()
                sb.append('\r')
                if (next >= 0) sb.append(next.toChar())
            } else if (b == '\n'.code) {
                return sb.toString()
            } else {
                sb.append(b.toChar())
            }
        }
    }

    fun close() {
        runCatching {
            process.outputStream.close()
            if (process.isAlive) {
                process.destroy()
                if (!process.waitFor(2000, java.util.concurrent.TimeUnit.MILLISECONDS) && process.isAlive) {
                    process.destroyForcibly()
                }
            }
        }
    }
}

/** MCP server 提供的远程工具（转发 tools/call 给 server） */
private class mcp_remote_tool(
    override val name: String,
    override val description: String,
    override val parameters_schema: JsonObject,
    private val session: mcp_server_session,
    private val remote_name: String
) : ai_tool {
    override suspend fun execute(params: JsonObject): String {
        return runCatching { session.call_tool(remote_name, params) }
            .getOrElse { "MCP 工具调用失败: ${it.message}" }
    }
}
