package com.jmwl.gostudio.activity

import com.jmwl.gostudio.editor.session.editor_activity_state
import com.jmwl.gostudio.editor.session.editor_open_tab
import com.jmwl.gostudio.editor.session.editor_pending_action
import com.jmwl.gostudio.editor.tabs.find_dirty_closable_tab_index
import com.jmwl.gostudio.editor.tabs.find_dirty_tab_index
import com.jmwl.gostudio.editor.tabs.ordered_pinned_first_tabs
import com.jmwl.gostudio.editor.tabs.pinned_tab_paths
import com.jmwl.gostudio.editor.tabs.pinned_tabs
import com.jmwl.gostudio.editor.tabs.remaining_tabs_after_close_others
import com.jmwl.gostudio.editor.settings.*
import com.jmwl.gostudio.editor.model.*
import com.jmwl.gostudio.editor.core.*

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.jmwl.gostudio.ui.toast.app_toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.jmwl.gostudio.core.logging.logger_manager
import com.jmwl.gostudio.toolchain.proot_manager
import com.jmwl.gostudio.toolchain.toolchain_manager
import com.jmwl.gostudio.toolchain.toolchain_runtime_provider
import com.jmwl.gostudio.project.detected_project
import com.jmwl.gostudio.project.project_build_config
import com.jmwl.gostudio.project.project_detector
import com.jmwl.gostudio.project.project_ide_config
import com.jmwl.gostudio.project.project_kind
import com.jmwl.gostudio.project.project_manager
import com.jmwl.gostudio.lsp.gopls.gopls_lsp_config
import com.jmwl.gostudio.lsp.gopls.gopls_lsp_project
import com.jmwl.gostudio.lsp.gopls.apply_code_action
import com.jmwl.gostudio.lsp.gopls.current_diagnostics
import com.jmwl.gostudio.lsp.gopls.request_code_actions
import com.jmwl.gostudio.lsp.gopls.request_definition
import com.jmwl.gostudio.editor.theme.editor_theme_manager
import com.jmwl.gostudio.ui.dialogs.editor.editor_exit_confirm_dialog
import com.jmwl.gostudio.ui.dialogs.editor.editor_unsaved_file_dialog
import com.jmwl.gostudio.ui.screens.editor.*
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.gostudio_application
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspLanguage
import io.github.rosemoe.sora.lsp.editor.LspEditorStatus
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class editor_activity : ComponentActivity() {
    private lateinit var project_dir: File
    private lateinit var editor: CodeEditor
    private val state = editor_activity_state()
    private val output_panel_state = editor_output_panel_state()
    private lateinit var detected_project_info: detected_project
    private var applying_editor_content = false
    private var current_textmate_scope: String? = null
    private var block_hint_job: Job? = null
    private var go_build_job: Job? = null
    private var go_run_job: Job? = null
    private var file_tree_job: Job? = null
    private var textmate_prewarm_started = false
    private var gopls_project: gopls_lsp_project? = null
    private var gopls_connect_job: Job? = null
    private val gopls_skipped_files = mutableSetOf<String>()

    /** 光标是否在可跳转定义的标识符上，true 时编辑器右上角显示跳转图标 */
    private val _can_goto_definition = MutableStateFlow(false)
    val can_goto_definition: kotlinx.coroutines.flow.StateFlow<Boolean> = _can_goto_definition
    /** 跳转定义进行中（请求 gopls 时短暂置 true，避免重复点击） */
    private val _goto_in_progress = MutableStateFlow(false)
    val goto_in_progress: kotlinx.coroutines.flow.StateFlow<Boolean> = _goto_in_progress

    /** 当前文件代码结构符号（gopls 优先，正则兜底；见 refresh_structure_symbols） */
    private var structure_symbols by mutableStateOf(emptyList<com.jmwl.gostudio.editor.core.editor_outline_symbol>())
    private var structure_job: Job? = null

    /** 当前文件的 gopls 诊断（问题页签 + 诊断弹层数据源） */
    private var file_diagnostics by mutableStateOf(emptyList<com.jmwl.gostudio.lsp.gopls.gopls_diagnostic>())
    private var diagnostics_job: Job? = null
    /** 诊断详情弹层状态 */
    private var sheet_diagnostic by mutableStateOf<com.jmwl.gostudio.lsp.gopls.gopls_diagnostic?>(null)
    private var sheet_actions by mutableStateOf(emptyList<com.jmwl.gostudio.lsp.gopls.gopls_code_action>())
    private var sheet_actions_loading by mutableStateOf(false)
    private var sheet_actions_job: Job? = null

    /** AI agent（在 project_dir 初始化后于 initialize_project 创建） */
    private var ai_agent: com.jmwl.gostudio.ai.ai_agent_loop? = null
    /** AI prompts 目录（供输入补全使用） */
    private var ai_global_prompts_dir: java.io.File? = null
    private var ai_project_prompts_dir: java.io.File? = null
    /** AI 弹窗打开触发器（编辑器选区 AI 动作时 +1） */
    private var ai_open_trigger by mutableStateOf(0)
    /** AI 文件变更通知器（工具改文件后刷新编辑器） */
    private var ai_file_change_notifier: com.jmwl.gostudio.ai.ai_file_change_notifier? = null
    /** AI 设置页覆盖层开关 */
    private var show_ai_settings by mutableStateOf(false)
    /** 会话级提供商/模型 override（null=跟随全局设置）。可观察以驱动选择器显示。 */
    private var _session_override by mutableStateOf<Pair<com.jmwl.gostudio.ai.ai_provider, String>?>(null)
    val session_override: Pair<com.jmwl.gostudio.ai.ai_provider, String>? get() = _session_override

    /**
     * 以下 4 个函数会在每次 Compose 重组时被调用（光标每移动一次就重组一次），
     * 必须走内存缓存 [cached_ai_settings]；直接用 load_ai_settings 会在主线程反复做
     * EncryptedSharedPreferences/Keystore I/O，导致光标移动卡顿。
     */
    /** 当前生效的提供商（会话 override 优先，否则全局） */
    private fun current_ai_provider(): com.jmwl.gostudio.ai.ai_provider =
        _session_override?.first ?: com.jmwl.gostudio.ai.cached_ai_settings(this).provider

    /** 当前生效的模型 */
    private fun current_ai_model(): String =
        _session_override?.second ?: com.jmwl.gostudio.ai.cached_ai_settings(this).model

    /** 各提供商可用的模型（来自全局 custom_models 缓存，按 base_url 映射到 provider） */
    private fun current_ai_available_models(): Map<com.jmwl.gostudio.ai.ai_provider, List<String>> {
        val s = com.jmwl.gostudio.ai.cached_ai_settings(this)
        return com.jmwl.gostudio.ai.ai_provider.entries.associateWith { p ->
            s.custom_models[p.base_url] ?: emptyList()
        }
    }

    /** 已配置 key 的供应商（会话栏选择器只显示这些） */
    private fun configured_ai_providers(): Set<com.jmwl.gostudio.ai.ai_provider> {
        val s = com.jmwl.gostudio.ai.cached_ai_settings(this)
        return s.api_keys.filter { it.value.isNotBlank() }.keys
    }

    private val file_tree_children_cache = mutableMapOf<String, List<editor_file_node>>()
    private lateinit var search_controller: editor_search_controller
    private lateinit var tab_lifecycle: editor_tab_lifecycle
    private val import_editor_font_launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            import_editor_font_from_uri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val project_path = intent.getStringExtra("project_path") ?: ""
        val project_name = intent.getStringExtra("project_name")
            ?: File(project_path).name.ifBlank { "项目" }

        project_dir = File(project_path)
        state.project_name = project_name.ifBlank { project_dir.name.ifBlank { "项目" } }
        state.expanded_paths = setOf(project_dir.absolutePath)
        state.project_exists = project_dir.exists() && project_dir.isDirectory
        detected_project_info = project_detector.detect_project(project_dir.absolutePath)
        append_detected_project_log()
        state.editor_settings = load_editor_settings(this)
        tab_lifecycle = create_tab_lifecycle()
        editor = create_code_editor()
        prewarm_textmate_languages()
        search_controller = editor_search_controller(
            editor = editor,
            can_search = { state.current_file_path != null },
            can_replace = { state.current_file_path != null && !state.read_only }
        )

        enableEdgeToEdge()
        setContent {
            app_theme_provider {
                editor_activity_content()
            }
        }
        initialize_project()
    }

    override fun onResume() {
        super.onResume()
        // 从全局设置改完编辑器配置返回后，重载让设置即时生效
        val latest = load_editor_settings(this)
        if (latest != state.editor_settings) {
            state.editor_settings = latest
            apply_editor_settings(latest)
        }
    }

    override fun onDestroy() {
        ai_agent?.shutdown() // 清理 MCP server 进程
        block_hint_job?.cancel()
        go_build_job?.cancel()
        go_run_job?.cancel()
        file_tree_job?.cancel()
        gopls_connect_job?.cancel()
        gopls_project?.dispose()
        gopls_project = null
        val tab_editors = state.open_tabs.mapNotNull { tab -> tab.editor }.toSet()
        state.open_tabs.toList().forEach { tab -> release_tab_editor(tab) }
        if (::editor.isInitialized && editor !in tab_editors) {
            runCatching { editor.setText("") }
            runCatching { editor.release() }
        }
        super.onDestroy()
    }

    private fun append_detected_project_log() {
        when (detected_project_info.kind) {
            project_kind.GO -> {
                detected_project_info.build_file_path?.let { path ->
                    output_panel_state.append_log("go.mod: $path")
                }
                detected_project_info.build_dir?.let { path ->
                    output_panel_state.append_log("输出目录: $path")
                }
            }
            project_kind.UNKNOWN -> {
                output_panel_state.append_log("未识别到项目（缺少 go.mod）", editor_output_line_level.WARNING)
            }
        }
    }

    @Composable
    private fun editor_activity_content() {
        val colors = app_theme_provider.colors
        val goto_running by goto_in_progress.collectAsState()

        BackHandler(enabled = true) {
            when {
                state.show_unsaved_dialog -> Unit
                state.show_exit_dialog -> state.show_exit_dialog = false
                else -> request_close_editor()
            }
        }

        LaunchedEffect(colors) {
            apply_colors_to_open_editors()
        }

        LaunchedEffect(Unit) {
            editor_theme_manager.version.collect {
                apply_colors_to_open_editors()
            }
        }

        LaunchedEffect(state.read_only, state.open_tabs.size) {
            open_editors().forEach { tab_editor -> tab_editor.isEditable = !state.read_only }
        }

        // 代码结构符号：打开文件时刷新（编辑中由 handle_editor_content_changed 防抖触发）
        LaunchedEffect(state.current_file_path) {
            refresh_structure_symbols()
            schedule_diagnostics_refresh()
        }

        editor_screen(
            project_name = state.project_name,
            project_root_path = project_dir.absolutePath,
            editor = editor,
            current_file_name = state.current_file_name,
            // 光标状态以 provider/StateFlow 下传，在叶子组件里读取，
            // 避免每次光标移动重组整个 editor_screen（见 editor_screen 注释）
            cursor_line_provider = { state.cursor_line },
            cursor_column_provider = { state.cursor_column },
            cursor_selected_provider = { state.cursor_selected },
            has_changes = state.has_changes,
            loading = state.loading,
            read_only = state.read_only,
            can_undo = state.can_undo,
            can_redo = state.can_redo,
            file_nodes = state.file_nodes,
            expanded_paths = state.expanded_paths,
            file_tree_loading = state.file_tree_loading,
            project_exists = state.project_exists,
            has_open_file = state.current_file_path != null,
            tabs = state.open_tabs.map { tab ->
                editor_tab_item(
                    path = tab.file_path,
                    title = tab.file_name,
                    has_changes = tab.has_changes,
                    pinned = tab.pinned
                )
            },
            selected_tab_path = state.current_file_path,
            toolbar_visible = state.toolbar_visible,
            output_panel_state = output_panel_state,
            terminal_cwd = project_dir.takeIf { it.isDirectory }?.absolutePath
                ?: toolchain_runtime_provider.paths().home_dir.absolutePath,
            terminal_extra_environment = if (project_dir.isDirectory) {
                toolchain_manager.project_environment(project_dir.absolutePath).environment
            } else {
                emptyMap()
            },
            on_toggle_toolbar = { state.toolbar_visible = !state.toolbar_visible },
            on_project_config_apply = { config, on_saved -> apply_project_config(config, on_saved) },
            on_select_tab = { path -> request_select_tab(path) },
            on_pin_tab = { path -> toggle_pin_tab(path) },
            on_close_tab = { path -> request_close_tab(path) },
            on_close_other_tabs = { path -> request_close_other_tabs(path) },
            on_close_all_tabs = { request_close_all_tabs() },
            on_build = { handle_build_button_click() },
            on_run = { run_go_project() },
            on_test = { run_go_tests() },
            on_save = { request_save_file() },
            on_format = { format_current_file() },
            on_toggle_read_only = { toggle_read_only() },
            on_undo = { undo() },
            on_redo = { redo() },
            on_search_change = { query, match_case, whole_word, regex ->
                update_search(query, match_case, whole_word, regex)
            },
            on_search_previous = { goto_search_result(forward = false) },
            on_search_next = { goto_search_result(forward = true) },
            on_replace_current = { replacement -> replace_current_match(replacement) },
            on_replace_all = { replacement -> replace_all_matches(replacement) },
            on_clear_search = { clear_search() },
            on_insert_symbol = { symbol -> insert_symbol(symbol) },
            on_create_file = { parent_path, name -> create_project_file(parent_path, name) },
            on_create_folder = { parent_path, name -> create_project_folder(parent_path, name) },
            on_refresh_files = { path -> refresh_file_tree(path) },
            on_rename_file_tree_node = { path, new_name -> rename_project_entry(path, new_name) },
            on_delete_file_tree_node = { path -> delete_project_entry(path) },
            on_directory_click = { path -> toggle_directory(path) },
            on_file_click = { path -> request_open_file(path) },
            on_file_position_click = { path, line, column -> open_file_at(path, line, column) },
            can_goto_definition_flow = can_goto_definition,
            goto_definition_running = goto_running,
            on_goto_definition = { goto_definition() },
            structure_file_name = state.current_file_path?.let { File(it).name },
            structure_symbols = structure_symbols,
            on_structure_navigate = { line -> navigate_to_structure_line(line) },
            on_project_search = { keyword -> search_project(keyword) },
            file_diagnostics = file_diagnostics,
            on_diagnostic_click = { diagnostic -> open_diagnostic_sheet(diagnostic) },
            ai_agent = ai_agent,
            on_open_ai_settings = { show_ai_settings = true },
            ai_current_provider = current_ai_provider(),
            ai_current_model = current_ai_model(),
            ai_available_models = current_ai_available_models(),
            ai_configured_providers = configured_ai_providers(),
            on_ai_session_model_change = { p, m -> _session_override = p to m },
            ai_global_prompts_dir = ai_global_prompts_dir,
            ai_project_prompts_dir = ai_project_prompts_dir,
            ai_open_trigger = ai_open_trigger
        )

        AnimatedVisibility(
            visible = show_ai_settings,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.editor_bg)
            ) {
                com.jmwl.gostudio.ui.screens.ai.ai_settings_screen(
                    initial = com.jmwl.gostudio.ai.load_ai_settings(this@editor_activity),
                    on_back = { show_ai_settings = false },
                    on_save = { new_settings ->
                        com.jmwl.gostudio.ai.save_ai_settings(this@editor_activity, new_settings)
                        show_ai_settings = false
                    },
                    project_dir = project_dir
                )
            }
        }

        BackHandler(enabled = show_ai_settings) {
            show_ai_settings = false
        }

        // 诊断详情弹层（问题页签点击条目打开）
        sheet_diagnostic?.let { diagnostic ->
            BackHandler { sheet_diagnostic = null }
            com.jmwl.gostudio.ui.screens.editor.editor_diagnostic_sheet(
                diagnostic = diagnostic,
                actions = sheet_actions,
                actions_loading = sheet_actions_loading,
                on_apply_action = { action -> apply_diagnostic_action(action) },
                on_dismiss = { sheet_diagnostic = null }
            )
        }

        if (state.show_exit_dialog) {
            editor_exit_confirm_dialog(
                on_confirm = { confirm_close_editor() },
                on_cancel = { state.show_exit_dialog = false }
            )
        }

        if (state.show_unsaved_dialog) {
            editor_unsaved_file_dialog(
                file_name = state.current_file_name,
                on_save = { save_pending_action() },
                on_discard = { discard_pending_action() }
            )
        }
    }

    private fun create_tab_lifecycle(): editor_tab_lifecycle {
        return editor_tab_lifecycle(
            context = this,
            settings = { state.editor_settings },
            create_textmate_language = { file_path -> create_configured_textmate_language(file_path) },
            with_applying_content = { action -> with_applying_editor_content(action) },
            on_content_changed = { handle_editor_content_changed() },
            on_selection_changed = { changed_editor -> handle_editor_selection_changed(changed_editor) },
            current_comment_action = { current_line_comment_action() },
            on_toggle_comment = { toggle_line_comment() },
            on_ai_action = { action, selection -> handle_ai_action_from_editor(action, selection) },
            initial_styles_timeout_ms = initial_editor_styles_timeout_ms
        )
    }

    /** 编辑器选区 AI 动作：打开 AI 弹窗并注入提示 */
    private fun handle_ai_action_from_editor(action: String, selection: String) {
        val agent = ai_agent ?: run {
            app_toast.show(this, "AI 助手未就绪", app_toast.LENGTH_SHORT)
            return
        }
        val prompt = when (action) {
            "explain" -> "请解释这段代码：\n```\n${selection.take(2000)}\n```"
            "fix" -> "这段代码有问题，请帮我修复：\n```\n${selection.take(2000)}\n```"
            else -> "请处理这段代码：\n```\n${selection.take(2000)}\n```"
        }
        ai_open_trigger += 1
        agent.send_user_message(prompt)
    }

    private fun create_code_editor(): CodeEditor {
        return tab_lifecycle.create_editor(null)
    }

    private fun create_tab_editor(file_path: String?): CodeEditor {
        return tab_lifecycle.create_editor(file_path)
    }

    private fun with_applying_editor_content(action: () -> Unit) {
        applying_editor_content = true
        try {
            action()
        } finally {
            applying_editor_content = false
        }
    }

    private fun handle_editor_content_changed() {
        if (!applying_editor_content) {
            active_tab()?.has_changes = true
            state.has_changes = true
        }
        update_history_state()
        schedule_block_end_hints_update()
        schedule_structure_refresh()
        schedule_diagnostics_refresh()
    }

    private fun handle_editor_selection_changed(changed_editor: CodeEditor) {
        val line = changed_editor.cursor.leftLine
        val column = changed_editor.cursor.leftColumn
        state.cursor_line = line + 1
        state.cursor_column = column + 1
        state.cursor_selected = changed_editor.cursor.isSelected

        // 判断光标是否在可跳转的标识符上（非选区状态，且光标紧邻字母/下划线）
        val on_identifier = if (!changed_editor.cursor.isSelected) {
            is_cursor_on_identifier(changed_editor, line, column)
        } else {
            false
        }
        _can_goto_definition.value = on_identifier

        active_tab()?.let { tab ->
            tab.cursor_line = line
            tab.cursor_column = column
        }
    }

    /** 判断光标是否位于标识符上（字母/数字/下划线），用于决定是否显示跳转定义图标 */
    private fun is_cursor_on_identifier(editor: CodeEditor, line: Int, column: Int): Boolean {
        val text = editor.text ?: return false
        // 优先看光标左侧字符，再看右侧
        if (column > 0) {
            val ch = text.charAt(line, column - 1)
            if (ch.isLetterOrDigit() || ch == '_') return true
        }
        if (column < text.getColumnCount(line)) {
            val ch = text.charAt(line, column)
            if (ch.isLetterOrDigit() || ch == '_') return true
        }
        return false
    }

    private fun update_editor_settings(settings: editor_settings_state) {
        apply_editor_settings(settings)
    }

    private fun apply_editor_settings(settings: editor_settings_state) {
        val gopls_settings_changed = state.editor_settings.gopls_enabled != settings.gopls_enabled ||
            state.editor_settings.gopls_completion != settings.gopls_completion ||
            state.editor_settings.gopls_signature_help != settings.gopls_signature_help ||
            state.editor_settings.gopls_document_highlight != settings.gopls_document_highlight ||
            state.editor_settings.gopls_formatting != settings.gopls_formatting ||
            state.editor_settings.gopls_hover != settings.gopls_hover
        state.editor_settings = settings
        save_editor_settings(this, settings)
        if (gopls_settings_changed) {
            reset_gopls_project()
        }
        open_editors().forEach { tab_editor ->
            apply_editor_behavior_settings(
                context = this,
                editor = tab_editor,
                settings = settings,
                file_path = state.open_tabs.firstOrNull { tab -> tab.editor == tab_editor }?.file_path ?: state.current_file_path,
                current_language = current_textmate_language(tab_editor)
            )
            tab_editor.invalidate()
        }
        schedule_block_end_hints_update()
    }

    private fun open_editors(): List<CodeEditor> {
        return (state.open_tabs.mapNotNull { tab -> tab.editor } + listOfNotNull(if (::editor.isInitialized) editor else null))
            .distinct()
    }

    private fun apply_colors_to_open_editors() {
        open_editors().forEach { tab_editor ->
            apply_editor_colors(this, tab_editor)
            tab_editor.invalidate()
        }
    }

    private fun apply_current_editor_behavior_settings(target: CodeEditor, settings: editor_settings_state) {
        apply_editor_behavior_settings(
            context = this,
            editor = target,
            settings = settings,
            file_path = state.current_file_path,
            current_language = current_textmate_language(target)
        )
    }

    private fun request_import_editor_font() {
        import_editor_font_launcher.launch("font/*")
    }

    private fun apply_project_config(config: project_ide_config, on_saved: () -> Unit) {
        project_manager.save_project_ide_config(project_dir.absolutePath, config)
            .onSuccess {
                on_saved()
                app_toast.show(this, "项目配置已应用", app_toast.LENGTH_SHORT)
                // 配置变更后重启 gopls，让其重新加载构建约束等
                reset_gopls_project()
            }
            .onFailure { error ->
                app_toast.show(this, "项目配置保存失败: ${error.message}", app_toast.LENGTH_LONG)
            }
    }

    private fun initialize_project() {
        // 创建 AI agent（带项目文件工具）
        setup_ai_agent()

        reload_file_tree {
            lifecycleScope.launch {
                if (!state.project_exists) {
                    state.status_text = "项目不存在"
                    return@launch
                }

                prewarm_textmate_languages()
                restore_pinned_tabs()
            }
        }
    }

    /**
     * 创建带项目文件工具的 AI agent。
     * 工具：read/write/edit/grep/ls（项目根限定）+ bash（proot 执行，带 Go 环境）。
     * 环境提供器：实时收集当前文件/光标/项目结构。
     */
    private fun setup_ai_agent() {
        val project = project_dir
        val home_dir = toolchain_runtime_provider.paths().home_dir
        val ai_root = java.io.File(home_dir, ".ai")
        ai_root.mkdirs()
        val global_skills_dir = java.io.File(ai_root, "skills")
        val global_prompts_dir = java.io.File(ai_root, "prompts")
        val sessions_dir = java.io.File(ai_root, "sessions")
        val project_skills_dir = java.io.File(project, ".ai/skills")
        val project_prompts_dir = java.io.File(project, ".ai/prompts")
        // 暴露给输入补全 UI 使用
        ai_global_prompts_dir = global_prompts_dir
        ai_project_prompts_dir = project_prompts_dir

        // 释放内置 skill 到全局目录（首次）
        runCatching { com.jmwl.gostudio.ai.skills.release_builtin_skills(this, global_skills_dir) }

        val go_env = runCatching {
            toolchain_manager.project_environment(project.absolutePath).environment
        }.getOrDefault(emptyMap())

        val registry = com.jmwl.gostudio.ai.tools.ai_tool_registry().apply {
            register(com.jmwl.gostudio.ai.tools.read_tool(project))
            register(com.jmwl.gostudio.ai.tools.edit_tool(project))
            register(com.jmwl.gostudio.ai.tools.grep_tool(project))
            register(com.jmwl.gostudio.ai.tools.ls_tool(project))
            register(com.jmwl.gostudio.ai.tools.write_tool(project))
            register(com.jmwl.gostudio.ai.tools.bash_tool(project, go_env))
        }

        val skill_manager = com.jmwl.gostudio.ai.skills.ai_skill_manager(global_skills_dir, project_skills_dir)
        registry.register(com.jmwl.gostudio.ai.tools.create_skill_tool(skill_manager))
        val input_processor = com.jmwl.gostudio.ai.ai_input_processor(
            project_dir = project,
            skill_manager = skill_manager,
            global_prompts_dir = global_prompts_dir,
            project_prompts_dir = project_prompts_dir
        )
        val session_store = com.jmwl.gostudio.ai.ai_session_store(sessions_dir)
        val mcp_manager = com.jmwl.gostudio.ai.mcp.ai_mcp_manager(project)
        val file_change_notifier = com.jmwl.gostudio.ai.ai_file_change_notifier()
        val steering_queue = com.jmwl.gostudio.ai.ai_steering_queue()

        // 文件变更监听：AI 改文件后刷新编辑器
        ai_file_change_notifier = file_change_notifier
        file_change_notifier.set_listener { changed_paths -> refresh_files_after_ai_edit(changed_paths) }

        ai_agent = com.jmwl.gostudio.ai.ai_agent_loop(
            settings_provider = {
                // 会话级 override：切了提供商/模型则覆盖全局设置（含回填对应供应商的 key）
                // 用内存缓存版本：agent 每轮都会调用，避免重复 Keystore I/O（保存设置时会同步缓存）
                val base = com.jmwl.gostudio.ai.cached_ai_settings(this)
                _session_override?.let { (p, m) ->
                    base.copy(
                        provider = p,
                        model = m,
                        base_url = p.base_url.ifBlank { base.base_url },
                        api_key = base.api_keys[p] ?: base.api_key
                    )
                } ?: base
            },
            env_provider = {
                com.jmwl.gostudio.ai.ai_environment_context(
                    project_dir = project,
                    project_name = project.name,
                    current_file_path = state.current_file_path,
                    current_file_name = state.current_file_name,
                    cursor_line = state.cursor_line,
                    cursor_column = state.cursor_column,
                    go_mod_content = runCatching { java.io.File(project, "go.mod").takeIf { it.isFile }?.readText() }.getOrNull(),
                    project_tree_overview = runCatching { com.jmwl.gostudio.ai.collect_tree_overview(project) }.getOrNull()
                )
            },
            tool_registry = registry,
            scope_launcher = { block -> lifecycleScope.launch { block() } },
            input_processor = input_processor,
            session_store = session_store,
            session_id = project.name,
            skill_manager = skill_manager,
            mcp_manager = mcp_manager,
            file_change_notifier = file_change_notifier,
            steering_queue = steering_queue
        )

        // 异步初始化（恢复会话 + 发现 skill + 启动 MCP）
        lifecycleScope.launch { ai_agent?.initialize() }
    }

    /** AI 改文件后刷新编辑器对应 tab */
    private fun refresh_files_after_ai_edit(changed_paths: List<String>) {
        for (path in changed_paths) {
            val file = java.io.File(path)
            val tab = state.open_tabs.firstOrNull { java.io.File(it.file_path).absolutePath == file.absolutePath }
            if (tab != null) {
                runCatching {
                    val new_content = file.readText()
                    val cursor = tab.editor?.cursor
                    tab.document.replace(0, tab.document.length, new_content)
                    cursor?.let { /* 保持光标位置 */ }
                    tab.content = new_content
                    tab.has_changes = false
                }
            }
        }
        // 刷新文件树
        reload_file_tree()
    }

    private fun prewarm_textmate_languages() {
        if (textmate_prewarm_started) return
        textmate_prewarm_started = true
        lifecycleScope.launch(Dispatchers.Default) {
            listOf("prewarm.go", "prewarm.mod").forEach { file_name ->
                runCatching {
                    gostudio_application.instance.create_textmate_language(file_name)?.let { language ->
                        language.setCompleterKeywords(go_completion_keywords)
                        language.destroy()
                    }
                }
            }
        }
    }

    private fun import_editor_font_from_uri(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { import_editor_font(this@editor_activity, uri) }
            }

            result.onSuccess { path ->
                update_editor_settings(
                    state.editor_settings.copy(
                        font_family = "imported",
                        imported_font_path = path
                    )
                )
            }.onFailure { error ->
                app_toast.show(this@editor_activity, "字体导入失败: ${error.message.orEmpty()}", app_toast.LENGTH_LONG)
            }
        }
    }


    private fun request_open_file(file_path: String) {
        open_file(file_path)
    }

    private fun request_select_tab(file_path: String) {
        if (file_path == state.current_file_path) return

        val index = find_tab_index(file_path)
        if (index >= 0) {
            attach_editor_tab(index)
        }
    }

    private fun request_close_tab(file_path: String) {
        capture_active_tab_state()
        val index = find_tab_index(file_path)
        if (index < 0) return

        val tab = state.open_tabs[index]
        if (tab.pinned) return

        if (tab.has_changes) {
            attach_editor_tab(index, capture_current = false)
            state.pending_action = editor_pending_action.CloseTab(file_path)
            state.show_unsaved_dialog = true
            return
        }

        close_tab(file_path)
    }

    private fun request_close_other_tabs(keep_file_path: String) {
        capture_active_tab_state()
        val dirty_index = find_dirty_closable_tab_index(state.open_tabs, keep_file_path)
        if (dirty_index >= 0) {
            attach_editor_tab(dirty_index, capture_current = false)
            state.pending_action = editor_pending_action.CloseOtherTabs(keep_file_path)
            state.show_unsaved_dialog = true
            return
        }

        close_other_tabs(keep_file_path)
    }

    private fun request_close_all_tabs() {
        capture_active_tab_state()
        val dirty_index = find_dirty_closable_tab_index(state.open_tabs)
        if (dirty_index >= 0) {
            attach_editor_tab(dirty_index, capture_current = false)
            state.pending_action = editor_pending_action.CloseAllTabs
            state.show_unsaved_dialog = true
            return
        }

        close_all_tabs()
    }

    private fun request_close_editor() {
        state.show_exit_dialog = true
    }

    private fun confirm_close_editor() {
        state.show_exit_dialog = false
        close_editor_after_confirmation()
    }

    private fun close_editor_after_confirmation() {
        capture_active_tab_state()
        val dirty_index = find_dirty_tab_index(state.open_tabs)
        if (dirty_index >= 0) {
            attach_editor_tab(dirty_index, capture_current = false)
            state.pending_action = editor_pending_action.CloseEditor
            state.show_unsaved_dialog = true
            return
        }

        finish()
    }

    private fun request_save_file(show_toast: Boolean = true, on_saved: (() -> Unit)? = null) {
        lifecycleScope.launch {
            if (save_current_file(show_toast)) {
                on_saved?.invoke()
            }
        }
    }

    private fun format_current_file() {
        val file_path = state.current_file_path ?: return
        if (state.read_only) return
        if (is_go_file(file_path) && !state.editor_settings.gopls_formatting) {
            app_toast.show(this, "gopls 格式化已关闭", app_toast.LENGTH_SHORT)
            return
        }
        val cursor = editor.cursor
        val accepted = if (cursor.isSelected) {
            editor.formatCodeAsync(cursor.left(), cursor.right())
        } else {
            editor.formatCodeAsync()
        }
        if (!accepted) {
            app_toast.show(this, "当前语言暂不支持格式化", app_toast.LENGTH_SHORT)
        }
    }

    private fun handle_build_button_click() {
        if (output_panel_state.task_running) {
            if (!output_panel_state.task_stopping) {
                output_panel_state.task_stopping = true
                go_build_job?.cancel()
                go_run_job?.cancel()
            }
            return
        }
        when (detected_project_info.kind) {
            project_kind.GO -> build_go_project()
            else -> app_toast.show(this, "当前项目不是 Go 项目", app_toast.LENGTH_SHORT)
        }
    }

    /**
     * 构建当前 Go 项目：执行 `go build`（按项目配置拼接 -tags/-ldflags/-trimpath/-p），
     * 输出到项目根的 bin/ 目录。结果流式写入输出面板。
     */
    private fun build_go_project() {
        if (detected_project_info.kind != project_kind.GO) {
            app_toast.show(this, "当前项目不是 Go 项目", app_toast.LENGTH_SHORT)
            return
        }
        if (go_build_job?.isActive == true || go_run_job?.isActive == true) {
            app_toast.show(this, "任务正在运行中", app_toast.LENGTH_SHORT)
            return
        }

        val project_environment = toolchain_manager.project_environment(project_dir.absolutePath)
        if (project_environment.missing.isNotEmpty()) {
            val message = project_environment.missing.joinToString("；")
            app_toast.show(this, message, app_toast.LENGTH_LONG)
            output_panel_state.append_output("错误: $message", editor_output_line_level.ERROR)
            return
        }

        go_build_job = lifecycleScope.launch {
            output_panel_state.selected_tab = editor_output_tab.Output
            output_panel_state.clear_output()
            output_panel_state.task_running = true
            output_panel_state.task_stopping = false
            if (!save_dirty_open_files(show_toast = false)) {
                output_panel_state.append_output("构建取消，文件保存失败", editor_output_line_level.ERROR)
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
                return@launch
            }

            val success = try {
                val build = project_manager.read_project_build_config(project_dir.absolutePath)
                val bin_dir = File(project_dir, "bin")
                bin_dir.mkdirs()
                // working_dir 与 -o 用 host 路径：base_args 会把项目目录 bind 到 guest 同名路径，
                // 使 go build 进程能 cd 成功，且路径与文件系统一致。
                val out_flag = " -o ${shell_quote(File(bin_dir, project_dir.name).absolutePath)}"
                proot_manager.execute_command_with_environment(
                    command = build_go_build_command(build) + out_flag + " .",
                    working_dir = project_dir.absolutePath,
                    extra_environment = project_environment.environment,
                    on_log = { line -> output_panel_state.append_output(line, output_level_for_line(line)) }
                )
            } catch (_: CancellationException) {
                output_panel_state.append_output("构建已停止", editor_output_line_level.WARNING)
                return@launch
            } finally {
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
            }

            if (success) {
                output_panel_state.append_output("构建完成", editor_output_line_level.SUCCESS)
                app_toast.show(this@editor_activity, "构建完成", app_toast.LENGTH_SHORT)
            } else {
                output_panel_state.append_output("构建失败", editor_output_line_level.ERROR)
                app_toast.show(this@editor_activity, "构建失败", app_toast.LENGTH_LONG)
            }
        }
    }

    /**
     * 运行当前 Go 项目：打开独立终端页面，在 PTY 会话中执行 `go run .`，
     * 程序需要标准输入时可直接在终端里交互输入。
     */
    private fun run_go_project() {
        if (detected_project_info.kind != project_kind.GO) {
            app_toast.show(this, "当前项目不是 Go 项目", app_toast.LENGTH_SHORT)
            return
        }

        val project_environment = toolchain_manager.project_environment(project_dir.absolutePath)
        if (project_environment.missing.isNotEmpty()) {
            val message = project_environment.missing.joinToString("；")
            app_toast.show(this, message, app_toast.LENGTH_LONG)
            output_panel_state.append_output("错误: $message", editor_output_line_level.ERROR)
            return
        }

        go_run_job = lifecycleScope.launch {
            if (!save_dirty_open_files(show_toast = false)) {
                app_toast.show(this@editor_activity, "运行取消，文件保存失败", app_toast.LENGTH_SHORT)
                return@launch
            }

            val build = project_manager.read_project_build_config(project_dir.absolutePath)
            startActivity(
                Intent(this@editor_activity, terminal_activity::class.java)
                    .putExtra(terminal_activity.EXTRA_RUN_COMMAND, build_go_run_command(build) + " .")
                    .putExtra(terminal_activity.EXTRA_RUN_TITLE, "go run")
                    .putExtra(terminal_activity.EXTRA_RUN_WORKING_DIR, project_dir.absolutePath)
                    .putStringArrayListExtra(
                        terminal_activity.EXTRA_RUN_ENVIRONMENT,
                        ArrayList(project_environment.environment.map { (key, value) -> "$key=$value" })
                    )
            )
        }
    }

    /**
     * 拼接 `go build` 命令：根据项目配置加入 -tags / -ldflags / -trimpath / -p / 调试符号。
     */
    private fun build_go_build_command(build: project_build_config): String {
        val parts = mutableListOf("go build")
        if (build.build_tags.isNotBlank()) parts.add("-tags ${shell_quote(build.build_tags)}")
        if (build.ldflags.isNotBlank()) parts.add("-ldflags ${shell_quote(build.ldflags)}")
        if (build.trimpath) parts.add("-trimpath")
        if (build.parallel_jobs > 0) parts.add("-p ${build.parallel_jobs}")
        if (build.build_type == "Debug") parts.add("-gcflags=\"all=-N -l\"")
        return parts.joinToString(" ")
    }

    /**
     * 拼接 `go run` 命令：复用 -tags / -ldflags（运行态参数）。
     */
    private fun build_go_run_command(build: project_build_config): String {
        val parts = mutableListOf("go run")
        if (build.build_tags.isNotBlank()) parts.add("-tags ${shell_quote(build.build_tags)}")
        if (build.ldflags.isNotBlank()) parts.add("-ldflags ${shell_quote(build.ldflags)}")
        return parts.joinToString(" ")
    }

    /**
     * 运行当前项目的测试（`go test ./...`），输出流式写入输出面板。
     */
    private fun run_go_tests() {
        if (detected_project_info.kind != project_kind.GO) {
            app_toast.show(this, "当前项目不是 Go 项目", app_toast.LENGTH_SHORT)
            return
        }
        if (go_build_job?.isActive == true || go_run_job?.isActive == true) {
            app_toast.show(this, "任务正在运行中", app_toast.LENGTH_SHORT)
            return
        }
        val project_environment = toolchain_manager.project_environment(project_dir.absolutePath)
        if (project_environment.missing.isNotEmpty()) {
            val message = project_environment.missing.joinToString("；")
            app_toast.show(this, message, app_toast.LENGTH_LONG)
            output_panel_state.append_output("错误: $message", editor_output_line_level.ERROR)
            return
        }

        go_build_job = lifecycleScope.launch {
            output_panel_state.selected_tab = editor_output_tab.Output
            output_panel_state.clear_output()
            output_panel_state.task_running = true
            output_panel_state.task_stopping = false
            if (!save_dirty_open_files(show_toast = false)) {
                output_panel_state.append_output("测试取消，文件保存失败", editor_output_line_level.ERROR)
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
                return@launch
            }
            val success = try {
                proot_manager.execute_command_with_environment(
                    command = "go test ./...",
                    working_dir = project_dir.absolutePath,
                    extra_environment = project_environment.environment,
                    on_log = { line -> output_panel_state.append_output(line, output_level_for_line(line)) }
                )
            } catch (_: CancellationException) {
                output_panel_state.append_output("测试已停止", editor_output_line_level.WARNING)
                return@launch
            } finally {
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
            }

            if (success) {
                output_panel_state.append_output("测试通过", editor_output_line_level.SUCCESS)
                app_toast.show(this@editor_activity, "测试通过", app_toast.LENGTH_SHORT)
            } else {
                output_panel_state.append_output("测试失败", editor_output_line_level.ERROR)
                app_toast.show(this@editor_activity, "测试失败", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun output_level_for_line(line: String): editor_output_line_level {
        val text = line.lowercase()
        return when {
            "error" in text || "failed" in text || "panic:" in text -> editor_output_line_level.ERROR
            "warning" in text -> editor_output_line_level.WARNING
            else -> editor_output_line_level.NORMAL
        }
    }

    private fun shell_quote(value: String): String {
        if (value.isEmpty()) return "''"
        return "'" + value.replace("'", "'\\''") + "'"
    }


    private fun save_pending_action() {
        request_save_file(show_toast = false) {
            state.show_unsaved_dialog = false
            run_pending_action()
        }
    }

    private fun discard_pending_action() {
        active_tab()?.has_changes = false
        state.has_changes = false
        state.show_unsaved_dialog = false
        run_pending_action()
    }

    private fun run_pending_action() {
        val action = state.pending_action
        state.pending_action = null
        when (action) {
            is editor_pending_action.CloseTab -> request_close_tab(action.file_path)
            is editor_pending_action.CloseOtherTabs -> request_close_other_tabs(action.keep_file_path)
            editor_pending_action.CloseAllTabs -> request_close_all_tabs()
            editor_pending_action.CloseEditor -> close_editor_after_confirmation()
            null -> Unit
        }
    }

    private fun open_file(file_path: String) {
        capture_active_tab_state()
        val existing_index = find_tab_index(File(file_path).absolutePath)
        if (existing_index >= 0) {
            attach_editor_tab(existing_index, capture_current = false)
            return
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                load_project_file(project_dir, file_path)
            }

            result.onSuccess { loaded_file ->
                open_loaded_file_tab(loaded_file)
            }.onFailure { error ->
                app_toast.show(this@editor_activity, "打开失败: ${error.message}", app_toast.LENGTH_LONG)
            }
        }
    }

    /**
     * 跳转到光标所在标识符的定义。请求 gopls 的 textDocument/definition，
     * 拿到第一个 Location 后打开对应文件并定位。标准库等只读路径跳转失败时提示。
     */
    fun goto_definition() {
        if (_goto_in_progress.value) return
        val tab = active_tab() ?: return
        val file = File(tab.file_path)
        val project = gopls_project ?: run {
            app_toast.show(this, "gopls 未就绪", app_toast.LENGTH_SHORT)
            return
        }
        if (!is_go_file(file.absolutePath)) return
        val cursor_line = editor.cursor.leftLine
        val cursor_column = editor.cursor.leftColumn

        _goto_in_progress.value = true
        lifecycleScope.launch {
            try {
                val location = withContext(Dispatchers.IO) {
                    project.request_definition(file, editor, cursor_line, cursor_column)
                }
                if (location == null) {
                    app_toast.show(this@editor_activity, "未找到定义", app_toast.LENGTH_SHORT)
                } else {
                    open_file_at(location.file_path, location.line, location.column)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                app_toast.show(this@editor_activity, "跳转失败: ${e.message}", app_toast.LENGTH_SHORT)
            } finally {
                _goto_in_progress.value = false
            }
        }
    }

    private fun open_file_at(file_path: String, line: Int, column: Int) {
        val normalized_file_path = File(file_path).absolutePath
        val existing_index = find_tab_index(normalized_file_path)
        if (existing_index >= 0) {
            attach_editor_tab(existing_index)
            move_cursor_to(line, column)
            return
        }

        capture_active_tab_state()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                load_project_file(project_dir, normalized_file_path)
            }

            result.onSuccess { loaded_file ->
                open_loaded_file_tab(loaded_file)
                move_cursor_to(line, column)
            }.onFailure { error ->
                app_toast.show(this@editor_activity, "打开失败: ${error.message}", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun create_open_tab(loaded_file: editor_loaded_file): editor_open_tab {
        val file = loaded_file.file
        return editor_open_tab(
            initial_file_path = file.absolutePath,
            initial_file_name = file.name,
            initial_status_text = relative_project_path(project_dir, file),
            initial_content = loaded_file.content
        )
    }

    private suspend fun open_loaded_file_tab(loaded_file: editor_loaded_file): editor_open_tab {
        val tab = create_open_tab(loaded_file)
        prepare_tab_editor_for_display(tab)
        state.open_tabs.add(tab)
        reorder_tabs_keep_active()
        attach_editor_tab(find_tab_index(tab.file_path), capture_current = false)
        return tab
    }

    private fun move_cursor_to(line: Int, column: Int) {
        val line_count = editor.text.lineCount.coerceAtLeast(1)
        val safe_line = line.coerceIn(0, line_count - 1)
        val safe_column = column.coerceIn(0, editor.text.getColumnCount(safe_line))
        editor.setSelection(safe_line, safe_column)
        editor.ensurePositionVisible(safe_line, safe_column)
        state.cursor_line = safe_line + 1
        state.cursor_column = safe_column + 1
        state.cursor_selected = false
        active_tab()?.let { tab ->
            tab.cursor_line = safe_line
            tab.cursor_column = safe_column
        }
        editor.requestFocus()
    }

    private suspend fun restore_pinned_tabs() {
        val pinned_paths = load_pinned_tab_paths(this, project_dir)
        if (pinned_paths.isEmpty()) return

        state.loading = true
        val loaded_files = withContext(Dispatchers.IO) {
            load_pinned_project_files(project_dir, pinned_paths)
        }
        state.loading = false

        loaded_files.forEach { loaded_file ->
            val tab = create_open_tab(loaded_file).apply { pinned = true }
            if (find_tab_index(tab.file_path) < 0) {
                prepare_tab_editor(tab)
                state.open_tabs.add(tab)
            }
        }
        reorder_tabs_keep_active()
        if (state.open_tabs.isNotEmpty() && state.current_file_path == null) {
            attach_editor_tab(0, capture_current = false)
        }
        save_pinned_tabs()
    }

    private fun save_pinned_tabs() {
        save_pinned_tab_paths(
            context = this,
            project_dir = project_dir,
            paths = pinned_tab_paths(state.open_tabs)
        )
    }

    private suspend fun save_dirty_open_files(show_toast: Boolean): Boolean {
        capture_active_tab_state()
        val dirty_tabs = state.open_tabs.filter { tab -> tab.has_changes }
        if (dirty_tabs.isEmpty()) return true

        for (tab in dirty_tabs) {
            val content = tab.editor?.text?.toString() ?: tab.content
            val result = withContext(Dispatchers.IO) {
                save_project_file(tab.file_path, content)
            }
            result.onSuccess {
                tab.content = content
                tab.has_changes = false
                tab.status_text = relative_project_path(project_dir, File(tab.file_path))
            }.onFailure { error ->
                app_toast.show(this, "保存失败: ${error.message}", app_toast.LENGTH_LONG)
                return false
            }
        }

        active_tab()?.let { tab ->
            state.content = tab.content
            state.has_changes = tab.has_changes
            state.status_text = if (tab.has_changes) tab.status_text else relative_project_path(project_dir, File(tab.file_path))
        } ?: run {
            state.has_changes = false
        }
        update_history_state()
        refresh_file_tree()
        if (show_toast) {
            app_toast.show(this, "已保存所有文件", app_toast.LENGTH_SHORT)
        }
        return true
    }

    private suspend fun save_current_file(show_toast: Boolean): Boolean {
        val file_path = state.current_file_path
        if (file_path == null) {
            app_toast.show(this, "没有打开文件", app_toast.LENGTH_SHORT)
            return false
        }

        val content = editor.text.toString()
        val result = withContext(Dispatchers.IO) {
            save_project_file(file_path, content)
        }

        result.onSuccess {
            active_tab()?.let { tab ->
                tab.content = content
                tab.has_changes = false
                tab.status_text = relative_project_path(project_dir, File(file_path))
            }
            state.content = content
            state.has_changes = false
            state.status_text = "已保存 ${File(file_path).name}"
            update_history_state()
            refresh_file_tree()
            if (show_toast) {
                app_toast.show(this, "已保存", app_toast.LENGTH_SHORT)
            }
            on_project_file_saved(file_path)
        }.onFailure { error ->
            app_toast.show(this, "保存失败: ${error.message}", app_toast.LENGTH_LONG)
        }

        return result.isSuccess
    }

    private fun on_project_file_saved(file_path: String) {
        val saved_file = File(file_path)
        val go_mod_file = File(project_dir, "go.mod")
        // 保存 go.mod 后重启 gopls，让其重新加载模块依赖与构建约束
        if (saved_file.absolutePath == go_mod_file.absolutePath &&
            detected_project_info.kind == project_kind.GO &&
            go_build_job?.isActive != true
        ) {
            output_panel_state.append_log("go.mod 已保存，重启 gopls")
            reset_gopls_project()
        }
    }

    private fun toggle_pin_tab(file_path: String) {
        capture_active_tab_state()
        val tab = state.open_tabs.getOrNull(find_tab_index(file_path)) ?: return
        tab.pinned = !tab.pinned
        reorder_tabs_keep_active()
        save_pinned_tabs()
    }

    private fun close_tab(file_path: String) {
        val active_path = state.current_file_path
        val index = find_tab_index(file_path)
        if (index < 0 || state.open_tabs[index].pinned) return

        val closing_tab = state.open_tabs[index]
        val closing_file_path = closing_tab.file_path
        val closing_active_tab = closing_file_path == active_path
        if (state.open_tabs.size == 1) {
            state.open_tabs.removeAt(index)
            release_tab_editor(closing_tab)
            reset_editor_state()
            return
        }

        if (closing_active_tab) {
            val new_index_after_close = index.coerceAtMost(state.open_tabs.lastIndex - 1)
            val next_tab = state.open_tabs[if (index < state.open_tabs.lastIndex) index + 1 else index - 1]
            state.open_tabs.removeAt(index)
            activate_editor_tab(new_index_after_close, next_tab)
        } else {
            state.open_tabs.removeAt(index)
            state.selected_tab_index = find_tab_index(active_path)
        }
        release_tab_editor(closing_tab)
    }

    private fun close_other_tabs(keep_file_path: String) {
        val remaining_tabs = remaining_tabs_after_close_others(state.open_tabs, keep_file_path)
        if (remaining_tabs.isEmpty() || remaining_tabs.size == state.open_tabs.size) return

        val removed_tabs = state.open_tabs.filter { tab -> tab !in remaining_tabs }
        state.open_tabs.clear()
        state.open_tabs.addAll(remaining_tabs)
        removed_tabs.forEach { tab -> release_tab_editor(tab) }
        reorder_tabs_keep_active()
        val next_index = find_tab_index(keep_file_path).takeIf { it >= 0 } ?: 0
        attach_editor_tab(next_index, capture_current = false)
    }

    private fun close_all_tabs() {
        val active_path = state.current_file_path
        val pinned_tabs = pinned_tabs(state.open_tabs)
        val removed_tabs = state.open_tabs.filter { tab -> tab !in pinned_tabs }
        state.open_tabs.clear()
        state.open_tabs.addAll(pinned_tabs)
        removed_tabs.forEach { tab -> release_tab_editor(tab) }

        if (state.open_tabs.isEmpty()) {
            reset_editor_state()
            return
        }

        val next_index = find_tab_index(active_path).takeIf { it >= 0 } ?: 0
        attach_editor_tab(next_index, capture_current = false)
    }

    private fun reorder_tabs_keep_active() {
        val active_path = state.current_file_path
        val ordered_tabs = ordered_pinned_first_tabs(state.open_tabs)
        state.open_tabs.clear()
        state.open_tabs.addAll(ordered_tabs)
        state.selected_tab_index = find_tab_index(active_path)
    }

    private fun attach_editor_tab(index: Int, capture_current: Boolean = true) {
        if (index !in state.open_tabs.indices) return
        if (capture_current && state.selected_tab_index != index) {
            capture_active_tab_state()
        }

        activate_editor_tab(index, state.open_tabs[index])
    }

    private fun activate_editor_tab(index: Int, tab: editor_open_tab) {
        val tab_editor = tab.editor ?: prepare_tab_editor(tab)
        editor = tab_editor
        search_controller.set_editor(editor)
        state.selected_tab_index = index
        state.current_file_path = tab.file_path
        state.current_file_name = tab.file_name
        state.content = tab.content
        state.status_text = tab.status_text
        state.has_changes = tab.has_changes

        clear_editor_diagnostics()
        restore_editor_selection(tab)
        update_history_state()
        editor.requestFocus()
        schedule_block_end_hints_update()
        connect_gopls_if_needed(tab)
    }

    private fun disabled_gopls_features(settings: editor_settings_state): Set<LspFeature> {
        return buildSet {
            if (!settings.gopls_completion) add(LspFeature.Completion)
            if (!settings.gopls_signature_help) add(LspFeature.SignatureHelp)
            if (!settings.gopls_document_highlight) add(LspFeature.DocumentHighlight)
            if (!settings.gopls_formatting) add(LspFeature.Formatting)
            if (!settings.gopls_hover) add(LspFeature.Hover)
        }
    }

    private fun reset_gopls_project() {
        gopls_connect_job?.cancel()
        restore_editor_languages_from_lsp()
        gopls_project?.dispose()
        gopls_project = null
        active_tab()?.let { connect_gopls_if_needed(it) }
    }

    private fun restore_editor_languages_from_lsp() {
        open_editors().forEach { tab_editor ->
            val language = tab_editor.editorLanguage
            if (language is LspLanguage) {
                language.wrapperLanguage?.let { tab_editor.setEditorLanguage(it) }
            }
        }
    }

    private fun connect_gopls_if_needed(tab: editor_open_tab) {
        gopls_connect_job?.cancel()
        val file = File(tab.file_path)
        if (!is_go_file(file.absolutePath)) return
        if (!state.editor_settings.gopls_enabled) return
        if (!toolchain_manager.is_gopls_installed()) {
            log_gopls_skip_once(file, "gopls 未安装，请在「开发工具」中安装 Go 工具链")
            return
        }

        val project_environment = toolchain_manager.project_environment(project_dir.absolutePath)
        val disabled_features = disabled_gopls_features(state.editor_settings)
        // gopls 实际路径由探测结果决定（apt=/usr/bin/gopls，go install=/home/go/bin/gopls，手动=/usr/local/go/bin/gopls）
        val gopls_command = toolchain_manager.installed_go()?.gopls_proot_path ?: "/usr/bin/gopls"
        val lsp_project = gopls_project ?: gopls_lsp_project(
            project_dir = project_dir,
            disabled_features = disabled_features,
            config_factory = { working_dir ->
                gopls_lsp_config(
                    runtime_paths = toolchain_runtime_provider.paths(),
                    project_dir = project_dir,
                    path = project_environment.environment["PATH"] ?: toolchain_manager.proot_path(),
                    gopls_command = gopls_command,
                    extra_environment = project_environment.environment,
                    disabled_features = disabled_features,
                    on_stderr = on_stderr@{ line ->
                        val message = clean_gopls_log_line(line) ?: return@on_stderr
                        lifecycleScope.launch(Dispatchers.Main) { output_panel_state.append_log("gopls: $message") }
                    }
                )
            }
        ).also { gopls_project = it }

        gopls_connect_job = lifecycleScope.launch {
            val lsp_editor = lsp_project.get_or_create_editor(file, editor)
            lsp_editor.isEnableHover = state.editor_settings.gopls_hover
            lsp_editor.isEnableSignatureHelp = state.editor_settings.gopls_signature_help
            lsp_editor.eventListener = { _, new_status, old_status ->
                if (new_status != old_status) {
                    log_gopls_status(file, new_status)
                    if (new_status == LspEditorStatus.CONNECTED) {
                        // 连接完成后 gopls 很快会推送首批诊断，稍等再读容器
                        schedule_diagnostics_refresh(1500L)
                    }
                }
            }
            if (!lsp_editor.isConnected) {
                output_panel_state.append_log("gopls: 正在连接 ${file.name}")
                val connected = lsp_project.connect(file, editor)
                if (!connected) {
                    output_panel_state.append_log("gopls: 连接失败 ${file.name}", editor_output_line_level.ERROR)
                }
            }
        }
    }

    /** gopls stderr 日志清洗：去掉带级别前缀的行，保留有用信息。 */
    private fun clean_gopls_log_line(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        return line.replace(Regex("^[A-Z]\\[[^]]+]\\s*"), "")
    }

    private fun log_gopls_skip_once(file: File, reason: String) {
        if (gopls_skipped_files.add("${file.absolutePath}:$reason")) {
            output_panel_state.append_log("gopls: 跳过 ${file.name}，$reason", editor_output_line_level.WARNING)
        }
    }

    private fun log_gopls_status(file: File, status: LspEditorStatus) {
        val message = when (status) {
            LspEditorStatus.CONNECTED -> "gopls: 已连接 ${file.name}"
            LspEditorStatus.DISCONNECTED -> "gopls: 已断开 ${file.name}"
            else -> return
        }
        lifecycleScope.launch { output_panel_state.append_log(message) }
    }

    // ---- 代码结构（大纲） ----

    /** 编辑中防抖刷新结构符号（150ms）。 */
    private fun schedule_structure_refresh() {
        structure_job?.cancel()
        structure_job = lifecycleScope.launch {
            delay(150)
            refresh_structure_symbols()
        }
    }

    /**
     * 重算当前文件的大纲符号：主线程读文本，Default 线程解析。
     * gopls documentSymbol 接入前的正则兜底实现。
     */
    private fun refresh_structure_symbols() {
        val file_path = state.current_file_path
        if (file_path == null || !file_path.endsWith(".go")) {
            structure_symbols = emptyList()
            return
        }
        val file_name = File(file_path).name
        val text = editor.text.toString()
        structure_job?.cancel()
        structure_job = lifecycleScope.launch(Dispatchers.Default) {
            val parsed = com.jmwl.gostudio.editor.core.editor_outline_parser.parse_go_outline(text, file_name)
            withContext(Dispatchers.Main) { structure_symbols = parsed }
        }
    }

    /** 代码结构面板点击跳转：光标落到目标行并滚动可见。 */
    private fun navigate_to_structure_line(line: Int) {
        val line_count = editor.text.lineCount.coerceAtLeast(1)
        val target = line.coerceIn(0, line_count - 1)
        editor.setSelection(target, 0)
        runCatching { editor.ensureSelectionVisible() }
        state.cursor_line = target + 1
        state.cursor_column = 1
    }

    // ---- LSP 诊断（问题页签 / 诊断弹层） ----

    /** 编辑后防抖刷新诊断（gopls 推送有延迟，默认 800ms 后读容器）。 */
    private fun schedule_diagnostics_refresh(delay_ms: Long = 800L) {
        diagnostics_job?.cancel()
        diagnostics_job = lifecycleScope.launch {
            delay(delay_ms)
            refresh_file_diagnostics()
        }
    }

    private fun refresh_file_diagnostics() {
        val project = gopls_project
        val path = state.current_file_path
        if (project == null || path == null) {
            file_diagnostics = emptyList()
            return
        }
        file_diagnostics = project.current_diagnostics(File(path))
    }

    /** 打开诊断详情弹层：跳到该行并异步查询快速修复。 */
    private fun open_diagnostic_sheet(diagnostic: com.jmwl.gostudio.lsp.gopls.gopls_diagnostic) {
        sheet_diagnostic = diagnostic
        sheet_actions = emptyList()
        sheet_actions_loading = true
        navigate_to_structure_line(diagnostic.line)
        sheet_actions_job?.cancel()
        sheet_actions_job = lifecycleScope.launch {
            val project = gopls_project
            val path = state.current_file_path
            val actions = if (project != null && path != null) {
                runCatching {
                    project.request_code_actions(File(path), editor, diagnostic.line)
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            sheet_actions = actions
            sheet_actions_loading = false
        }
    }

    /** 应用快速修复后：关闭弹层并延时刷新诊断。 */
    private fun apply_diagnostic_action(action: com.jmwl.gostudio.lsp.gopls.gopls_code_action) {
        val project = gopls_project
        val path = state.current_file_path
        if (project != null && path != null) {
            val ok = runCatching { project.apply_code_action(File(path), action) }.getOrDefault(false)
            if (!ok) {
                app_toast.show(this, "修复应用失败", app_toast.LENGTH_SHORT)
            }
        }
        sheet_diagnostic = null
        schedule_diagnostics_refresh()
    }

    // ---- 全局搜索（proot grep） ----

    /** grep 输出行解析：./relative/path.go:行号:内容 */
    private val grep_hit_pattern = Regex("^(\\./[^:]+):(\\d+):(.*)$")

    /**
     * 在项目内全局搜索（proot `grep -rn --include=*.go`，排除 vendor/.git/bin，
     * 限 300 条命中）。空关键字返回空结果。
     */
    private suspend fun search_project(keyword: String): List<editor_project_search_hit> {
        val trimmed = keyword.trim()
        if (trimmed.length < 2 || !project_dir.isDirectory) return emptyList()
        val hits = mutableListOf<editor_project_search_hit>()
        val environment = toolchain_manager.project_environment(project_dir.absolutePath).environment
        val escaped = trimmed.replace("'", "'\\''")
        proot_manager.execute_command_with_environment(
            command = "grep -rn --include=\"*.go\" --exclude-dir=vendor --exclude-dir=.git --exclude-dir=bin -- '$escaped' . | head -n 300",
            working_dir = project_dir.absolutePath,
            extra_environment = environment,
            on_log = { line ->
                grep_hit_pattern.find(line.trim())?.let { match ->
                    val relative = match.groupValues[1].removePrefix("./")
                    val absolute = File(project_dir, relative).absolutePath
                    hits += editor_project_search_hit(
                        path = absolute,
                        relative_path = relative,
                        file_name = relative.substringAfterLast('/'),
                        line = match.groupValues[2].toIntOrNull() ?: return@let,
                        preview = match.groupValues[3].take(160)
                    )
                }
            }
        )
        return hits
    }

    private fun restore_editor_selection(tab: editor_open_tab) {
        val line_count = editor.text.lineCount.coerceAtLeast(1)
        val line = tab.cursor_line.coerceIn(0, line_count - 1)
        val column = tab.cursor_column.coerceIn(0, editor.text.getColumnCount(line))
        editor.setSelection(line, column)
        state.cursor_line = line + 1
        state.cursor_column = column + 1
        state.cursor_selected = false
    }

    private fun reset_editor_state() {
        state.toolbar_visible = true
        state.selected_tab_index = -1
        state.current_file_path = null
        current_textmate_scope = null
        state.current_file_name = "未打开文件"
        state.content = ""
        state.cursor_line = 1
        state.cursor_column = 1
        state.cursor_selected = false
        state.can_undo = false
        state.can_redo = false
        state.has_changes = false
        state.loading = false
        state.status_text = "请选择左侧文件"
        editor = create_code_editor()
        search_controller.set_editor(editor)
        clear_search()
    }

    private fun capture_active_tab_state() {
        val tab = active_tab() ?: return
        val content = editor.text.toString()
        tab.content = content
        state.content = content
        tab.cursor_line = editor.cursor.leftLine
        tab.cursor_column = editor.cursor.leftColumn
    }

    private fun active_tab(): editor_open_tab? {
        return state.open_tabs.getOrNull(state.selected_tab_index)
    }

    private fun release_tab_editor(tab: editor_open_tab) {
        gopls_project?.let { project ->
            val file = File(tab.file_path)
            lifecycleScope.launch(Dispatchers.IO) { project.close_file(file) }
        }
        tab_lifecycle.release(tab)
    }

    private fun find_tab_index(file_path: String?): Int {
        if (file_path == null) return -1
        return state.open_tabs.indexOfFirst { it.file_path == file_path }
    }

    private fun reload_file_tree(on_complete: (() -> Unit)? = null) {
        file_tree_job?.cancel()
        file_tree_job = lifecycleScope.launch {
            state.file_tree_loading = true
            val root = project_dir
            val root_path = root.absolutePath
            val root_loaded = root_path in file_tree_children_cache
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val exists = root.exists() && root.isDirectory
                    val root_children = if (exists && !root_loaded) {
                        load_file_tree_directory(root)
                    } else {
                        null
                    }
                    exists to root_children
                }
            }
            state.file_tree_loading = false
            result.onSuccess { (exists, root_children) ->
                if (root_children != null) file_tree_children_cache[root_path] = root_children
                state.project_exists = exists
                state.file_nodes = build_lazy_visible_file_nodes(root, state.expanded_paths, file_tree_children_cache)
            }.onFailure { error ->
                state.project_exists = false
                state.file_nodes = emptyList()
                app_toast.show(this@editor_activity, "刷新文件失败: ${error.message}", app_toast.LENGTH_LONG)
            }
            on_complete?.invoke()
        }
    }

    private fun toggle_directory(path: String) {
        val absolute_path = File(path).absolutePath
        if (absolute_path in state.expanded_paths) {
            state.expanded_paths = state.expanded_paths - absolute_path
            reload_file_tree()
            return
        }

        state.expanded_paths = state.expanded_paths + absolute_path
        load_file_tree_directory_if_needed(absolute_path)
    }

    private fun refresh_file_tree(directory_path: String? = null, on_complete: (() -> Unit)? = null) {
        if (!directory_path.isNullOrBlank()) {
            val absolute_path = File(directory_path).absolutePath
            state.expanded_paths = state.expanded_paths + absolute_path
            file_tree_children_cache.remove(absolute_path)
            load_file_tree_directory_if_needed(absolute_path, force = true, on_complete = on_complete)
        } else {
            file_tree_children_cache.remove(project_dir.absolutePath)
            reload_file_tree(on_complete)
        }
    }

    private fun load_file_tree_directory_if_needed(path: String, force: Boolean = false, on_complete: (() -> Unit)? = null) {
        if (!force && path in file_tree_children_cache) {
            state.file_nodes = build_lazy_visible_file_nodes(project_dir, state.expanded_paths, file_tree_children_cache)
            on_complete?.invoke()
            return
        }
        file_tree_job?.cancel()
        file_tree_job = lifecycleScope.launch {
            state.file_tree_loading = true
            val result = withContext(Dispatchers.IO) {
                runCatching { load_file_tree_directory(File(path)) }
            }
            state.file_tree_loading = false
            result.onSuccess { children ->
                file_tree_children_cache[path] = children
                state.file_nodes = build_lazy_visible_file_nodes(project_dir, state.expanded_paths, file_tree_children_cache)
            }.onFailure { error ->
                app_toast.show(this@editor_activity, "刷新文件失败: ${error.message}", app_toast.LENGTH_LONG)
                state.expanded_paths = state.expanded_paths - path
            }
        }
    }

    private fun create_project_file(parent_path: String, name: String) {
        create_project_entry(parent_path = parent_path, name = name, directory = false)
    }

    private fun create_project_folder(parent_path: String, name: String) {
        create_project_entry(parent_path = parent_path, name = name, directory = true)
    }

    private fun create_project_entry(parent_path: String, name: String, directory: Boolean) {
        lifecycleScope.launch {
            val result = project_manager.create_project_entry(
                project_path = project_dir.absolutePath,
                parent_path = parent_path,
                name = name,
                directory = directory
            )

            result.onSuccess { target ->
                val parent_path = target.parentFile?.absolutePath
                if (parent_path != null) {
                    state.expanded_paths = state.expanded_paths + parent_path
                }
                if (directory) {
                    state.expanded_paths = state.expanded_paths + target.absolutePath
                } else {
                    app_toast.show(this@editor_activity, "已创建文件", app_toast.LENGTH_SHORT)
                    request_open_file(target.absolutePath)
                }
                refresh_file_tree(parent_path)
            }.onFailure { error ->
                app_toast.show(this@editor_activity, "创建失败: ${error.message.orEmpty()}", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun rename_project_entry(path: String, new_name: String) {
        lifecycleScope.launch {
            val result = project_manager.rename_project_entry(
                project_path = project_dir.absolutePath,
                path = path,
                new_name = new_name
            )

            result.onSuccess { (old_path, new_path) ->
                sync_tabs_after_rename(old_path, new_path)
                sync_file_tree_cache_after_rename(old_path, new_path)
                state.expanded_paths = state.expanded_paths
                    .map { replace_path_prefix(it, old_path, new_path) }
                    .toSet()
                save_pinned_tabs()
                refresh_file_tree(File(new_path).parentFile?.absolutePath)
            }.onFailure { error ->
                app_toast.show(this@editor_activity, "重命名失败: ${error.message.orEmpty()}", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun delete_project_entry(path: String) {
        lifecycleScope.launch {
            val source = project_manager.resolve_project_entry_for_delete(project_dir.absolutePath, path).getOrElse { error ->
                app_toast.show(this@editor_activity, "删除失败: ${error.message.orEmpty()}", app_toast.LENGTH_LONG)
                return@launch
            }
            val source_path = source.absolutePath
            val dirty_tab = state.open_tabs.firstOrNull { tab ->
                is_same_or_child_path(tab.file_path, source_path) && tab.has_changes
            }
            if (dirty_tab != null) {
                app_toast.show(this@editor_activity, "请先保存或关闭未保存文件: ${dirty_tab.file_name}", app_toast.LENGTH_LONG)
                return@launch
            }

            val result = project_manager.delete_project_entry(project_dir.absolutePath, source_path)
            result.onSuccess { (deleted_path, parent_path) ->
                remove_tabs_for_deleted_entry(deleted_path)
                remove_file_tree_cache_for_path(deleted_path)
                state.expanded_paths = state.expanded_paths
                    .filterNot { is_same_or_child_path(it, deleted_path) }
                    .toSet()
                save_pinned_tabs()
                refresh_file_tree(parent_path)
            }.onFailure { error ->
                app_toast.show(this@editor_activity, "删除失败: ${error.message.orEmpty()}", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun sync_file_tree_cache_after_rename(old_path: String, new_path: String) {
        val updated_cache = file_tree_children_cache.mapKeys { (path, _) -> replace_path_prefix(path, old_path, new_path) }
        file_tree_children_cache.clear()
        file_tree_children_cache.putAll(updated_cache)
        remove_file_tree_cache_for_path(new_path)
        file_tree_children_cache.remove(File(new_path).parentFile?.absolutePath)
    }

    private fun remove_file_tree_cache_for_path(path: String) {
        file_tree_children_cache.keys
            .filter { is_same_or_child_path(it, path) }
            .forEach { file_tree_children_cache.remove(it) }
    }

    private fun sync_tabs_after_rename(old_path: String, new_path: String) {
        state.open_tabs.forEach { tab ->
            if (is_same_or_child_path(tab.file_path, old_path)) {
                val updated_path = replace_path_prefix(tab.file_path, old_path, new_path)
                tab.file_path = updated_path
                tab.file_name = File(updated_path).name
                tab.status_text = relative_project_path(project_dir, File(updated_path))
            }
        }

        val current_path = state.current_file_path
        if (current_path != null && is_same_or_child_path(current_path, old_path)) {
            val updated_path = replace_path_prefix(current_path, old_path, new_path)
            state.current_file_path = updated_path
            state.current_file_name = File(updated_path).name
            state.status_text = relative_project_path(project_dir, File(updated_path))
            state.selected_tab_index = find_tab_index(updated_path)
            apply_textmate_language(updated_path)
        }
    }

    private fun remove_tabs_for_deleted_entry(deleted_path: String) {
        val active_path = state.current_file_path
        val selected_index = state.selected_tab_index
        val affected_tabs = state.open_tabs.filter { tab -> is_same_or_child_path(tab.file_path, deleted_path) }
        if (affected_tabs.isEmpty()) return

        state.open_tabs.removeAll(affected_tabs.toSet())
        affected_tabs.forEach { tab -> release_tab_editor(tab) }
        if (state.open_tabs.isEmpty()) {
            reset_editor_state()
            return
        }

        if (active_path != null && !is_same_or_child_path(active_path, deleted_path)) {
            val active_index = find_tab_index(active_path)
            if (active_index >= 0) {
                state.selected_tab_index = active_index
                return
            }
        }
        attach_editor_tab(selected_index.coerceIn(0, state.open_tabs.lastIndex), capture_current = false)
    }

    private fun toggle_read_only() {
        state.read_only = !state.read_only
    }

    private fun undo() {
        if (state.current_file_path != null) {
            editor.undo()
            update_history_state()
        }
    }

    private fun redo() {
        if (state.current_file_path != null) {
            editor.redo()
            update_history_state()
        }
    }

    private fun current_line_comment_action(): Boolean? {
        return current_line_comment_state()?.should_uncomment
    }

    private fun current_line_comment_state(): editor_line_comment_state? {
        if (state.current_file_path == null) return null

        val cursor = editor.cursor
        val text = editor.text
        val line_count = text.lineCount
        if (line_count <= 0) return null

        var start_line = cursor.leftLine.coerceIn(0, line_count - 1)
        var end_line = cursor.rightLine.coerceIn(0, line_count - 1)
        if (start_line > end_line) {
            val tmp = start_line
            start_line = end_line
            end_line = tmp
        }

        if (cursor.isSelected && cursor.rightColumn == 0 && end_line > start_line) {
            end_line--
        }

        val target_lines = (start_line..end_line).map { line -> line to text.getLineString(line) }
        return create_line_comment_state(target_lines)
    }

    private fun toggle_line_comment() {
        if (state.read_only || state.current_file_path == null) return

        val comment_state = current_line_comment_state() ?: return
        val text = editor.text

        text.beginBatchEdit()
        try {
            comment_state.lines.asReversed().forEach { (line, line_text) ->
                if (line_text.isBlank()) return@forEach

                val indent_length = line_comment_indent_length(line_text)
                if (comment_state.should_uncomment) {
                    if (!is_toggleable_line_comment(line_text)) return@forEach

                    val after_indent = line_text.substring(indent_length)
                    when {
                        after_indent.startsWith("// ") -> text.delete(line, indent_length, line, indent_length + 3)
                        after_indent.startsWith("//") -> text.delete(line, indent_length, line, indent_length + 2)
                    }
                } else if (!is_line_commented(line_text)) {
                    text.insert(line, indent_length, "// ")
                }
            }
        } finally {
            text.endBatchEdit()
        }

        update_history_state()
        editor.invalidate()
    }

    private fun insert_symbol(symbol: String) {
        if (state.read_only || state.current_file_path == null) return

        val commit_text = resolve_editor_symbol_commit_text(symbol)
        if (commit_text == "\t") {
            editor.indentOrCommitTab()
        } else {
            editor.commitText(commit_text, false, true)
        }
        update_history_state()
    }

    private fun update_search(
        query: String,
        match_case: Boolean,
        whole_word: Boolean,
        regex: Boolean
    ): Boolean {
        val result = search_controller.update_search(query, match_case, whole_word, regex)
        result.status_text?.let { status -> state.status_text = status }
        return result.has_match
    }

    private fun goto_search_result(forward: Boolean) {
        search_controller.goto_search_result(forward)?.let { status ->
            state.status_text = status
        }
    }

    private fun replace_current_match(replacement: String) {
        val result = search_controller.replace_current_match(replacement)
        if (result.changed) update_history_state()
        result.status_text?.let { status -> state.status_text = status }
    }

    private fun replace_all_matches(replacement: String) {
        val result = search_controller.replace_all_matches(replacement)
        if (result.changed) update_history_state()
        result.status_text?.let { status -> state.status_text = status }
    }

    private fun clear_search() {
        search_controller.clear_search()
    }


    private suspend fun prepare_tab_editor_for_display(tab: editor_open_tab): CodeEditor {
        return tab_lifecycle.prepare_for_display(tab)
    }

    private fun prepare_tab_editor(tab: editor_open_tab): CodeEditor {
        return tab_lifecycle.prepare(tab)
    }

    private fun set_editor_document(content: Content) {
        tab_lifecycle.set_content(editor, content)
        schedule_block_end_hints_update()
    }

    private fun set_editor_content(content: String) {
        tab_lifecycle.set_content(editor, content)
        schedule_block_end_hints_update()
    }

    private fun schedule_block_end_hints_update() {
        block_hint_job?.cancel()
        if (!::editor.isInitialized || !state.editor_settings.block_end_hints || state.current_file_path == null || !is_go_file(state.current_file_path)) {
            if (::editor.isInitialized) editor.inlayHints = null
            return
        }

        val file_path = state.current_file_path
        val tab_editor = editor
        block_hint_job = lifecycleScope.launch {
            delay(block_end_hint_update_delay_ms)
            val hints = build_editor_block_end_hints(tab_editor) ?: return@launch
            if (file_path == state.current_file_path && editor === tab_editor) {
                editor.inlayHints = hints
            }
        }
    }

    private fun apply_textmate_language(file_path: String) {
        val scope_name = gostudio_application.instance.get_language_scope_name(file_path)
        val current_textmate_language = current_textmate_language()

        if (current_textmate_scope == scope_name && current_textmate_language != null) {
            apply_textmate_language_settings(current_textmate_language, state.editor_settings, file_path)
            editor.setEditorLanguage(current_textmate_language)
            apply_current_editor_behavior_settings(editor, state.editor_settings)
            return
        }

        val language = create_configured_textmate_language(file_path)
        editor.setEditorLanguage(language)

        current_textmate_scope = scope_name
        apply_current_editor_behavior_settings(editor, state.editor_settings)
    }

    private fun current_textmate_language(target: CodeEditor = editor): TextMateLanguage? {
        return when (val language = target.editorLanguage) {
            is TextMateLanguage -> language
            is LspLanguage -> language.wrapperLanguage as? TextMateLanguage
            else -> null
        }
    }

    private fun create_configured_textmate_language(file_path: String): TextMateLanguage {
        return (gostudio_application.instance.create_textmate_language(file_path)
            ?: TextMateLanguage.create("source.go", false)).also { language ->
            apply_textmate_language_settings(language, state.editor_settings, file_path)
        }
    }

    private fun clear_editor_diagnostics() {
        if (::editor.isInitialized) {
            editor.diagnostics = null
        }
    }

    private fun update_history_state() {
        state.can_undo = state.current_file_path != null && editor.isUndoEnabled && editor.canUndo()
        state.can_redo = state.current_file_path != null && editor.isUndoEnabled && editor.canRedo()
    }

    private companion object {
        private const val block_end_hint_update_delay_ms = 180L
        private const val initial_editor_styles_timeout_ms = 800L
    }

}
