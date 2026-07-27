package com.jmwl.gostudio.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** AI 助手设置：提供商 / API key / 模型 / 工具开关。用 EncryptedSharedPreferences 加密存储 API key。 */

internal const val ai_settings_prefs_name = "ai_settings"

/**
 * 预置的 AI 提供商（均兼容 OpenAI /chat/completions 格式，Anthropic 在 ai_client 单独适配）。
 * default_models 是该供应商常用模型列表，供会话栏快速切换。
 */
enum class ai_provider(
    val display_name: String,
    val base_url: String,
    val default_model: String,
    val default_models: List<String>
) {
    CUSTOM("自定义", "", "", emptyList()),
    ZHIPU("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4/", "glm-4.6",
        listOf("glm-4.6", "glm-4.5", "glm-4.5-air", "glm-4.5-x", "glm-4-plus", "glm-4-air", "glm-4-flash", "glm-4-flashx", "glm-4-long")),
    ZHIPU_CODING("智谱 GLM (编程套餐)", "https://open.bigmodel.cn/api/coding/paas/v4/", "glm-4.6",
        listOf("glm-4.6", "glm-4.5", "glm-4.5-air", "glm-4.5-x")),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", "deepseek-chat",
        listOf("deepseek-v4-flash", "deepseek-v4-pro", "deepseek-chat", "deepseek-reasoner")),
    KIMI("Kimi", "https://api.moonshot.cn/v1", "moonshot-v1-auto",
        listOf("moonshot-v1-auto", "moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k", "kimi-latest")),
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o",
        listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini", "o3-mini")),
    XAI("xAI (Grok)", "https://api.x.ai/v1", "grok-2",
        listOf("grok-2", "grok-2-latest", "grok-beta")),
    ANTHROPIC("Anthropic (Claude)", "https://api.anthropic.com/v1", "claude-sonnet-4-5",
        listOf("claude-sonnet-4-5", "claude-opus-4", "claude-haiku-4", "claude-3-7-sonnet", "claude-3-5-haiku"));

    val is_openai_compatible: Boolean get() = this != ANTHROPIC
    /** 是否支持 /v1/models（Anthropic 无标准端点，故不支持） */
    val supports_model_list: Boolean get() = this != ANTHROPIC && this != CUSTOM
}

data class ai_settings_state(
    val provider: ai_provider = ai_provider.ZHIPU,
    val base_url: String = ai_provider.ZHIPU.base_url,
    val model: String = ai_provider.ZHIPU.default_model,
    val api_key: String = "",
    /** 每个供应商独立的 API key（会话栏切供应商时回填对应 key） */
    val api_keys: Map<ai_provider, String> = emptyMap(),
    /** 动态获取的模型列表缓存，key=base_url（避免重复请求 /v1/models） */
    val custom_models: Map<String, List<String>> = emptyMap(),
    /** 上下文窗口上限（用于截断历史消息），默认 60000 token 约等于 24 万字符估算 */
    val max_context_chars: Int = 200_000,
    val enable_tools: Boolean = true,
    val enable_bash: Boolean = true,
    val enable_write: Boolean = true,
    /** agent loop 最大轮次（防失控） */
    val max_agent_iterations: Int = 20,
    // ===== AI 行为 =====
    /** 思考深度：0=标准 1=深入 2=极致（影响推理轮次倾向） */
    val thinking_depth: Int = 0,
    /** 自定义系统提示词（追加到默认 system prompt 之后） */
    val custom_system_prompt: String = "",
    /** 交流语气：friendly / professional / concise */
    val conversation_tone: String = "friendly",
    /** verbose 模式：展示 agent 中间步骤（读文件/执行命令等思考过程） */
    val show_thinking_process: Boolean = true,
    /** 思考过程自动展开（false=折叠只显示标题） */
    val auto_expand_thinking: Boolean = false
)

/**
 * 切换到新提供商：保留所有供应商的 key（api_keys）+ 自定义模型缓存，
 * 回填新提供商的 key 到 api_key，并预置 base_url / model。
 * 自定义(CUSTOM)只切 provider，保留用户已填的 base_url/model/key。
 */
fun switch_provider(state: ai_settings_state, new_provider: ai_provider): ai_settings_state {
    if (new_provider == ai_provider.CUSTOM) {
        return state.copy(provider = new_provider)
    }
    return state.copy(
        provider = new_provider,
        base_url = new_provider.base_url,
        model = new_provider.default_model,
        api_key = state.api_keys[new_provider] ?: ""
    )
}

/** 兼容旧调用：从默认状态切到指定提供商（会清空 key，仅用于无 state 的场景） */
fun default_state_for_provider(provider: ai_provider): ai_settings_state {
    return ai_settings_state(
        provider = provider,
        base_url = provider.base_url,
        model = provider.default_model
    )
}

