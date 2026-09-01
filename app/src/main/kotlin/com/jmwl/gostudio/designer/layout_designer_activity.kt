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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.jmwl.gostudio.runtime.runtime_layout_loader
import com.jmwl.gostudio.ui.theme.app_theme_provider
import java.io.File

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

        setContent {
            MaterialTheme {
                designer_screen(
                    initial_xml = initial,
                    on_back = { finish() },
                    on_save = { xml ->
                        layout_file.writeText(xml)
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_SAVED, true))
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PROJECT_DIR = "project_dir"
        const val EXTRA_SAVED = "saved"
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

private val CONTAINERS = setOf("LinearLayout", "FrameLayout", "RelativeLayout", "ScrollView", "HorizontalScrollView")
private val PALETTE = listOf("TextView", "EditText", "Button", "ImageView", "CheckBox", "Switch", "LinearLayout", "ScrollView")

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
        "layout_marginTop" to "8dp"
    )
    when (tag) {
        "LinearLayout" -> { m["orientation"] = "vertical"; m["id"] = next_id(root, tag) }
        "ScrollView" -> { m["layout_height"] = "match_parent" }
        else -> m["id"] = next_id(root, tag)
    }
    when (tag) {
        "TextView" -> m["text"] = "文本"
        "Button" -> m["text"] = "按钮"
        "EditText" -> m["hint"] = "输入"
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
    root.children.forEach { c -> find_node(c, target)?.let { return it } }
    return null
}

/* ---------- 设计器界面 ---------- */

@Composable
private fun designer_screen(
    initial_xml: String,
    on_back: () -> Unit,
    on_save: (String) -> Unit
) {
    var xml by remember { mutableStateOf(initial_xml) }
    var tree by remember { mutableStateOf(parse_xml(initial_xml)) }
    var selected by remember { mutableStateOf<d_node?>(null) }
    var preview_revision by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    fun rebuild_from_tree() {
        xml = serialize(tree)
        preview_revision++
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = on_back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                    Text("布局设计器", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { on_save(serialize(tree)) }) { Text("保存", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            // 左抽屉：组件面板（宽 120dp）
            ComponentPalette(
                on_add = { tag ->
                    val target = selected?.takeIf { it.is_container } ?: tree
                    val node = d_node(tag, default_attrs(tag, tree))
                    node.parent = target
                    target.children.add(node)
                    selected = node
                    tree = tree.deep_copy().also { selected = find_node(it, node) }
                    rebuild_from_tree()
                },
                modifier = Modifier.width(120.dp).fillMaxHeight()
            )
            // 中间：实时预览（点选控件）
            Box(Modifier.weight(1f).fillMaxHeight()) {
                RealtimePreview(
                    xml = xml,
                    revision = preview_revision,
                    on_select = { id -> selected = find_by_id(tree, id) }
                )
            }
            // 右抽屉：属性面板（宽 240dp）
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
                modifier = Modifier.width(240.dp).fillMaxHeight()
            )
        }
    }
}

private fun find_by_id(root: d_node, id: String): d_node? {
    if (root.id == id) return root
    root.children.forEach { c -> find_by_id(c, id)?.let { return it } }
    return null
}

/* 左抽屉：组件列表 */
@Composable
private fun ComponentPalette(on_add: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
        Text("组件", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        PALETTE.forEach { tag ->
            Surface(
                onClick = { on_add(tag) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tag, fontSize = 12.sp)
                }
            }
        }
    }
}

/* 中间：实时预览（原生渲染 + 点选） */
@Composable
private fun RealtimePreview(xml: String, revision: Int, on_select: (String) -> Unit) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<runtime_layout_loader.Result?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(xml, revision) {
        error = null
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val tmp = File.createTempFile("design", ".xml", context.cacheDir)
                tmp.writeText(xml)
                val r = runtime_layout_loader(context).load(tmp)
                tmp.delete()
                r
            }
        }.fold(onSuccess = { result = it }, onFailure = { error = it.message })
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        error?.let {
            Text("解析错误: $it", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
        }
        result?.let { r ->
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
                        view.setOnClickListener { on_select(id) }
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
    on_change: () -> Unit,
    on_delete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val attr_list = listOf(
        "id", "text", "hint", "textSize", "textColor", "background",
        "layout_width", "layout_height", "layout_marginTop", "layout_marginBottom",
        "layout_gravity", "gravity", "orientation", "padding"
    )

    Column(modifier.verticalScroll(rememberScrollState()).padding(10.dp)) {
        if (node == null) {
            Text("未选中组件\n点击预览中的控件", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(node.tag, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            node.id.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(6.dp))
                Text("#$it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            if (!is_root) {
                IconButton(onClick = on_delete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        attr_list.forEach { attr ->
            var value by remember(node, attr) { mutableStateOf(node.attrs[attr] ?: "") }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    if (it.isBlank()) node.attrs.remove(attr) else node.attrs[attr] = it
                },
                label = { Text(attr, fontSize = 9.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = on_change, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Text("应用", fontSize = 12.sp)
        }
    }
}
