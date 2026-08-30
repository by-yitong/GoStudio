package com.jmwl.gostudio.activity

import android.content.Intent
import android.os.Bundle
import com.jmwl.gostudio.ui.toast.app_toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.jmwl.gostudio.project.project_manager
import com.jmwl.gostudio.project.recent_project_info
import com.jmwl.gostudio.toolchain.install_garble
import com.jmwl.gostudio.toolchain.install_gcc
import com.jmwl.gostudio.toolchain.install_git
import com.jmwl.gostudio.toolchain.install_go
import com.jmwl.gostudio.toolchain.install_gopls
import com.jmwl.gostudio.toolchain.install_go_toolchain
import com.jmwl.gostudio.toolchain.toolchain_manager
import com.jmwl.gostudio.ui.screens.main.main_navigation
import com.jmwl.gostudio.ui.screens.main.main_tools_install_status
import com.jmwl.gostudio.ui.screens.main.recent_project
import com.jmwl.gostudio.ui.screens.main.toolchain_action
import com.jmwl.gostudio.ui.screens.main.toolchain_custom_install_request
import com.jmwl.gostudio.ui.screens.main.toolchain_trigger
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.theme.app_theme_type
import com.jmwl.gostudio.ui.theme.theme_manager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class main_activity : ComponentActivity() {
    private var recent_projects by mutableStateOf<List<recent_project>>(emptyList())
    private var toolchain_status by mutableStateOf(main_tools_install_status())
    private var current_theme by mutableStateOf(app_theme_type.SYSTEM)
    private var scale_value by mutableStateOf(1f)
    private var toolchain_tasks by mutableStateOf<List<toolchain_trigger>>(emptyList())
    private var custom_toolchain_dialog by mutableStateOf<toolchain_custom_install_request?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            app_theme_provider {
                // 启动后静默检查更新（仅发现新版本时弹窗）
                val context = androidx.compose.ui.platform.LocalContext.current
                val update_controller = androidx.compose.runtime.remember {
                    com.jmwl.gostudio.update.app_update_controller(context)
                }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2500)
                    update_controller.check()
                }
                com.jmwl.gostudio.ui.dialogs.main.app_update_dialog(
                    controller = update_controller,
                    on_dismiss = { update_controller.reset() }
                )

                main_navigation(
                    recent_projects = recent_projects,
                    toolchain_status = toolchain_status,
                    current_theme = current_theme,
                    scale_value = scale_value,
                    toolchain_tasks = toolchain_tasks,
                    custom_toolchain_dialog = custom_toolchain_dialog,
                    on_back_to_background = { moveTaskToBack(true) },
                    on_terminal = ::open_terminal,
                    on_project_click = ::open_recent_project,
                    on_project_remove = ::remove_recent_project,
                    on_create_project = ::create_project,
                    on_open_project = ::open_project_path,
                    on_toolchain_trigger_change = { trigger ->
                        toolchain_tasks = if (trigger != null) {
                            toolchain_tasks + trigger
                        } else {
                            toolchain_tasks.drop(1)
                        }
                    },
                    on_custom_toolchain_dialog_change = { custom_toolchain_dialog = it },
                    on_theme_change = ::set_theme,
                    on_scale_change = ::set_scale,
                    on_run_toolchain_task = ::run_toolchain_task,
                    on_toolchain_task_success = ::on_toolchain_task_success
                )
            }
        }

        load_initial_data()
    }

    private fun load_initial_data() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                toolchain_manager.cleanup_removed_toolchain_environment()
            }
            reload_recent_projects()
            refresh_toolchain_status()
            current_theme = theme_manager.theme.value
            scale_value = theme_manager.scale.value
        }
    }

    private suspend fun reload_recent_projects() {
        recent_projects = project_manager.get_recent_projects().map { it.to_ui_recent_project() }
    }

    private suspend fun refresh_toolchain_status() {
        toolchain_status = withContext(Dispatchers.IO) {
            val go = toolchain_manager.installed_go()
            main_tools_install_status(
                go_installed = go != null,
                gopls_installed = toolchain_manager.is_gopls_installed(),
                git_installed = toolchain_manager.is_git_installed(),
                installed_go_version = go?.version.orEmpty()
            )
        }
    }

    private fun open_terminal() {
        startActivity(Intent(this, terminal_activity::class.java))
    }

    private fun set_theme(theme: app_theme_type) {
        theme_manager.set_theme(this, theme)
        current_theme = theme
    }

    private fun set_scale(scale: Float) {
        theme_manager.set_scale(this, scale)
        scale_value = scale
    }

    private fun open_recent_project(project: recent_project) {
        lifecycleScope.launch {
            val project_dir = File(project.path)
            if (!project_dir.exists() || !project_dir.isDirectory) {
                project_manager.remove_recent_project(project.path)
                reload_recent_projects()
                app_toast.show(this@main_activity, "项目不存在，已从最近项目移除", app_toast.LENGTH_LONG)
                return@launch
            }

            project_manager.ensure_project_config(project.path)
            val result = project_manager.add_recent_project(project.path)
            result.onSuccess { info ->
                reload_recent_projects()
                open_editor(info.name, info.path)
            }.onFailure { error ->
                app_toast.show(this@main_activity, "打开失败: ${error.message}", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun remove_recent_project(project: recent_project) {
        lifecycleScope.launch {
            project_manager.remove_recent_project(project.path)
            reload_recent_projects()
            app_toast.show(this@main_activity, "已从最近项目移除", app_toast.LENGTH_SHORT)
        }
    }

    private fun create_project(
        project_name: String,
        template_id: String
    ) {
        lifecycleScope.launch {
            val result = project_manager.create_project(
                name = project_name,
                template_id = template_id
            )
            result.onSuccess { project_dir ->
                project_manager.add_recent_project(project_dir.absolutePath)
                reload_recent_projects()
                app_toast.show(this@main_activity, "项目已创建: ${project_dir.name}", app_toast.LENGTH_SHORT)
                open_editor(project_dir.name, project_dir.absolutePath)
            }.onFailure { error ->
                app_toast.show(this@main_activity, "创建失败: ${error.message}", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun open_project_path(project_path: String) {
        lifecycleScope.launch {
            project_manager.ensure_project_config(project_path)
            val result = project_manager.add_recent_project(project_path)
            result.onSuccess { project ->
                reload_recent_projects()
                open_editor(project.name, project.path)
            }.onFailure { error ->
                app_toast.show(this@main_activity, "打开失败: ${error.message}", app_toast.LENGTH_LONG)
            }
        }
    }

    private fun open_editor(project_name: String, project_path: String) {
        project_manager.ensure_project_clang_format(project_path)
        val intent = Intent(this, editor_activity::class.java).apply {
            putExtra("project_name", project_name)
            putExtra("project_path", project_path)
        }
        startActivity(intent)
    }

    private suspend fun run_toolchain_task(
        trigger: toolchain_trigger,
        on_log: (String) -> Unit,
        on_progress: (Int) -> Unit
    ): Boolean {
        return when (trigger.action) {
            toolchain_action.INSTALL_GO -> install_go_toolchain(on_log, on_progress)
            toolchain_action.INSTALL_GOPLS -> install_gopls(on_log, on_progress)
            toolchain_action.INSTALL_GIT -> install_git(on_log, on_progress)
            toolchain_action.INSTALL_GCC -> install_gcc(on_log, on_progress)
            toolchain_action.INSTALL_GARBLE -> install_garble(on_log, on_progress)
        }
    }

    private fun on_toolchain_task_success(trigger: toolchain_trigger) {
        toolchain_tasks = toolchain_tasks.drop(1)
        // 工具链已变化，让 installed_go() 探测缓存失效（编辑器重组热路径依赖它）
        toolchain_manager.invalidate_go_probe()
        app_toast.show(this, "${trigger.title} 完成", app_toast.LENGTH_SHORT)
        lifecycleScope.launch { refresh_toolchain_status() }
    }

    private fun recent_project_info.to_ui_recent_project(): recent_project {
        return recent_project(
            name = name,
            path = path,
            go_version = go_version,
            last_opened = last_opened
        )
    }
}
