package com.jmwl.gostudio.designer

import android.app.Activity
import android.content.Intent
import android.graphics.Color as AColor
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.jmwl.gostudio.runtime.runtime_layout_loader
import com.jmwl.gostudio.ui.theme.app_theme_provider
import java.io.File
import kotlin.math.roundToInt

/**
 * AndLua 式布局设计器（独立页面）：
 * 左抽屉=组件面板 / 中间=实时预览（可点选） / 右抽屉=选中组件属性。
 * 通过 Intent 传入项目目录，读取 layout.xml；返回时写回结果。
 */
class layout_designer_activity : androidx.activity.ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val project_dir = intent.getStringExtra(EXTRA_PROJECT_DIR) ?: run { finish(); return }
        val layout_file = File(project_dir, "layout.xml")
        val initial = if (layout_file.isFile) layout_file.readText() else DEFAULT_LAYOUT

        // 状态栏颜色与工作区背景一致
        window.statusBarColor = android.graphics.Color.parseColor("#1B1C1F")
        window.navigationBarColor = android.graphics.Color.parseColor("#1B1C1F")

        setContent {
            MaterialTheme {
                designer_screen(
                    initial_xml = initial,
                    project_dir = File(project_dir),
                    on_save = { xml ->
                        layout_file.writeText(xml)
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_SAVED, true))
                        finish()
                    },
                    on_open_event = { xml, component_id, event_type, component_tag ->
                        layout_file.writeText(xml)
                        setResult(
                            RESULT_OK,
                            Intent()
                                .putExtra(EXTRA_SAVED, true)
                                .putExtra(EXTRA_EVENT_COMPONENT_ID, component_id)
                                .putExtra(EXTRA_EVENT_TYPE, event_type)
                                .putExtra(EXTRA_EVENT_COMPONENT_TAG, component_tag)
                        )
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PROJECT_DIR = "project_dir"
        const val EXTRA_SAVED = "saved"
        const val EXTRA_EVENT_COMPONENT_ID = "event_component_id"
        const val EXTRA_EVENT_TYPE = "event_type"
        const val EXTRA_EVENT_COMPONENT_TAG = "event_component_tag"
        const val DEFAULT_LAYOUT = """<LinearLayout orientation="vertical" gravity="center" padding="24dp">

    <TextView id="tv" text="你好" textSize="24sp"/>
    <Button id="btn" text="点我" layout_marginTop="16dp"/>

</LinearLayout>"""
    }
}

/* ---------- 组件树模型 ---------- */

private class d_node(
    val tag: String,
    val attrs: MutableMap<String, String> = mutableMapOf(),
    val children: MutableList<d_node> = mutableListOf(),
    var parent: d_node? = null
) {
    val id get() = attrs["id"] ?: ""
    val is_container get() = tag in CONTAINERS
    fun deep_copy(): d_node {
        val copy = d_node(tag, attrs.toMutableMap())
        children.forEach { copy.children.add(it.deep_copy().also { c -> c.parent = copy }) }
        return copy
    }
}

