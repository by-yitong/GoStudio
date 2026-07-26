package com.jmwl.gostudio.ai

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.jmwl.gostudio.ai.tools.ai_tool_registry
import com.jmwl.gostudio.ai.tools.execute_safely
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * AI Agent 会话状态 + 调度循环。
 *
 * 由 editor_activity / main_activity 持有。UI 通过 [messages] 和 [is_running] 重组。
 *
 * 线程模型：agent 循环跑在 IO 协程；所有对 [messages] 的修改都通过主线程 Handler 转发，
 * 保证 Compose 的 mutableStateListOf 在主线程被访问。
 *
 * @param scope_launcher 把协程块挂到持有者的 CoroutineScope（通常是 lifecycleScope）
 */
class ai_agent_loop(
    private val settings_provider: () -> ai_settings_state,
    private val env_provider: () -> ai_environment_context,
    private val tool_registry: ai_tool_registry,
    private val scope_launcher: (suspend () -> Unit) -> Job
) {
    val messages = mutableStateListOf<ai_message>()

    private val _is_running = MutableStateFlow(false)
    val is_running: StateFlow<Boolean> = _is_running

    private var current_job: Job? = null
    private var cancelled = false
    private val main_handler = Handler(Looper.getMainLooper())

    /** 主线程上操作 messages */
    private fun on_main(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action()
        else main_handler.post { action() }
    }

    fun send_user_message(text: String) {
        if (_is_running.value) return
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
        on_main { messages.add(ai_message(role = ai_message_role.USER, text = text)) }
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
        on_main { messages.clear() }
    }

    private fun start_loop() {
        cancelled = false
        _is_running.value = true
        current_job = scope_launcher { run_agent_loop() }
    }

    /**
     * 核心 agent 循环：组装请求 → 流式接收 → 有 tool_calls 就执行 → 继续。
     * 直到模型不再调工具、达到最大轮次、或被取消。
     */
    private suspend fun run_agent_loop() {
        val settings = settings_provider()
        val env = env_provider()
        val client = ai_client(settings)
        var iteration = 0

        while (iteration < settings.max_agent_iterations && !cancelled) {
            iteration++

            val enabled_tool_names = if (settings.enable_tools) tool_registry.all().map { it.name } else emptyList()
            val system_prompt = build_system_prompt(env, enabled_tool_names)
            // 取主线程 messages 的快照（避免循环中改动它）
            val history_snapshot = on_main_and_wait { messages.toList() }
            val request_messages = buildList {
                add(ai_message(role = ai_message_role.SYSTEM, text = system_prompt))
                addAll(history_snapshot.filter { it.role != ai_message_role.SYSTEM && !it.is_error })
            }
            val trimmed = trim_context(request_messages, settings.max_context_chars)
            val tools_api = if (settings.enable_tools) tool_registry.to_api_tools() else emptyList()

            // 创建 assistant 消息占位，加入 UI（主线程）
            val assistant_msg = ai_message(role = ai_message_role.ASSISTANT, streaming = true)
            on_main { messages.add(assistant_msg) }
            val msg_index_holder = intArrayOf(-1)
            on_main { msg_index_holder[0] = messages.size - 1 }

            val collected_tool_calls = mutableListOf<ai_tool_call>()
            var had_error = false

            client.stream_chat(trimmed, tools_api, object : ai_stream_callback {
                override fun on_text(delta: String) {
                    assistant_msg.text += delta
                    val idx = msg_index_holder[0]
                    on_main { if (idx in messages.indices) messages[idx] = messages[idx] }
                }
                override fun on_done(tool_calls: List<ai_tool_call>) {
                    collected_tool_calls.addAll(tool_calls)
                }
                override fun on_error(message: String) {
                    assistant_msg.text = "⚠️ $message"
                    assistant_msg.is_error = true
                    had_error = true
                    val idx = msg_index_holder[0]
                    on_main { if (idx in messages.indices) messages[idx] = messages[idx] }
                }
            })

            assistant_msg.streaming = false
            on_main { msg_index_holder[0].let { idx -> if (idx in messages.indices) messages[idx] = messages[idx] } }

            if (had_error || cancelled) break
            if (collected_tool_calls.isEmpty()) break

            // 把 tool_calls 固化到 assistant 消息（供下一轮 API 带上）
            val execs = collected_tool_calls.map { ai_tool_execution(call = it) }
            on_main {
                val idx = msg_index_holder[0]
                if (idx in messages.indices) {
                    messages[idx] = messages[idx].copy(tool_calls = collected_tool_calls, tool_executions = execs)
                }
            }

            // 执行工具
            for (exec in execs) {
                if (cancelled) break
                exec.status = ai_tool_status.RUNNING
                on_main { msg_index_holder[0].let { idx -> if (idx in messages.indices) messages[idx] = update_execution(messages[idx], exec) } }

                val tool = tool_registry.get(exec.call.name)
                val params = JsonParser.parseString(exec.call.arguments_json).asJsonObject
                exec.result = if (tool != null) {
                    val timeout = if (exec.call.name == "bash") 60_000L else 15_000L
                    tool.execute_safely(params, timeout)
                } else {
                    exec.error_message = "未知工具: ${exec.call.name}"
                    "未知工具: ${exec.call.name}"
                }
                exec.status = if (exec.error_message != null) ai_tool_status.ERROR else ai_tool_status.DONE
                on_main { msg_index_holder[0].let { idx -> if (idx in messages.indices) messages[idx] = update_execution(messages[idx], exec) } }

                // tool 结果作为 tool 消息追加
                on_main {
                    messages.add(ai_message(
                        role = ai_message_role.TOOL,
                        text = exec.to_result_content(),
                        tool_call_id = exec.call.id
                    ))
                }
            }
        }

        if (iteration >= settings.max_agent_iterations && !cancelled) {
            on_main {
                messages.add(ai_message(
                    role = ai_message_role.ASSISTANT,
                    text = "（已达到最大轮次 ${settings.max_agent_iterations}，停止以避免失控。如需继续请重新描述需求。）"
                ))
            }
        }
        on_main { _is_running.value = false }
    }

    private fun update_execution(msg: ai_message, exec: ai_tool_execution): ai_message {
        val newExecs = msg.tool_executions.toMutableList()
        val idx = newExecs.indexOfFirst { it.call.id == exec.call.id }
        if (idx >= 0) newExecs[idx] = exec else newExecs.add(exec)
        return msg.copy(tool_executions = newExecs)
    }

    /** 在主线程执行并等待结果（用于读取 messages 快照） */
    private suspend fun <T> on_main_and_wait(action: () -> T): T = withContext(Dispatchers.Main) { action() }

    /** 上下文截断：保留 system + 末尾若干条，丢弃开头不完整的 assistant-tool 序列 */
    private fun trim_context(msgs: List<ai_message>, max_chars: Int): List<ai_message> {
        if (msgs.size <= 2) return msgs
        val system = msgs.first { it.role == ai_message_role.SYSTEM }
        val rest = msgs.filter { it.role != ai_message_role.SYSTEM }
        var total = system.text.length
        val kept = mutableListOf<ai_message>()
        for (m in rest.asReversed()) {
            val size = m.text.length + m.tool_calls.sumOf { it.arguments_json.length + it.name.length }
            if (total + size > max_chars) break
            kept.add(0, m)
            total += size
        }
        // 兜底：开头不能是孤立的 tool 结果或只有 tool_calls 的 assistant
        val safe = kept.dropWhile { it.role == ai_message_role.TOOL }
            .dropWhile { it.role == ai_message_role.ASSISTANT && it.tool_calls.isNotEmpty() && it.text.isBlank() }
        return listOf(system) + safe
    }
}
