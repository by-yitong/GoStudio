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

        // 状态栏颜色与工作区背景一致
        window.statusBarColor = android.graphics.Color.parseColor("#1B1C1F")
        window.navigationBarColor = android.graphics.Color.parseColor("#1B1C1F")

        setContent {
            MaterialTheme {
                designer_screen(
                    initial_xml = initial,
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
private val WIDGET_TAGS = listOf("TextView", "EditText", "Button", "ImageView", "CheckBox", "Switch")
private val LAYOUT_TAGS = listOf("LinearLayout", "FrameLayout", "RelativeLayout", "ScrollView", "HorizontalScrollView")
private val TAG_CN = mapOf(
    "LinearLayout" to "线性布局", "FrameLayout" to "帧布局", "RelativeLayout" to "相对布局",
    "ScrollView" to "滚动视图", "HorizontalScrollView" to "横向滚动",
    "TextView" to "文本", "EditText" to "输入框", "Button" to "按钮",
    "ImageView" to "图片", "CheckBox" to "复选框", "Switch" to "开关"
)
private val ATTR_CN = mapOf(
    "id" to "ID", "text" to "文本", "hint" to "提示", "textSize" to "字号",
    "textColor" to "文字颜色", "background" to "背景", "layout_width" to "宽度",
    "layout_height" to "高度", "layout_marginTop" to "上边距", "layout_marginBottom" to "下边距",
    "layout_gravity" to "位置", "gravity" to "内容对齐", "orientation" to "方向", "padding" to "内边距"
)
private fun tag_cn(tag: String) = TAG_CN[tag] ?: tag

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
    on_save: (String) -> Unit
) {
    var xml by remember { mutableStateOf(initial_xml) }
    var tree by remember { mutableStateOf(parse_xml(initial_xml)) }
    var selected by remember { mutableStateOf<d_node?>(null) }
    var preview_revision by remember { mutableIntStateOf(0) }
    var left_open by remember { mutableStateOf(true) }
    var right_open by remember { mutableStateOf(false) }
    var clipboard by remember { mutableStateOf<d_node?>(null) }
    val context = LocalContext.current
    val colors = app_theme_provider.colors

    fun rebuild_from_tree() {
        xml = serialize(tree)
        preview_revision++
    }

    Box(Modifier.fillMaxSize()) {
            // 中间：实时预览（全屏，点选控件后自动弹右抽屉）
            RealtimePreview(
                xml = xml,
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
                    onClick = { on_save(serialize(tree)) },
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
            AddGrid(tags = WIDGET_TAGS, on_add = on_add)
        }
        Spacer(Modifier.height(6.dp))
        ExpandableCard(title = "添加布局", modifier = Modifier.padding(horizontal = 10.dp)) {
            AddGrid(tags = LAYOUT_TAGS, on_add = on_add)
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

/* 添加网格（两列） */
@Composable
private fun AddGrid(tags: List<String>, on_add: (String) -> Unit) {
    val colors = app_theme_provider.colors
    tags.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { tag ->
                Surface(
                    onClick = { on_add(tag) },
                    shape = RoundedCornerShape(8.dp),
                    color = colors.editor_button_bg.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        tag_cn(tag),
                        fontSize = 11.sp,
                        color = colors.editor_text,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/* 左抽屉：组件列表 */
/* 中间：实时预览（原生渲染 + 点选） */
@Composable
private fun RealtimePreview(xml: String, revision: Int, on_select: (String) -> Unit, modifier: Modifier = Modifier) {
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
                val r = runtime_layout_loader(context).load(tmp)
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
                IconButton(onClick = on_delete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = androidx.compose.ui.graphics.Color(0xFFFF6B6B), modifier = Modifier.size(15.dp))
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
                    // 输入即生效：立即刷新预览
                    on_change()
                },
                label = { Text(ATTR_CN[attr] ?: attr, fontSize = 9.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
        }
    }
}