private val CONTAINERS = setOf(
    "LinearLayout", "FrameLayout", "RelativeLayout", "ScrollView", "HorizontalScrollView",
    "NestedScrollView", "GridLayout", "TableLayout", "TableRow", "RadioGroup", "ViewFlipper"
)
private val WIDGET_TAGS = listOf(
    "TextView", "EditText", "AutoCompleteTextView", "Button", "ImageView", "ImageButton",
    "CheckBox", "RadioButton", "Switch", "ToggleButton", "ProgressBar", "SeekBar",
    "RatingBar", "Spinner", "ListView", "GridView", "DatePicker", "TimePicker",
    "CalendarView", "NumberPicker", "Chronometer", "TextClock", "VideoView",
    "WebView", "View", "Space"
)
private val LAYOUT_TAGS = listOf(
    "LinearLayout", "FrameLayout", "RelativeLayout", "GridLayout", "TableLayout",
    "TableRow", "RadioGroup", "ScrollView", "HorizontalScrollView",
    "NestedScrollView", "ViewFlipper"
)
private val TAG_CN = mapOf(
    "LinearLayout" to "线性布局", "FrameLayout" to "帧布局", "RelativeLayout" to "相对布局",
    "GridLayout" to "网格布局", "TableLayout" to "表格布局", "TableRow" to "表格行",
    "RadioGroup" to "单选组", "ScrollView" to "滚动视图", "HorizontalScrollView" to "横向滚动",
    "NestedScrollView" to "嵌套滚动", "ViewFlipper" to "翻页布局",
    "TextView" to "文本", "EditText" to "输入框", "AutoCompleteTextView" to "自动补全输入框",
    "Button" to "按钮", "ImageView" to "图片", "ImageButton" to "图片按钮",
    "CheckBox" to "复选框", "RadioButton" to "单选框", "Switch" to "开关",
    "ToggleButton" to "开关按钮", "ProgressBar" to "进度条", "SeekBar" to "拖动条",
    "RatingBar" to "评分条", "Spinner" to "下拉框", "ListView" to "列表",
    "GridView" to "网格列表", "DatePicker" to "日期选择器", "TimePicker" to "时间选择器",
    "CalendarView" to "日历", "NumberPicker" to "数字选择器", "Chronometer" to "计时器",
    "TextClock" to "文本时钟", "VideoView" to "视频", "WebView" to "网页",
    "View" to "占位视图", "Space" to "空白占位"
)
private val COMMON_ATTRS = listOf(
    "id", "layout_width", "layout_height", "layout_margin", "layout_marginTop", "layout_marginBottom",
    "layout_marginLeft", "layout_marginRight",
    "background", "visibility", "enabled", "padding"
)
private val TEXT_ATTRS = listOf("text", "hint", "textSize", "textColor", "textColorHint", "gravity", "singleLine", "maxLines")
private val LAYOUT_ONLY_ATTRS = listOf("orientation", "gravity", "columnCount", "rowCount")
private val TAG_ATTRS = mapOf(
    "ImageView" to listOf("src", "scaleType"),
    "ImageButton" to listOf("src", "scaleType"),
    "CheckBox" to listOf("checked", "text"),
    "RadioButton" to listOf("checked", "text"),
    "Switch" to listOf("checked", "text"),
    "ToggleButton" to listOf("textOn", "textOff"),
    "ProgressBar" to listOf("max", "progress"),
    "SeekBar" to listOf("max", "progress"),
    "RatingBar" to listOf("numStars", "rating", "stepSize"),
    "DatePicker" to listOf("spinnersShown", "calendarViewShown"),
    "NumberPicker" to listOf("minValue", "maxValue", "value"),
    "Chronometer" to listOf("countDown", "autoStart"),
    "TextClock" to listOf("format12Hour", "format24Hour"),
    "ViewFlipper" to listOf("autoStart", "flipInterval")
)
private val CONTAINER_EXTRA_ATTRS = mapOf(
    "LinearLayout" to listOf("orientation", "gravity"),
    "RadioGroup" to listOf("orientation", "gravity"),
    "GridLayout" to listOf("orientation", "columnCount", "rowCount")
)
private val PARENT_ATTRS = mapOf(
    "LinearLayout" to listOf("layout_weight", "layout_gravity"),
    "RadioGroup" to listOf("layout_weight", "layout_gravity"),
    "TableLayout" to listOf("layout_weight", "layout_gravity"),
    "TableRow" to listOf("layout_weight", "layout_gravity"),
    "FrameLayout" to listOf("layout_gravity"),
    "ScrollView" to listOf("layout_gravity"),
    "HorizontalScrollView" to listOf("layout_gravity"),
    "NestedScrollView" to listOf("layout_gravity"),
    "ViewFlipper" to listOf("layout_gravity"),
    "RelativeLayout" to listOf(
        "layout_centerInParent", "layout_centerHorizontal", "layout_centerVertical",
        "layout_alignParentTop", "layout_alignParentBottom", "layout_alignParentLeft", "layout_alignParentRight"
    ),
    "GridLayout" to listOf("layout_row", "layout_column", "layout_rowSpan", "layout_columnSpan")
)
private val ATTR_CN = mapOf(
    "id" to "ID", "text" to "文本", "hint" to "提示", "textSize" to "字号",
    "textColor" to "文字颜色", "textColorHint" to "提示颜色", "background" to "背景",
    "layout_width" to "宽度", "layout_height" to "高度", "layout_margin" to "外边距",
    "layout_marginTop" to "上边距", "layout_marginBottom" to "下边距",
    "layout_marginLeft" to "左边距", "layout_marginRight" to "右边距",
    "layout_gravity" to "位置", "gravity" to "内容对齐", "orientation" to "方向",
    "padding" to "内边距", "visibility" to "可见性", "enabled" to "可用",
    "singleLine" to "单行", "maxLines" to "最大行数", "src" to "图片", "scaleType" to "缩放方式",
    "checked" to "选中", "textOn" to "开启文本", "textOff" to "关闭文本",
    "max" to "最大值", "progress" to "当前值", "numStars" to "星星数",
    "rating" to "评分", "stepSize" to "步长", "columnCount" to "列数",
    "rowCount" to "行数", "layout_weight" to "权重", "checkedButton" to "选中项",
    "layout_row" to "所在行", "layout_column" to "所在列", "layout_rowSpan" to "跨行数",
    "layout_columnSpan" to "跨列数", "layout_centerInParent" to "父容器居中",
    "layout_centerHorizontal" to "水平居中", "layout_centerVertical" to "垂直居中",
    "layout_alignParentTop" to "贴住顶部", "layout_alignParentBottom" to "贴住底部",
    "layout_alignParentLeft" to "贴住左侧", "layout_alignParentRight" to "贴住右侧",
    "spinnersShown" to "显示滚轮", "calendarViewShown" to "显示日历",
    "minValue" to "最小值", "value" to "当前值", "countDown" to "倒计时",
    "autoStart" to "自动开始", "format12Hour" to "12小时格式", "format24Hour" to "24小时格式",
    "flipInterval" to "切换间隔"
)

private enum class property_kind { TEXT, MULTILINE_TEXT, SINGLE_CHOICE, MULTI_CHOICE, BOOLEAN, NUMBER, DIMENSION, COLOR, SIZE, IMAGE }

private data class property_spec(
    val kind: property_kind,
    val options: List<String> = emptyList(),
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val unit: String = "dp"
)

private val SIZE_OPTIONS = listOf("wrap_content", "match_parent")
private val GRAVITY_OPTIONS = listOf("top", "bottom", "left", "right", "center", "center_vertical", "center_horizontal")

/** 属性编辑器类型：枚举用单选/多选，数值用滑杆，尺寸/颜色/文本分别适配。 */
private fun property_spec_for(attr: String): property_spec = when (attr) {
    "orientation" -> property_spec(property_kind.SINGLE_CHOICE, listOf("vertical", "horizontal"))
    "layout_width", "layout_height" -> property_spec(property_kind.SIZE, SIZE_OPTIONS)
    "gravity", "layout_gravity" -> property_spec(property_kind.MULTI_CHOICE, GRAVITY_OPTIONS)
    "visibility" -> property_spec(property_kind.SINGLE_CHOICE, listOf("visible", "invisible", "gone"))
    "enabled", "singleLine", "checked", "spinnersShown", "calendarViewShown", "countDown",
    "autoStart", "layout_centerInParent", "layout_centerHorizontal", "layout_centerVertical",
    "layout_alignParentTop", "layout_alignParentBottom", "layout_alignParentLeft", "layout_alignParentRight"
        -> property_spec(property_kind.BOOLEAN, listOf("false", "true"))
    "maxLines" -> property_spec(property_kind.NUMBER, min = 1f, max = 30f, step = 1f)
    "numStars" -> property_spec(property_kind.NUMBER, min = 1f, max = 10f, step = 1f)
    "columnCount", "rowCount", "layout_row", "layout_column", "layout_rowSpan", "layout_columnSpan"
        -> property_spec(property_kind.NUMBER, min = 0f, max = 12f, step = 1f)
    "flipInterval" -> property_spec(property_kind.NUMBER, min = 100f, max = 10000f, step = 100f)
    "max", "progress", "minValue", "maxValue", "value" -> property_spec(property_kind.NUMBER, min = 0f, max = 1000f, step = 1f)
    "rating" -> property_spec(property_kind.NUMBER, min = 0f, max = 5f, step = 0.5f)
    "stepSize" -> property_spec(property_kind.NUMBER, min = 0.1f, max = 1f, step = 0.1f)
    "layout_weight" -> property_spec(property_kind.NUMBER, min = 0f, max = 10f, step = 0.5f)
    "padding", "layout_margin", "layout_marginTop", "layout_marginBottom", "layout_marginLeft", "layout_marginRight"
        -> property_spec(property_kind.DIMENSION, max = 128f)
    "textSize" -> property_spec(property_kind.DIMENSION, min = 8f, max = 72f, unit = "sp")
    "background", "textColor", "textColorHint" -> property_spec(property_kind.COLOR)
    "text", "hint", "textOn", "textOff" -> property_spec(property_kind.MULTILINE_TEXT)
    "src" -> property_spec(property_kind.IMAGE)
    "scaleType" -> property_spec(
        property_kind.SINGLE_CHOICE,
        listOf("fitCenter", "center", "centerCrop", "centerInside", "fitStart", "fitEnd", "fitXY")
    )
    "format12Hour", "format24Hour" -> property_spec(property_kind.TEXT)
    else -> property_spec(property_kind.TEXT)
}

