package com.jmwl.gostudio.designer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
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
import com.jmwl.gostudio.runtime.runtime_layout_loader
import com.jmwl.gostudio.runtime.strip_xml_comments
import com.jmwl.gostudio.ui.toast.app_toast
import com.jmwl.gostudio.ui.theme.app_theme_provider
import java.io.File
import kotlin.math.roundToInt

/**
 * AndLua 式布局设计器（独立页面，底部弹窗一体化交互）：
 * 中间=实时预览（点选组件弹底部属性面板，就地编辑即改即生效）/ 顶栏=组件树、条件添加按钮、保存。
 * 属性未覆盖的类型经「更多属性」走原对话框兜底；添加目录与组件树全部走底部弹窗（见 layout_designer_sheets.kt）。
 * 通过 Intent 传入项目目录，读取 layout.xml；返回时写回结果。
 * 可选 EXTRA_LAYOUT_FILE 指定编辑项目内其他 xml 布局文件（默认 layout.xml）。
 */
class layout_designer_activity : androidx.activity.ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val project_dir = intent.getStringExtra(EXTRA_PROJECT_DIR) ?: run { finish(); return }
        val layout_file = intent.getStringExtra(EXTRA_LAYOUT_FILE)
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?: File(project_dir, "layout.xml")
        val initial = if (layout_file.isFile) layout_file.readText() else DEFAULT_LAYOUT
        // 布局有语法错误时进不去设计器：提示后返回编辑器修 XML，不崩溃也不误存空布局
        val initial_tree = try {
            parse_xml(initial)
        } catch (e: Exception) {
            app_toast.show(this, "布局 XML 语法错误：${e.message}", app_toast.LENGTH_LONG)
            finish()
            return
        }

        // 沉浸式系统栏：透明状态栏 + 由主题决定图标深浅（app_theme_provider 内统一处理）
        enableEdgeToEdge()
        setContent {
            app_theme_provider {
                designer_screen(
                    initial_xml = initial,
                    initial_tree = initial_tree,
                    project_dir = File(project_dir),
                    on_save = { xml ->
                        layout_file.writeText(xml)
                        setResult(RESULT_OK, saved_result(layout_file))
                        finish()
                    },
                    on_open_event = { xml, component_id, event_type, component_tag ->
                        layout_file.writeText(xml)
                        setResult(
                            RESULT_OK,
                            saved_result(layout_file)
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
        const val EXTRA_LAYOUT_FILE = "layout_file"
        const val EXTRA_SAVED = "saved"

        /** 保存成功的返回 Intent：带上实际编辑的布局文件路径，供调用方定向刷新。 */
        private fun saved_result(layout_file: File): Intent =
            Intent().putExtra(EXTRA_SAVED, true).putExtra(EXTRA_LAYOUT_FILE, layout_file.absolutePath)
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

internal class d_node(
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
internal val WIDGET_TAGS = listOf(
    "TextView", "EditText", "AutoCompleteTextView", "Button", "ImageView", "ImageButton",
    "CheckBox", "RadioButton", "Switch", "ToggleButton", "ProgressBar", "SeekBar",
    "RatingBar", "Spinner", "ListView", "GridView", "DatePicker", "TimePicker",
    "CalendarView", "NumberPicker", "Chronometer", "TextClock", "VideoView",
    "WebView", "View", "Space"
)
internal val LAYOUT_TAGS = listOf(
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
internal val ATTR_CN = mapOf(
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

internal enum class property_kind { TEXT, MULTILINE_TEXT, SINGLE_CHOICE, MULTI_CHOICE, BOOLEAN, NUMBER, DIMENSION, COLOR, SIZE, IMAGE }

internal data class property_spec(
    val kind: property_kind,
    val options: List<String> = emptyList(),
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val unit: String = "dp"
)

internal val SIZE_OPTIONS = listOf("wrap_content", "match_parent")
private val GRAVITY_OPTIONS = listOf("top", "bottom", "left", "right", "center", "center_vertical", "center_horizontal")

/** 属性编辑器类型：枚举用单选/多选，数值用滑杆，尺寸/颜色/文本分别适配。 */
internal fun property_spec_for(attr: String): property_spec = when (attr) {
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

internal fun property_display(attr: String, value: String): String {
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
internal data class property_group(val title: String, val attrs: List<String>)

internal fun property_groups_for(attrs: List<String>): List<property_group> {
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

private val ADAPTER_TAGS = setOf("Spinner", "ListView", "GridView")

internal fun component_events(tag: String): List<Pair<String, String>> {
    val events = mutableListOf(
        // AdapterView 类组件无法触发普通 click，运行时改发 item_click
        if (tag in ADAPTER_TAGS) "item_click" to "条目点击事件" else "click" to "点击事件",
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

internal fun tag_cn(tag: String) = TAG_CN[tag] ?: tag

/** 按控件类型和父容器返回属性字段，避免给普通按钮显示无意义的日期/列表属性。 */
internal fun attrs_for(tag: String, parent_tag: String? = null): List<String> {
    val result = COMMON_ATTRS.toMutableList()
    if (tag == "TextView" || tag == "EditText" || tag == "AutoCompleteTextView" ||
        tag in setOf("Button", "CheckBox", "RadioButton", "Switch", "ToggleButton", "Chronometer", "TextClock")
    ) result.addAll(TEXT_ATTRS)
    if (tag in CONTAINERS) result.addAll(LAYOUT_ONLY_ATTRS + (CONTAINER_EXTRA_ATTRS[tag] ?: emptyList()))
    parent_tag?.let { result.addAll(PARENT_ATTRS[it] ?: emptyList()) }
    TAG_ATTRS[tag]?.let { result.addAll(it) }
    return result.distinct()
}

private fun parse_xml(xml: String): d_node? {
    val parser = android.util.Xml.newPullParser()
    parser.setInput(strip_xml_comments(xml).reader())
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
    // 空文档返回 null：设计器进入无根状态，添加的第一个组件将成为新根
    return root
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

private fun find_by_id(root: d_node, id: String): d_node? {
    if (root.id == id) return root
    root.children.forEach { c -> find_by_id(c, id)?.let { return it } }
    return null
}

/* ---------- 设计器界面 ---------- */

@Composable
private fun designer_screen(
    initial_xml: String,
    initial_tree: d_node?,
    project_dir: File,
    on_save: (String) -> Unit,
    on_open_event: (String, String, String, String) -> Unit
) {
    var xml by remember { mutableStateOf(initial_xml) }
    // tree 为 null 表示根布局已删除、布局被清空，此时添加的第一个组件将成为新根
    var tree by remember { mutableStateOf(initial_tree) }
    var selected by remember { mutableStateOf<d_node?>(null) }
    var preview_revision by remember { mutableIntStateOf(0) }
    var clipboard by remember { mutableStateOf<d_node?>(null) }
    var has_unsaved_changes by remember { mutableStateOf(false) }
    var show_exit_confirm by remember { mutableStateOf(false) }
    // 底部弹窗：属性面板 / 组件树 / 添加目录（0=组件 1=布局，null=关）
    var show_property_sheet by remember { mutableStateOf(false) }
    var show_tree_sheet by remember { mutableStateOf(false) }
    var catalog_section by remember { mutableStateOf<Int?>(null) }
    // 组件树里点选后让预览滚动到该组件可见
    var scroll_token by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val colors = app_theme_provider.colors

    fun current_xml(): String = tree?.let { serialize(it) } ?: ""

    fun rebuild_from_tree() {
        xml = current_xml()
        preview_revision++
        has_unsaved_changes = true
    }

    fun select(node: d_node?) {
        selected = node
    }

    /** 添加组件/布局；target 为空时落到选中容器（无则根节点），布局为空时创建新根。 */
    fun add_component(tag: String, target: d_node?, select_new: Boolean = true) {
        val cur = tree
        val node: d_node
        if (cur == null) {
            // 布局已清空：第一个添加的组件/布局直接作为新根节点（空占位树使 id 从 1 编号）
            node = d_node(tag, default_attrs(tag, d_node("")))
            tree = node
        } else {
            val t = target?.takeIf { it.is_container } ?: cur
            node = d_node(tag, default_attrs(tag, cur))
            node.parent = t
            t.children.add(node)
            tree = cur.deep_copy()
        }
        val copy = tree!!
        if (select_new) {
            select(find_node(copy, node))
        } else {
            // 保持当前选中（容器）指向重建后的新对象
            selected?.let { keep -> selected = find_node(copy, keep) }
        }
        rebuild_from_tree()
    }

    fun delete_component(node: d_node) {
        val cur = tree ?: return
        if (node === cur) {
            // 删除根布局：清空整个布局，之后添加的第一个组件成为新根
            tree = null
            select(null)
            show_property_sheet = false
        } else {
            remove_node(cur, node)
            tree = cur.deep_copy()
            if (selected === node) {
                select(null)
                show_property_sheet = false
            } else {
                selected?.let { keep -> selected = find_node(tree!!, keep) }
            }
        }
        rebuild_from_tree()
    }

    fun move_node(node: d_node, direction: Int) {
        val cur = tree ?: return
        val parent = node.parent ?: return
        val index = parent.children.indexOf(node)
        val new_index = index + direction
        if (new_index !in parent.children.indices) return
        parent.children.removeAt(index)
        parent.children.add(new_index, node)
        tree = cur.deep_copy()
        selected?.let { keep -> selected = find_node(tree!!, keep) }
        rebuild_from_tree()
    }

    fun paste_into(container: d_node) {
        val clip = clipboard ?: return
        val cur = tree ?: return
        val copy = clip.deep_copy()
        // 重置复制的 id 避免冲突
        if (copy.attrs.containsKey("id")) {
            copy.attrs["id"] = next_id(cur, copy.tag)
        }
        copy.parent = container
        container.children.add(copy)
        tree = cur.deep_copy()
        select(find_node(tree!!, copy))
        rebuild_from_tree()
    }

    BackHandler(enabled = has_unsaved_changes && !show_exit_confirm) {
        show_exit_confirm = true
    }
    // 弹窗打开时返回键先关弹窗（后注册者优先）
    val sheet_open = show_property_sheet || show_tree_sheet || catalog_section != null
    BackHandler(enabled = sheet_open) {
        show_property_sheet = false
        show_tree_sheet = false
        catalog_section = null
    }

    if (show_exit_confirm) {
        AlertDialog(
            onDismissRequest = { show_exit_confirm = false },
            title = { Text("保存修改", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("布局尚未保存，要保存后退出吗？", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    show_exit_confirm = false
                    on_save(current_xml())
                }) { Text("保存", color = colors.editor_icon) }
            },
            dismissButton = {
                TextButton(onClick = {
                    show_exit_confirm = false
                    has_unsaved_changes = false
                    (context as? Activity)?.finish()
                }) { Text("丢弃", color = colors.danger) }
            },
            containerColor = colors.editor_bg,
            titleContentColor = colors.editor_text,
            textContentColor = colors.editor_hint
        )
    }

    Column(Modifier.fillMaxSize().background(colors.editor_bg)) {
        // 顶栏：组件树 / 条件添加按钮 / 保存
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { show_tree_sheet = true },
                shape = RoundedCornerShape(50),
                color = colors.editor_button_bg,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.AccountTree,
                    contentDescription = "组件树",
                    tint = if (show_tree_sheet) colors.editor_icon else colors.editor_toolbar_icon,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            // 选中容器（或布局为空）时才有添加入口
            androidx.compose.animation.AnimatedVisibility(
                visible = selected?.is_container == true || tree == null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    top_bar_add_button("添加组件") { catalog_section = 0 }
                    Spacer(Modifier.width(8.dp))
                    top_bar_add_button("添加布局") { catalog_section = 1 }
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = {
                    has_unsaved_changes = false
                    on_save(current_xml())
                },
                shape = RoundedCornerShape(50),
                color = colors.editor_button_bg,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "保存",
                    tint = colors.editor_toolbar_icon,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
        }

        // 中间：实时预览（点选组件 → 弹底部属性面板）
        Box(Modifier.fillMaxWidth().weight(1f).navigationBarsPadding()) {
            RealtimePreview(
                xml = xml,
                project_dir = project_dir,
                revision = preview_revision,
                selected_id = selected?.id,
                scroll_token = scroll_token,
                on_select = { id ->
                    tree?.let { find_by_id(it, id) }?.let {
                        select(it)
                        show_property_sheet = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 属性面板：选中组件的属性行式列表（点行弹窗编辑）
    if (show_property_sheet && selected != null) {
        val node = selected!!
        designer_property_sheet(
            node = node,
            is_root = node === tree,
            project_dir = project_dir,
            on_attr_change = { attr, value ->
                val cur = tree
                val live = selected
                if (cur != null && live != null) {
                    if (value.isBlank()) live.attrs.remove(attr) else live.attrs[attr] = value
                    tree = cur.deep_copy()
                    selected = find_node(tree!!, live)
                    rebuild_from_tree()
                }
            },
            on_delete = { selected?.let { delete_component(it) } },
            on_add_child = { tag -> add_component(tag, target = selected, select_new = false) },
            on_open_catalog = {
                show_property_sheet = false
                catalog_section = 0
            },
            on_open_event = { component_id, event_type ->
                val cur = tree
                if (cur != null) {
                    on_open_event(serialize(cur), component_id, event_type, node.tag)
                }
            },
            on_dismiss = { show_property_sheet = false }
        )
    }

    // 添加目录：组件 24 / 布局 11 分组（中文名）
    catalog_section?.let { section ->
        designer_catalog_sheet(
            initial_section = section,
            has_root = tree != null,
            on_add = { tag ->
                catalog_section = null
                add_component(tag, target = selected?.takeIf { it.is_container })
            },
            on_dismiss = { catalog_section = null }
        )
    }

    // 组件树：与预览选中双向同步
    if (show_tree_sheet) {
        designer_tree_sheet(
            root = tree,
            selected = selected,
            has_clipboard = clipboard != null,
            clipboard_tag = clipboard?.tag,
            on_select = { node ->
                select(node)
                scroll_token++
            },
            on_copy = { node -> clipboard = node.deep_copy() },
            on_paste = { container -> paste_into(container) },
            on_delete = { node -> delete_component(node) },
            on_move = { node, direction -> move_node(node, direction) },
            on_dismiss = { show_tree_sheet = false }
        )
    }
}

/** 顶栏「添加组件 / 添加布局」胶囊按钮（选中容器或空布局时出现） */
@Composable
private fun top_bar_add_button(text: String, on_click: () -> Unit) {
    val colors = app_theme_provider.colors
    Surface(
        onClick = on_click,
        shape = RoundedCornerShape(50),
        color = colors.editor_icon.copy(alpha = 0.14f),
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp).heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = colors.editor_icon,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 12.sp, color = colors.editor_icon)
        }
    }
}

/* 中间：实时预览（原生渲染 + 点选 + overlay 选中描边） */
@Composable
private fun RealtimePreview(
    xml: String,
    project_dir: File,
    revision: Int,
    selected_id: String?,
    scroll_token: Int,
    on_select: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<runtime_layout_loader.Result?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val colors = app_theme_provider.colors
    val canvas_bg = colors.editor_bg.toArgb()
    val stroke_color = colors.editor_icon.toArgb()
    val fill_color = colors.editor_icon.copy(alpha = 0.16f).toArgb()

    LaunchedEffect(xml, revision) {
        error = null
        result = null
        // 布局被清空时不解析，显示空状态占位
        if (xml.isBlank()) return@LaunchedEffect
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

    // 从组件树选中时把预览滚动到该组件可见（无滚动父容器时无副作用）
    LaunchedEffect(scroll_token, selected_id) {
        if (scroll_token > 0 && !selected_id.isNullOrBlank()) {
            val view = result?.views?.get(selected_id) ?: return@LaunchedEffect
            view.post {
                view.requestRectangleOnScreen(android.graphics.Rect(0, 0, view.width, view.height))
            }
        }
    }

    Box(modifier.fillMaxSize().background(colors.editor_button_bg.copy(alpha = 0.3f))) {
        if (xml.isBlank()) {
            // 空布局占位提示
            Text(
                "布局为空\n点顶部「添加组件 / 添加布局」创建根节点",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        error?.let {
            Text("解析错误: $it", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
        }
        result?.let { r ->
            AndroidView(
                factory = { ctx ->
                    val frame = FrameLayout(ctx)
                    frame.setBackgroundColor(canvas_bg)
                    frame
                },
                update = { frame ->
                    frame.removeAllViews()
                    (r.root.parent as? ViewGroup)?.removeView(r.root)
                    frame.addView(r.root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    r.views.forEach { (id, view) ->
                        // 所有可点控件：点击=选中，不触发输入/长按
                        view.isFocusable = false
                        view.isFocusableInTouchMode = false
                        view.isLongClickable = false
                        if (view is android.widget.EditText) {
                            view.isCursorVisible = false
                            view.setInputType(android.text.InputType.TYPE_NULL)
                            view.keyListener = null
                        }
                        // AdapterView 禁止 setOnClickListener（系统会抛异常），改用触摸选中
                        if (view is android.widget.AdapterView<*>) {
                            view.setOnTouchListener { _, _ -> on_select(id); false }
                        } else {
                            view.setOnClickListener { on_select(id) }
                        }
                    }
                    // 选中高亮：前景 overlay 描边 + 半透明填充，不改控件背景
                    r.views.values.forEach { it.overlay.clear() }
                    selected_id?.takeIf { it.isNotBlank() }?.let { id ->
                        r.views[id]?.let { v ->
                            v.overlay.add(
                                selection_highlight_drawable(
                                    stroke_color = stroke_color,
                                    fill_color = fill_color,
                                    density = v.resources.displayMetrics.density
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 选中描边 Drawable：画在 ViewOverlay 之上，不污染控件自身背景 */
private class selection_highlight_drawable(
    private val stroke_color: Int,
    private val fill_color: Int,
    private val density: Float
) : android.graphics.drawable.Drawable() {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: android.graphics.Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val radius = density * 4f
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = fill_color
        canvas.drawRoundRect(0f, 0f, w, h, radius, radius, paint)
        val stroke = density * 2f
        val half = stroke / 2f
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = stroke_color
        paint.strokeWidth = stroke
        canvas.drawRoundRect(half, half, w - half, h - half, radius, radius, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(color_filter: android.graphics.ColorFilter?) {
        paint.colorFilter = color_filter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}

internal fun format_number(value: Float): String =
    if (value % 1f == 0f) value.roundToInt().toString() else {
        val one_decimal = (value * 10f).roundToInt() / 10f
        one_decimal.toString()
    }

internal fun copy_selected_image(context: android.content.Context, uri: android.net.Uri, project_dir: File): String? {
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

/** 解析用户布局里的颜色值（解析失败回退主题色，由调用方传入） */
internal fun parse_preview_color(value: String, fallback: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color =
    runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(value)) }
        .getOrDefault(fallback)
