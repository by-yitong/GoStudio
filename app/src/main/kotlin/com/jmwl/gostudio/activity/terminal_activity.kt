package com.jmwl.gostudio.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jmwl.gostudio.ui.terminal.launch_command_tab
import com.jmwl.gostudio.ui.terminal.remember_terminal_state
import com.jmwl.gostudio.ui.terminal.terminal_close_last_behavior
import com.jmwl.gostudio.ui.terminal.terminal_panel
import com.jmwl.gostudio.ui.theme.app_theme_provider

class terminal_activity : ComponentActivity() {

    companion object {
        /** 要执行的命令（如 `go run .`），携带此参数时页面直接以命令会话打开 */
        const val EXTRA_RUN_COMMAND = "run_command"
        /** 命令会话标签页标题 */
        const val EXTRA_RUN_TITLE = "run_title"
        /** 命令工作目录（host 绝对路径，如项目目录） */
        const val EXTRA_RUN_WORKING_DIR = "run_working_dir"
        /** 额外环境变量，元素格式为 "key=value" */
        const val EXTRA_RUN_ENVIRONMENT = "run_environment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            app_theme_provider {
                terminal_screen_content()
            }
        }
    }

    @Composable
    private fun terminal_screen_content() {
        val terminal_state = remember_terminal_state()
        val run_command = remember { intent.getStringExtra(EXTRA_RUN_COMMAND) }
        val run_title = remember { intent.getStringExtra(EXTRA_RUN_TITLE) }
        val run_working_dir = remember { intent.getStringExtra(EXTRA_RUN_WORKING_DIR) }
        val run_environment = remember {
            intent.getStringArrayListExtra(EXTRA_RUN_ENVIRONMENT)
                ?.mapNotNull { entry ->
                    val separator = entry.indexOf('=')
                    if (separator > 0) {
                        entry.substring(0, separator) to entry.substring(separator + 1)
                    } else {
                        null
                    }
                }
                ?.toMap()
                ?: emptyMap()
        }
        val cwd = run_working_dir ?: java.io.File(filesDir, "home").absolutePath
        // 无运行参数时保持原默认：proot 内工作目录用 guest 路径 /home
        val proot_work_dir = run_working_dir ?: "/home"

        // 运行入口：先建命令会话；terminal_panel 见已有标签就不会再建默认 shell
        LaunchedEffect(Unit) {
            if (run_command != null && terminal_state.terminal_tabs.isEmpty()) {
                terminal_state.launch_command_tab(
                    context = this@terminal_activity,
                    title = run_title ?: "run",
                    host_working_dir = cwd,
                    command = run_command,
                    extra_environment = run_environment
                )
            }
        }

        val colors = app_theme_provider.colors
        val terminal_background = Color(colors.terminal_background.toLong() and 0xFFFFFFFFL)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = terminal_background
        ) { inner_padding ->
            terminal_panel(
                state = terminal_state,
                cwd = cwd,
                proot_work_dir = proot_work_dir,
                compact = false,
                fill_panel_background = true,
                show_tab_separators = true,
                show_keyboard = true,
                text_size = 28,
                close_last_behavior = terminal_close_last_behavior.ClosePanel,
                on_last_tab_closed = { finish() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner_padding)
            )
        }
    }
}
