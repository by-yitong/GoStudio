package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider

/** layout.xml 中可被 Go 代码操作的组件。 */
data class editor_layout_component(
    val id: String,
    val tag: String,
    val title: String
)

data class editor_code_template(
    val id: String,
    val title: String,
    val description: String,
    val code: String
)

private val text_widgets = setOf(
    "TextView", "EditText", "AutoCompleteTextView", "Button", "CheckBox",
    "RadioButton", "Switch", "ToggleButton", "Chronometer", "TextClock"
)
private val checked_widgets = setOf("CheckBox", "RadioButton", "Switch", "ToggleButton")

private fun component_templates(component: editor_layout_component): List<editor_code_template> {
    val id = component.id
    val result = mutableListOf(
        editor_code_template(
            "click",
            "点击事件",
            "组件被点击时触发",
            if (component.tag == "Button") {
                "app.Button(\"$id\").OnClick(func() {\n\tapp.Log(\"$id 被点击\")\n})"
            } else {
                "app.OnClick(\"$id\", func() {\n\tapp.Log(\"$id 被点击\")\n})"
            }
        ),
        editor_code_template(
            "long_click",
            "长按事件",
            "组件被长按时触发",
            "app.OnLongClick(\"$id\", func() {\n\tapp.Log(\"$id 被长按\")\n})"
        )
    )
    if (component.tag in text_widgets) {
        result += editor_code_template(
            "text_change", "文本变化", "文本内容改变时触发",
            "app.OnTextChanged(\"$id\", func(text string) {\n\tapp.Log(\"文本: \" + text)\n})"
        )
    }
    if (component.tag in checked_widgets) {
        result += editor_code_template(
            "checked_change", "选中变化", "复选 / 单选 / 开关状态改变时触发",
            "app.OnCheckedChange(\"$id\", func(checked bool) {\n\tapp.Log(\"选中: \", checked)\n})"
        )
    }
    if (component.tag == "SeekBar" || component.tag == "ProgressBar") {
        result += editor_code_template(
            "progress_change", "进度变化", "拖动进度或评分变化时触发",
            "app.OnProgressChange(\"$id\", func(progress int) {\n\tapp.Log(\"进度: \", progress)\n})"
        )
    }
    if (component.tag == "RatingBar") {
        result += editor_code_template(
            "rating_change", "评分变化", "用户修改评分时触发",
            "app.OnRatingChange(\"$id\", func(rating float64) {\n\tapp.Log(\"评分: \", rating)\n})"
        )
    }
    if (component.tag == "DatePicker") {
        result += editor_code_template(
            "date_change", "日期变化", "用户选择新日期时触发",
            "app.OnDateChange(\"$id\", func(date string) {\n\tapp.Log(\"日期: \" + date)\n})"
        )
    }
    if (component.tag == "TimePicker") {
        result += editor_code_template(
            "time_change", "时间变化", "用户选择新时间时触发",
            "app.OnTimeChange(\"$id\", func(time string) {\n\tapp.Log(\"时间: \" + time)\n})"
        )
    }
    return result
}

private val lifecycle_templates = listOf(
    editor_code_template("create", "onCreate", "App 被创建", "app.OnCreate(func() {\n\tapp.Log(\"生命周期: create\")\n})"),
    editor_code_template("start", "onStart", "App 可见", "app.OnStart(func() {\n\tapp.Log(\"生命周期: start\")\n})"),
    editor_code_template("resume", "onResume", "App 进入前台", "app.OnResume(func() {\n\tapp.Log(\"生命周期: resume\")\n})"),
    editor_code_template("pause", "onPause", "App 失去前台", "app.OnPause(func() {\n\tapp.Log(\"生命周期: pause\")\n})"),
    editor_code_template("stop", "onStop", "App 不可见", "app.OnStop(func() {\n\tapp.Log(\"生命周期: stop\")\n})"),
    editor_code_template("destroy", "onDestroy", "App 被销毁", "app.OnDestroy(func() {\n\tapp.Log(\"生命周期: destroy\")\n})")
)

