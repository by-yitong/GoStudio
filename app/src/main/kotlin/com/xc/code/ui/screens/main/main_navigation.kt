package com.xc.code.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xc.code.ui.dialogs.common.install_progress_dialog
import com.xc.code.ui.dialogs.main.new_project_dialog
import com.xc.code.ui.dialogs.main.open_project_dialog
import com.xc.code.ui.dialogs.main.toolchain_custom_install_dialog
import com.xc.code.ui.screens.editor.editor_settings_screen
import com.xc.code.ui.screens.editor.editor_theme_settings_screen
import com.xc.code.ui.theme.app_theme_provider
import com.xc.code.ui.theme.app_theme_type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun main_navigation(
    recent_projects: List<recent_project>,
    ndk_versions: List<String>,
    cmake_versions: List<String>,
    toolchain_status: main_tools_install_status,
    current_theme: app_theme_type,
    scale_value: Float,
    toolchain_tasks: List<toolchain_trigger>,
    custom_toolchain_dialog: toolchain_custom_install_request?,
    on_back_to_background: () -> Unit,
    on_terminal: () -> Unit,
    on_project_click: (recent_project) -> Unit,
    on_project_remove: (recent_project) -> Unit,
    on_create_project: (String, String, String, String, String, String, String) -> Unit,
    on_open_project: (String) -> Unit,
    on_toolchain_trigger_change: (toolchain_trigger?) -> Unit,
    on_custom_toolchain_dialog_change: (toolchain_custom_install_request?) -> Unit,
    on_theme_change: (app_theme_type) -> Unit,
    on_scale_change: (Float) -> Unit,
    on_run_toolchain_task: suspend (toolchain_trigger, (String) -> Unit, (Int) -> Unit) -> Boolean,
    on_toolchain_task_success: (toolchain_trigger) -> Unit
) {
    val colors = app_theme_provider.colors
    val nav_controller = rememberNavController()
    val current_back_stack by nav_controller.currentBackStackEntryAsState()
    var show_new_project_dialog by remember { mutableStateOf(false) }
    var show_open_project_dialog by remember { mutableStateOf(false) }
    val active_toolchain_trigger = toolchain_tasks.firstOrNull()
    var toolchain_dialog_visible by remember(active_toolchain_trigger) { mutableStateOf(true) }

    val gradient_brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to colors.gradient_start,
            0.10f to colors.gradient_middle,
            0.20f to colors.gradient_end
        )
    )

    BackHandler(enabled = true) {
        if (current_back_stack?.destination?.route != "main") {
            nav_controller.popBackStack()
        } else {
            on_back_to_background()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient_brush)
    ) {
        NavHost(
            navController = nav_controller,
            startDestination = "main",
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally() },
            exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally() },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally() },
            popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally() }
        ) {
            composable("main") {
                main_screen(
                    on_new_project = { show_new_project_dialog = true },
                    on_open_project = { show_open_project_dialog = true },
                    recent_projects = recent_projects,
                    on_tools = { nav_controller.navigate("tools") },
                    on_plugins = { nav_controller.navigate("plugins") },
                    on_settings = { nav_controller.navigate("settings") },
                    on_terminal = on_terminal,
                    on_ai = { nav_controller.navigate("agent") },
                    on_project_click = on_project_click,
                    on_project_remove = on_project_remove
                )
            }

            composable("tools") {
                main_tools_screen(
                    on_back = { nav_controller.popBackStack() },
                    on_install_cmake = { version, source, sha256 ->
                        on_toolchain_trigger_change(
                            toolchain_trigger(
                                title = "安装 CMake $version",
                                action = toolchain_action.INSTALL_CMAKE,
                                source = source,
                                version = version,
                                sha256 = sha256
                            )
                        )
                    },
                    on_custom_install_cmake = {
                        on_custom_toolchain_dialog_change(
                            toolchain_custom_install_request("自定义安装 CMake") { path ->
                                on_toolchain_trigger_change(
                                    toolchain_trigger(
                                        title = "自定义安装 CMake",
                                        action = toolchain_action.INSTALL_CMAKE_ARCHIVE,
                                        source = path
                                    )
                                )
                            }
                        )
                    },
                    on_uninstall_cmake = { version ->
                        on_toolchain_trigger_change(
                            toolchain_trigger(
                                title = "卸载 CMake $version",
                                action = toolchain_action.UNINSTALL_CMAKE,
                                version = version
                            )
                        )
                    },
                    on_install_ndk = { version, source, sha256 ->
                        on_toolchain_trigger_change(
                            toolchain_trigger(
                                title = "安装 NDK $version",
                                action = toolchain_action.INSTALL_NDK_URL,
                                source = source,
                                version = version,
                                sha256 = sha256
                            )
                        )
                    },
                    on_custom_install_ndk = {
                        on_custom_toolchain_dialog_change(
                            toolchain_custom_install_request("自定义安装 NDK") { path ->
                                on_toolchain_trigger_change(
                                    toolchain_trigger(
                                        title = "自定义安装 NDK",
                                        action = toolchain_action.INSTALL_NDK_ARCHIVE,
                                        source = path
                                    )
                                )
                            }
                        )
                    },
                    on_uninstall_ndk = { version ->
                        on_toolchain_trigger_change(
                            toolchain_trigger(
                                title = "卸载 NDK $version",
                                action = toolchain_action.UNINSTALL_NDK,
                                version = version
                            )
                        )
                    },
                    install_status = toolchain_status
                )
            }

            composable("plugins") { placeholder_screen("插件") { nav_controller.popBackStack() } }
            composable("settings") {
                main_settings_screen(
                    on_back = { nav_controller.popBackStack() },
                    on_theme_click = { nav_controller.navigate("theme_settings") },
                    on_editor_click = { nav_controller.navigate("editor_settings") },
                    on_about_click = { nav_controller.navigate("about") }
                )
            }
            composable("about") {
                main_about_screen(
                    on_back = { nav_controller.popBackStack() }
                )
            }
            composable("theme_settings") {
                main_theme_settings_screen(
                    current_theme = current_theme,
                    scale_value = scale_value,
                    on_theme_change = on_theme_change,
                    on_scale_change = on_scale_change,
                    on_back = { nav_controller.popBackStack() }
                )
            }
            composable("editor_settings") {
                editor_settings_screen(
                    on_back = { nav_controller.popBackStack() },
                    on_theme_click = { nav_controller.navigate("editor_theme_settings") }
                )
            }
            composable("editor_theme_settings") { editor_theme_settings_screen(on_back = { nav_controller.popBackStack() }) }
            composable("agent") { placeholder_screen("智能") { nav_controller.popBackStack() } }
        }

        if (active_toolchain_trigger != null && !toolchain_dialog_visible) {
            Button(
                onClick = { toolchain_dialog_visible = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .wrapContentSize(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.card_bg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = colors.card_text_title, modifier = Modifier.size(16.dp))
                    Text(text = if (toolchain_tasks.size > 1) "后台任务 ${toolchain_tasks.size}" else "后台任务", color = colors.card_text_title)
                }
            }
        }
    }

    if (show_new_project_dialog) {
        new_project_dialog(
            ndk_versions = ndk_versions,
            cmake_versions = cmake_versions,
            on_dismiss = { show_new_project_dialog = false },
            on_create = { project_name, project_path, template_id, ndk_version, cmake_version, android_platform, cpp_standard ->
                show_new_project_dialog = false
                on_create_project(project_name, project_path, template_id, ndk_version, cmake_version, android_platform, cpp_standard)
            }
        )
    }

    if (show_open_project_dialog) {
        open_project_dialog(
            on_dismiss = { show_open_project_dialog = false },
            on_open = { project_path ->
                show_open_project_dialog = false
                on_open_project(project_path)
            }
        )
    }

    custom_toolchain_dialog?.let { request ->
        toolchain_custom_install_dialog(
            title = request.title,
            on_dismiss = { on_custom_toolchain_dialog_change(null) },
            on_install = { archive_path ->
                on_custom_toolchain_dialog_change(null)
                request.on_install(archive_path)
            }
        )
    }

    active_toolchain_trigger?.let { trigger ->
        install_progress_dialog(
            title = trigger.title,
            task = { on_log, on_progress -> on_run_toolchain_task(trigger, on_log, on_progress) },
            on_dismiss = {
                toolchain_dialog_visible = true
                on_toolchain_trigger_change(null)
            },
            on_success = {
                toolchain_dialog_visible = true
                on_toolchain_task_success(trigger)
            },
            on_minimize = { toolchain_dialog_visible = false },
            visible = toolchain_dialog_visible
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun placeholder_screen(title: String, on_back: () -> Unit) {
    val colors = app_theme_provider.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = colors.title_large) },
                navigationIcon = {
                    IconButton(onClick = on_back) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = colors.top_button_icon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding_values ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding_values),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$title 页面开发中...", color = colors.subtitle)
        }
    }
}
