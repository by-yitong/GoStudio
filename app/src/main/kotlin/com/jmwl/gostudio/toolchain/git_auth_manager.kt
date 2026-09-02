package com.jmwl.gostudio.toolchain

import android.content.Context
import android.system.Os
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.jmwl.gostudio.gostudio_application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

/** Git 登录方式。HTTPS Token / 密码、SSH Key、GitHub OAuth 与终端全局配置都支持。 */
enum class git_auth_method(val display_name: String) {
    NONE("匿名 / 不登录"),
    HTTPS_TOKEN("HTTPS Token"),
    HTTPS_PASSWORD("HTTPS 账号密码"),
    SSH_KEY("SSH 私钥"),
    GITHUB_OAUTH("GitHub OAuth"),
    SYSTEM("终端 Git 配置")
}

data class git_auth_config(
    val method: git_auth_method = git_auth_method.NONE,
    val host: String = "github.com",
    val username: String = "",
    val token: String = "",
    val password: String = "",
    val ssh_private_key: String = "",
    val ssh_passphrase: String = "",
    val oauth_client_id: String = ""
) {
    val is_https: Boolean get() = method == git_auth_method.HTTPS_TOKEN ||
        method == git_auth_method.HTTPS_PASSWORD ||
        method == git_auth_method.GITHUB_OAUTH
}

data class github_device_prompt(
    val verification_uri: String,
    val user_code: String,
    val expires_in: Int
)

object git_auth_manager {
    private const val prefs_name = "gostudio_git_auth"
    private const val config_key = "git_auth_config"
    private const val oauth_scopes = "repo read:org gist workflow"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cached_config: git_auth_config? = null

    fun load(context: Context): git_auth_config {
        cached_config?.let { return it }
        val value = runCatching { prefs(context).getString(config_key, null) }
            .getOrNull()
        val config = value?.let { runCatching { gson.fromJson(it, git_auth_config::class.java) }.getOrNull() }
            ?: git_auth_config()
        cached_config = config
        return config
    }

    fun save(context: Context, config: git_auth_config) {
        prefs(context).edit().putString(config_key, gson.toJson(config)).apply()
        cached_config = config
        clear_runtime_credentials()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(config_key).apply()
        cached_config = git_auth_config()
        clear_runtime_credentials()
    }

    fun active_config(): git_auth_config = load(gostudio_application.instance)

    /**
     * 为 proot 内的 git 准备认证环境。凭据只写入 0600 权限的临时 askpass 脚本，
     * 不拼进 git 命令，避免 URL、进程参数或日志泄露 Token/密码。
     */
    fun authentication_environment(config: git_auth_config = active_config()): Map<String, String> {
        return when (config.method) {
            git_auth_method.HTTPS_TOKEN,
            git_auth_method.HTTPS_PASSWORD,
            git_auth_method.GITHUB_OAUTH -> {
                val credential = when (config.method) {
                    git_auth_method.HTTPS_PASSWORD -> config.password
                    else -> config.token
                }
                if (config.username.isBlank() || credential.isBlank()) return emptyMap()
                val script = write_askpass_script(
                    name = "https-askpass.sh",
                    host = config.host.trim().lowercase().ifBlank { "github.com" },
                    username = config.username,
                    password = credential
                )
                linkedMapOf(
                    "GIT_ASKPASS" to script.second,
                    "GIT_TERMINAL_PROMPT" to "0",
                    "DISPLAY" to ":0"
                )
            }
            git_auth_method.SSH_KEY -> {
                if (config.ssh_private_key.isBlank()) return emptyMap()
                val key = write_private_key(config.ssh_private_key)
                val askpass = if (config.ssh_passphrase.isNotBlank()) {
                    write_ssh_askpass_script(config.ssh_passphrase).second
                } else ""
                linkedMapOf(
                    "GIT_SSH_COMMAND" to ssh_command(key.second),
                    "GIT_TERMINAL_PROMPT" to "0"
                ).apply {
                    if (askpass.isNotBlank()) {
                        put("SSH_ASKPASS", askpass)
                        put("SSH_ASKPASS_REQUIRE", "force")
                        put("DISPLAY", ":0")
                    }
                }
            }
            git_auth_method.NONE, git_auth_method.SYSTEM -> emptyMap()
        }
    }

    suspend fun login_with_github_device(
        client_id: String,
        on_prompt: suspend (github_device_prompt) -> Unit
    ): Result<git_auth_config> = withContext(Dispatchers.IO) {
        runCatching {
            require(client_id.isNotBlank()) { "OAuth Client ID 不能为空" }
            val code_response = post_form(
                url = "https://github.com/login/device/code",
                form = mapOf(
                    "client_id" to client_id.trim(),
                    "scope" to oauth_scopes
                )
            )
            val device_code = code_response["device_code"]
                ?: throw IllegalStateException(device_error(code_response))
            val user_code = code_response["user_code"]
                ?: throw IllegalStateException("GitHub 未返回设备码")
            val verification_uri = code_response["verification_uri"]
                ?: throw IllegalStateException("GitHub 未返回验证地址")
            val interval = code_response["interval"]?.toIntOrNull()?.coerceAtLeast(1) ?: 5
            val expires_in = code_response["expires_in"]?.toIntOrNull() ?: 900
            withContext(Dispatchers.Main) {
                on_prompt(github_device_prompt(verification_uri, user_code, expires_in))
            }

            val deadline = System.currentTimeMillis() + expires_in * 1000L
            while (System.currentTimeMillis() < deadline) {
                delay(interval * 1000L)
                val token_response = post_form(
                    url = "https://github.com/login/oauth/access_token",
                    form = mapOf(
                        "client_id" to client_id.trim(),
                        "device_code" to device_code,
                        "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"
                    )
                )
                val token = token_response["access_token"]
                if (!token.isNullOrBlank()) {
                    return@runCatching git_auth_config(
                        method = git_auth_method.GITHUB_OAUTH,
                        host = "github.com",
                        username = github_username(token).getOrDefault("github-user"),
                        token = token,
                        oauth_client_id = client_id.trim()
                    )
                }
                when (token_response["error"]) {
                    null -> throw IllegalStateException("GitHub OAuth 响应格式错误")
                    "authorization_pending" -> Unit
                    "slow_down" -> delay(interval * 1000L)
                    "expired_token" -> throw IllegalStateException("设备码已过期，请重新登录")
                    "access_denied" -> throw IllegalStateException("已取消 GitHub 授权")
                    else -> throw IllegalStateException(device_error(token_response))
                }
            }
            throw IllegalStateException("GitHub OAuth 登录超时")
        }
    }

