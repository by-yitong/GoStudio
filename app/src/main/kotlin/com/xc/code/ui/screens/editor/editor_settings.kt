package com.xc.code.ui.screens.editor

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.xc.code.editor.model.editor_settings_state
import com.xc.code.editor.settings.import_editor_font
import com.xc.code.editor.settings.load_editor_settings
import com.xc.code.editor.settings.save_editor_settings
import com.xc.code.ui.theme.app_theme_provider
import com.xc.code.ui.toast.app_toast
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
        containerColor = Color.Transparent
    ) { padding_values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding_values)
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(35.dp),
                    shape = CircleShape,
                    color = colors.top_button_bg,
                    onClick = on_back
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = colors.top_button_icon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(35.dp))
            }

            Spacer(modifier = Modifier.height(30.dp))

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Text(
                    text = "编辑器",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.title_highlight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "设置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.subtitle
                )
            }

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