private fun property_display(attr: String, value: String): String {
    if (value.isBlank()) return "未设置"
    return when (property_spec_for(attr).kind) {
        property_kind.BOOLEAN -> if (value.equals("true", true) || value == "1") "是" else "否"
        property_kind.SINGLE_CHOICE -> when (value) {
            "vertical" -> "垂直"; "horizontal" -> "水平"
            "fitCenter" -> "完整显示居中"; "center" -> "原图居中"
            "centerCrop" -> "裁剪填满"; "centerInside" -> "完整显示"
            "fitStart" -> "靠上/左适配"; "fitEnd" -> "靠下/右适配"; "fitXY" -> "拉伸填满"
            "wrap_content" -> "自适应内容"; "match_parent" -> "填满父容器"
            "visible" -> "可见"; "invisible" -> "不可见"; "gone" -> "移除占位"
            else -> value
        }
        property_kind.IMAGE -> value.substringAfterLast('/')
        property_kind.MULTI_CHOICE -> value.split("|", ",", " ").filter { it.isNotBlank() }.joinToString(" / ") {
            when (it) { "top" -> "顶部"; "bottom" -> "底部"; "left" -> "左侧"; "right" -> "右侧";
                "center" -> "居中"; "center_vertical" -> "垂直居中"; "center_horizontal" -> "水平居中"; else -> it }
        }
        else -> value
    }
}
private data class property_group(val title: String, val attrs: List<String>)

private fun property_groups_for(attrs: List<String>): List<property_group> {
    val consumed = mutableSetOf<String>()
    fun take(predicate: (String) -> Boolean): List<String> {
        val values = attrs.filter { it !in consumed && predicate(it) }
        consumed += values
        return values
    }
    val basic = take {
        it in setOf("id", "layout_width", "layout_height", "visibility", "enabled", "background", "padding")
    }
    val layout = take {
        it.startsWith("layout_") || it in setOf(
            "orientation", "gravity", "columnCount", "rowCount"
        )
    }
    val text = take {
        it in setOf(
            "text", "hint", "textSize", "textColor", "textColorHint",
            "singleLine", "maxLines"
        )
    }
    val component = attrs.filter { it !in consumed }
    return listOf(
        "基础属性" to basic,
        "布局属性" to layout,
        "文本属性" to text,
        "组件属性" to component
    ).mapNotNull { (title, values) ->
        if (values.isEmpty()) null else property_group(title, values)
    }
}

private fun component_events(tag: String): List<Pair<String, String>> {
    val events = mutableListOf(
        "click" to "点击事件",
        "long_click" to "长按事件"
    )
    if (tag in setOf(
            "TextView", "EditText", "AutoCompleteTextView", "Button", "CheckBox",
            "RadioButton", "Switch", "ToggleButton", "Chronometer", "TextClock"
        )
    ) events += "text_change" to "文本变化事件"
    if (tag in setOf("CheckBox", "RadioButton", "Switch", "ToggleButton")) {
        events += "checked_change" to "选中变化事件"
    }
    if (tag == "SeekBar" || tag == "ProgressBar") events += "progress_change" to "进度变化事件"
    if (tag == "RatingBar") events += "rating_change" to "评分变化事件"
    if (tag == "DatePicker") events += "date_change" to "日期变化事件"
    if (tag == "TimePicker") events += "time_change" to "时间变化事件"
    return events
}

private fun tag_cn(tag: String) = TAG_CN[tag] ?: tag

/** 按控件类型和父容器返回属性字段，避免给普通按钮显示无意义的日期/列表属性。 */
private fun attrs_for(tag: String, parent_tag: String? = null): List<String> {
    val result = COMMON_ATTRS.toMutableList()
    if (tag == "TextView" || tag == "EditText" || tag == "AutoCompleteTextView" ||
        tag in setOf("Button", "CheckBox", "RadioButton", "Switch", "ToggleButton", "Chronometer", "TextClock")
    ) result.addAll(TEXT_ATTRS)
    if (tag in CONTAINERS) result.addAll(LAYOUT_ONLY_ATTRS + (CONTAINER_EXTRA_ATTRS[tag] ?: emptyList()))
    parent_tag?.let { result.addAll(PARENT_ATTRS[it] ?: emptyList()) }
    TAG_ATTRS[tag]?.let { result.addAll(it) }
    return result.distinct()
}

