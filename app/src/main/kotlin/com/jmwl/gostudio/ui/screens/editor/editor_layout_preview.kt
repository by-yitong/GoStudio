package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import com.jmwl.gostudio.ui.theme.app_theme_provider
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.StringReader
import java.io.StringWriter
import kotlin.math.max

/* ---------- 数据模型 ---------- */

private class design_node(
    val tag: String,
    val attrs: MutableMap<String, String> = mutableMapOf(),
    val children: MutableList<design_node> = mutableListOf()
) {
    var id: String get() = attrs["id"] ?: ""
        set(v) { attrs["id"] = v }
    val is_container get() = tag in CONTAINER_TAGS
}

private val CONTAINER_TAGS = setOf("LinearLayout", "FrameLayout", "RelativeLayout", "ScrollView", "HorizontalScrollView")
private val PALETTE = listOf("TextView", "EditText", "Button", "ImageView", "CheckBox", "Switch", "LinearLayout", "ScrollView")

/* ---------- XML 解析/序列化 ---------- */

private fun parse_design_xml(xml: String): design_node {
    val parser = android.util.Xml.newPullParser()
    parser.setInput(StringReader(xml))
    var event = parser.eventType
    var root: design_node? = null
    val stack = ArrayDeque<design_node>()
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                val node = design_node(parser.name)
                for (i in 0 until parser.attributeCount) {
                    node.attrs[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                }
                if (root == null) root = node else stack.lastOrNull()?.children?.add(node)
                stack.addLast(node)
            }
            XmlPullParser.END_TAG -> stack.removeLastOrNull()
        }
        event = parser.next()
    }
    return root ?: design_node("LinearLayout")
}

private fun serialize_design_xml(node: design_node): String {
    val writer = StringWriter()
    val serializer = android.util.Xml.newSerializer()
    serializer.setOutput(writer)
    serializer.startDocument("utf-8", null)
    write_node(serializer, node)
    serializer.endDocument()
    return writer.toString()
}

private fun write_node(serializer: XmlSerializer, node: design_node) {
    serializer.startTag("", node.tag)
    node.attrs.forEach { (k, v) -> serializer.attribute("", k, v) }
    node.children.forEach { write_node(serializer, it) }
    serializer.endTag("", node.tag)
}

/* ---------- 设计器 ---------- */

