package com.jmwl.gostudio.ai

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.jmwl.gostudio.ai.tools.ai_tool_registry
import com.jmwl.gostudio.ai.tools.execute_safely
import com.jmwl.gostudio.ai.tools.string_or
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * AI Agent 会话状态 + 调度循环（接入 skill/@引用/AGENTS.md/MCP/持久化/压缩/steering 全部能力）。
 *
 * 新增能力（均通过构造函数可选注入，不传则降级为第一阶段行为）：
 * - [input_processor]：@文件引用、/命令模板、/skill 激活
 * - [session_store] + [session_id]：会话持久化（JSONL）
 * - [skill_manager]：skill 索引注入 system prompt
 * - [mcp_manager]：MCP 工具服务器（start/stop 生命周期）
 * - [file_change_notifier]：write/edit 改文件后通知编辑器刷新
 * - [steering_queue]：运行中排队新消息
 */
class ai_agent_loop(
    private val settings_provider: () -> ai_settings_state,
    private val env_provider: () -> ai_environment_context,
    private val tool_registry: ai_tool_registry,
    private val scope_launcher: (suspend () -> Unit) -> Job,
    private val input_processor: ai_input_processor? = null,
    private val session_store: ai_session_store? = null,
    private val session_id: String = "default",
    private val skill_manager: com.jmwl.gostudio.ai.skills.ai_skill_manager? = null,
    private val mcp_manager: com.jmwl.gostudio.ai.mcp.ai_mcp_manager? = null,
    private val file_change_notifier: ai_file_change_notifier? = null,
    private val steering_queue: ai_steering_queue? = null
) {
    val messages = mutableStateListOf<ai_message>()

    private val _is_running = MutableStateFlow(false)
    val is_running: StateFlow<Boolean> = _is_running

    /** MCP server 连接数（UI 可展示） */
    private val _mcp_server_count = MutableStateFlow(0)
    val mcp_server_count: StateFlow<Int> = _mcp_server_count

    private var current_job: Job? = null
    private var cancelled = false
    private val main_handler = Handler(Looper.getMainLooper())

    /** 是否已初始化（启动时拉起 MCP、加载 skill、恢复会话） */
    private var initialized = false

    private fun on_main(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action()
        else main_handler.post { action() }
    }

    /** 初始化：启动 MCP、发现 skill、恢复历史会话。在 IO 线程跑一次。 */
    suspend fun initialize() {
        if (initialized) return
        initialized = true
        withContext(Dispatchers.IO) {
            // 恢复历史会话
            session_store?.let {
                val history = it.load_session(session_id)
                if (history.isNotEmpty()) on_main { messages.addAll(history) }
            }
            // 发现 skill
            skill_manager?.discover()
            // 启动 MCP server
            mcp_manager?.let {
                val count = it.start(tool_registry)
                _mcp_server_count.value = count
            }
        }
    }

    fun send_user_message(text: String) {
        if (_is_running.value) {
            // 运行中：排队为 steering 消息
            steering_queue?.enqueue(text)
            return
        }
        val settings = settings_provider()
        if (!settings.is_configured()) {
            on_main {
                messages.add(ai_message(
                    role = ai_message_role.ASSISTANT,
                    text = "⚠️ AI 还没配置。请点击右上角 ⚙️ 设置，填写 API 提供商和密钥。",
                    is_error = true
                ))
            }
            return
        }
        // 过 input_processor（@引用、/命令、/skill）
        val processed = input_processor?.process(text) ?: text
        on_main { messages.add(ai_message(role = ai_message_role.USER, text = processed)) }
        start_loop()
    }

    fun cancel() {
        cancelled = true
        current_job?.cancel()
        on_main {
            _is_running.value = false
            messages.lastOrNull { it.streaming }?.let { it.streaming = false }
        }
    }

    fun clear_messages() {
        if (_is_running.value) cancel()
        steering_queue?.clear()
        on_main { messages.clear() }
        session_store?.delete_session(session_id)
    }

    /** 退出时清理 MCP server */
    fun shutdown() {
        mcp_manager?.stop()
    }

    private fun start_loop() {
        cancelled = false
        _is_running.value = true
        current_job = scope_launcher { run_agent_loop() }
    }

    private suspend fun run_agent_loop() = withContext(Dispatchers.IO) {
        try {
        val settings = settings_provider()
        val env = env_provider()
        val client = ai_client(settings)
        var iteration = 0

        while (iteration < settings.max_agent_iterations && !cancelled) {
            iteration++

            val enabled_tool_names = if (settings.enable_tools) tool_registry.all().map { it.name } else emptyList()
            val system_prompt = build_full_system_prompt(env, enabled_tool_names, settings)
            val history_snapshot = on_main_and_wait { messages.toList() }
            val request_messages = buildList {
                add(ai_message(role = ai_message_role.SYSTEM, text = system_prompt))
                addAll(history_snapshot.filter { it.role != ai_message_role.SYSTEM && !it.is_error })
            }
            // 用 compaction 替代简单截断（优先模型摘要，失败退化启发式）
            val trimmed = ai_compaction.compact(
                request_messages.filter { it.role != ai_message_role.SYSTEM },
                settings.max_context_chars,
                client = client
            )
            val final_messages = listOf(request_messages.first { it.role == ai_message_role.SYSTEM }) + trimmed
            val tools_api = if (settings.enable_tools) tool_registry.to_api_tools() else emptyList()

            val assistant_msg = ai_message(role = ai_message_role.ASSISTANT, streaming = true)
            on_main { messages.add(assistant_msg) }
            val msg_index_holder = intArrayOf(-1)
            on_main { msg_index_holder[0] = messages.size - 1 }

            val collected_tool_calls = mutableListOf<ai_tool_call>()
            var had_error = false

            client.stream_chat(final_messages, tools_api, object : ai_stream_callback {
                override fun on_text(delta: String) {
                    assistant_msg.text += delta
                    val idx = msg_index_holder[0]
                    // 用 copy() 创建新实例触发 Compose 更新（同引用 set 不会重组）
                    on_main { if (idx in messages.indices) messages[idx] = assistant_msg.copy() }
                }
                override fun on_done(tool_calls: List<ai_tool_call>) {
                    collected_tool_calls.addAll(tool_calls)
                }
                override fun on_error(message: String) {
                    assistant_msg.text = "⚠️ $message"
                    assistant_msg.is_error = true
                    had_error = true
                    val idx = msg_index_holder[0]
                    on_main { if (idx in messages.indices) messages[idx] = assistant_msg.copy() }
                }
            })

            assistant_msg.streaming = false
            on_main { msg_index_holder[0].let { idx -> if (idx in messages.indices) messages[idx] = assistant_msg.copy() } }

            if (had_error || cancelled) break
            if (collected_tool_calls.isEmpty()) break

            // 固化 tool_calls
            val execs = collected_tool_calls.map { ai_tool_execution(call = it) }
            on_main {
                val idx = msg_index_holder[0]
                if (idx in messages.indices) {
                    messages[idx] = messages[idx].copy(tool_calls = collected_tool_calls, tool_executions = execs)
                }
            }

            val changed_files = mutableListOf<String>()

            for (exec in execs) {
                if (cancelled) break
                exec.status = ai_tool_status.RUNNING
                on_main { msg_index_holder[0].let { idx -> if (idx in messages.indices) messages[idx] = update_execution(messages[idx], exec) } }

                val tool = tool_registry.get(exec.call.name)
                val params = JsonParser.parseString(exec.call.arguments_json).asJsonObject
                // 按设置开关过滤 write/bash
                val blocked = when (exec.call.name) {
                    "write" -> !settings.enable_write
                    "bash" -> !settings.enable_bash
                    else -> false
                }
                exec.result = if (blocked) {
                    exec.error_message = "该工具已被设置禁用"
                    "该工具已被设置禁用（请在 AI 设置里开启）"
                } else if (tool != null) {
                    val timeout = if (exec.call.name == "bash") 60_000L else 15_000L
                    tool.execute_safely(params, timeout)
                } else {
                    exec.error_message = "未知工具: ${exec.call.name}"
                    "未知工具: ${exec.call.name}"
                }
                exec.status = if (exec.error_message != null) ai_tool_status.ERROR else ai_tool_status.DONE

                // write/edit 改了文件，记录路径通知编辑器刷新
                if (exec.call.name in listOf("write", "edit") && exec.error_message == null) {
                    params.string_or("path").takeIf { it.isNotBlank() }?.let { changed_files.add(it) }
                }

                on_main { msg_index_holder[0].let { idx -> if (idx in messages.indices) messages[idx] = update_execution(messages[idx], exec) } }
                on_main {
                    messages.add(ai_message(
                        role = ai_message_role.TOOL,
                        text = exec.to_result_content(),
                        tool_call_id = exec.call.id
                    ))
                }
            }

            // 通知编辑器刷新被改的文件
            if (changed_files.isNotEmpty()) {
                file_change_notifier?.notify_changed(changed_files)
            }
            // 持久化会话
            persist_session()
        }

        // 处理 steering 队列：有排队消息则作为新 user 消息继续
        val steering = steering_queue?.drain() ?: emptyList()
        on_main { _is_running.value = false }
        if (steering.isNotEmpty() && !cancelled) {
            for (msg in steering) {
                val processed = input_processor?.process(msg) ?: msg
                on_main { messages.add(ai_message(role = ai_message_role.USER, text = processed)) }
            }
            start_loop()
            return@withContext
        }

        if (iteration >= settings.max_agent_iterations && !cancelled) {
            on_main {
                messages.add(ai_message(
                    role = ai_message_role.ASSISTANT,
                    text = "（已达到最大轮次 ${settings.max_agent_iterations}，停止以避免失控。如需继续请重新描述需求。）"
                ))
            }
        }
        persist_session()
        } catch (e: Throwable) {
            // 捕获 loop 内任何异常，显示到对话里（避免静默失败）
            val err_text = "⚠️ agent loop 异常: ${e.javaClass.simpleName}: ${e.message ?: ""}"
            on_main {
                messages.add(ai_message(role = ai_message_role.ASSISTANT, text = err_text, is_error = true))
                _is_running.value = false
            }
        } finally {
            on_main { _is_running.value = false }
        }
    }

    /** 构建完整 system prompt：基础 + AGENTS.md + skill 索引 + 用户自定义提示词 */
    private fun build_full_system_prompt(env: ai_environment_context, tools: List<String>, settings: ai_settings_state): String {
        val base = build_system_prompt(env, tools, settings.conversation_tone)
        val sb = StringBuilder(base)
        // AGENTS.md / .ai 上下文
        val context_files = read_context_files(env.project_dir)
        if (context_files.isNotBlank()) {
            sb.appendLine().appendLine(context_files)
        }
        // skill 索引
        val skills = skill_manager?.skill_index_text()
        if (!skills.isNullOrEmpty()) {
            sb.appendLine().appendLine("## 技能（Skills）").appendLine(skills)
        }
        // 用户自定义提示词
        if (settings.custom_system_prompt.isNotBlank()) {
            sb.appendLine().appendLine("## 附加指令").appendLine(settings.custom_system_prompt.trim())
        }
        return sb.toString()
    }

    private suspend fun persist_session() {
        session_store?.let { store ->
            val snapshot = on_main_and_wait { messages.toList() }
            withContext(Dispatchers.IO) { store.save_session(session_id, snapshot) }
        }
    }

    private fun update_execution(msg: ai_message, exec: ai_tool_execution): ai_message {
        val newExecs = msg.tool_executions.toMutableList()
        val idx = newExecs.indexOfFirst { it.call.id == exec.call.id }
        if (idx >= 0) newExecs[idx] = exec else newExecs.add(exec)
        return msg.copy(tool_executions = newExecs)
    }

    private suspend fun <T> on_main_and_wait(action: () -> T): T = withContext(Dispatchers.Main) { action() }
}
