package com.jmwl.gostudio.ui.screens.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ai.ai_agent_loop
import com.jmwl.gostudio.ai.ai_provider
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** 待发送的附件（已转换为可直接拼接的文本片段） */
private data class ai_pending_attachment(
    val name: String,
    val content: String
)

private const val ai_attachment_max_text_bytes = 64 * 1024
private const val ai_attachment_max_file_bytes = 8 * 1024 * 1024

/**
 * AI 助手全屏页：
 * - 左上角图标打开左侧抽屉，展示最近会话（切换/新建/重命名/删除）
 * - 顶部为模型选择器 + 设置 + 关闭
 * - 底部输入框：左侧 + 号选择上传文件/图片，右侧蓝色发送按钮
 */
@Composable
fun ai_chat_page(
    agent: ai_agent_loop,
    on_close: () -> Unit,
    on_open_settings: () -> Unit,
    current_provider: ai_provider,
    current_model: String,
    available_models: Map<ai_provider, List<String>> = emptyMap(),
    configured_providers: Set<ai_provider> = emptySet(),
    on_session_model_change: (ai_provider, String) -> Unit = { _, _ -> },
    project_dir: java.io.File? = null,
    global_prompts_dir: java.io.File? = null,
    project_prompts_dir: java.io.File? = null,
    suggestion_prompts: List<String> = default_suggestion_prompts,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var cursor_pos by remember { mutableStateOf(0) }
    val is_running by agent.is_running.collectAsState()
    val list_state = rememberLazyListState()
    val show_thinking = remember { com.jmwl.gostudio.ai.load_ai_settings(context).show_thinking_process }
    val last_text_len = agent.messages.lastOrNull()?.text?.length ?: 0

    // 最近会话抽屉
    var drawer_open by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf(agent.list_sessions()) }
    val drawer_progress = remember { Animatable(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val drawer_width = 300.dp
    val drawer_width_px = with(density) { drawer_width.toPx() }
    val drawer_progress_value = drawer_progress.value
    val drawer_offset_px = (-(drawer_width_px * (1f - drawer_progress_value))).roundToInt()

    LaunchedEffect(drawer_open) {
        drawer_progress.animateTo(
            targetValue = if (drawer_open) 1f else 0f,
            animationSpec = tween(220)
        )
    }

    // 返回键：先关抽屉，再关页面
    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            drawer_open -> drawer_open = false
            else -> on_close()
        }
    }

    // 新消息 or 流式增长时自动滚到底
    LaunchedEffect(agent.messages.size, last_text_len) {
        if (agent.messages.isNotEmpty()) {
            list_state.animateScrollToItem(agent.messages.size - 1)
        }
    }

    // 待发送附件
    val attachments = remember { mutableStateListOf<ai_pending_attachment>() }
    var plus_menu_open by remember { mutableStateOf(false) }

    fun append_attachment(name: String, content: String) {
        attachments.add(ai_pending_attachment(name, content))
    }

    fun upload_target_dir(): java.io.File {
        return (if (project_dir != null && project_dir.isDirectory) {
            java.io.File(project_dir, "ai_uploads")
        } else {
            java.io.File(context.filesDir, "ai_uploads")
        }).apply { mkdirs() }
    }

    fun query_display_name(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull() ?: "file"
    }

    // 上传文件：文本内容直接拼进消息；二进制文件复制进项目 ai_uploads 后附路径
    val file_picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                }
            }.getOrNull()
            if (bytes == null) {
                withContext(Dispatchers.Main) { toast_attachment_error(context) }
                return@launch
            }
            if (bytes.size > ai_attachment_max_file_bytes) {
                withContext(Dispatchers.Main) { toast_attachment_too_large(context) }
                return@launch
            }
            val name = query_display_name(uri)
            val is_text = bytes.indexOf(0) < 0
            if (is_text) {
                val text = String(bytes, Charsets.UTF_8)
                val clipped = if (text.length > ai_attachment_max_text_bytes) {
                    text.take(ai_attachment_max_text_bytes) + "\n…(内容过长已截断)"
                } else {
                    text
                }
                withContext(Dispatchers.Main) {
                    append_attachment(name, "[附件: $name]\n$clipped\n[附件结束]")
                }
            } else {
                val target = java.io.File(upload_target_dir(), name)
                runCatching { target.writeBytes(bytes) }
                withContext(Dispatchers.Main) {
                    append_attachment(name, "[文件已复制到项目: ${target.absolutePath}]")
                }
            }
        }
    }

    // 上传图片：复制进项目 ai_uploads，消息中附上路径
    val image_picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                }
            }.getOrNull()
            if (bytes == null) {
                withContext(Dispatchers.Main) { toast_attachment_error(context) }
                return@launch
            }
            if (bytes.size > ai_attachment_max_file_bytes) {
                withContext(Dispatchers.Main) { toast_attachment_too_large(context) }
                return@launch
            }
            var name = query_display_name(uri).ifBlank { "image" }
            if (!name.contains('.')) name += ".png"
            val target = java.io.File(upload_target_dir(), name)
            runCatching { target.writeBytes(bytes) }
            withContext(Dispatchers.Main) {
                append_attachment(name, "[图片已保存到项目: ${target.absolutePath}]")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.editor_bg)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏：会话抽屉入口 + 模型选择 + 设置 + 关闭
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    sessions = agent.list_sessions()
                    drawer_open = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "最近会话",
                        tint = colors.top_button_icon
                    )
                }
                ai_model_selector(
                    current_provider = current_provider,
                    current_model = current_model,
                    available_models = available_models,
                    configured_providers = configured_providers,
                    on_session_model_change = on_session_model_change,
                    on_open_settings = on_open_settings,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = on_open_settings, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "AI 设置", tint = colors.top_button_icon, modifier = Modifier.size(19.dp))
                }
                IconButton(onClick = on_close, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.top_button_icon, modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = colors.input_border.copy(alpha = 0.3f))

            // 消息流
            if (agent.messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = colors.subtitle.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Text("问点什么呢？", fontSize = 14.sp, color = colors.subtitle)
                        Text("可以问我 Go 编程问题、解释代码、修复错误", fontSize = 11.sp, color = colors.subtitle.copy(alpha = 0.7f))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            suggestion_prompts.take(6).forEach { prompt ->
                                ai_suggestion_chip(prompt) { input = prompt }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = list_state,
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    itemsIndexed(agent.messages, key = { index, msg -> "$index-${msg.timestamp}-${msg.role.name}" }) { index, msg ->
                        ai_message_bubble(
                            message = msg,
                            show_thinking = show_thinking,
                            on_copy = { copy_text_to_clipboard(context, msg.text) },
                            on_share = { share_text(context, msg.text) },
                            on_delete = { agent.delete_message(index) },
                            on_regenerate = if (msg.role == com.jmwl.gostudio.ai.ai_message_role.ASSISTANT) {
                                { agent.regenerate_last() }
                            } else null,
                            on_edit = if (msg.role == com.jmwl.gostudio.ai.ai_message_role.USER) {
                                { new_text -> agent.edit_and_resend_user(index, new_text) }
                            } else null
                        )
                    }
                }
            }

            // 待发送附件 chips
            if (attachments.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    attachments.forEachIndexed { index, attachment ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.card_bg
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = colors.subtitle,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = attachment.name,
                                    fontSize = 11.sp,
                                    color = colors.card_text_title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移除附件",
                                    tint = colors.subtitle,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { attachments.removeAt(index) }
                                )
                            }
                        }
                    }
                }
            }

            // 输入区：+ 号（附件） | 输入框 | 发送/停止
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp).imePadding().navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // + 号：选择上传文件/图片
                Box {
                    FilledIconButton(
                        onClick = { plus_menu_open = true },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.card_bg,
                            contentColor = colors.top_button_icon
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加附件", modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(
                        expanded = plus_menu_open,
                        onDismissRequest = { plus_menu_open = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("上传文件", color = colors.dialog_text, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                plus_menu_open = false
                                file_picker.launch("*/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("上传图片", color = colors.dialog_text, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                plus_menu_open = false
                                image_picker.launch("image/*")
                            }
                        )
                    }
                }

                // 输入框 + 自动补全浮层（Box 锚点）
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            cursor_pos = it.length
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入问题…  用 @ 引用文件，/ 调用命令", fontSize = 12.sp, color = colors.input_hint) },
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.input_text,
                            unfocusedTextColor = colors.input_text,
                            focusedBorderColor = colors.title_highlight,
                            unfocusedBorderColor = colors.input_border,
                            cursorColor = colors.title_highlight,
                            focusedContainerColor = colors.card_bg,
                            unfocusedContainerColor = colors.card_bg
                        )
                    )
                    ai_input_completion_overlay(
                        text = input,
                        cursor = cursor_pos,
                        project_dir = project_dir,
                        global_prompts_dir = global_prompts_dir,
                        project_prompts_dir = project_prompts_dir,
                        on_select = { insert, trigger_start, _ ->
                            input = input.substring(0, trigger_start) + insert
                            cursor_pos = input.length
                        }
                    )
                }

                if (is_running) {
                    FilledIconButton(
                        onClick = { agent.cancel() },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.danger)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "停止", tint = colors.dialog_clone_text, modifier = Modifier.size(20.dp))
                    }
                } else {
                    FilledIconButton(
                        onClick = {
                            val text = input.trim()
                            if (text.isNotEmpty() || attachments.isNotEmpty()) {
                                val attachment_block = attachments.joinToString("\n\n") { it.content }
                                val full_text = listOf(attachment_block, text)
                                    .filter { it.isNotBlank() }
                                    .joinToString("\n\n")
                                agent.send_user_message(full_text)
                                attachments.clear()
                                input = ""
                                cursor_pos = 0
                            }
                        },
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        enabled = input.isNotBlank() || attachments.isNotEmpty(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.title_highlight,
                            disabledContainerColor = colors.title_highlight.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送", tint = colors.dialog_clone_text, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 左侧抽屉遮罩
        if (drawer_open) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = drawer_width)
                    .background(colors.editor_bg.copy(alpha = 0.35f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { drawer_open = false })
                    }
            )
        }

        // 左侧抽屉：最近会话
        if (drawer_open || drawer_progress_value > 0f) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(drawer_width)
                    .offset { IntOffset(drawer_offset_px, 0) },
                color = colors.editor_bg,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                ai_session_history_screen(
                    sessions = sessions,
                    current_session_id = agent.current_session_id(),
                    on_back = { drawer_open = false },
                    on_switch = { new_id ->
                        scope.launch {
                            agent.switch_session(new_id)
                            drawer_open = false
                        }
                    },
                    on_new = {
                        agent.new_session()
                        sessions = agent.list_sessions()
                        drawer_open = false
                    },
                    on_rename = { _, title ->
                        agent.rename_session(title)
                        sessions = agent.list_sessions()
                    },
                    on_delete = { id ->
                        agent.delete_session_by_id(id)
                        sessions = agent.list_sessions()
                    }
                )
            }
        }
    }
}

private fun toast_attachment_error(context: Context) {
    Toast.makeText(context, "读取文件失败", Toast.LENGTH_SHORT).show()
}

private fun toast_attachment_too_large(context: Context) {
    Toast.makeText(context, "文件超过 8MB，无法上传", Toast.LENGTH_SHORT).show()
}