private fun parse_xml(xml: String): d_node {
    val parser = android.util.Xml.newPullParser()
    parser.setInput(xml.reader())
    var event = parser.eventType
    var root: d_node? = null
    val stack = ArrayDeque<d_node>()
    while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
        when (event) {
            org.xmlpull.v1.XmlPullParser.START_TAG -> {
                val node = d_node(parser.name)
                for (i in 0 until parser.attributeCount) node.attrs[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                if (root == null) root = node else { stack.last().children.add(node); node.parent = stack.last() }
                stack.addLast(node)
            }
            org.xmlpull.v1.XmlPullParser.END_TAG -> stack.removeLastOrNull()
        }
        event = parser.next()
    }
    return root ?: d_node("LinearLayout")
}

private fun serialize(node: d_node): String {
    val sb = StringBuilder()
    fun write(n: d_node, indent: Int) {
        sb.append("  ".repeat(indent)).append('<').append(n.tag)
        n.attrs.forEach { (k, v) -> sb.append(' ').append(k).append("=\"").append(v.replace("\"", "&quot;")).append('"') }
        if (n.children.isEmpty()) { sb.append("/>\n"); return }
        sb.append(">\n")
        n.children.forEach { write(it, indent + 1) }
        sb.append("  ".repeat(indent)).append("</").append(n.tag).append(">\n")
    }
    write(node, 0)
    return sb.toString()
}

private fun next_id(root: d_node, tag: String): String {
    var count = 0
    fun walk(n: d_node) { if (n.tag == tag) count++; n.children.forEach(::walk) }
    walk(root)
    return "${tag.lowercase()}${count + 1}"
}

private fun default_attrs(tag: String, root: d_node): MutableMap<String, String> {
    val m = mutableMapOf(
        "layout_width" to "wrap_content",
        "layout_height" to "wrap_content",
        "layout_marginTop" to "8dp",
        "id" to next_id(root, tag)
    )
    when (tag) {
        "LinearLayout" -> m["orientation"] = "vertical"
        "RadioGroup" -> m["orientation"] = "vertical"
        "ScrollView", "NestedScrollView" -> m["layout_height"] = "match_parent"
        "HorizontalScrollView" -> m["layout_width"] = "match_parent"
        "GridLayout" -> { m["columnCount"] = "2"; m["layout_width"] = "match_parent" }
        "TableLayout" -> m["layout_width"] = "match_parent"
        "ViewFlipper" -> { m["layout_width"] = "match_parent"; m["layout_height"] = "match_parent" }
    }
    when (tag) {
        "TextView" -> m["text"] = "文本"
        "Button" -> m["text"] = "按钮"
        "EditText" -> m["hint"] = "输入"
        "AutoCompleteTextView" -> m["hint"] = "自动补全"
        "CheckBox" -> m["text"] = "复选框"
        "RadioButton" -> m["text"] = "单选框"
        "Switch" -> m["text"] = "开关"
        "ToggleButton" -> m["text"] = "开关按钮"
        "ImageView" -> { m["layout_width"] = "96dp"; m["layout_height"] = "96dp"; m["background"] = "#C8CACD" }
        "ImageButton" -> { m["layout_width"] = "48dp"; m["layout_height"] = "48dp"; m["background"] = "#C8CACD" }
        "SeekBar" -> { m["layout_width"] = "match_parent"; m["max"] = "100"; m["progress"] = "30" }
        "ProgressBar" -> { m["max"] = "100"; m["progress"] = "30" }
        "RatingBar" -> { m["numStars"] = "5"; m["rating"] = "4"; m["stepSize"] = "1" }
        "Spinner" -> m["layout_width"] = "match_parent"
        "ListView" -> { m["layout_width"] = "match_parent"; m["layout_height"] = "180dp" }
        "GridView" -> { m["layout_width"] = "match_parent"; m["layout_height"] = "180dp"; m["numColumns"] = "3" }
        "DatePicker" -> { m["layout_width"] = "wrap_content"; m["layout_height"] = "wrap_content" }
        "TimePicker" -> { m["layout_width"] = "wrap_content"; m["layout_height"] = "wrap_content" }
        "CalendarView" -> { m["layout_width"] = "match_parent"; m["layout_height"] = "240dp" }
        "NumberPicker" -> { m["layout_width"] = "90dp"; m["layout_height"] = "140dp"; m["minValue"] = "0"; m["maxValue"] = "100"; m["value"] = "30" }
        "Chronometer" -> m["textSize"] = "18sp"
        "TextClock" -> { m["format24Hour"] = "HH:mm:ss"; m["layout_width"] = "wrap_content"; m["layout_height"] = "wrap_content" }
        "VideoView" -> { m["layout_width"] = "240dp"; m["layout_height"] = "140dp"; m["background"] = "#111111" }
        "WebView" -> { m["layout_width"] = "match_parent"; m["layout_height"] = "180dp"; m["background"] = "#FFFFFF" }
        "View" -> { m["layout_width"] = "120dp"; m["layout_height"] = "48dp"; m["background"] = "#C8CACD" }
        "Space" -> { m["layout_width"] = "16dp"; m["layout_height"] = "16dp" }
    }
    return m
}

private fun remove_node(root: d_node, target: d_node): Boolean {
    fun walk(n: d_node): Boolean {
        if (n.children.remove(target)) return true
        return n.children.any(::walk)
    }
    return walk(root)
}

private fun find_node(root: d_node, target: d_node): d_node? {
    if (root === target) return root
    // deep_copy 后引用不同，按内容匹配：tag + id + 子节点数
    fun match(n: d_node): d_node? {
        if (n.tag == target.tag && n.id == target.id && n.children.size == target.children.size) return n
        n.children.forEach { c -> match(c)?.let { return it } }
        return null
    }
    return match(root)
}

/* ---------- 设计器界面 ---------- */

