package com.jmwl.gostudio.ui.dialogs.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.toolchain.goproxy_store
import com.jmwl.gostudio.toolchain.proot_manager
import com.jmwl.gostudio.toolchain.toolchain_manager
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.toast.app_toast
import kotlinx.coroutines.launch

private const val custom_key = "自定义"

/**
 * 模块代理（GOPROXY）切换弹窗：内置源单选 + 自定义输入，
 * 一键测速（并发 GET 各源的 @latest 小文件）并把最快的自动设为推荐。
 * 应用后写入 app 设置并 `go env -w` 同步到 proot 的 go env 文件，
 * 终端与 app 自跑命令随即都走新源。
 */
@Composable
fun goproxy_settings_dialog(
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val current_name = goproxy_store.current_display_name()
    var selected_key by remember { mutableStateOf(current_name) }
    var custom_text by remember {
        mutableStateOf(if (current_name == custom_key) goproxy_store.current() else "")
    }
    var testing by remember { mutableStateOf(false) }
    var latencies by remember { mutableStateOf<Map<String, Long?>>(emptyMap()) }
    var fastest_name by remember { mutableStateOf<String?>(null) }

    fun start_speed_test() {
        if (testing) return
        testing = true
        scope.launch {
            val results = goproxy_store.speed_test(custom_text.takeIf { it.isNotBlank() })
            latencies = results.associate { it.candidate.name to it.latencyMs }
            // 把可用的最快源设为推荐（自动选中，用户仍可改选）
            fastest_name = results.firstOrNull { it.latencyMs != null }?.candidate?.name
            fastest_name?.let { selected_key = it }
            testing = false
        }
    }

    fun apply_selection() {
        val is_custom = selected_key == custom_key
        val url = (if (is_custom) custom_text.trim() else goproxy_store.builtin_sources
            .firstOrNull { it.name == selected_key }?.url).orEmpty()
        if (url.isBlank()) {
            app_toast.show(context, "请输入自定义代理地址", app_toast.LENGTH_SHORT)
            return
        }
        scope.launch {
            goproxy_store.set(context, selected_key, url)
            // go 已装时同步写进 go env 文件（终端里的 go 命令读这份文件）
            var written = true
            if (toolchain_manager.is_go_installed()) {
                written = proot_manager.execute_command_with_environment(
                    command = "go env -w GOPROXY=$url GOSUMDB=${goproxy_store.sumdb_for(url)}",
                    working_dir = "/home",
                    on_log = {}
                )
            }
            app_toast.show(
                context,
                if (written) "已切换到 $selected_key" else "已保存，但写入 go env 失败",
                if (written) app_toast.LENGTH_SHORT else app_toast.LENGTH_LONG
            )
            on_dismiss()
        }
    }

    AlertDialog(
        onDismissRequest = on_dismiss,
        containerColor = colors.dialog_bg,
        title = {
            Text(
                text = "模块代理 (GOPROXY)",
                color = colors.dialog_text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 380.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    for (source in goproxy_store.builtin_sources) {
                        goproxy_source_row(
                            name = source.name,
                            url = source.url,
                            note = source.note,
                            selected = selected_key == source.name,
                            latency_ms = latencies[source.name],
                            is_fastest = fastest_name == source.name,
                            on_select = { selected_key = source.name }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    goproxy_source_row(
                        name = custom_key,
                        url = custom_text.ifBlank { "手动输入代理地址" },
                        note = "",
                        selected = selected_key == custom_key,
                        latency_ms = latencies[custom_key],
                        is_fastest = fastest_name == custom_key,
                        on_select = { selected_key = custom_key }
                    )
                    OutlinedTextField(
                        value = custom_text,
                        onValueChange = {
                            custom_text = it
                            if (it.isNotBlank()) selected_key = custom_key
                        },
                        placeholder = {
                            Text("https://example.com/goproxy,direct", fontSize = 12.sp, color = colors.subtitle)
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = colors.dialog_text),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, top = 2.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            color = colors.title_highlight,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("测速中…", color = colors.subtitle, fontSize = 12.sp)
                    } else {
                        TextButton(onClick = { start_speed_test() }) {
                            Text("测速并推荐最快", color = colors.title_highlight, fontSize = 13.sp)
                        }
                    }
                }
                Text(
                    text = "应用后终端与构建命令立即走新源；Go 未安装时仅保存，安装后自动生效。",
                    color = colors.subtitle,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { apply_selection() }) {
                Text("应用", color = colors.title_highlight)
            }
        },
        dismissButton = {
            TextButton(onClick = on_dismiss) {
                Text("取消", color = colors.subtitle)
            }
        }
    )
}

@Composable
private fun goproxy_source_row(
    name: String,
    url: String,
    note: String,
    selected: Boolean,
    latency_ms: Long?,
    is_fastest: Boolean,
    on_select: () -> Unit
) {
    val colors = app_theme_provider.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { on_select() }
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = on_select,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.title_highlight,
                unselectedColor = colors.subtitle
            ),
            modifier = Modifier.size(36.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = colors.dialog_text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (is_fastest) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.success.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "最快",
                            color = colors.success,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                text = url,
                color = colors.subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
            if (note.isNotBlank()) {
                Text(
                    text = note,
                    color = colors.subtitle,
                    fontSize = 10.sp
                )
            }
        }
        latency_text(latency_ms, is_fastest)
    }
}

@Composable
private fun latency_text(latency_ms: Long?, tested: Boolean) {
    val colors = app_theme_provider.colors
    if (!tested) return
    val text = when {
        latency_ms == null -> "不可用"
        else -> "${latency_ms}ms"
    }
    Text(
        text = text,
        color = if (latency_ms == null) colors.danger else colors.success,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 6.dp)
    )
}