@Composable
fun editor_layout_preview(
    xml_content: String,
    on_close: () -> Unit,
    on_content_change: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    var root by remember(xml_content) { mutableStateOf(parse_design_xml(xml_content)) }
    var selected by remember { mutableStateOf<design_node?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // 每次 xml_content 变化重新解析（从代码切回来时同步）
    LaunchedEffect(xml_content) {
        runCatching { root = parse_design_xml(xml_content) }
            .onFailure { error = it.message }
    }

    fun commit() {
        runCatching { on_content_change(serialize_design_xml(root)) }
            .onFailure { error = it.message }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.editor_bg)) {
        // 顶栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("可视化设计", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.editor_text)
            Spacer(Modifier.width(10.dp))
            Text("点控件添加 · 点画布选中 · 底部改属性", fontSize = 10.sp, color = colors.editor_hint, modifier = Modifier.weight(1f))
            error?.let { Text(it, fontSize = 9.sp, color = Color(0xFFFF6B6B), maxLines = 1) }
            IconButton(onClick = on_close, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = colors.editor_icon, modifier = Modifier.size(17.dp))
            }
        }
        HorizontalDivider(color = colors.editor_divider.copy(alpha = 0.5f))

        // 画布（线框图，点击选中）
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().pointerInput(root, selected) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pos = event.changes.firstOrNull()?.position
                        if (pos != null && event.changes.first().pressed) {
                            selected = hit_test(root, Offset(pos.x, pos.y))
                        }
                    }
                }
            }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                draw_node(root, 0f, 0f, size.width, null, selected)
            }
        }

        // 属性面板
        selected?.let { sel ->
            HorizontalDivider(color = colors.editor_divider.copy(alpha = 0.5f))
            design_property_panel(
                node = sel,
                is_root = sel === root,
                on_delete = {
                    if (sel !== root) {
                        remove_node(root, sel)
                        selected = null
                        commit()
                    }
                },
                on_change = {
                    root = root.copy_tree() // 触发重组
                    commit()
                }
            )
        }

        // 控件面板
        HorizontalDivider(color = colors.editor_divider.copy(alpha = 0.5f))
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            items(PALETTE) { tag ->
                AssistChip(
                    onClick = {
                        val target = selected?.takeIf { it.is_container } ?: root
                        val node = design_node(tag, default_attrs(tag, count_id(root, tag)))
                        target.children.add(node)
                        selected = node
                        commit()
                    },
                    label = { Text(tag, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(13.dp)) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

/* ---------- 线框绘制 ---------- */

private fun estimate_height(node: design_node, density: Float): Float {
    return when {
        node.is_container -> 24f * density + node.children.sumOf { estimate_height(it, density).toInt() }.toFloat()
        node.tag == "EditText" -> 52f * density
        node.tag == "Button" -> 48f * density
        else -> 40f * density
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.draw_node(
    node: design_node,
    x: Float, y: Float, width: Float,
    depth_parent: design_node?,
    selected: design_node?,
    density: Float = 2.5f
): Float {
    val h = estimate_height(node, density)
    val is_sel = node === selected
    val stroke = if (is_sel) Color(0xFF6C8CFF) else if (node.is_container) Color(0xFF5C6BC0) else Color(0xFF9E9E9E)
    val fill = if (node.is_container) Color(0x118C9EFF) else Color(0x11FFFFFF)
    drawRect(fill, topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(width, h))
    drawRect(stroke, topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(width, h), style = androidx.compose.ui.graphics.drawscope.Stroke(if (is_sel) 3f else 1.5f))
    // 标签：tag + id
    val label = "${node.tag}${if (node.id.isNotBlank()) " #${node.id}" else ""}"
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(200, 160, 170, 190)
        textSize = 10f * density
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(label, x + 8f * density, y + 14f * density, paint)
    node.rect = Rect(x, y, x + width, y + h)
    // 子节点垂直排列（简化预览）
    if (node.is_container) {
        var cy = y + 24f * density
        node.children.forEach { child ->
            val cw = if (child.attrs["layout_width"] == "match_parent") width else width * 0.6f
            val cx = if (child.attrs["layout_gravity"] == "center") x + (width - cw) / 2 else x + 8f * density
            cy += draw_node(child, cx, cy, cw, node, selected, density)
        }
    }
    return h
}

// 给 design_node 挂 rect 便于命中测试（非序列化字段）
private var design_node.rect: Rect
    get() = tag_rects.getOrPut(this) { Rect.Zero }
    set(v) { tag_rects[this] = v }
private val tag_rects = java.util.WeakHashMap<design_node, Rect>()

private fun hit_test(root: design_node, pos: Offset): design_node? {
    var best: design_node? = null
    var best_area = Float.MAX_VALUE
    fun walk(node: design_node) {
        val r = node.rect
        if (pos.x >= r.left && pos.x <= r.right && pos.y >= r.top && pos.y <= r.bottom) {
            val area = (r.right - r.left) * (r.bottom - r.top)
            if (area < best_area) { best = node; best_area = area }
        }
        node.children.forEach(::walk)
    }
    walk(root)
    return best
}

private fun remove_node(root: design_node, target: design_node) {
    fun walk(node: design_node): Boolean {
        if (node.children.remove(target)) return true
        return node.children.any(::walk)
    }
    walk(root)
}

private fun count_id(root: design_node, tag: String): Int {
    var count = 0
    fun walk(node: design_node) {
        if (node.tag == tag) count++
        node.children.forEach(::walk)
    }
    walk(root)
    return count
}

private fun default_attrs(tag: String, index: Int): MutableMap<String, String> {
    val attrs = mutableMapOf(
        "layout_width" to "wrap_content",
        "layout_height" to "wrap_content",
        "layout_marginTop" to "8dp"
    )
    if (tag == "LinearLayout") attrs["orientation"] = "vertical"
    if (tag == "ScrollView") attrs["layout_height"] = "match_parent"
    if (tag in setOf("TextView", "EditText", "Button")) attrs["id"] = "${tag.lowercase()}$index"
    if (tag == "TextView") attrs["text"] = "文本"
    if (tag == "Button") attrs["text"] = "按钮"
    if (tag == "EditText") attrs["hint"] = "输入"
    return attrs
}

private fun design_node.copy_tree(): design_node {
    val copy = design_node(tag, attrs.toMutableMap())
    children.forEach { copy.children.add(it.copy_tree()) }
    return copy
}

/* ---------- 属性面板 ---------- */

@Composable
private fun design_property_panel(
    node: design_node,
    is_root: Boolean,
    on_delete: () -> Unit,
    on_change: () -> Unit
) {
    val colors = app_theme_provider.colors
    val text_attrs = listOf("id", "text", "hint", "textSize", "textColor", "background")
    val layout_attrs = listOf("layout_width", "layout_height", "layout_marginTop", "layout_marginBottom", "layout_gravity", "orientation")
    val editable = text_attrs + layout_attrs

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${node.tag}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.editor_icon)
            node.id.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(6.dp))
                Text("#$it", fontSize = 11.sp, color = colors.editor_hint)
            }
            Spacer(Modifier.weight(1f))
            if (!is_root) {
                IconButton(onClick = on_delete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = Color(0xFFFF6B6B), modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 属性网格
        editable.chunked(2).forEach { row_attrs ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row_attrs.forEach { attr ->
                    var value by remember(node, attr) { mutableStateOf(node.attrs[attr] ?: "") }
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            value = it
                            if (it.isBlank()) node.attrs.remove(attr) else node.attrs[attr] = it
                        },
                        label = { Text(attr, fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                }
                if (row_attrs.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Button(
            onClick = on_change,
            modifier = Modifier.fillMaxWidth().height(36.dp).padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp)
        ) { Text("应用到代码", fontSize = 12.sp) }
    }
}