@Composable
private fun designer_screen(
    initial_xml: String,
    project_dir: File,
    on_save: (String) -> Unit,
    on_open_event: (String, String, String, String) -> Unit
) {
    var xml by remember { mutableStateOf(initial_xml) }
    var tree by remember { mutableStateOf(parse_xml(initial_xml)) }
    var selected by remember { mutableStateOf<d_node?>(null) }
    var preview_revision by remember { mutableIntStateOf(0) }
    var left_open by remember { mutableStateOf(true) }
    var right_open by remember { mutableStateOf(false) }
    var clipboard by remember { mutableStateOf<d_node?>(null) }
    var has_unsaved_changes by remember { mutableStateOf(false) }
    var show_exit_confirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = app_theme_provider.colors

    fun rebuild_from_tree() {
        xml = serialize(tree)
        preview_revision++
        has_unsaved_changes = true
    }

    BackHandler(enabled = has_unsaved_changes && !show_exit_confirm) {
        show_exit_confirm = true
    }

    if (show_exit_confirm) {
        AlertDialog(
            onDismissRequest = { show_exit_confirm = false },
            title = { Text("保存修改", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("布局尚未保存，要保存后退出吗？", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    show_exit_confirm = false
                    on_save(serialize(tree))
                }) { Text("保存", color = colors.editor_icon) }
            },
            dismissButton = {
                TextButton(onClick = {
                    show_exit_confirm = false
                    has_unsaved_changes = false
                    (context as? Activity)?.finish()
                }) { Text("丢弃", color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)) }
            },
            containerColor = colors.editor_bg,
            titleContentColor = colors.editor_text,
            textContentColor = colors.editor_hint
        )
    }

    Box(Modifier.fillMaxSize()) {
            // 中间：实时预览（全屏，点选控件后自动弹右抽屉）
            RealtimePreview(
                xml = xml,
                project_dir = project_dir,
                revision = preview_revision,
                on_select = { id ->
                    selected = find_by_id(tree, id)
                },
                modifier = Modifier.fillMaxSize()
            )

            // 悬浮顶栏：组件树 / 保存 / 属性（半透明圆角按钮悬浮在预览上）
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 组件树
                Surface(
                    onClick = { left_open = !left_open },
                    shape = RoundedCornerShape(50),
                    color = colors.editor_bg.copy(alpha = 0.85f),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = "组件树",
                        tint = if (left_open) colors.editor_icon else colors.editor_hint,
                        modifier = Modifier.padding(10.dp).size(19.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                // 保存
                Surface(
                    onClick = {
                        has_unsaved_changes = false
                        on_save(serialize(tree))
                    },
                    shape = RoundedCornerShape(50),
                    color = colors.editor_bg.copy(alpha = 0.85f),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "保存",
                        tint = colors.editor_icon,
                        modifier = Modifier.padding(10.dp).size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                // 属性
                Surface(
                    onClick = { right_open = !right_open },
                    shape = RoundedCornerShape(50),
                    color = colors.editor_bg.copy(alpha = 0.85f),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "属性",
                        tint = if (right_open) colors.editor_icon else colors.editor_hint,
                        modifier = Modifier.padding(10.dp).size(19.dp)
                    )
                }
            }

            // 抽屉互斥：打开左关右
            LaunchedEffect(left_open) { if (left_open) right_open = false }
            LaunchedEffect(right_open) { if (right_open) left_open = false }

            // 点击抽屉外区域关闭抽屉（透明遮罩浮在预览上，位于抽屉下层）
            if (left_open || right_open) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            left_open = false
                            right_open = false
                        }
                )
            }

            // 左抽屉：组件树（从 XML 解析的层级结构，点击选中）
            androidx.compose.animation.AnimatedVisibility(
                visible = left_open,
                enter = androidx.compose.animation.slideInHorizontally { -it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutHorizontally { -it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight()
            ) {
                Surface(shadowElevation = 8.dp, color = colors.editor_bg) {
                    ComponentTree(
                        root = tree,
                        selected = selected,
                        on_select = { node ->
                            selected = node
                        },
                        on_add = { tag ->
                            val target = selected?.takeIf { it.is_container } ?: tree
                            val node = d_node(tag, default_attrs(tag, tree))
                            node.parent = target
                            target.children.add(node)
                            selected = node
                            tree = tree.deep_copy().also { selected = find_node(it, node) }
                            rebuild_from_tree()
                        },
                        on_copy = { node -> clipboard = node.deep_copy() },
                        on_paste = { container ->
                            clipboard?.let { clip ->
                                val copy = clip.deep_copy()
                                // 重置复制的 id 避免冲突
                                if (copy.attrs.containsKey("id")) {
                                    copy.attrs["id"] = next_id(tree, copy.tag)
                                }
                                copy.parent = container
                                container.children.add(copy)
                                tree = tree.deep_copy().also { selected = find_node(it, copy) }
                                rebuild_from_tree()
                            }
                        },
                        on_delete = { node ->
                            remove_node(tree, node)
                            if (selected === node) selected = null
                            tree = tree.deep_copy()
                            rebuild_from_tree()
                        },
                        on_move = { node, direction ->
                            val parent = node.parent
                            if (parent != null) {
                                val index = parent.children.indexOf(node)
                            val new_index = index + direction
                            if (new_index in parent.children.indices) {
                                parent.children.removeAt(index)
                                parent.children.add(new_index, node)
                                tree = tree.deep_copy().also { selected = find_node(it, node) }
                                rebuild_from_tree()
                                }
                            }
                        },
                        clipboard_node = clipboard,
                        modifier = Modifier.width(220.dp).fillMaxHeight()
                    )
                }
            }

            // 右抽屉：属性面板（从右滑入，浮层）
            androidx.compose.animation.AnimatedVisibility(
                visible = right_open,
                enter = androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutHorizontally { it } + androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            ) {
                Surface(shadowElevation = 8.dp, color = colors.editor_bg) {
                    PropertyDrawer(
                        node = selected,
                        is_root = selected === tree,
                        on_change = {
                            tree = tree.deep_copy().also { selected = find_node(it, selected!!) }
                            rebuild_from_tree()
                        },
                        on_delete = {
                            if (selected != null && selected !== tree) {
                                remove_node(tree, selected!!)
                                selected = null
                                tree = tree.deep_copy()
                                rebuild_from_tree()
                            }
                        },
                        project_dir = project_dir,
                        on_open_event = { component_id, event_type ->
                            selected?.tag?.let { tag ->
                                on_open_event(serialize(tree), component_id, event_type, tag)
                            }
                        },
                        modifier = Modifier.width(240.dp).fillMaxHeight()
                    )
                }
            }
        }
    }

