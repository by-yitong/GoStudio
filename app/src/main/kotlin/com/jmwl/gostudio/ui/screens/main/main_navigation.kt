package com.jmwl.gostudio.ui.screens.main

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.jmwl.gostudio.ai.ai_environment_context
import com.jmwl.gostudio.ai.ai_agent_loop
import com.jmwl.gostudio.ai.tools.ai_tool_registry
import com.jmwl.gostudio.ai.load_ai_settings
import com.jmwl.gostudio.ai.save_ai_settings
import com.jmwl.gostudio.ui.dialogs.common.install_progress_dialog
import com.jmwl.gostudio.ui.dialogs.main.new_project_dialog
import com.jmwl.gostudio.ui.dialogs.main.open_project_dialog
import com.jmwl.gostudio.ui.dialogs.main.toolchain_custom_install_dialog
import com.jmwl.gostudio.ui.screens.ai.ai_chat_screen
import com.jmwl.gostudio.ui.screens.ai.ai_settings_screen
import com.jmwl.gostudio.ui.screens.editor.editor_settings_screen
import com.jmwl.gostudio.ui.screens.editor.editor_theme_settings_screen
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.theme.app_theme_type
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun main_navigation(
    recent_projects: List<recent_project>,
    toolchain_status: main_tools_install_status,
    current_theme: app_theme_type,
    scale_value: Float,
    toolchain_tasks: List<toolchain_trigger>,
    custom_toolchain_dialog: toolchain_custom_install_request?,
    on_back_to_background: () -> Unit,
    on_terminal: () -> Unit,
    on_project_click: (recent_project) -> Unit,
    on_project_remove: (recent_project) -> Unit,
    on_create_project: (String, String) -> Unit,
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
    val context = LocalContext.current
    val coroutine_scope = rememberCoroutineScope()

    // AI agent：主界面用通用问答（无项目上下文，不带文件工具）
    val ai_agent = remember {
        val home_dir = com.jmwl.gostudio.toolchain.toolchain_runtime_provider.paths().home_dir
        val ai_root = java.io.File(home_dir, ".ai").apply { mkdirs() }
        val global_skills_dir = java.io.File(ai_root, "skills")
        val global_prompts_dir = java.io.File(ai_root, "prompts")
        runCatching { com.jmwl.gostudio.ai.skills.release_builtin_skills(context, global_skills_dir) }
        val skill_mgr = com.jmwl.gostudio.ai.skills.ai_skill_manager(global_skills_dir, null)
        ai_agent_loop(
            settings_provider = { load_ai_settings(context) },
            env_provider = { ai_environment_context() },
            tool_registry = ai_tool_registry(),
            scope_launcher = { block -> coroutine_scope.launch { block() } },
            input_processor = com.jmwl.gostudio.ai.ai_input_processor(
                project_dir = null,
                skill_manager = skill_mgr,
                global_prompts_dir = global_prompts_dir,
                project_prompts_dir = null
            ),
            session_store = com.jmwl.gostudio.ai.ai_session_store(java.io.File(ai_root, "sessions")),
            session_id = "global",
            skill_manager = skill_mgr
        )
    }
    var ai_settings_state by remember { mutableStateOf(load_ai_settings(context)) }

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
                    on_install_go = {
                        on_toolchain_trigger_change(
                            toolchain_trigger(
                                title = "安装 Go 工具链",
                                action = toolchain_action.INSTALL_GO
                            )
                        )
                    },
                    on_install_gopls = {
                        on_toolchain_trigger_change(
                            toolchain_trigger(
                                title = "安装 gopls",
                                action = toolchain_action.INSTALL_GOPLS
                            )
                        )
                    },
                    on_install_git = {
                        on_toolchain_trigger_change(
                            toolchain_trigger(
                                title = "安装 git",
                                action = toolchain_action.INSTALL_GIT
                            )
                        )
                    },
                    on_install_garble = {
                        on_toolchain_trigger_change(
                            toolchain_trigger(
                                title = "安装 garble",
                                action = toolchain_action.INSTALL_GARBLE
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
            composable("agent") {
                androidx.compose.runtime.LaunchedEffect(Unit) { ai_agent.initialize() }
                ai_chat_screen(
                    agent = ai_agent,
                    on_back = { nav_controller.popBackStack() },
                    on_open_settings = { nav_controller.navigate("ai_settings") }
                )
            }
            composable("ai_settings") {
                ai_settings_screen(
                    initial = ai_settings_state,
                    on_back = { nav_controller.popBackStack() },
                    on_save = { new_settings ->
                        save_ai_settings(context, new_settings)
                        ai_settings_state = new_settings
                        nav_controller.popBackStack()
                    }
                )
            }
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
            on_dismiss = { show_new_project_dialog = false },
            on_create = { project_name, template_id ->
                show_new_project_dialog = false
                on_create_project(project_name, template_id)
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
