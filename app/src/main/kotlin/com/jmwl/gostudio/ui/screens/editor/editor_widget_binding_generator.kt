package com.jmwl.gostudio.ui.screens.editor

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * 由布局 XML 生成 Go 控件声明（ui.go）：
 * layout.xml -> uiWidgets/ui/bindUI，floats/note.xml -> noteWidgets/noteUI/bindNoteUI。
 * 工厂方法与 appsdk 中同名：TextView -> app.TextView(id) *appsdk.TextView。
 */

/** xml 中一个带 id 的组件及其生成信息。 */
data class editor_widget_binding(
    val id: String,
    val tag: String,
    val field: String,
    val factory: String,
    val type: String
)

data class editor_widget_binding_source(
    val file_name: String,
    val struct_name: String,
    val var_name: String,
    val bind_func: String
)

data class editor_widget_binding_result(
    val source: editor_widget_binding_source,
    val bindings: List<editor_widget_binding>,
    val content: String
)

/** 与 runtime_layout_loader 支持的控件一致；appsdk 均有同名工厂方法。 */
private val binding_supported_tags = setOf(
    "LinearLayout", "FrameLayout", "RelativeLayout", "GridLayout", "TableLayout", "TableRow",
    "RadioGroup", "ScrollView", "HorizontalScrollView", "NestedScrollView", "ViewFlipper",
    "TextView", "Button", "EditText", "AutoCompleteTextView", "ImageView", "ImageButton",
    "CheckBox", "RadioButton", "Switch", "ToggleButton", "ProgressBar", "SeekBar", "RatingBar",
    "Space", "View", "Spinner", "ListView", "GridView", "DatePicker", "TimePicker",
    "CalendarView", "NumberPicker", "Chronometer", "TextClock", "VideoView", "WebView"
)

/** id -> Go 字段名：按非字母数字分段、各段首字母大写；冲突时追加序号。 */
private fun go_field_name(id: String, used: MutableSet<String>): String {
    var name = id.split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotEmpty() }
        .joinToString("") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    if (name.isEmpty() || name.first().isDigit()) name = "Widget$name"
    var candidate = name
    var suffix = 2
    while (!used.add(candidate)) {
        candidate = name + suffix
        suffix++
    }
    return candidate
}

/** xml 文件名 -> 小驼峰标识符前缀：note.xml -> note，layout.xml 特例 -> ui。 */
private fun binding_base_name(xml_file_name: String): String {
    if (xml_file_name == "layout.xml") return "ui"
    val base = xml_file_name.substringBeforeLast(".xml")
        .split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotEmpty() }
        .mapIndexed { index, part ->
            if (index == 0) part.replaceFirstChar { it.lowercaseChar() }
            else part.replaceFirstChar { it.uppercaseChar() }
        }
        .joinToString("")
        .ifEmpty { "widget" }
    return if (base.first().isDigit()) "w$base" else base
}

fun editor_widget_binding_source_for(xml_file_name: String): editor_widget_binding_source {
    val base = binding_base_name(xml_file_name)
    val is_main_layout = base == "ui"
    return editor_widget_binding_source(
        file_name = if (is_main_layout) "ui.go" else "${base}_ui.go",
        struct_name = "${base}Widgets",
        var_name = base,
        bind_func = if (is_main_layout) "bindUI" else "bind${base.replaceFirstChar { it.uppercaseChar() }}"
    )
}

/** 解析 xml 中所有带 id 且受支持的组件；重复 id 视为错误。 */
fun editor_parse_widget_binding_tags(xml: String): Result<List<Pair<String, String>>> {
    return runCatching {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())
        val found = mutableListOf<Pair<String, String>>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name in binding_supported_tags) {
                val id = (0 until parser.attributeCount)
                    .firstOrNull { parser.getAttributeName(it) == "id" }
                    ?.let { parser.getAttributeValue(it) }
                    ?.trim().orEmpty()
                if (id.isNotEmpty()) found.add(id to parser.name)
            }
            event = parser.next()
        }
        val duplicates = found.groupingBy { it.first }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "布局里有重复的控件 id：${duplicates.joinToString("、")}" }
        found
    }
}

