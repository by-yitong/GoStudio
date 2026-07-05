package com.xc.code.activity

import android.content.Intent
import android.os.Bundle
import com.xc.code.ui.toast.app_toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.xc.code.project.project_manager
import com.xc.code.project.recent_project_info
import com.xc.code.toolchain.install_cmake_from_archive
import com.xc.code.toolchain.install_cmake_from_url
import com.xc.code.toolchain.install_ndk_from_archive
import com.xc.code.toolchain.install_ndk_from_url
import com.xc.code.toolchain.toolchain_manager
import com.xc.code.toolchain.uninstall_cmake_tool
import com.xc.code.toolchain.uninstall_ndk_tool
import com.xc.code.ui.screens.main.main_navigation
import com.xc.code.ui.screens.main.main_tools_install_status
import com.xc.code.ui.screens.main.recent_project
import com.xc.code.ui.screens.main.toolchain_action
import com.xc.code.ui.screens.main.toolchain_custom_install_request
import com.xc.code.ui.screens.main.toolchain_trigger
import com.xc.code.ui.theme.app_theme_provider
import com.xc.code.ui.theme.app_theme_type
import com.xc.code.ui.theme.theme_manager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class main_activity : ComponentActivity() {
    private var recent_projects by mutableStateOf<List<recent_project>>(emptyList())
    private var toolchain_status by mutableStateOf(main_tools_install_status())
    private var ndk_versions by mutableStateOf(emptyList<String>())
    private var cmake_versions by mutableStateOf(emptyList<String>())
    private var current_theme by mutableStateOf(app_theme_type.SYSTEM)
    private var scale_value by mutableStateOf(1f)
    private var toolchain_tasks by mutableStateOf<List<toolchain_trigger>>(emptyList())
    private var custom_toolchain_dialog by mutableStateOf<toolchain_custom_install_request?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            app_theme_provider {
                main_navigation(
                    recent_projects = recent_projects,
                    ndk_versions = ndk_versions,
                    cmake_versions = cmake_versions,
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
        val snapshot = withContext(Dispatchers.IO) {
            val installed_ndks = toolchain_manager.available_ndk_versions()
            val installed_cmakes = toolchain_manager.available_cmake_versions()
            val status = main_tools_install_status(
                cmake_installed = toolchain_manager.is_cmake_installed(),
                ndk_installed = toolchain_manager.is_ndk_installed(),
                installed_cmake_versions = installed_cmakes.toSet(),
                installed_ndk_versions = toolchain_manager.installed_ndk_version_keys()
            )
            Triple(installed_ndks, installed_cmakes, status)
        }
        ndk_versions = snapshot.first
        cmake_versions = snapshot.second
        toolchain_status = snapshot.third
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
        project_path: String,
        template_id: String,
        ndk_version: String,
        cmake_version: String,
        android_platform: String,
        cpp_standard: String
    ) {
        lifecycleScope.launch {
            val result = project_manager.create_project(
                name = project_name,
                path = project_path,
                template_id = template_id,
                ndk_version = ndk_version,
                cmake_version = cmake_version,
                android_platform = android_platform,
                cpp_standard = cpp_standard
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
            toolchain_action.INSTALL_CMAKE -> install_cmake_from_url(trigger.version, trigger.source, trigger.sha256, on_log, on_progress)
            toolchain_action.INSTALL_CMAKE_ARCHIVE -> install_cmake_from_archive(trigger.source, on_log, on_progress)
            toolchain_action.UNINSTALL_CMAKE -> uninstall_cmake_tool(trigger.version, on_log, on_progress)
            toolchain_action.INSTALL_NDK_URL -> install_ndk_from_url(trigger.version, trigger.source, trigger.sha256, on_log, on_progress)
            toolchain_action.INSTALL_NDK_ARCHIVE -> install_ndk_from_archive(trigger.source, on_log, on_progress)
            toolchain_action.UNINSTALL_NDK -> uninstall_ndk_tool(trigger.version, on_log, on_progress)
        }
    }

    private fun on_toolchain_task_success(trigger: toolchain_trigger) {
        toolchain_tasks = toolchain_tasks.drop(1)
        app_toast.show(this, "${trigger.title} 完成", app_toast.LENGTH_SHORT)
        lifecycleScope.launch { refresh_toolchain_status() }
    }

    private fun recent_project_info.to_ui_recent_project(): recent_project {
        return recent_project(
            name = name,
            path = path,
            cmake_version = cmake_version,
            ndk_version = ndk_version,
            last_opened = last_opened
        )
    }
}
