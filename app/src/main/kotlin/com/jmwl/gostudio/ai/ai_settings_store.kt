package com.jmwl.gostudio.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** AI 助手设置：提供商 / API key / 模型 / 工具开关。用 EncryptedSharedPreferences 加密存储 API key。 */

internal const val ai_settings_prefs_name = "ai_settings"

/** 预置的 AI 提供商（均兼容 OpenAI /chat/completions 格式，Anthropic 在 ai_client 单独适配） */
enum class ai_provider(val display_name: String, val base_url: String, val default_model: String) {
    CUSTOM("自定义", "", ""),
    ZHIPU("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4/", "glm-4.6"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", "deepseek-chat"),
    KIMI("Kimi", "https://api.moonshot.cn/v1", "moonshot-v1-auto"),
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o"),
    XAI("xAI (Grok)", "https://api.x.ai/v1", "grok-2"),
    ANTHROPIC("Anthropic (Claude)", "https://api.anthropic.com/v1", "claude-sonnet-4-5");

    val is_openai_compatible: Boolean get() = this != ANTHROPIC
}

data class ai_settings_state(
    val provider: ai_provider = ai_provider.ZHIPU,
    val base_url: String = ai_provider.ZHIPU.base_url,
    val model: String = ai_provider.ZHIPU.default_model,
    val api_key: String = "",
    /** 上下文窗口上限（用于截断历史消息），默认 60000 token 约等于 24 万字符估算 */
    val max_context_chars: Int = 200_000,
    val enable_tools: Boolean = true,
    val enable_bash: Boolean = true,
    val enable_write: Boolean = true,
    /** agent loop 最大轮次（防失控） */
    val max_agent_iterations: Int = 20
)

/** 提供商切换时，base_url/model 跟随预置值（用户可后续手改） */
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

fun load_ai_settings(context: Context): ai_settings_state {
    val prefs = encrypted_prefs(context)
    val providerName = prefs.getString("provider", ai_provider.ZHIPU.name) ?: ai_provider.ZHIPU.name
    val provider = runCatching { ai_provider.valueOf(providerName) }.getOrDefault(ai_provider.ZHIPU)
    return ai_settings_state(
        provider = provider,
        base_url = prefs.getString("base_url", provider.base_url) ?: provider.base_url,
        model = prefs.getString("model", provider.default_model) ?: provider.default_model,
        api_key = prefs.getString("api_key", "") ?: "",
        max_context_chars = prefs.getInt("max_context_chars", 200_000),
        enable_tools = prefs.getBoolean("enable_tools", true),
        enable_bash = prefs.getBoolean("enable_bash", true),
        enable_write = prefs.getBoolean("enable_write", true),
        max_agent_iterations = prefs.getInt("max_agent_iterations", 20)
    )
}

fun save_ai_settings(context: Context, settings: ai_settings_state) {
    encrypted_prefs(context).edit().apply {
        putString("provider", settings.provider.name)
        putString("base_url", settings.base_url)
        putString("model", settings.model)
        putString("api_key", settings.api_key)
        putInt("max_context_chars", settings.max_context_chars)
        putBoolean("enable_tools", settings.enable_tools)
        putBoolean("enable_bash", settings.enable_bash)
        putBoolean("enable_write", settings.enable_write)
        putInt("max_agent_iterations", settings.max_agent_iterations)
    }.apply()
}

/** 是否已配置可用（有 base_url + model + api_key） */
fun ai_settings_state.is_configured(): Boolean {
    return base_url.isNotBlank() && model.isNotBlank() && api_key.isNotBlank()
}