private val native_templates = listOf(
    editor_code_template(
        "toast", "Toast", "弹出系统提示",
        "app.Toast(\"你好，GoStudio\")"
    ),
    editor_code_template(
        "vibrate", "振动", "调用系统振动器",
        "app.Vibrate(300)"
    ),
    editor_code_template(
        "clipboard_set", "写剪贴板", "复制文本到系统剪贴板",
        "app.SetClipboard(\"要复制的文本\")"
    ),
    editor_code_template(
        "clipboard_get", "读剪贴板", "读取系统剪贴板内容",
        "text, err := app.GetClipboard()\nif err != nil {\n\tapp.Log(\"读取失败: \", err)\n} else {\n\tapp.Log(\"剪贴板: \" + text)\n}"
    ),
    editor_code_template(
        "open_url", "打开链接", "调用系统浏览器",
        "app.OpenURL(\"https://go.dev\")"
    ),
    editor_code_template(
        "share", "系统分享", "调起系统分享面板",
        "app.Share(\"分享标题\", \"分享内容\")"
    ),
    editor_code_template(
        "device_info", "设备信息", "获取机型、系统、屏幕等信息",
        "info, err := app.DeviceInfo()\nif err != nil {\n\tapp.Log(\"读取设备失败: \", err)\n} else {\n\tapp.Log(info.Model, info.Android, info.Width, info.Height)\n}"
    )
)

/** 供设计器“跳转事件”复用的组件事件模板。 */
fun editor_component_event_templates(component: editor_layout_component): List<editor_code_template> =
    component_templates(component)

@Composable
fun editor_code_generator_dialog(
    components: List<editor_layout_component>,
    on_insert: (String) -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    var tab by remember { mutableIntStateOf(0) }
    var selected_component by remember { mutableStateOf(components.firstOrNull()) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(selected_component?.id) {
        selected = setOf("click")
    }

    val templates = when (tab) {
        0 -> selected_component?.let { component_templates(it) } ?: emptyList()
        1 -> lifecycle_templates
        else -> native_templates
    }
    val code = templates.filter { it.id in selected }.joinToString("\n\n") { it.code }

    AlertDialog(
        onDismissRequest = on_dismiss,
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank(),
                onClick = {
                    on_insert("\n$code\n")
                    on_dismiss()
                }
            ) { Text("插入") }
        },
        dismissButton = {
            TextButton(onClick = on_dismiss) { Text("取消") }
        },
        title = { Text("生成代码", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0; selected = setOf("click") }, text = { Text("组件事件") })
                    Tab(selected = tab == 1, onClick = { tab = 1; selected = setOf("create") }, text = { Text("生命周期") })
                    Tab(selected = tab == 2, onClick = { tab = 2; selected = setOf("toast") }, text = { Text("系统 API") })
                }
                Spacer(Modifier.height(10.dp))
                if (tab == 0) {
                    if (components.isEmpty()) {
                        Text("layout.xml 里暂无带 id 的组件", color = colors.editor_hint, fontSize = 11.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(components, key = { it.id }) { component ->
                                FilterChip(
                                    selected = selected_component?.id == component.id,
                                    onClick = { selected_component = component },
                                    label = { Text(component.title, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.height(190.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(templates, key = { it.id }) { template ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (template.id in selected) selected - template.id else selected + template.id
                                }
                        ) {
                            Checkbox(
                                checked = template.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + template.id else selected - template.id
                                }
                            )
                            Column(Modifier.padding(top = 12.dp)) {
                                Text(template.title, fontSize = 12.sp, color = colors.editor_text)
                                Text(template.description, fontSize = 10.sp, color = colors.editor_hint)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    code.ifBlank { "选择要生成的内容" },
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (code.isBlank()) colors.editor_hint else colors.editor_text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
        },
        containerColor = colors.editor_bg,
        titleContentColor = colors.editor_text,
        textContentColor = colors.editor_text
    )
}
