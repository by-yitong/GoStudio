package com.jmwl.gostudio.ai

import android.os.Handler
import android.os.Looper
import com.jmwl.gostudio.ai.tools.ai_tool_registry
import com.jmwl.gostudio.ai.tools.execute_safely
import com.jmwl.gostudio.ai.tools.string_or
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * AI Agent 会话状态 + 调度循环（接入 skill/@引用/AGENTS.md/MCP/持久化/压缩/steering/暂停 全部能力）。
 *
 * 新增能力（均通过构造函数可选注入，不传则降级为第一阶段行为）：
 * - [input_processor]：@文件引用、/命令模板、/skill 激活
 * - [session_store] + [session_id]：会话持久化（JSONL）
 * - [skill_manager]：skill 索引注入 system prompt
 * - [mcp_manager]：MCP 工具服务器（start/stop 生命周期）
 * - [file_change_notifier]：write/edit 改文件后通知编辑器刷新
 * - [steering_queue]：运行中排队新消息（当前步骤结束后自动发送）
 *
 * 自动上下文压缩（参考 pi 的 threshold compaction）：
 * 每次发送/每轮请求前估算上下文用量，超过阈值（[ai_settings_state.compact_threshold_percent]）
 * 时把旧消息摘要成一条 checkpoint 消息并回写 [messages]（UI 显示为摘要卡片，持久化）。
 */