    fun github_username(token: String): Result<String> = runCatching {
        require(token.isNotBlank()) { "Token 不能为空" }
        val request = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            require(response.isSuccessful) {
                "GitHub 返回 ${response.code}${if (body.isBlank()) "" else ": ${body.take(200)}"}"
            }
            val map = gson.fromJson(body, Map::class.java)
            map["login"]?.toString() ?: throw IllegalStateException("GitHub 未返回用户名")
        }
    }

    fun valid(config: git_auth_config): Boolean = when (config.method) {
        git_auth_method.NONE, git_auth_method.SYSTEM -> true
        git_auth_method.HTTPS_TOKEN, git_auth_method.GITHUB_OAUTH ->
            config.username.isNotBlank() && config.token.isNotBlank()
        git_auth_method.HTTPS_PASSWORD ->
            config.username.isNotBlank() && config.password.isNotBlank()
        git_auth_method.SSH_KEY -> config.ssh_private_key.isNotBlank()
    }

    private fun prefs(context: Context) = runCatching {
        EncryptedSharedPreferences.create(
            context,
            prefs_name,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("${prefs_name}_fallback", Context.MODE_PRIVATE)
    }

    private fun auth_root(): File {
        val root = File(toolchain_runtime_provider.paths().gostudio_dir, ".git-auth")
        root.mkdirs()
        Os.chmod(root.absolutePath, 448)
        return root
    }

    private fun guest_path(file: File): String =
        toolchain_runtime_provider.paths().host_to_guest_path(file.absolutePath)

    private fun write_askpass_script(
        name: String,
        host: String,
        username: String,
        password: String
    ): Pair<File, String> {
        val file = File(auth_root(), name)
        val user = Base64.getEncoder().encodeToString(username.toByteArray(Charsets.UTF_8))
        val pass = Base64.getEncoder().encodeToString(password.toByteArray(Charsets.UTF_8))
        val expected_host = Base64.getEncoder().encodeToString(host.toByteArray(Charsets.UTF_8))
        file.writeText(
            """#!/bin/sh
prompt="${'$'}1"
expected_host=$(printf '%s' '${'$'}expected_host' | base64 -d)
case "${'$'}prompt" in
  *"${'$'}expected_host"*) ;;
  *) exit 1 ;;
esac
case "${'$'}prompt" in
  *[Uu]sername*) printf '%s' '${'$'}user' | base64 -d ;;
  *) printf '%s' '${'$'}pass' | base64 -d ;;
esac
"""
        )
        Os.chmod(file.absolutePath, 448)
        return file to guest_path(file)
    }

    private fun write_ssh_askpass_script(passphrase: String): Pair<File, String> {
        val encoded = Base64.getEncoder().encodeToString(passphrase.toByteArray(Charsets.UTF_8))
        val file = File(auth_root(), "ssh-askpass.sh")
        file.writeText("#!/bin/sh\nprintf '%s' '$encoded' | base64 -d\n")
        Os.chmod(file.absolutePath, 448)
        return file to guest_path(file)
    }

    private fun write_private_key(content: String): Pair<File, String> {
        val file = File(auth_root(), "gostudio-git-key")
        file.writeText(content.trimEnd() + "\n")
        Os.chmod(file.absolutePath, 384)
        return file to guest_path(file)
    }

    fun ssh_command(config: git_auth_config): String {
        if (config.ssh_private_key.isBlank()) return "ssh"
        return ssh_command(write_private_key(config.ssh_private_key).second)
    }

    private fun ssh_command(key_path: String): String =
        "ssh -i '$key_path' -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new"

    private fun clear_runtime_credentials() {
        runCatching {
            auth_root().listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.writeText("")
                    file.delete()
                }
            }
        }
    }

    private suspend fun post_form(url: String, form: Map<String, String>): Map<String, String> {
        val builder = FormBody.Builder()
        form.forEach { (key, value) -> builder.add(key, value) }
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(builder.build())
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            require(response.isSuccessful) { "GitHub 返回 ${response.code}" }
            gson.fromJson(body, Map::class.java).mapKeys { it.key.toString() }
                .mapValues { it.value?.toString().orEmpty() }
        }
    }

    private fun device_error(response: Map<String, String>): String =
        response["error_description"] ?: response["error"] ?: "GitHub OAuth 登录失败"
}
