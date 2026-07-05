package com.xc.code.activity

import com.xc.code.editor.session.editor_activity_state
import com.xc.code.editor.session.editor_open_tab
import com.xc.code.editor.session.editor_pending_action
import com.xc.code.editor.tabs.find_dirty_closable_tab_index
import com.xc.code.editor.tabs.find_dirty_tab_index
import com.xc.code.editor.tabs.ordered_pinned_first_tabs
import com.xc.code.editor.tabs.pinned_tab_paths
import com.xc.code.editor.tabs.pinned_tabs
import com.xc.code.editor.tabs.remaining_tabs_after_close_others
import com.xc.code.editor.settings.*
import com.xc.code.editor.model.*
import com.xc.code.editor.core.*

import android.net.Uri
import android.os.Bundle
import com.xc.code.ui.toast.app_toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.xc.code.core.logging.logger_manager
import com.xc.code.toolchain.proot_manager
import com.xc.code.toolchain.toolchain_manager
import com.xc.code.toolchain.toolchain_runtime_provider
import com.xc.code.project.detected_project
import com.xc.code.project.project_detector
import com.xc.code.project.project_ide_config
import com.xc.code.project.project_kind
import com.xc.code.project.project_manager
import com.xc.code.lsp.clangd.clangd_lsp_config
import com.xc.code.lsp.clangd.clangd_lsp_project
import com.xc.code.editor.theme.editor_theme_manager
import com.xc.code.ui.dialogs.editor.editor_exit_confirm_dialog
import com.xc.code.ui.dialogs.editor.editor_unsaved_file_dialog
import com.xc.code.ui.screens.editor.*
import com.xc.code.ui.theme.app_theme_provider
import com.xc.code.xc_application
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import io.github.rosemoe.sora.lsp.editor.LspLanguage
import io.github.rosemoe.sora.lsp.editor.LspEditorStatus
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var cmake_configure_job: Job? = null
    private var cmake_build_job: Job? = null
    private var file_tree_job: Job? = null
    private var textmate_prewarm_started = false
    private var clangd_project: clangd_lsp_project? = null
    private var clangd_connect_job: Job? = null
    private val clangd_skipped_files = mutableSetOf<String>()
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

    override fun onDestroy() {
        block_hint_job?.cancel()
        cmake_configure_job?.cancel()
        cmake_build_job?.cancel()
        file_tree_job?.cancel()
        clangd_connect_job?.cancel()
        clangd_project?.dispose()
        clangd_project = null
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
            project_kind.CMAKE -> {
                detected_project_info.build_dir?.let { path ->
                    output_panel_state.append_log("Build dir: $path")
                }
                detected_project_info.build_file_path?.let { path ->
                    output_panel_state.append_log("CMakeLists: $path")
                }
                detected_project_info.compile_commands_path?.let { path ->
                    output_panel_state.append_log("compile_commands: $path")
                }
            }
            project_kind.UNKNOWN -> {
                output_panel_state.append_log("未识别到 CMake 项目", editor_output_line_level.WARNING)
            }
        }
    }

    @Composable
    private fun editor_activity_content() {
        val colors = app_theme_provider.colors

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

        editor_screen(
            project_name = state.project_name,
            project_root_path = project_dir.absolutePath,
            editor = editor,
            current_file_name = state.current_file_name,
            cursor_line = state.cursor_line,
            cursor_column = state.cursor_column,
            cursor_selected = state.cursor_selected,
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
            editor_settings = state.editor_settings,
            output_panel_state = output_panel_state,
            terminal_cwd = project_dir.takeIf { it.isDirectory }?.absolutePath
                ?: toolchain_runtime_provider.paths().home_dir.absolutePath,
            terminal_extra_environment = if (project_dir.isDirectory) {
                toolchain_manager.project_environment(project_dir.absolutePath).environment
            } else {
                emptyMap()
            },
            on_toggle_toolbar = { state.toolbar_visible = !state.toolbar_visible },
            on_editor_settings_change = { settings -> update_editor_settings(settings) },
            on_project_config_apply = { config, on_saved -> apply_project_config(config, on_saved) },
            on_import_editor_font = { request_import_editor_font() },
            on_select_tab = { path -> request_select_tab(path) },
            on_pin_tab = { path -> toggle_pin_tab(path) },
            on_close_tab = { path -> request_close_tab(path) },
            on_close_other_tabs = { path -> request_close_other_tabs(path) },
            on_close_all_tabs = { request_close_all_tabs() },
            on_build = { handle_build_button_click() },
            on_configure_cmake = { configure_cmake_project(clean_build = true, show_toast = true) },
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
            on_file_position_click = { path, line, column -> open_file_at(path, line, column) }
        )

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
            initial_styles_timeout_ms = initial_editor_styles_timeout_ms
        )
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
    }

    private fun handle_editor_selection_changed(changed_editor: CodeEditor) {
        val line = changed_editor.cursor.leftLine
        val column = changed_editor.cursor.leftColumn
        state.cursor_line = line + 1
        state.cursor_column = column + 1
        state.cursor_selected = changed_editor.cursor.isSelected
        active_tab()?.let { tab ->
            tab.cursor_line = line
            tab.cursor_column = column
        }
    }

    private fun update_editor_settings(settings: editor_settings_state) {
        apply_editor_settings(settings)
    }

    private fun apply_editor_settings(settings: editor_settings_state) {
        val clangd_settings_changed = state.editor_settings.clangd_enabled != settings.clangd_enabled ||
            state.editor_settings.clangd_completion != settings.clangd_completion ||
            state.editor_settings.clangd_signature_help != settings.clangd_signature_help ||
            state.editor_settings.clangd_document_highlight != settings.clangd_document_highlight ||
            state.editor_settings.clangd_formatting != settings.clangd_formatting ||
            state.editor_settings.clangd_hover != settings.clangd_hover
        state.editor_settings = settings
        save_editor_settings(this, settings)
        if (clangd_settings_changed) {
            reset_clangd_project()
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
                configure_cmake_project(clean_build = true, show_toast = true)
            }
            .onFailure { error ->
                app_toast.show(this, "项目配置保存失败: ${error.message}", app_toast.LENGTH_LONG)
            }
    }

    private fun initialize_project() {
        reload_file_tree {
            lifecycleScope.launch {
                if (!state.project_exists) {
                    state.status_text = "项目不存在"
                    return@launch
                }

                prewarm_textmate_languages()
                restore_pinned_tabs()
                configure_cmake_project_if_needed()
            }
        }
    }

    private fun prewarm_textmate_languages() {
        if (textmate_prewarm_started) return
        textmate_prewarm_started = true
        lifecycleScope.launch(Dispatchers.Default) {
            listOf("prewarm.c", "prewarm.cpp").forEach { file_name ->
                runCatching {
                    xc_application.instance.create_textmate_language(file_name)?.let { language ->
                        language.setCompleterKeywords(c_cpp_completion_keywords)
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
        if (is_c_family_file(file_path) && !state.editor_settings.clangd_formatting) {
            app_toast.show(this, "clangd 格式化已关闭", app_toast.LENGTH_SHORT)
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
                output_panel_state.task_subtitle = "正在停止任务"
                cmake_configure_job?.cancel()
                cmake_build_job?.cancel()
            }
            return
        }
        build_cmake_project()
    }

    private fun build_cmake_project() {
        if (detected_project_info.kind != project_kind.CMAKE) {
            app_toast.show(this, "当前项目不是 CMake 项目", app_toast.LENGTH_SHORT)
            return
        }
        if (cmake_configure_job?.isActive == true || cmake_build_job?.isActive == true) {
            app_toast.show(this, "任务正在运行中", app_toast.LENGTH_SHORT)
            return
        }

        val build_dir = detected_project_info.build_dir ?: File(project_dir, "build").absolutePath
        val project_environment = toolchain_manager.project_environment(project_dir.absolutePath)
        if (project_environment.missing.isNotEmpty()) {
            val message = project_environment.missing.joinToString("；")
            app_toast.show(this, message, app_toast.LENGTH_LONG)
            output_panel_state.append_output("错误: $message", editor_output_line_level.ERROR)
            return
        }
        val ndk = project_environment.ndk
        val cmake_toolchain_file = ndk?.cmake_toolchain_file
        if (ndk == null || cmake_toolchain_file.isNullOrBlank()) {
            app_toast.show(this, "项目未配置可用 NDK", app_toast.LENGTH_LONG)
            output_panel_state.append_output("错误: 项目未配置可用 NDK", editor_output_line_level.ERROR)
            return
        }

        cmake_build_job = lifecycleScope.launch {
            output_panel_state.selected_tab = editor_output_tab.Output
            output_panel_state.clear_output()
            output_panel_state.task_title = "构建输出"
            output_panel_state.task_subtitle = "正在保存文件"
            output_panel_state.task_running = true
            output_panel_state.task_stopping = false
            if (!save_dirty_open_files(show_toast = false)) {
                output_panel_state.append_output("构建取消，文件保存失败", editor_output_line_level.ERROR)
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
                return@launch
            }
            output_panel_state.task_subtitle = "正在构建项目"

            val success = try {
                if (!reset_cmake_build_dir(build_dir)) {
                    output_panel_state.append_output("构建失败，无法清理 build 目录", editor_output_line_level.ERROR)
                    false
                } else {
                    val android_config = project_cmake_config(project_dir)
                    val configure_command = create_cmake_configure_command(
                        source_dir = project_dir.absolutePath,
                        build_dir = build_dir,
                        cmake_toolchain_file = cmake_toolchain_file,
                        existing_generator = null,
                        android_config = android_config
                    )
                    val configure_success = proot_manager.execute_command_with_environment(
                        command = configure_command,
                        working_dir = project_dir.absolutePath,
                        extra_environment = project_environment.environment,
                        on_log = { line -> output_panel_state.append_output(line, output_level_for_cmake_line(line)) }
                    )
                    if (!configure_success) {
                        false
                    } else {
                        val parallel_arg = android_config.parallel_jobs.takeIf { it > 0 }?.let { " $it" }.orEmpty()
                        val build_command = "cmake --build ${shell_quote(build_dir)} --parallel$parallel_arg"
                        proot_manager.execute_command_with_environment(
                            command = build_command,
                            working_dir = project_dir.absolutePath,
                            extra_environment = project_environment.environment,
                            on_log = { line -> output_panel_state.append_output(line, output_level_for_cmake_line(line)) }
                        )
                    }
                }
            } catch (_: CancellationException) {
                output_panel_state.append_output("构建已停止", editor_output_line_level.WARNING)
                return@launch
            } finally {
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
            }

            if (success) {
                detected_project_info = project_detector.detect_project(project_dir.absolutePath)
                output_panel_state.append_output("构建完成", editor_output_line_level.SUCCESS)
                app_toast.show(this@editor_activity, "构建完成", app_toast.LENGTH_SHORT)
            } else {
                output_panel_state.append_output("构建失败", editor_output_line_level.ERROR)
                app_toast.show(this@editor_activity, "构建失败", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun configure_cmake_project() {
        configure_cmake_project(clean_build = true, show_toast = true)
    }

    private fun configure_cmake_project_if_needed() {
        configure_cmake_project(clean_build = true, show_toast = false)
    }

    private fun configure_cmake_project(clean_build: Boolean, show_toast: Boolean, on_success: (() -> Unit)? = null) {
        if (detected_project_info.kind != project_kind.CMAKE) {
            if (show_toast) app_toast.show(this, "当前项目不是 CMake 项目", app_toast.LENGTH_SHORT)
            return
        }

        val build_dir = detected_project_info.build_dir ?: File(project_dir, "build").absolutePath
        val compile_commands_path = File(build_dir, "compile_commands.json").absolutePath

        val project_environment = toolchain_manager.project_environment(project_dir.absolutePath)
        if (project_environment.missing.isNotEmpty()) {
            val message = project_environment.missing.joinToString("；")
            if (show_toast) app_toast.show(this, message, app_toast.LENGTH_LONG)
            output_panel_state.append_output("错误: $message", editor_output_line_level.ERROR)
            return
        }
        val ndk = project_environment.ndk
        val cmake_toolchain_file = ndk?.cmake_toolchain_file
        if (ndk == null || cmake_toolchain_file.isNullOrBlank()) {
            if (show_toast) app_toast.show(this, "项目未配置可用 NDK", app_toast.LENGTH_LONG)
            output_panel_state.append_output("错误: 项目未配置可用 NDK", editor_output_line_level.ERROR)
            return
        }

        if (cmake_configure_job?.isActive == true) {
            if (show_toast) app_toast.show(this, "CMake 正在配置中", app_toast.LENGTH_SHORT)
            return
        }

        cmake_configure_job = lifecycleScope.launch {
            output_panel_state.selected_tab = editor_output_tab.Output
            output_panel_state.clear_output()
            output_panel_state.task_title = "构建输出"
            output_panel_state.task_subtitle = "正在保存文件"
            output_panel_state.task_running = true
            output_panel_state.task_stopping = false
            if (!save_dirty_open_files(show_toast = false)) {
                output_panel_state.append_output("CMake 初始化取消，文件保存失败", editor_output_line_level.ERROR)
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
                return@launch
            }
            output_panel_state.task_subtitle = if (show_toast) "正在配置 CMake" else "正在初始化 CMake"
            val android_config = project_cmake_config(project_dir)

            if (clean_build && !reset_cmake_build_dir(build_dir)) {
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
                output_panel_state.append_output("CMake 配置失败，无法清理 build 目录", editor_output_line_level.ERROR)
                if (show_toast) app_toast.show(this@editor_activity, "无法清理 build 目录", app_toast.LENGTH_LONG)
                return@launch
            }

            val command = create_cmake_configure_command(
                source_dir = project_dir.absolutePath,
                build_dir = build_dir,
                cmake_toolchain_file = cmake_toolchain_file,
                existing_generator = if (clean_build) null else read_existing_cmake_generator(build_dir),
                android_config = android_config
            )
            val success = try {
                proot_manager.execute_command_with_environment(
                    command = command,
                    working_dir = project_dir.absolutePath,
                    extra_environment = project_environment.environment,
                    on_log = { line -> output_panel_state.append_output(line, output_level_for_cmake_line(line)) }
                )
            } catch (_: CancellationException) {
                output_panel_state.append_output("CMake 初始化已暂停", editor_output_line_level.WARNING)
                return@launch
            } finally {
                output_panel_state.task_running = false
                output_panel_state.task_stopping = false
            }

            if (success && File(compile_commands_path).isFile) {
                detected_project_info = project_detector.detect_project(project_dir.absolutePath)
                output_panel_state.append_output("CMake 配置完成", editor_output_line_level.SUCCESS)
                if (show_toast) app_toast.show(this@editor_activity, "CMake 配置完成", app_toast.LENGTH_SHORT)
                on_success?.invoke()
            } else {
                output_panel_state.append_output("CMake 配置失败，未生成 compile_commands.json", editor_output_line_level.ERROR)
                if (show_toast) app_toast.show(this@editor_activity, "CMake 配置失败", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun create_cmake_configure_command(
        source_dir: String,
        build_dir: String,
        cmake_toolchain_file: String,
        existing_generator: String?,
        android_config: cmake_android_config
    ): String {
        val command = mutableListOf(
            "cmake",
            "-S", shell_quote(source_dir),
            "-B", shell_quote(build_dir)
        )
        if (existing_generator.isNullOrBlank()) {
            command += listOf("-G", shell_quote("Ninja"))
        }
        command += listOf(
            "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
            "-DCMAKE_TOOLCHAIN_FILE=${shell_quote(cmake_toolchain_file)}"
        )
        android_config.abi?.let { command += "-DANDROID_ABI=${shell_quote(it)}" }
        android_config.platform?.let { command += "-DANDROID_PLATFORM=${shell_quote(it)}" }
        android_config.cpp_standard?.let {
            command += "-DCMAKE_CXX_STANDARD=${shell_quote(it)}"
            command += "-DCMAKE_CXX_STANDARD_REQUIRED=ON"
            command += "-DCMAKE_CXX_EXTENSIONS=OFF"
        }
        android_config.build_type?.let { command += "-DCMAKE_BUILD_TYPE=${shell_quote(it)}" }
        val extra_args = enabled_cmake_args(android_config.extra_cmake_args)
        if (extra_args.isNotBlank()) {
            command += extra_args
        }
        return command.joinToString(" ")
    }

    private fun project_cmake_config(project_dir: File): cmake_android_config {
        val inferred = infer_cmake_android_config(project_dir)
        val build = project_manager.read_project_build_config(project_dir.absolutePath)
        val abi = build.abi.ifBlank { inferred.abi.orEmpty() }.takeIf { it.isNotBlank() }
        val platform = build.platform.ifBlank { inferred.platform.orEmpty() }.takeIf { it.isNotBlank() }
        return cmake_android_config(
            abi = abi,
            platform = platform,
            cpp_standard = build.cpp_standard,
            build_type = build.build_type,
            parallel_jobs = build.parallel_jobs,
            extra_cmake_args = build.extra_cmake_args
        )
    }

    private fun infer_cmake_android_config(project_dir: File): cmake_android_config {
        val cmake_file = File(project_dir, "CMakeLists.txt")
        if (!cmake_file.isFile) return cmake_android_config()
        val content = cmake_file.readText()
        return cmake_android_config(
            abi = infer_cmake_android_abi(content),
            platform = infer_cmake_android_platform(content)
        )
    }

    private fun infer_cmake_android_abi(content: String): String? {
        if ("CMAKE_ANDROID_ARCH_ABI" !in content && "ANDROID_ABI" !in content) return null
        val explicit_abi = Regex("set\\s*\\(\\s*ANDROID_ABI\\s+([^\\s)]+)", RegexOption.IGNORE_CASE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it in setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86") }
        if (explicit_abi != null) return explicit_abi
        return Regex("ANDROID_ABI\\s+STREQUAL\\s+\"(arm64-v8a|armeabi-v7a|x86_64|x86)\"", RegexOption.IGNORE_CASE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun infer_cmake_android_platform(content: String): String? {
        val explicit_platform = if ("ANDROID_PLATFORM" in content || "CMAKE_SYSTEM_VERSION" in content) {
            Regex("(?<![A-Za-z0-9_-])(?:android-)?(?:2[1-9]|3[0-9]|4[0-9])(?![A-Za-z0-9_-])")
                .findAll(content)
                .map { it.value.let { value -> if (value.startsWith("android-")) value else "android-$value" } }
                .toSet()
                .singleOrNull()
        } else {
            null
        }
        if (explicit_platform != null) return explicit_platform
        return when {
            uses_android_vulkan(content) -> "android-24"
            else -> null
        }
    }

    private fun uses_android_vulkan(content: String): Boolean {
        return Regex("find_library\\s*\\([^)]*\\bvulkan\\b", RegexOption.IGNORE_CASE).containsMatchIn(content) ||
            Regex("target_link_libraries\\s*\\([^)]*\\bvulkan\\b", RegexOption.IGNORE_CASE).containsMatchIn(content)
    }

    private fun enabled_cmake_args(value: String): String {
        return value.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .joinToString(" ")
    }

    private data class cmake_android_config(
        val abi: String? = null,
        val platform: String? = null,
        val cpp_standard: String? = null,
        val build_type: String? = null,
        val parallel_jobs: Int = 0,
        val extra_cmake_args: String = ""
    )

    private fun read_existing_cmake_generator(build_dir: String): String? {
        val cache_file = File(build_dir, "CMakeCache.txt")
        if (!cache_file.isFile) return null
        return cache_file.useLines { lines ->
            lines.firstOrNull { it.startsWith("CMAKE_GENERATOR:INTERNAL=") }
                ?.substringAfter('=')
                ?.takeIf { it.isNotBlank() }
        }
    }

    private fun reset_cmake_build_dir(build_dir: String): Boolean {
        val root = project_dir.canonicalFile
        val target = File(build_dir).canonicalFile
        if (target == root || target.parentFile?.canonicalFile != root || target.name != "build") return false
        if (!target.exists()) return true
        return target.deleteRecursively()
    }

    private fun output_level_for_cmake_line(line: String): editor_output_line_level {
        val text = line.lowercase()
        return when {
            "error" in text || "failed" in text -> editor_output_line_level.ERROR
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
            configure_cmake_after_cmakelists_save(file_path)
        }.onFailure { error ->
            app_toast.show(this, "保存失败: ${error.message}", app_toast.LENGTH_LONG)
        }

        return result.isSuccess
    }

    private fun configure_cmake_after_cmakelists_save(file_path: String) {
        val saved_file = File(file_path)
        val root_cmake_file = File(project_dir, "CMakeLists.txt")
        if (saved_file.absolutePath != root_cmake_file.absolutePath) return
        if (detected_project_info.kind != project_kind.CMAKE) return
        if (cmake_configure_job?.isActive == true || cmake_build_job?.isActive == true) return

        output_panel_state.append_log("CMakeLists.txt 已保存，自动初始化 CMake")
        configure_cmake_project(clean_build = false, show_toast = false) {
            reset_clangd_project()
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
        connect_clangd_if_needed(tab)
    }

    private fun disabled_clangd_features(settings: editor_settings_state): Set<LspFeature> {
        return buildSet {
            if (!settings.clangd_completion) add(LspFeature.Completion)
            if (!settings.clangd_signature_help) add(LspFeature.SignatureHelp)
            if (!settings.clangd_document_highlight) add(LspFeature.DocumentHighlight)
            if (!settings.clangd_formatting) add(LspFeature.Formatting)
            if (!settings.clangd_hover) add(LspFeature.Hover)
        }
    }

    private fun reset_clangd_project() {
        clangd_connect_job?.cancel()
        restore_editor_languages_from_clangd()
        clangd_project?.dispose()
        clangd_project = null
        active_tab()?.let { connect_clangd_if_needed(it) }
    }

    private fun restore_editor_languages_from_clangd() {
        open_editors().forEach { tab_editor ->
            val language = tab_editor.editorLanguage
            if (language is LspLanguage) {
                language.wrapperLanguage?.let { tab_editor.setEditorLanguage(it) }
            }
        }
    }

    private fun connect_clangd_if_needed(tab: editor_open_tab) {
        clangd_connect_job?.cancel()
        val file = File(tab.file_path)
        if (!is_c_family_file(file.absolutePath)) return
        if (!state.editor_settings.clangd_enabled) return
        val build_dir = File(detected_project_info.build_dir ?: File(project_dir, "build").absolutePath)
        val compile_commands = File(build_dir, "compile_commands.json")
        if (!compile_commands.isFile) {
            log_clangd_skip_once(file, "缺少 compile_commands.json，请先执行 CMake 初始化")
            return
        }

        val project_environment = toolchain_manager.project_environment(project_dir.absolutePath)
        val ndk = project_environment.ndk ?: run {
            log_clangd_skip_once(file, "项目未配置可用 NDK")
            return
        }
        val disabled_features = disabled_clangd_features(state.editor_settings)
        val lsp_project = clangd_project ?: clangd_lsp_project(
            project_dir = project_dir,
            disabled_features = disabled_features,
            config_factory = {
                clangd_lsp_config(
                    runtime_paths = toolchain_runtime_provider.paths(),
                    project_dir = project_dir,
                    build_dir = build_dir,
                    path = project_environment.environment["PATH"] ?: toolchain_manager.proot_path(),
                    ndk_llvm_bin_proot_dir = ndk.llvm_bin_proot_dir,
                    extra_environment = project_environment.environment,
                    disabled_features = disabled_features,
                    on_stderr = on_stderr@{ line ->
                        val message = clean_clangd_log_line(line) ?: return@on_stderr
                        lifecycleScope.launch(Dispatchers.Main) { output_panel_state.append_log("clangd: $message") }
                    }
                )
            }
        ).also { clangd_project = it }

        clangd_connect_job = lifecycleScope.launch {
            val lsp_editor = lsp_project.get_or_create_editor(file, editor)
            lsp_editor.isEnableHover = state.editor_settings.clangd_hover
            lsp_editor.isEnableSignatureHelp = state.editor_settings.clangd_signature_help
            lsp_editor.eventListener = { _, new_status, old_status ->
                if (new_status != old_status) {
                    log_clangd_status(file, new_status)
                }
            }
            if (!lsp_editor.isConnected) {
                output_panel_state.append_log("clangd: 正在连接 ${file.name}")
                val connected = lsp_project.connect(file, editor)
                if (!connected) {
                    output_panel_state.append_log("clangd: 连接失败 ${file.name}", editor_output_line_level.ERROR)
                }
            }
        }
    }

    private fun clean_clangd_log_line(line: String): String? {
        if (line.matches(Regex("^[I]\\[[^]]+].*"))) return null
        val trimmed = line.trimStart()
        if (trimmed.startsWith(configured_ndk_clang_prefix())) return null
        if (trimmed.startsWith("/home/xcode/ndk/") && " -- " in trimmed) return null
        return line.replace(Regex("^[A-Z]\\[[^]]+]\\s*"), "")
    }

    private fun configured_ndk_clang_prefix(): String {
        val ndk = toolchain_manager.project_environment(project_dir.absolutePath).ndk ?: return "/home/xcode/ndk/"
        return ndk.llvm_bin_proot_dir.trimEnd('/') + "/clang"
    }

    private fun log_clangd_skip_once(file: File, reason: String) {
        if (clangd_skipped_files.add("${file.absolutePath}:$reason")) {
            output_panel_state.append_log("clangd: 跳过 ${file.name}，$reason", editor_output_line_level.WARNING)
        }
    }

    private fun log_clangd_status(file: File, status: LspEditorStatus) {
        val message = when (status) {
            LspEditorStatus.CONNECTED -> "clangd: 已连接 ${file.name}"
            LspEditorStatus.DISCONNECTED -> "clangd: 已断开 ${file.name}"
            else -> return
        }
        lifecycleScope.launch(Dispatchers.Main) { output_panel_state.append_log(message) }
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
        clangd_project?.let { project ->
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
        if (!::editor.isInitialized || !state.editor_settings.block_end_hints || state.current_file_path == null || !is_c_family_file(state.current_file_path)) {
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
        val scope_name = xc_application.instance.get_language_scope_name(file_path)
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
        return (xc_application.instance.create_textmate_language(file_path)
            ?: TextMateLanguage.create("source.cpp", false)).also { language ->
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