/** 生成 ui.go 全文；布局里没有可绑定的控件时报错。 */
fun editor_generate_widget_bindings(xml: String, xml_file_name: String): Result<editor_widget_binding_result> {
    return editor_parse_widget_binding_tags(xml).mapCatching { tags ->
        editor_build_widget_bindings(tags, xml_file_name)
    }
}

/** 由 (id, tag) 列表构造 ui.go 内容；纯函数，便于测试。 */
fun editor_build_widget_bindings(tags: List<Pair<String, String>>, xml_file_name: String): editor_widget_binding_result {
    require(tags.isNotEmpty()) { "布局里没有带 id 的可绑定控件" }
    val source = editor_widget_binding_source_for(xml_file_name)
    val used = mutableSetOf<String>()
    val bindings = tags.map { (id, tag) ->
        editor_widget_binding(
            id = id,
            tag = tag,
            field = go_field_name(id, used),
            factory = tag,
            type = "*appsdk.$tag"
        )
    }
    val field_width = bindings.maxOf { it.field.length }
    val content = buildString {
        appendLine("// Code generated by GoStudio from $xml_file_name. DO NOT EDIT.")
        appendLine()
        appendLine("package main")
        appendLine()
        appendLine("import \"gostudio/appsdk\"")
        appendLine()
        appendLine("// ${source.struct_name} 是 $xml_file_name 中带 id 控件的句柄集合。")
        appendLine("type ${source.struct_name} struct {")
        bindings.forEach {
            appendLine("\t${it.field.padEnd(field_width)} ${it.type}")
        }
        appendLine("}")
        appendLine()
        appendLine("var ${source.var_name} ${source.struct_name}")
        appendLine()
        appendLine("// ${source.bind_func} 绑定 $xml_file_name 的控件，请在 appsdk.Start() 之后调用一次。")
        appendLine("func ${source.bind_func}(app *appsdk.App) {")
        bindings.forEach {
            appendLine("\t${source.var_name}.${it.field} = app.${it.factory}(\"${it.id}\")")
        }
        appendLine("}")
    }
    return editor_widget_binding_result(source, bindings, content)
}

enum class editor_bind_call_status { INSERTED, ALREADY_PRESENT, ANCHOR_MISSING }

data class editor_bind_call_insertion(
    val content: String,
    val status: editor_bind_call_status
)

/** 在入口 Go 文件的 appsdk.Start() 之后插入 bind 调用；已存在则原样返回。 */
fun editor_insert_bind_call(entry_content: String, bind_func: String): editor_bind_call_insertion {
    val lines = entry_content.split("\n")
    val already = lines.any { Regex("""^\s*$bind_func\(\w+\)\s*$""").matches(it) }
    if (already) return editor_bind_call_insertion(entry_content, editor_bind_call_status.ALREADY_PRESENT)

    val anchor = Regex("""(\w+)\s*:?=\s*appsdk\.Start\(""")
    val anchor_index = lines.indexOfFirst { anchor.containsMatchIn(it) }
    if (anchor_index < 0) return editor_bind_call_insertion(entry_content, editor_bind_call_status.ANCHOR_MISSING)

    // 调用处使用用户实际的 app 变量名（不一定是 app）
    val app_var = anchor.find(lines[anchor_index])!!.groupValues[1]
    val indent = lines[anchor_index].takeWhile { it == ' ' || it == '\t' }
    val new_lines = lines.toMutableList()
    new_lines.add(anchor_index + 1, "$indent$bind_func($app_var)")
    return editor_bind_call_insertion(new_lines.joinToString("\n"), editor_bind_call_status.INSERTED)
}