private fun find_by_id(root: d_node, id: String): d_node? {
    if (root.id == id) return root
    root.children.forEach { c -> find_by_id(c, id)?.let { return it } }
    return null
}

/* 左抽屉：组件树 + 添加面板 */
@Composable
private fun ComponentTree(
    root: d_node,
    selected: d_node?,
    on_select: (d_node) -> Unit,
    on_add: (String) -> Unit,
    on_copy: (d_node) -> Unit,
    on_paste: (d_node) -> Unit,
    on_delete: (d_node) -> Unit,
    on_move: (d_node, Int) -> Unit,
    clipboard_node: d_node?,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        Text(
            "组件树",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = app_theme_provider.colors.editor_icon,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        // 树形列表
        TreeRow(root, 0, selected, on_select, on_copy, on_paste, on_delete, on_move, clipboard_node != null, clipboard_node?.tag)
        // 折叠卡片：组件 / 布局
        Spacer(Modifier.height(10.dp))
        ExpandableCard(title = "添加组件", modifier = Modifier.padding(horizontal = 10.dp)) {
            AddList(tags = WIDGET_TAGS, on_add = on_add)
        }
        Spacer(Modifier.height(6.dp))
        ExpandableCard(title = "添加布局", modifier = Modifier.padding(horizontal = 10.dp)) {
            AddList(tags = LAYOUT_TAGS, on_add = on_add)
        }
    }
}

@Composable
private fun TreeRow(
    node: d_node,
    depth: Int,
    selected: d_node?,
    on_select: (d_node) -> Unit,
    on_copy: (d_node) -> Unit,
    on_paste: (d_node) -> Unit,
    on_delete: (d_node) -> Unit,
    on_move: (d_node, Int) -> Unit,
    has_clipboard: Boolean,
    clipboard_tag: String?
) {
    val is_sel = node === selected
    val is_root = depth == 0
    val colors = app_theme_provider.colors
    var menu_open by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { on_select(node) },
                    onLongClick = { menu_open = true }
                )
                .background(if (is_sel) colors.editor_icon.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                .padding(start = (12 + depth * 16).dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.is_container) {
                Text("▾ ", fontSize = 10.sp, color = colors.editor_hint)
            } else {
                Text("· ", fontSize = 10.sp, color = colors.editor_hint)
            }
            Text(
                tag_cn(node.tag),
                fontSize = 12.sp,
                color = if (is_sel) colors.editor_icon else colors.editor_text,
                fontWeight = if (is_sel) FontWeight.Bold else FontWeight.Normal
            )
            node.id.takeIf { it.isNotBlank() }?.let {
                Text("  #$it", fontSize = 10.sp, color = colors.editor_hint)
            }
        }

        DropdownMenu(expanded = menu_open, onDismissRequest = { menu_open = false }) {
            DropdownMenuItem(
                text = { Text("复制", fontSize = 13.sp) },
                onClick = { on_copy(node); menu_open = false }
            )
            if (!is_root) {
                DropdownMenuItem(
                    text = { Text("删除", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)) },
                    onClick = { on_delete(node); menu_open = false }
                )
                DropdownMenuItem(
                    text = { Text("上移", fontSize = 13.sp) },
                    onClick = { on_move(node, -1); menu_open = false }
                )
                DropdownMenuItem(
                    text = { Text("下移", fontSize = 13.sp) },
                    onClick = { on_move(node, 1); menu_open = false }
                )
            }
            if (node.is_container && has_clipboard) {
                DropdownMenuItem(
                    text = { Text("粘贴${clipboard_tag?.let { " (${tag_cn(it)})" } ?: ""}", fontSize = 13.sp) },
                    onClick = { on_paste(node); menu_open = false }
                )
            }
        }
    }
    node.children.forEach { child ->
        TreeRow(child, depth + 1, selected, on_select, on_copy, on_paste, on_delete, on_move, has_clipboard, clipboard_tag)
    }
}

/* 折叠卡片（固定高度内容区可滚动） */
@Composable
private fun ExpandableCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val colors = app_theme_provider.colors
    Column(modifier) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(10.dp),
            color = colors.editor_button_bg
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 12.sp, color = colors.editor_text, modifier = Modifier.weight(1f))
                Text(if (expanded) "▾" else "▸", fontSize = 12.sp, color = colors.editor_hint)
            }
        }
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Column(
                Modifier.fillMaxWidth().height(180.dp).verticalScroll(rememberScrollState()).padding(top = 4.dp)
            ) {
                content()
            }
        }
    }
}

/* 添加列表：单行展示，空间更紧凑 */
@Composable
private fun AddList(tags: List<String>, on_add: (String) -> Unit) {
    val colors = app_theme_provider.colors
    tags.forEach { tag ->
        Surface(
            onClick = { on_add(tag) },
            shape = RoundedCornerShape(8.dp),
            color = colors.editor_button_bg.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        ) {
            Text(
                tag_cn(tag),
                fontSize = 11.sp,
                color = colors.editor_text,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)
            )
        }
    }
}