class ai_agent_loop(
    private val settings_provider: () -> ai_settings_state,
    private val env_provider: () -> ai_environment_context,
    private val tool_registry: ai_tool_registry,
    private val scope_launcher: (suspend () -> Unit) -> Job,
    private val input_processor: ai_input_processor? = null,
    private val session_store: ai_session_store? = null,
    private var session_id: String = "default",
    private val skill_manager: com.jmwl.gostudio.ai.skills.ai_skill_manager? = null,
    private val mcp_manager: com.jmwl.gostudio.ai.mcp.ai_mcp_manager? = null,
    private val file_change_notifier: ai_file_change_notifier? = null,
    private val steering_queue: ai_steering_queue? = null
) {
    /**
     * 消息列表（不可变快照，每次变更发射新 List）。
     *
     * 历史教训：这里曾是 mutableStateListOf + 后台线程 Handler post + sendApplyNotifications
     * 的组合，依赖 Compose 快照通知时序，先后打了 5 个补丁（copy() 强制重组、快照失效通知、
     * 身份/下标三级定位、占位原子添加、finally 兜底清 streaming）仍出现「回复完成但 UI 卡在
     * 思考中、退出重进才显示」——同页的 is_running 等 StateFlow 状态却一直正常。
     * 根治：消息与其它状态统一走 StateFlow 通道，发射即重组，与线程/快照/Handler 时序解耦。
     * 变更统一经 [mutate_messages] 在主线程串行执行（读-改-写原子性靠单线程 confinement 保证）。
     */
    private val _messages = MutableStateFlow<List<ai_message>>(emptyList())
    val messages: StateFlow<List<ai_message>> = _messages

    private val _is_running = MutableStateFlow(false)
    val is_running: StateFlow<Boolean> = _is_running

    /** 正在执行上下文压缩（UI 显示压缩指示器） */
    private val _compaction_running = MutableStateFlow(false)
    val compaction_running: StateFlow<Boolean> = _compaction_running

    /** 估算的上下文用量（0..1+，相对 effective_context_chars；UI 用量徽标） */
    private val _context_usage = MutableStateFlow(0f)
    val context_usage: StateFlow<Float> = _context_usage

    /** 运行中排队的 steering 消息数（UI 反馈「已排队 N 条」） */
    private val _queued_count = MutableStateFlow(0)
    val queued_count: StateFlow<Int> = _queued_count

    /** MCP server 连接数（UI 可展示） */
    private val _mcp_server_count = MutableStateFlow(0)
    val mcp_server_count: StateFlow<Int> = _mcp_server_count

    private var current_job: Job? = null
    private var cancelled = false
    private val main_handler = Handler(Looper.getMainLooper())

    /** 最近一次构建的 system prompt 长度（上下文用量估算用，避免每次重读 AGENTS.md） */
    @Volatile private var last_system_prompt_len = 0

    /** 是否已初始化（启动时拉起 MCP、加载 skill、恢复会话） */
    private var initialized = false

    private fun on_main(action: () -> Unit) {
        // 所有 _messages 读-改-写与 is_running 复位都排进主线程队列串行执行：
        // 既保证变更原子性，也保证「最终消息落地 → is_running=false」的顺序
        //（反过来会出现回复完成瞬间先看到发送按钮、思考中气泡闪一下才变内容的竞态）。
        if (Looper.myLooper() == Looper.getMainLooper()) action()
        else main_handler.post(action)
    }

    /** 在主线程对消息列表做原子读-改-写（发射新 List 通知 UI） */
    private fun mutate_messages(transform: (MutableList<ai_message>) -> Unit) {
        on_main {
            val next = _messages.value.toMutableList()
            transform(next)
            _messages.value = next
        }
    }

    /**
     * 把 [target] 的最新状态刷进消息列表。
     * uid 在 copy() 间保持不变，直接按 uid 定位替换；找不到（会话中途被清空/切换）则追加，
     * 保证错误信息等最终内容不会静默丢失（不会卡在流式占位）。
     */
    private fun post_assistant_refresh(target: ai_message) {
        mutate_messages { list ->
            val idx = list.indexOfFirst { it.uid == target.uid }
            if (idx >= 0) list[idx] = target.copy() else list.add(target.copy())
        }
    }

    /** 初始化：启动 MCP、发现 skill、恢复历史会话。在 IO 线程跑一次。 */
    suspend fun initialize() {
        if (initialized) return
        initialized = true
        withContext(Dispatchers.IO) {
            // 恢复历史会话：优先本作用域上次激活的会话（项目内新建/切换过的会话重进时续上）
            session_store?.let {
                val target_id = it.read_current_session_id() ?: session_id
                if (target_id != session_id) session_id = target_id
                val history = it.load_session(target_id)
                if (history.isNotEmpty()) on_main { _messages.value = history }
            }
            // 发现 skill
            skill_manager?.discover()
            // 启动 MCP server
            mcp_manager?.let {
                val count = it.start(tool_registry)
                _mcp_server_count.value = count
            }
            on_main { refresh_context_usage() }
        }
    }

    fun send_user_message(text: String) {
        if (_is_running.value) {
            // 运行中：排队为 steering 消息
            steering_queue?.enqueue(text)
            _queued_count.value = steering_queue?.size() ?: 0
            return
        }
        val settings = settings_provider()
        if (!settings.is_configured()) {
            mutate_messages { list ->
                list.add(ai_message(
                    role = ai_message_role.ASSISTANT,
                    text = "⚠️ AI 还没配置。请点击右上角 ⚙️ 设置，填写 API 提供商和密钥。",
                    is_error = true
                ))
            }
            return
        }
        // 过 input_processor（@引用、/命令、/skill）
        val processed = input_processor?.process(text) ?: text
        on_main {
            _messages.value = _messages.value + ai_message(role = ai_message_role.USER, text = processed)
            refresh_context_usage()
        }
        start_loop()
    }

    /**
     * 停止：取消正在跑的 loop（打断流式回复/工具调用），排队消息作废。
     */
    fun cancel() {
        cancelled = true
        _queued_count.value = 0
        current_job?.cancel()
        on_main {
            _is_running.value = false
            // 清掉流式占位标记，避免「思考中」残留
            _messages.value = _messages.value.map { if (it.streaming) it.copy(streaming = false) else it }
        }
    }

    fun clear_messages() {
        if (_is_running.value) cancel()
        steering_queue?.clear()
        _queued_count.value = 0
        on_main {
            _messages.value = emptyList()
            refresh_context_usage()
        }
        session_store?.delete_session(session_id)
    }

    /** 当前会话 id */
    fun current_session_id(): String = session_id

    /** 列出所有历史会话（UI 展示用） */
    fun list_sessions(): List<ai_session_meta> = session_store?.list_sessions() ?: emptyList()

    /** 切换到指定会话：清内存 → 加载该会话历史 → 重设 session_id */
    suspend fun switch_session(new_id: String) {
        if (_is_running.value) cancel()
        steering_queue?.clear()
        _queued_count.value = 0
        session_id = new_id
        session_store?.set_current_session_id(new_id)
        val history = session_store?.load_session(new_id) ?: emptyList()
        on_main {
            _messages.value = history
            refresh_context_usage()
        }
    }

    /** 新建空会话：生成时间戳 id，清内存 */
    fun new_session(): String {
        if (_is_running.value) cancel()
        steering_queue?.clear()
        _queued_count.value = 0
        val new_id = "chat-" + System.currentTimeMillis()
        session_id = new_id
        session_store?.set_current_session_id(new_id)
        on_main {
            _messages.value = emptyList()
            refresh_context_usage()
        }
        return new_id
    }

    /** 重命名当前会话（只改 sidecar 标题） */
    fun rename_session(new_title: String) {
        session_store?.rename_session(session_id, new_title)
    }

    /** 删除指定会话（不切换当前） */
    fun delete_session_by_id(id: String) {
        session_store?.delete_session(id)
    }

    /**
     * 重新生成最后一条 assistant 回复：
     * 找到最后一条 assistant 消息及其后的所有 TOOL 消息，一并移除，然后用上一条 user 消息重跑。
     */
    fun regenerate_last() {
        if (_is_running.value) return
        val current = _messages.value
        if (current.isEmpty()) return
        val last_assistant_idx = current.indexOfLast { it.role == ai_message_role.ASSISTANT }
        if (last_assistant_idx < 0) return
        // 删除该 assistant 及其后所有消息后，必须还剩 user 消息才能重跑
        if (current.subList(0, last_assistant_idx).none { it.role == ai_message_role.USER }) return
        val target_uid = current[last_assistant_idx].uid
        mutate_messages { list ->
            val idx = list.indexOfFirst { it.uid == target_uid }
            if (idx >= 0) while (list.size > idx) list.removeAt(list.size - 1)
        }
        persist_session_via_scope()
        start_loop()
    }

    /**
     * 删除指定索引的单条消息。若删的是 assistant，连带其后属于它的 TOOL 结果消息。
     */
    fun delete_message(index: Int) {
        if (_is_running.value) return
        if (index !in _messages.value.indices) return
        val msg = _messages.value[index]
        mutate_messages { list ->
            val idx = list.indexOfFirst { it.uid == msg.uid }
            if (idx < 0) return@mutate_messages
            list.removeAt(idx)
            // 若删的是 assistant，移除紧随其后的 TOOL 消息（直到下一条 user/assistant）
            if (msg.role == ai_message_role.ASSISTANT) {
                while (list.size > idx && list[idx].role == ai_message_role.TOOL) {
                    list.removeAt(idx)
                }
            }
            // 若删的是 user，其后紧随的 assistant+tool 一并删（避免悬空）
            if (msg.role == ai_message_role.USER) {
                while (list.size > idx && list[idx].role != ai_message_role.USER) {
                    list.removeAt(idx)
                }
            }
            refresh_context_usage()
        }
        persist_session_via_scope()
    }

    /**
     * 编辑某条 user 消息并重发：改写文本，删除其后所有消息，重新跑 loop。
     */
    fun edit_and_resend_user(index: Int, new_text: String) {
        if (_is_running.value) return
        val target = _messages.value.getOrNull(index) ?: return
        if (target.role != ai_message_role.USER) return
        val trimmed = new_text.trim()
        if (trimmed.isEmpty()) return
        val processed = input_processor?.process(trimmed) ?: trimmed
        mutate_messages { list ->
            val idx = list.indexOfFirst { it.uid == target.uid }
            if (idx < 0) return@mutate_messages
            list[idx] = list[idx].copy(text = processed)
            // 删除其后所有消息
            while (list.size > idx + 1) list.removeAt(list.size - 1)
            refresh_context_usage()
        }
        persist_session_via_scope()
        start_loop()
    }

    /** 手动立即压缩当前上下文（设置/徽标菜单入口） */
    fun compact_now() {
        if (_is_running.value || _compaction_running.value) return
        val settings = settings_provider()
        if (!settings.is_configured()) return
        scope_launcher {
            perform_compaction(settings, force = true)
            on_main { refresh_context_usage() }
        }
    }

    private fun persist_session_via_scope() {
        scope_launcher {
            persist_session()
        }
    }

    /** 退出时清理 MCP server */
    fun shutdown() {
        mcp_manager?.stop()
    }

    private fun start_loop() {
        cancelled = false
        // 每次用户发送/steering 续跑重新计轮次（与旧版行为一致，防单轮失控）
        iteration_guard = 0
        _is_running.value = true
        current_job = scope_launcher { run_agent_loop() }
    }

    private suspend fun run_agent_loop() {
        // 在 withContext 外捕获外层 Job（launch 返回的那个），供 finally 判断「本 loop 是否仍是当前 loop」
        val this_job = kotlin.coroutines.coroutineContext[Job]
        withContext(Dispatchers.IO) {
        try {
        var settings = settings_provider()
        val env = env_provider()

        while (true) {
            // 每轮重读设置：会话内切换模型即时生效（下一轮请求用新模型）
            settings = settings_provider()
            if (iteration_guard >= settings.max_agent_iterations || cancelled) break
            iteration_guard++

            val enabled_tool_names = if (settings.enable_tools) tool_registry.all().map { it.name } else emptyList()
            val system_prompt = build_full_system_prompt(env, enabled_tool_names, settings)
            last_system_prompt_len = system_prompt.length

            // 自动上下文压缩：发送前/每轮请求前检查阈值（长工具输出可能中途超限）
            if (settings.auto_compact) {
                perform_compaction(settings, force = false)
            } else {
                hard_trim_history(settings)
            }
            on_main { refresh_context_usage() }

            // 经主线程读取：保证排在 send_user_message 等刚 post 的消息写入之后（请求不丢用户消息）
            val history_snapshot = on_main_and_wait { _messages.value }
            val request_messages = buildList {
                add(ai_message(role = ai_message_role.SYSTEM, text = system_prompt))
                addAll(history_snapshot.filter {
                    it.role != ai_message_role.SYSTEM && !it.is_error && !it.is_system_notice
                })
            }
            val final_messages = request_messages
            val client = ai_client(settings)
            val tools_api = if (settings.enable_tools) tool_registry.to_api_tools() else emptyList()

            val assistant_msg = ai_message(role = ai_message_role.ASSISTANT, streaming = true)
            // 占位以 copy() 入列（避免 IO 线程还在原地变异的实例被组合直接读取）；
            // 后续刷新一律按 uid 定位（copy 保持 uid），无下标竞态。
            mutate_messages { list -> list.add(assistant_msg.copy()) }

            val collected_tool_calls = mutableListOf<ai_tool_call>()
            var had_error = false

            client.stream_chat(final_messages, tools_api, object : ai_stream_callback {
                override fun on_text(delta: String) {
                    assistant_msg.text += delta
                    post_assistant_refresh(assistant_msg)
                }
                override fun on_reasoning(delta: String) {
                    // reasoning 模型的思考链增量（UI 展示用，不发给 API）
                    assistant_msg.reasoning += delta
                    post_assistant_refresh(assistant_msg)
                }
                override fun on_done(tool_calls: List<ai_tool_call>) {
                    collected_tool_calls.addAll(tool_calls)
                }
                override fun on_error(message: String) {
                    // 已流出部分内容时保留原文，错误附在后面（不覆盖）
                    assistant_msg.text = buildString {
                        if (assistant_msg.text.isNotBlank()) {
                            append(assistant_msg.text.trimEnd()).append("\n\n")
                        }
                        append("⚠️ ").append(message)
                    }
                    assistant_msg.is_error = true
                    assistant_msg.streaming = false
                    had_error = true
                    post_assistant_refresh(assistant_msg)
                }
            })

            assistant_msg.streaming = false
            post_assistant_refresh(assistant_msg)

            if (had_error || cancelled) break
            if (collected_tool_calls.isEmpty()) break

            // 固化 tool_calls
            val execs = collected_tool_calls.map { ai_tool_execution(call = it) }
            mutate_messages { list ->
                val idx = list.indexOfFirst { it.uid == assistant_msg.uid }
                if (idx >= 0) {
                    list[idx] = list[idx].copy(tool_calls = collected_tool_calls, tool_executions = execs)
                }
            }

            val changed_files = mutableListOf<String>()

            for (exec in execs) {
                // 取消：不启动下一个工具（已完成的保留）
                if (cancelled) break
                exec.status = ai_tool_status.RUNNING
                mutate_messages { list ->
                    val idx = list.indexOfFirst { it.uid == assistant_msg.uid }
                    if (idx >= 0) list[idx] = update_execution(list[idx], exec)
                }

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

                // 工具执行完成：更新状态卡片（DONE/ERROR）
                mutate_messages { list ->
                    val idx = list.indexOfFirst { it.uid == assistant_msg.uid }
                    if (idx >= 0) list[idx] = update_execution(list[idx], exec)
                }
                on_main {
                    _messages.value = _messages.value + ai_message(
                        role = ai_message_role.TOOL,
                        text = exec.to_result_content(),
                        tool_call_id = exec.call.id
                    )
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
        _queued_count.value = 0
        on_main { _is_running.value = false }
        if (steering.isNotEmpty() && !cancelled) {
            for (msg in steering) {
                val processed = input_processor?.process(msg) ?: msg
                mutate_messages { list -> list.add(ai_message(role = ai_message_role.USER, text = processed)) }
            }
            start_loop()
            return@withContext
        }

        if (iteration_guard >= settings.max_agent_iterations && !cancelled) {
            on_main {
                _messages.value = _messages.value + ai_message(
                    role = ai_message_role.ASSISTANT,
                    text = "（已达到最大轮次 ${settings.max_agent_iterations}，停止以避免失控。如需继续请重新描述需求。）"
                )
            }
        }
        persist_session()
        on_main { refresh_context_usage() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 用户停止/切换会话导致的协程取消不是错误，不显示「异常」气泡
            throw e
        } catch (e: Throwable) {
            // 捕获 loop 内任何异常，显示到对话里（避免静默失败）
            val err_text = "⚠️ agent loop 异常: ${e.javaClass.simpleName}: ${e.message ?: ""}"
            on_main {
                _messages.value = _messages.value + ai_message(
                    role = ai_message_role.ASSISTANT, text = err_text, is_error = true
                )
                _is_running.value = false
            }
        } finally {
            // 只在「本 loop 仍是当前 loop」时复位（steering 续跑会启动新 loop，不能误伤新状态）
            on_main {
                if (current_job === this_job) {
                    // 兜底：结束任何残留的流式占位，防止「思考中」永久卡住
                    _messages.value = _messages.value.map { if (it.streaming) it.copy(streaming = false) else it }
                    _is_running.value = false
                }
                refresh_context_usage()
            }
        }
        }
    }

    /** 循环轮次计数（每次发送/续跑由 start_loop 重置，防单次任务失控） */
    private var iteration_guard = 0

    /**
     * 自动/手动压缩：阈值触发（或 force）时把旧消息摘要成 checkpoint 并回写 messages。
     * 回写后摘要消息带 is_summary 标记（UI 折叠卡片），并立即持久化。
     * @return 是否执行了压缩
     */
    private suspend fun perform_compaction(settings: ai_settings_state, force: Boolean): Boolean {
        val limit_chars = settings.effective_context_chars()
        val used_chars = on_main_and_wait {
            last_system_prompt_len + _messages.value.sumOf { it.estimated_chars().toLong() }
        }
        if (!force && !ai_compaction.should_compact(used_chars, limit_chars, settings.compact_threshold_percent)) {
            return false
        }
        // 近期保留量：上限的 20%，至少 8000 字符（参考 pi keepRecentTokens=20K/200K 的比例）
        val keep_recent = (limit_chars * 0.2f).toInt().coerceAtLeast(8_000)
        // 已有摘要 → 增量合并模式
        val prev_summary = on_main_and_wait { _messages.value.firstOrNull { it.is_summary }?.text }

        _compaction_running.value = true
        try {
            val snapshot = on_main_and_wait { _messages.value }
            val result = ai_compaction.compact(snapshot, keep_recent, ai_client(settings), prev_summary)
            if (result == null) return false
            // 回写：[新摘要] + 保留的近期消息
            on_main {
                val kept = _messages.value.drop(result.compacted_count)
                _messages.value = listOf(result.summary_message) + kept
            }
            persist_session()
            return true
        } finally {
            _compaction_running.value = false
        }
    }

    /**
     * 兜底截断（自动压缩关闭时）：超出上限就从最老的消息开始丢弃（不生成摘要）。
     * 切点复用压缩的边界规则，避免拆散 assistant/tool 配对。
     */
    private suspend fun hard_trim_history(settings: ai_settings_state) {
        val limit_chars = settings.effective_context_chars()
        val used_chars = on_main_and_wait { _messages.value.sumOf { it.estimated_chars().toLong() } }
        if (used_chars <= limit_chars) return
        // 保留近期约 60% 上限
        val keep_recent = (limit_chars * 0.6f).toInt()
        val snapshot = on_main_and_wait { _messages.value }
        val cut = ai_compaction.find_cut_point(snapshot, keep_recent)
        if (cut <= 0) return
        on_main { _messages.value = _messages.value.drop(cut) }
    }

    /** 估算上下文用量并更新 StateFlow（主线程调用；徽标/压缩判断的数据源） */
    private fun refresh_context_usage() {
        val settings = settings_provider()
        val limit = settings.effective_context_chars().toLong().coerceAtLeast(1)
        val used = last_system_prompt_len + _messages.value.sumOf { it.estimated_chars().toLong() }
        _context_usage.value = (used.toFloat() / limit).coerceIn(0f, 1.5f)
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
            val snapshot = on_main_and_wait { _messages.value }
            withContext(Dispatchers.IO) { store.save_session(session_id, snapshot) }
        }
    }

    private fun update_execution(msg: ai_message, exec: ai_tool_execution): ai_message {
        val newExecs = msg.tool_executions.toMutableList()
        val idx = newExecs.indexOfFirst { it.call.id == exec.call.id }
        if (idx >= 0) newExecs[idx] = exec else newExecs.add(exec)
        return msg.copy(tool_executions = newExecs)
    }

    private suspend fun <T> on_main_and_wait(action: () -> T): T = withContext(Dispatchers.Main) {
        action()
    }
}
