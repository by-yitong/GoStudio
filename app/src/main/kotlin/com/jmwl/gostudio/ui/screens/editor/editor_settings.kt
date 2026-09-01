package com.jmwl.gostudio.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.editor.model.editor_settings_state
import com.jmwl.gostudio.editor.settings.import_editor_font
import com.jmwl.gostudio.editor.settings.load_editor_settings
import com.jmwl.gostudio.editor.settings.save_editor_settings
import com.jmwl.gostudio.ui.components.sub_page_top_bar
import com.jmwl.gostudio.ui.theme.app_theme_provider
import com.jmwl.gostudio.ui.toast.app_toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editor_settings_screen(
    on_back: () -> Unit,
    on_theme_click: () -> Unit
) {
    val context = LocalContext.current
    val colors = app_theme_provider.colors
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(load_editor_settings(context)) }

    fun update_settings(next_settings: editor_settings_state) {
        settings = next_settings
        save_editor_settings(context, next_settings)
    }

    val import_font_launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { import_editor_font(context, uri) }
            }
            result.onSuccess { path ->
                update_settings(
                    settings.copy(
                        font_family = "imported",
                        imported_font_path = path
                    )
                )
                app_toast.show(context, "字体已导入", app_toast.LENGTH_SHORT)
            }.onFailure { error ->
                app_toast.show(context, "字体导入失败: ${error.message.orEmpty()}", app_toast.LENGTH_LONG)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = { sub_page_top_bar("编辑器设置", on_back) }
    ) { padding_values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding_values)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            editor_settings_panel(
                settings = settings,
                on_settings_change = ::update_settings,
                on_import_font = { import_font_launcher.launch("font/*") },
                on_open_theme_settings = on_theme_click,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