/* 左抽屉：组件列表 */
/* 中间：实时预览（原生渲染 + 点选） */
@Composable
private fun RealtimePreview(
    xml: String,
    project_dir: File,
    revision: Int,
    on_select: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<runtime_layout_loader.Result?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // 选中高亮色（半透明青蓝，与工作区强调色一致）
    val highlight = 0x335CCFE6.toInt()

    LaunchedEffect(xml, revision) {
        error = null
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val tmp = File.createTempFile("design", ".xml", context.cacheDir)
                tmp.writeText(xml)
                val r = runtime_layout_loader(context).load(tmp, project_dir)
                tmp.delete()
                r
            }
        }.fold(onSuccess = { result = it }, onFailure = { error = it.message })
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        error?.let {
            Text("解析错误: $it", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
        }
        result?.let { r ->
            var selected_id by remember { mutableStateOf<String?>(null) }
            AndroidView(
                factory = { ctx ->
                    val frame = FrameLayout(ctx)
                    frame.setBackgroundColor(0xFFF5F5F8.toInt())
                    frame
                },
                update = { frame ->
                    frame.removeAllViews()
                    (r.root.parent as? ViewGroup)?.removeView(r.root)
                    frame.addView(r.root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    r.views.forEach { (id, view) ->
                        // 所有可点控件：点击=选中高亮，不触发输入/长按
                        view.isFocusable = false
                        view.isFocusableInTouchMode = false
                        view.isLongClickable = false
                        if (view is android.widget.EditText) {
                            view.isCursorVisible = false
                            view.setInputType(android.text.InputType.TYPE_NULL)
                            view.keyListener = null
                        }
                        view.setOnClickListener {
                            // 清除旧高亮
                            r.views[selected_id]?.setBackgroundColor(0x00000000)
                            // 设置新高亮
                            view.setBackgroundColor(highlight)
                            selected_id = id
                            on_select(id)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/* 右抽屉：属性编辑 */
@Composable
private fun PropertyDrawer(
    node: d_node?,
    is_root: Boolean,
    project_dir: File,
    on_change: () -> Unit,
    on_delete: () -> Unit,
    on_open_event: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    val defined_attrs = attrs_for(node?.tag ?: "", node?.parent?.tag)
    val attr_list = defined_attrs + node?.attrs?.keys.orEmpty().filter { it !in defined_attrs }

    var show_delete_confirm by remember { mutableStateOf(false) }

    // 删除确认弹窗
    if (show_delete_confirm) {
        AlertDialog(
            onDismissRequest = { show_delete_confirm = false },
            title = { Text("删除组件", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("确定删除 ${tag_cn(node?.tag ?: "")}${node?.id?.takeIf { it.isNotBlank() }?.let { " (#$it)" } ?: ""} 吗？", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    show_delete_confirm = false
                    on_delete()
                }) { Text("删除", color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { show_delete_confirm = false }) { Text("取消") }
            },
            containerColor = app_theme_provider.colors.editor_bg,
            titleContentColor = app_theme_provider.colors.editor_text,
            textContentColor = app_theme_provider.colors.editor_hint
        )
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(10.dp)) {
        if (node == null) {
            Text("未选中组件\n点击预览或组件树", fontSize = 11.sp, color = app_theme_provider.colors.editor_hint)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(node.tag, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = app_theme_provider.colors.editor_icon)
            node.id.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(6.dp))
                Text("#$it", fontSize = 11.sp, color = app_theme_provider.colors.editor_hint)
            }
            Spacer(Modifier.weight(1f))
            if (!is_root) {
                IconButton(onClick = { show_delete_confirm = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = androidx.compose.ui.graphics.Color(0xFFFF6B6B), modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        var editing_attr by remember(node) { mutableStateOf<String?>(null) }
        var expanded_group by remember(node) { mutableStateOf<String?>(null) }
        property_groups_for(attr_list).forEach { group ->
            PropertyGroupCard(
                title = group.title,
                attrs = group.attrs,
                node = node,
                expanded = expanded_group == group.title,
                on_toggle = {
                    expanded_group = if (expanded_group == group.title) null else group.title
                },
                on_edit = { editing_attr = it }
            )
        }

        if (!is_root) {
            Spacer(Modifier.height(10.dp))
            Text(
                "事件",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.editor_icon,
                modifier = Modifier.padding(start = 2.dp)
            )
            Spacer(Modifier.height(5.dp))
            if (node.id.isBlank()) {
                Text(
                    "先设置 ID 后才能生成事件",
                    fontSize = 10.sp,
                    color = colors.editor_hint,
                    modifier = Modifier.padding(start = 2.dp)
                )
            } else {
                component_events(node.tag).forEach { event ->
                    Surface(
                        onClick = { on_open_event(node.id, event.first) },
                        shape = RoundedCornerShape(8.dp),
                        color = colors.editor_icon.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Text(
                            event.second,
                            fontSize = 11.sp,
                            color = colors.editor_icon,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }

        editing_attr?.let { attr ->
            PropertyEditDialog(
                node = node,
                attr = attr,
                project_dir = project_dir,
                on_confirm = { newValue ->
                    if (newValue.isBlank()) node.attrs.remove(attr) else node.attrs[attr] = newValue
                    editing_attr = null
                    // 确认后立即刷新 XML 与预览
                    on_change()
                },
                on_dismiss = { editing_attr = null }
            )
        }
    }
}


/* 属性折叠面板：标题固定，内容区固定高度并独立滚动 */
@Composable
private fun PropertyGroupCard(
    title: String,
    attrs: List<String>,
    node: d_node,
    expanded: Boolean,
    on_toggle: () -> Unit,
    on_edit: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Surface(
            onClick = on_toggle,
            shape = RoundedCornerShape(8.dp),
            color = colors.editor_button_bg.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.editor_text,
                    modifier = Modifier.weight(1f)
                )
                Text("${attrs.size} 项", fontSize = 9.sp, color = colors.editor_hint)
                Spacer(Modifier.width(6.dp))
                Text(if (expanded) "▾" else "▸", fontSize = 11.sp, color = colors.editor_hint)
            }
        }
        if (expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp)
            ) {
                attrs.forEach { attr ->
                    PropertyRow(
                        attr = attr,
                        value = node.attrs[attr] ?: "",
                        node = node,
                        on_edit = on_edit
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    attr: String,
    value: String,
    node: d_node,
    on_edit: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    Surface(
        onClick = { on_edit(attr) },
        shape = RoundedCornerShape(8.dp),
        color = colors.editor_button_bg.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                ATTR_CN[attr] ?: attr,
                fontSize = 11.sp,
                color = colors.editor_text,
                modifier = Modifier.weight(1f)
            )
            if (property_spec_for(attr).kind == property_kind.COLOR && value.isNotBlank()) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(parse_preview_color(value), RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                property_display(attr, value),
                fontSize = 10.sp,
                color = if (value.isBlank()) colors.editor_hint else colors.editor_icon,
                maxLines = 1
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "修改",
                tint = colors.editor_hint,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/* 属性弹窗：按属性类型提供单选、多选、开关、滑杆、颜色与文本编辑 */
@Composable
private fun PropertyEditDialog(
    node: d_node,
    attr: String,
    project_dir: File,
    on_confirm: (String) -> Unit,
    on_dismiss: () -> Unit
) {
    val spec = property_spec_for(attr)
    val initial = node.attrs[attr] ?: ""
    var text by remember(attr, node) { mutableStateOf(initial) }
    var flags by remember(attr, node) {
        mutableStateOf(initial.split("|", ",", " ").filter { it.isNotBlank() })
    }
    var number by remember(attr, node) {
        mutableFloatStateOf(initial.toFloatOrNull() ?: spec.min)
    }
    var checked by remember(attr, node) { mutableStateOf(initial.equals("true", true) || initial == "1") }

    AlertDialog(
        onDismissRequest = on_dismiss,
        title = { Text(ATTR_CN[attr] ?: attr, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                when (spec.kind) {
                    property_kind.SIZE -> {
                        var custom by remember(attr, node) { mutableStateOf(initial.isNotBlank() && initial !in SIZE_OPTIONS) }
                        spec.options.forEach { option ->
                            Row(
                                Modifier.fillMaxWidth().clickable { custom = false; text = option }.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = !custom && text == option, onClick = { custom = false; text = option })
                                Text(property_display(attr, option), fontSize = 13.sp)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { custom = true; if (text in SIZE_OPTIONS) text = "" }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = custom, onClick = { custom = true; if (text in SIZE_OPTIONS) text = "" })
                            Text("自定义尺寸", fontSize = 13.sp)
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            enabled = custom,
                            singleLine = true,
                            label = { Text("如 96dp") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    property_kind.SINGLE_CHOICE -> spec.options.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().clickable { text = option }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = text == option, onClick = { text = option })
                            Text(property_display(attr, option), fontSize = 13.sp)
                        }
                    }
                    property_kind.MULTI_CHOICE -> {
                        Text("可多选，组合后作用于布局", fontSize = 10.sp, color = app_theme_provider.colors.editor_hint)
                        spec.options.forEach { option ->
                            val checked_now = option in flags
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    flags = if (checked_now) flags - option else flags + option
                                    text = flags.joinToString("|")
                                }.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked_now,
                                    onCheckedChange = {
                                        flags = if (it) flags + option else flags - option
                                        text = flags.joinToString("|")
                                    }
                                )
                                Text(property_display(attr, option), fontSize = 13.sp)
                            }
                        }
                    }
                    property_kind.BOOLEAN -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = checked, onCheckedChange = { checked = it; text = it.toString() })
                        Spacer(Modifier.width(8.dp))
                        Text(if (checked) "开启" else "关闭", fontSize = 13.sp)
                    }
                    property_kind.NUMBER, property_kind.DIMENSION -> {
                        if (spec.kind == property_kind.NUMBER) {
                            Slider(
                                value = number,
                                onValueChange = { number = (it / spec.step).roundToInt() * spec.step },
                                valueRange = spec.min..spec.max,
                                steps = if (spec.step >= (spec.max - spec.min)) 0 else (((spec.max - spec.min) / spec.step).toInt() - 2).coerceAtLeast(0)
                            )
                            Text("当前：${format_number(number)}", fontSize = 11.sp, color = app_theme_provider.colors.editor_hint)
                            Spacer(Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = if (spec.kind == property_kind.NUMBER) {
                                format_number(number)
                            } else text,
                            onValueChange = {
                                text = it
                                it.toFloatOrNull()?.let { v -> number = v.coerceIn(spec.min, spec.max) }
                            },
                            singleLine = true,
                            label = { Text(if (spec.kind == property_kind.DIMENSION) "如 16${spec.unit}" else "数值") },
                            keyboardOptions = if (spec.kind == property_kind.NUMBER) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    property_kind.IMAGE -> {
                        val context = LocalContext.current
                        val image_launcher = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri ->
                            if (uri != null) {
                                val saved = copy_selected_image(context, uri, project_dir)
                                if (saved != null) text = saved
                            }
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            label = { Text("项目相对路径，如 images/logo.png") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { image_launcher.launch("image/*") }) {
                            Text("从相册 / 文件选择", color = app_theme_provider.colors.editor_icon)
                        }
                    }
                    property_kind.COLOR -> {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            label = { Text("颜色值，如 #5CCFE6") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("#1B1C1F", "#FFFFFF", "#5CCFE6", "#FF6B6B", "#66BB6A", "#FFCA28").forEach { color ->
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .background(parse_preview_color(color), RoundedCornerShape(6.dp))
                                        .clickable { text = color }
                                )
                            }
                        }
                    }
                    property_kind.MULTILINE_TEXT -> OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    property_kind.TEXT -> OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        label = { Text("内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { on_confirm("") }) { Text("清除", color = app_theme_provider.colors.editor_hint) }
                TextButton(onClick = {
                    val result = when (spec.kind) {
                        property_kind.BOOLEAN -> checked.toString()
                        property_kind.NUMBER -> format_number(number)
                        else -> text
                    }
                    on_confirm(result)
                }) { Text("确定", color = app_theme_provider.colors.editor_icon) }
            }
        },
        dismissButton = { TextButton(onClick = on_dismiss) { Text("取消") } },
        containerColor = app_theme_provider.colors.editor_bg,
        titleContentColor = app_theme_provider.colors.editor_text,
        textContentColor = app_theme_provider.colors.editor_text
    )
}

private fun format_number(value: Float): String =
    if (value % 1f == 0f) value.roundToInt().toString() else {
        val one_decimal = (value * 10f).roundToInt() / 10f
        one_decimal.toString()
    }

private fun copy_selected_image(context: android.content.Context, uri: android.net.Uri, project_dir: File): String? {
    val dir = File(project_dir, "images").apply { mkdirs() }
    val extension = when (context.contentResolver.getType(uri)?.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "jpg"
    }
    val target = File(dir, "image_${System.currentTimeMillis()}.$extension")
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        "images/${target.name}"
    }.getOrNull()
}

private fun parse_preview_color(value: String): androidx.compose.ui.graphics.Color =
    runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(value)) }
        .getOrDefault(androidx.compose.ui.graphics.Color(0xFF5CCFE6.toInt()))
