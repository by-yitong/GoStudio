package com.jmwl.gostudio.ui.screens.editor

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
            "app.OnDateChange(\"$id\", func(date string) {\n\tapp.Log(\"日期: \", date)\n})"
        )
    }
    if (component.tag == "TimePicker") {
        result += editor_code_template(
            "time_change", "时间变化", "用户选择新时间时触发",
            "app.OnTimeChange(\"$id\", func(time string) {\n\tapp.Log(\"时间: \", time)\n})"
        )
    }
    return result
}

/** 供设计器“跳转事件”复用的组件事件模板。 */
fun editor_component_event_templates(component: editor_layout_component): List<editor_code_template> =
    component_templates(component)