private fun master_key(context: Context): MasterKey {
    return MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
}

/** 获取加密的 SharedPreferences（API key 落盘加密） */
private fun encrypted_prefs(context: Context) = runCatching {
    EncryptedSharedPreferences.create(
        context,
        ai_settings_prefs_name,
        master_key(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}.getOrElse {
    // 加密存储初始化失败（极少见，如设备无 keystore）时回退到普通私有 prefs，避免完全不可用
    context.getSharedPreferences("${ai_settings_prefs_name}_fallback", Context.MODE_PRIVATE)
}

private val settings_gson = Gson()

fun load_ai_settings(context: Context): ai_settings_state {
    val prefs = encrypted_prefs(context)
    val providerName = prefs.getString("provider", ai_provider.ZHIPU.name) ?: ai_provider.ZHIPU.name
    val provider = runCatching { ai_provider.valueOf(providerName) }.getOrDefault(ai_provider.ZHIPU)
    val api_key = prefs.getString("api_key", "") ?: ""

    // 反序列化每供应商 key 的 map；老数据没有则把单 key 迁移到当前 provider
    val api_keys: Map<ai_provider, String> = run {
        val json = prefs.getString("api_keys_json", null)
        if (json != null) {
            runCatching {
                val type = object : TypeToken<Map<String, String>>() {}.type
                @Suppress("UNCHECKED_CAST")
                val raw = settings_gson.fromJson<Map<String, String>>(json, type) ?: emptyMap()
                raw.mapNotNull { (k, v) ->
                    runCatching { ai_provider.valueOf(k) }.getOrNull()?.let { it to v }
                }.toMap()
            }.getOrDefault(emptyMap())
        } else if (api_key.isNotBlank()) {
            mapOf(provider to api_key)
        } else {
            emptyMap()
        }
    }

    // 反序列化动态获取的模型缓存
    val custom_models: Map<String, List<String>> = run {
        val json = prefs.getString("custom_models_json", null) ?: return@run emptyMap()
        runCatching {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            val parsed: Map<String, List<String>> = settings_gson.fromJson(json, type) ?: emptyMap()
            parsed
        }.getOrDefault(emptyMap())
    }

    return ai_settings_state(
        provider = provider,
        base_url = prefs.getString("base_url", provider.base_url) ?: provider.base_url,
        model = prefs.getString("model", provider.default_model) ?: provider.default_model,
        api_key = api_key,
        api_keys = api_keys,
        custom_models = custom_models,
        max_context_chars = prefs.getInt("max_context_chars", 200_000),
        enable_tools = prefs.getBoolean("enable_tools", true),
        enable_bash = prefs.getBoolean("enable_bash", true),
        enable_write = prefs.getBoolean("enable_write", true),
        max_agent_iterations = prefs.getInt("max_agent_iterations", 20),
        thinking_depth = prefs.getInt("thinking_depth", 0),
        custom_system_prompt = prefs.getString("custom_system_prompt", "") ?: "",
        conversation_tone = prefs.getString("conversation_tone", "friendly") ?: "friendly",
        show_thinking_process = prefs.getBoolean("show_thinking_process", true),
        auto_expand_thinking = prefs.getBoolean("auto_expand_thinking", false)
    )
}

fun save_ai_settings(context: Context, settings: ai_settings_state) {
    encrypted_prefs(context).edit().apply {
        putString("provider", settings.provider.name)
        putString("base_url", settings.base_url)
        putString("model", settings.model)
        putString("api_key", settings.api_key)
        putString("api_keys_json", settings_gson.toJson(settings.api_keys.mapKeys { it.key.name }))
        putString("custom_models_json", settings_gson.toJson(settings.custom_models))
        putInt("max_context_chars", settings.max_context_chars)
        putBoolean("enable_tools", settings.enable_tools)
        putBoolean("enable_bash", settings.enable_bash)
        putBoolean("enable_write", settings.enable_write)
        putInt("max_agent_iterations", settings.max_agent_iterations)
        putInt("thinking_depth", settings.thinking_depth)
        putString("custom_system_prompt", settings.custom_system_prompt)
        putString("conversation_tone", settings.conversation_tone)
        putBoolean("show_thinking_process", settings.show_thinking_process)
        putBoolean("auto_expand_thinking", settings.auto_expand_thinking)
    }.apply()
}

/** 是否已配置可用（有 base_url + model + api_key） */
fun ai_settings_state.is_configured(): Boolean {
    return base_url.isNotBlank() && model.isNotBlank() && api_key.isNotBlank()
}
