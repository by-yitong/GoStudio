package com.jmwl.gostudio.ui.screens.main

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.toolchain.git_auth_config
import com.jmwl.gostudio.toolchain.git_auth_manager
import com.jmwl.gostudio.toolchain.git_auth_method
import com.jmwl.gostudio.toolchain.git_manager
import com.jmwl.gostudio.ui.components.sub_page_top_bar
import com.jmwl.gostudio.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun git_settings_screen(on_back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = app_theme_provider.colors
    var config by remember { mutableStateOf(git_auth_manager.load(context)) }
    var test_url by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var oauth_running by remember { mutableStateOf(false) }
    var device_prompt by remember { mutableStateOf<com.jmwl.gostudio.toolchain.github_device_prompt?>(null) }
    var token_visible by remember { mutableStateOf(false) }
    var password_visible by remember { mutableStateOf(false) }
    var key_visible by remember { mutableStateOf(false) }
    var passphrase_visible by remember { mutableStateOf(false) }

    val key_launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                config = config.copy(
                    ssh_private_key = context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().decodeToString()
                    }.orEmpty(),
                    method = git_auth_method.SSH_KEY
                )
                message = "SSH 私钥已导入"
            }.onFailure { message = "私钥读取失败：${it.message}" }
        }
    }

    fun open_url(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { message = "请手动打开：$url" }
    }

    fun start_oauth() {
        if (oauth_running) return
        oauth_running = true
        message = "正在请求 GitHub 设备码..."
        scope.launch {
            git_auth_manager.login_with_github_device(config.oauth_client_id) { prompt ->
                device_prompt = prompt
                message = "请在 GitHub 页面输入 ${prompt.user_code}"
                open_url(prompt.verification_uri)
            }.onSuccess { logged_in ->
                device_prompt = null
                git_auth_manager.save(context, logged_in)
                config = logged_in
                message = "GitHub OAuth 登录成功：${logged_in.username}"
            }.onFailure { error ->
                device_prompt = null
                message = error.message ?: "GitHub OAuth 登录失败"
            }
            oauth_running = false
        }
    }

    fun test() {
        if (testing) return
        testing = true
        message = "正在测试 Git 登录..."
        scope.launch {
            val result = git_manager.test_authentication(config, test_url)
            message = result.second.ifBlank { if (result.first) "认证成功" else "认证失败" }
            testing = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { sub_page_top_bar("Git 登录", on_back) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "登录后可克隆、拉取、推送私有仓库。凭据使用 EncryptedSharedPreferences 加密保存，Git 命令日志不显示密码。",
                fontSize = 13.sp,
                color = colors.subtitle
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                git_auth_method.entries.forEach { method ->
                    SelectableAuthCard(
                        title = method.display_name,
                        description = auth_description(method),
                        selected = config.method == method,
                        colors = colors,
                        onClick = { config = config.copy(method = method) }
                    )
                }
            }

            git_setting_card(colors) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("登录信息", color = colors.card_text_title, fontSize = 16.sp)
                    if (config.method != git_auth_method.NONE) {
                        GitField(
                            value = config.host,
                            on_value_change = { config = config.copy(host = it) },
                            label = "Git 服务器",
                            placeholder = "github.com",
                            colors = colors
                        )
                    }

                    when (config.method) {
                        git_auth_method.HTTPS_TOKEN, git_auth_method.GITHUB_OAUTH -> {
                            GitField(config.username, { config = config.copy(username = it) }, "用户名", "GitHub 用户名", colors)
                            GitField(
                                config.token,
                                { config = config.copy(token = it) },
                                if (config.method == git_auth_method.GITHUB_OAUTH) "OAuth Token" else "Personal Access Token",
                                "ghp_ / github_pat_...",
                                colors,
                                secret = !token_visible,
                                trailing = {
                                    IconButton(onClick = { token_visible = !token_visible }) {
                                        Icon(if (token_visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                    }
                                }
                            )
                        }
                        git_auth_method.HTTPS_PASSWORD -> {
                            GitField(config.username, { config = config.copy(username = it) }, "用户名", "Git 用户名", colors)
                            GitField(
                                config.password,
                                { config = config.copy(password = it) },
                                "密码 / Token",
                                "密码或访问令牌",
                                colors,
                                secret = !password_visible,
                                trailing = {
                                    IconButton(onClick = { password_visible = !password_visible }) {
                                        Icon(if (password_visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                    }
                                }
                            )
                        }
                        git_auth_method.SSH_KEY -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { key_launcher.launch(arrayOf("*/*")) }) { Text("导入私钥") }
                                OutlinedButton(onClick = { key_visible = !key_visible }) {
                                    Text(if (key_visible) "隐藏" else "查看")
                                }
                            }
                            OutlinedTextField(
                                value = config.ssh_private_key,
                                onValueChange = { config = config.copy(ssh_private_key = it) },
                                label = { Text("OpenSSH 私钥") },
                                placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                                visualTransformation = if (key_visible) VisualTransformation.None else PasswordVisualTransformation(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                            GitField(
                                config.ssh_passphrase,
                                { config = config.copy(ssh_passphrase = it) },
                                "私钥口令（可选）",
                                "encrypted key passphrase",
                                colors,
                                secret = !passphrase_visible,
                                trailing = {
                                    IconButton(onClick = { passphrase_visible = !passphrase_visible }) {
                                        Icon(if (passphrase_visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                    }
                                }
                            )
                        }
                        git_auth_method.GITHUB_OAUTH -> {
                            GitField(
                                config.oauth_client_id,
                                { config = config.copy(oauth_client_id = it) },
                                "OAuth Client ID",
                                "GitHub OAuth App 的 Client ID",
                                colors
                            )
                            Button(enabled = !oauth_running, onClick = ::start_oauth) {
                                Text(if (oauth_running) "等待授权..." else "设备码登录 GitHub")
                            }
                            Text(
                                "在 GitHub 创建 OAuth App 时启用 Device Flow，然后填入它的 Client ID。",
                                fontSize = 12.sp,
                                color = colors.subtitle
                            )
                            device_prompt?.let { prompt ->
                                Text(
                                    "打开 ${prompt.verification_uri}，输入 ${prompt.user_code}",
                                    fontSize = 13.sp,
                                    color = colors.title_highlight
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }

            if (config.method == git_auth_method.NONE ||
                config.method == git_auth_method.SYSTEM ||
                config.method == git_auth_method.HTTPS_PASSWORD
            ) {
                git_setting_card(colors) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("认证测试", color = colors.card_text_title, fontSize = 16.sp)
                        GitField(test_url, { test_url = it }, "测试仓库地址", "https://github.com/owner/private-repo.git", colors)
                        Button(enabled = !testing, onClick = ::test) { Text(if (testing) "测试中..." else "测试登录") }
                    }
                }
            } else {
                Button(enabled = !testing, onClick = ::test, modifier = Modifier.fillMaxWidth()) {
                    Text(if (testing) "测试中..." else "测试登录")
                }
            }

            if (message.isNotBlank()) {
                Text(message, fontSize = 13.sp, color = colors.title_highlight)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = git_auth_manager.valid(config),
                    onClick = {
                        git_auth_manager.save(context, config)
                        message = "Git 登录配置已保存"
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("保存") }
                OutlinedButton(
                    onClick = {
                        git_auth_manager.clear(context)
                        config = git_auth_config()
                        test_url = ""
                        message = "已退出 Git 登录"
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("退出登录") }
            }
        }
    }
}

private fun auth_description(method: git_auth_method): String = when (method) {
    git_auth_method.NONE -> "匿名访问公开仓库"
    git_auth_method.HTTPS_TOKEN -> "GitHub/Gitee/GitLab PAT，推荐用于私有仓库"
    git_auth_method.HTTPS_PASSWORD -> "HTTPS Basic 认证（GitHub 已不支持账号密码，请用 Token）"
    git_auth_method.SSH_KEY -> "OpenSSH 私钥，支持口令；适合 push/pull"
    git_auth_method.GITHUB_OAUTH -> "GitHub 设备码授权，需要 OAuth App Client ID"
    git_auth_method.SYSTEM -> "复用终端 ~/.gitconfig、credential helper 与默认 SSH Key"
}

@Composable
private fun git_setting_card(colors: app_colors, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.card_bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun GitField(
    value: String,
    on_value_change: (String) -> Unit,
    label: String,
    placeholder: String,
    colors: app_colors,
    secret: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = on_value_change,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Uri),
        trailingIcon = trailing,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableAuthCard(
    title: String,
    description: String,
    selected: Boolean,
    colors: app_colors,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) colors.card_pressed else colors.card_bg,
        border = if (selected) BorderStroke(1.dp, colors.title_highlight) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.padding(start = 4.dp)) {
                Text(title, fontSize = 15.sp, color = colors.card_text_title)
                Text(description, fontSize = 12.sp, color = colors.subtitle)
            }
        }
    }
}
