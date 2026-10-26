package com.jmwl.gostudio.designer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jmwl.gostudio.ui.theme.app_theme_provider
import java.io.File
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 布局设计器底部弹窗集合（一体化交互）：
 * - designer_property_sheet：点选组件后的属性面板（头部添加/删除入口 + 单页滚动分段 + 顶部胶囊锚点 + 「更多属性」对话框兜底）
 * - designer_catalog_sheet：添加目录（组件 / 布局分组，中文名）
 * - designer_tree_sheet：组件树（层级缩进 / 展开折叠 / 与预览双向选中 / 长按层级操作 / 排序入口）
 * - designer_reorder_sheet：拖拽排序（面包屑切换层级，长按拖动整组落位）
 * 数据模型与预览渲染见 layout_designer_activity.kt。
 */

/** 颜色属性预设（写入用户布局的数据值，非界面主题色） */
private val COLOR_PRESETS = listOf("#1B1C1F", "#FFFFFF", "#5CCFE6", "#FF6B6B", "#66BB6A", "#FFCA28")

/* ---------- 通用小件 ---------- */

/** 属性面板顶部锚点胶囊：点击滚动到对应分段，选中态沿用 dialog_icon 选中样式 */
@Composable
private fun section_pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = app_theme_provider.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) colors.dialog_icon.copy(alpha = 0.16f) else colors.dialog_card_bg,
        border = BorderStroke(
            width = if (selected) 1.dp else 0.5.dp,
            color = if (selected) colors.dialog_icon else colors.editor_divider
        ),
        modifier = Modifier.height(30.dp)
    ) {
        Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                fontSize = 11.sp,
                color = if (selected) colors.dialog_icon else colors.dialog_hint,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

/** 仪表盘卡片：圆角 + 细描边 + 浅色底 */
@Composable
private fun sheet_card(content: @Composable () -> Unit) {
    val colors = app_theme_provider.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.dialog_card_bg,
        border = BorderStroke(0.5.dp, colors.editor_divider),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun sheet_title(text: String, onClose: (() -> Unit)? = null) {
    val colors = app_theme_provider.colors
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.dialog_text,
            modifier = Modifier.weight(1f)
        )
        if (onClose != null) {
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.dialog_hint)
            }
        }
    }
}

/* ---------- 属性底部面板 ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun designer_property_sheet(
    node: d_node,
    is_root: Boolean,
    project_dir: File,
    on_attr_change: (attr: String, value: String) -> Unit,
    on_delete: () -> Unit,
    on_open_catalog: (section: Int) -> Unit,
    on_open_event: (component_id: String, event_type: String) -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val max_height = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
    val defined_attrs = attrs_for(node.tag, node.parent?.tag)
    val attr_list = defined_attrs + node.attrs.keys.filter { it !in defined_attrs }
    var show_delete_confirm by remember { mutableStateOf(false) }
    var editing_attr by remember { mutableStateOf<String?>(null) }

    if (show_delete_confirm) {
        AlertDialog(
            onDismissRequest = { show_delete_confirm = false },
            title = { Text("删除组件", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                val desc = "确定删除 ${tag_cn(node.tag)}${node.id.takeIf { it.isNotBlank() }?.let { " (#$it)" } ?: ""} 吗？"
                Text(if (is_root) "根布局删除后整个布局将被清空，确定吗？" else desc, fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    show_delete_confirm = false
                    on_delete()
                }) { Text("删除", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { show_delete_confirm = false }) { Text("取消") }
            },
            containerColor = colors.dialog_bg,
            titleContentColor = colors.dialog_text,
            textContentColor = colors.dialog_hint
        )
    }

    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        sheetState = sheet_state,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(max_height)
                .padding(horizontal = 16.dp)
        ) {
            // 单页滚动：属性各组一段（基础/布局/文本/组件，空组自动隐藏）+ 非根节点末尾事件段
            val groups = property_groups_for(attr_list)
            val sections = if (is_root) groups else groups + listOf(property_group("事件", emptyList()))
            val list_state = rememberLazyListState()
            val scope = rememberCoroutineScope()
            // 切换组件时回到分段顶部；就地改属性不重置（tag+id 未变不触发）
            LaunchedEffect(node.tag, node.id) { list_state.scrollToItem(0) }
            // 滚动联动：首个可见 item 的序号即当前高亮胶囊
            val active_index by remember { derivedStateOf { list_state.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0 } }

            // 标题行：组件中文名 / tag / id + 添加（仅容器）+ 删除
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tag_cn(node.tag), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.dialog_text)
                        Spacer(Modifier.width(6.dp))
                        Text(node.tag, fontSize = 10.sp, color = colors.dialog_hint)
                    }
                    node.id.takeIf { it.isNotBlank() }?.let {
                        Text("#$it", fontSize = 11.sp, color = colors.dialog_hint)
                    }
                }
                // 容器才有「添加组件 / 添加布局」入口，目录关闭后回开属性面板
                if (node.is_container) {
                    IconButton(onClick = { on_open_catalog(0) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.AddBox, contentDescription = "添加组件", tint = colors.dialog_icon, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { on_open_catalog(1) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Dashboard, contentDescription = "添加布局", tint = colors.dialog_icon, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { show_delete_confirm = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = colors.danger, modifier = Modifier.size(18.dp))
                }
            }

            // 顶部小胶囊锚点：点击滚动到对应分段，宽度不够可横向划动
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                sections.forEachIndexed { index, section ->
                    section_pill(
                        text = section.title,
                        selected = index == active_index,
                        onClick = { scope.launch { list_state.animateScrollToItem(index) } }
                    )
                }
            }

            // 单页滚动内容：每段一个 item，段标题 + 属性行平铺
            LazyColumn(
                state = list_state,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                sections.forEachIndexed { index, section ->
                    item(key = "sec_$index") {
                        Column(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(12.dp))
                            Text(section.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.dialog_text)
                            if (section.title == "事件") {
                                Spacer(Modifier.height(8.dp))
                                if (node.id.isBlank()) {
                                    Text("先设置 ID 后才能生成事件", fontSize = 12.sp, color = colors.dialog_hint)
                                } else {
                                    component_events(node.tag).forEach { event ->
                                        Surface(
                                            onClick = { on_open_event(node.id, event.first) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.dialog_icon.copy(alpha = 0.14f),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        ) {
                                            Text(
                                                event.second,
                                                fontSize = 12.sp,
                                                color = colors.dialog_icon,
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                section.attrs.forEach { attr ->
                                    more_attr_row(attr = attr, value = node.attrs[attr] ?: "", on_edit = { editing_attr = attr })
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // 属性编辑对话框（属性行点击进入，沿用类型化编辑/清除/原始值全部能力）
    editing_attr?.let { attr ->
        PropertyEditDialog(
            node = node,
            attr = attr,
            project_dir = project_dir,
            on_confirm = { newValue ->
                on_attr_change(attr, newValue)
                editing_attr = null
            },
            on_dismiss = { editing_attr = null }
        )
    }
}

/* ---------- 更多属性行（打开对话框） ---------- */

@Composable
private fun more_attr_row(attr: String, value: String, on_edit: () -> Unit) {
    val colors = app_theme_provider.colors
    Surface(
        onClick = on_edit,
        shape = RoundedCornerShape(8.dp),
        color = colors.dialog_input_bg,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(ATTR_CN[attr] ?: attr, fontSize = 11.sp, color = colors.dialog_text, modifier = Modifier.weight(1f))
            if (property_spec_for(attr).kind == property_kind.COLOR && value.isNotBlank()) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(parse_preview_color(value, colors.dialog_icon), RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                property_display(attr, value),
                fontSize = 10.sp,
                color = if (value.isBlank()) colors.dialog_hint else colors.dialog_icon,
                maxLines = 1
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = "修改", tint = colors.dialog_hint, modifier = Modifier.size(13.dp))
        }
    }
}

/* ---------- 属性对话框（兜底，沿用原交互） ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PropertyEditDialog(
    node: d_node,
    attr: String,
    project_dir: File,
    on_confirm: (String) -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val max_height = (LocalConfiguration.current.screenHeightDp * 0.72f).dp
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

    fun confirm_result(): String = when (spec.kind) {
        property_kind.BOOLEAN -> checked.toString()
        property_kind.NUMBER -> format_number(number)
        else -> text
    }

    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        sheetState = sheet_state,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(max_height)
        ) {
            // 标题行：中文属性名 + 原始属性名
            Column(Modifier.fillMaxWidth()) {
                Text(ATTR_CN[attr] ?: attr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.dialog_text)
                Spacer(Modifier.height(2.dp))
                Text(attr, fontSize = 11.sp, color = colors.dialog_hint)
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                when (spec.kind) {
                    property_kind.SIZE -> {
                        var custom by remember(attr, node) { mutableStateOf(initial.isNotBlank() && initial !in SIZE_OPTIONS) }
                        spec.options.forEach { option ->
                            Row(
                                Modifier.fillMaxWidth().clickable { custom = false; text = option }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = !custom && text == option, onClick = { custom = false; text = option })
                                Text(property_display(attr, option), fontSize = 14.sp)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { custom = true; if (text in SIZE_OPTIONS) text = "" }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = custom, onClick = { custom = true; if (text in SIZE_OPTIONS) text = "" })
                            Text("自定义尺寸", fontSize = 14.sp)
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
                            Modifier.fillMaxWidth().clickable { text = option }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = text == option, onClick = { text = option })
                            Text(property_display(attr, option), fontSize = 14.sp)
                        }
                    }
                    property_kind.MULTI_CHOICE -> {
                        Text("可多选，组合后作用于布局", fontSize = 12.sp, color = colors.dialog_hint)
                        spec.options.forEach { option ->
                            val checked_now = option in flags
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    flags = if (checked_now) flags - option else flags + option
                                    text = flags.joinToString("|")
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked_now,
                                    onCheckedChange = {
                                        flags = if (it) flags + option else flags - option
                                        text = flags.joinToString("|")
                                    }
                                )
                                Text(property_display(attr, option), fontSize = 14.sp)
                            }
                        }
                    }
                    property_kind.BOOLEAN -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = checked, onCheckedChange = { checked = it; text = it.toString() })
                        Spacer(Modifier.width(8.dp))
                        Text(if (checked) "开启" else "关闭", fontSize = 14.sp)
                    }
                    property_kind.NUMBER, property_kind.DIMENSION -> {
                        if (spec.kind == property_kind.NUMBER) {
                            Slider(
                                value = number,
                                onValueChange = { number = (it / spec.step).roundToInt() * spec.step },
                                valueRange = spec.min..spec.max,
                                steps = if (spec.step >= (spec.max - spec.min)) 0 else (((spec.max - spec.min) / spec.step).toInt() - 2).coerceAtLeast(0)
                            )
                            Text("当前：${format_number(number)}", fontSize = 12.sp, color = colors.dialog_hint)
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
                            Text("从相册 / 文件选择", color = colors.dialog_icon)
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
                        Spacer(Modifier.height(12.dp))
                        // 当前颜色实时预览：输入什么色值立刻看到
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(parse_preview_color(text.ifBlank { "#00000000" }, colors.dialog_icon), RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = colors.dialog_input_border,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            COLOR_PRESETS.forEach { color ->
                                val selected = text.equals(color, ignoreCase = true)
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .background(parse_preview_color(color, colors.dialog_icon), RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) colors.dialog_icon else colors.dialog_input_border,
                                            shape = RoundedCornerShape(8.dp)
                                        )
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
            Spacer(Modifier.height(16.dp))
            // 底部操作行：清除 | 取消 | 确定
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { on_confirm("") }) { Text("清除", color = colors.dialog_hint) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = on_dismiss) { Text("取消", color = colors.dialog_hint) }
                TextButton(onClick = { on_confirm(confirm_result()) }) { Text("确定", color = colors.dialog_icon) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- 添加目录底部面板 ---------- */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun designer_catalog_sheet(
    initial_section: Int,
    has_root: Boolean,
    on_add: (tag: String) -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val max_height = (LocalConfiguration.current.screenHeightDp * 0.7f).dp
    val sections = if (initial_section == 1) {
        listOf("布局" to LAYOUT_TAGS, "组件" to WIDGET_TAGS)
    } else {
        listOf("组件" to WIDGET_TAGS, "布局" to LAYOUT_TAGS)
    }

    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        sheetState = sheet_state,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(max_height)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            sheet_title("添加到布局", on_dismiss)
            if (!has_root) {
                Text(
                    "布局为空：第一个添加的组件或布局将成为根节点",
                    fontSize = 11.sp,
                    color = colors.dialog_hint,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            sections.forEach { (title, tags) ->
                Spacer(Modifier.height(8.dp))
                sheet_card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.dialog_text)
                            Spacer(Modifier.width(6.dp))
                            Text("${tags.size} 个", fontSize = 10.sp, color = colors.dialog_hint)
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag -> catalog_chip(tag, on_add) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun catalog_chip(tag: String, on_add: (String) -> Unit) {
    val colors = app_theme_provider.colors
    Surface(
        onClick = { on_add(tag) },
        shape = RoundedCornerShape(12.dp),
        color = colors.dialog_card_bg,
        border = BorderStroke(0.5.dp, colors.editor_divider),
        modifier = Modifier.widthIn(min = 88.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(tag_cn(tag), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.dialog_text)
            Spacer(Modifier.height(2.dp))
            Text(tag, fontSize = 9.sp, color = colors.dialog_hint)
        }
    }
}

/* ---------- 组件树底部面板 ---------- */

private fun count_nodes(node: d_node): Int = 1 + node.children.sumOf(::count_nodes)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun designer_tree_sheet(
    root: d_node?,
    selected: d_node?,
    has_clipboard: Boolean,
    clipboard_tag: String?,
    on_select: (d_node) -> Unit,
    on_copy: (d_node) -> Unit,
    on_paste: (d_node) -> Unit,
    on_delete: (d_node) -> Unit,
    on_move: (d_node, Int) -> Unit,
    on_open_reorder: () -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val max_height = (LocalConfiguration.current.screenHeightDp * 0.72f).dp
    // 折叠集合按路径记录；面板关闭即重置（默认全展开）
    val collapsed = remember { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        sheetState = sheet_state,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(max_height)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "组件树",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.dialog_text,
                    modifier = Modifier.weight(1f)
                )
                root?.let {
                    Text("${count_nodes(it)} 个节点", fontSize = 10.sp, color = colors.dialog_hint)
                    Spacer(Modifier.width(8.dp))
                }
                // 排序入口：仅非空布局显示，进入拖拽排序弹窗
                if (root != null) {
                    IconButton(onClick = on_open_reorder, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Sort, contentDescription = "排序", tint = colors.dialog_hint, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = on_dismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.dialog_hint)
                }
            }
            if (root == null) {
                Text(
                    "布局为空\n点顶部「添加组件 / 添加布局」创建根节点",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = colors.dialog_hint,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
                )
            } else {
                tree_node_row(
                    node = root,
                    depth = 0,
                    path = "root",
                    selected = selected,
                    collapsed = collapsed,
                    has_clipboard = has_clipboard,
                    clipboard_tag = clipboard_tag,
                    on_select = on_select,
                    on_copy = on_copy,
                    on_paste = on_paste,
                    on_delete = on_delete,
                    on_move = on_move
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun tree_node_row(
    node: d_node,
    depth: Int,
    path: String,
    selected: d_node?,
    collapsed: MutableState<Set<String>>,
    has_clipboard: Boolean,
    clipboard_tag: String?,
    on_select: (d_node) -> Unit,
    on_copy: (d_node) -> Unit,
    on_paste: (d_node) -> Unit,
    on_delete: (d_node) -> Unit,
    on_move: (d_node, Int) -> Unit
) {
    val colors = app_theme_provider.colors
    val is_sel = node === selected
    val is_root = depth == 0
    val node_path = "$path/${node.id.ifBlank { node.tag }}"
    val expanded = node_path !in collapsed.value
    var menu_open by remember { mutableStateOf(false) }
    var confirm_delete_root by remember { mutableStateOf(false) }

    if (confirm_delete_root) {
        AlertDialog(
            onDismissRequest = { confirm_delete_root = false },
            title = { Text("删除根布局", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("将删除根布局及全部子组件，整个布局会被清空。确定吗？", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirm_delete_root = false
                    on_delete(node)
                }) { Text("删除", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirm_delete_root = false }) { Text("取消") }
            },
            containerColor = colors.dialog_bg,
            titleContentColor = colors.dialog_text,
            textContentColor = colors.dialog_hint
        )
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .combinedClickable(
                    onClick = { on_select(node) },
                    onLongClick = { menu_open = true }
                )
                .background(if (is_sel) colors.dialog_icon.copy(alpha = 0.14f) else Color.Transparent)
                .padding(start = (4 + depth * 16).dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.is_container) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clickable {
                            collapsed.value = if (expanded) collapsed.value + node_path else collapsed.value - node_path
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = if (expanded) "折叠" else "展开",
                        tint = if (is_sel) colors.dialog_icon else colors.dialog_hint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(Modifier.size(36.dp))
            }
            Text(
                tag_cn(node.tag),
                fontSize = 13.sp,
                color = if (is_sel) colors.dialog_icon else colors.dialog_text,
                fontWeight = if (is_sel) FontWeight.Bold else FontWeight.Normal
            )
            node.id.takeIf { it.isNotBlank() }?.let {
                Text("  #$it", fontSize = 10.sp, color = colors.dialog_hint)
            }
            if (node.is_container && node.children.isNotEmpty()) {
                Spacer(Modifier.weight(1f))
                Text("${node.children.size} 项", fontSize = 10.sp, color = colors.dialog_hint)
            }
        }

        DropdownMenu(expanded = menu_open, onDismissRequest = { menu_open = false }) {
            DropdownMenuItem(
                text = { Text("复制", fontSize = 13.sp) },
                onClick = { on_copy(node); menu_open = false }
            )
            if (is_root) {
                DropdownMenuItem(
                    text = { Text("删除根布局", fontSize = 13.sp, color = colors.danger) },
                    onClick = { menu_open = false; confirm_delete_root = true }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("删除", fontSize = 13.sp, color = colors.danger) },
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
    if (node.is_container && expanded) {
        node.children.forEachIndexed { index, child ->
            tree_node_row(
                node = child,
                depth = depth + 1,
                path = "$node_path/$index",
                selected = selected,
                collapsed = collapsed,
                has_clipboard = has_clipboard,
                clipboard_tag = clipboard_tag,
                on_select = on_select,
                on_copy = on_copy,
                on_paste = on_paste,
                on_delete = on_delete,
                on_move = on_move
            )
        }
    }
}

/* ---------- 拖拽排序底部面板 ---------- */

/**
 * 拖拽排序当前层级子组件：
 * - 面包屑沿 parent 链回到 root，点祖先胶囊切换排序层级
 * - 行长按启动拖拽，拖动中该行浮起，其余行按目标位置让位平移，松手整组落位
 * - 树每次重建后 root 是新对象，按内容重定位存活引用，层级失效则退回 root
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun designer_reorder_sheet(
    root: d_node?,
    initial_parent: d_node?,
    on_reorder: (parent: d_node, ordered: List<d_node>) -> Unit,
    on_dismiss: () -> Unit
) {
    val colors = app_theme_provider.colors
    val sheet_state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val max_height = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    val row_height_px = with(LocalDensity.current) { 52.dp.toPx() }
    var current_parent by remember { mutableStateOf(initial_parent ?: root) }
    val live_parent = remember(root, current_parent) {
        // 委托属性不能智能转换，取局部值判空后再定位
        val cur = current_parent
        if (root != null && cur != null) find_node(root, cur) ?: root else root
    }
    val children = live_parent?.children.orEmpty()
    // 面包屑链：root → 当前层级
    val crumbs = remember(live_parent) {
        val chain = mutableListOf<d_node>()
        var walk: d_node? = live_parent
        while (walk != null) { chain.add(walk); walk = walk.parent }
        chain.asReversed()
    }
    var dragging_index by remember { mutableStateOf(-1) }
    var drag_offset by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = on_dismiss,
        sheetState = sheet_state,
        containerColor = colors.dialog_bg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(max_height)
                .padding(horizontal = 16.dp)
        ) {
            sheet_title("拖拽排序", on_dismiss)
            // 面包屑：点祖先胶囊回到该层级，当前层级加粗
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                crumbs.forEachIndexed { i, crumb ->
                    if (i > 0) Text("  ›  ", fontSize = 11.sp, color = colors.dialog_hint)
                    val is_current = i == crumbs.lastIndex
                    Surface(
                        onClick = { if (!is_current) current_parent = crumb },
                        shape = RoundedCornerShape(50),
                        color = if (is_current) colors.dialog_icon.copy(alpha = 0.16f) else colors.dialog_card_bg,
                        border = BorderStroke(
                            width = if (is_current) 1.dp else 0.5.dp,
                            color = if (is_current) colors.dialog_icon else colors.editor_divider
                        )
                    ) {
                        Text(
                            tag_cn(crumb.tag),
                            fontSize = 11.sp,
                            fontWeight = if (is_current) FontWeight.Bold else FontWeight.Normal,
                            color = if (is_current) colors.dialog_text else colors.dialog_hint,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (children.size < 2) {
                Text(
                    "子组件不足 2 个，无需排序",
                    fontSize = 12.sp,
                    color = colors.dialog_hint,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(children.size) { index ->
                        val item = children[index]
                        val is_dragging = index == dragging_index
                        // 目标位置：拖拽距离换算行数后夹在列表范围内，其余行据此让位
                        val drag_target = if (dragging_index >= 0)
                            (dragging_index + (drag_offset / row_height_px).roundToInt()).coerceIn(0, children.lastIndex)
                        else -1
                        val translation = when {
                            is_dragging -> drag_offset
                            dragging_index >= 0 && index > dragging_index && index <= drag_target -> -row_height_px
                            dragging_index >= 0 && index < dragging_index && index >= drag_target -> row_height_px
                            else -> 0f
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .zIndex(if (is_dragging) 1f else 0f)
                                .graphicsLayer { translationY = translation }
                                .background(if (is_dragging) colors.dialog_icon.copy(alpha = 0.14f) else Color.Transparent)
                                .pointerInput(children) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragging_index = index
                                            drag_offset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            drag_offset += amount.y
                                        },
                                        onDragEnd = {
                                            val from = dragging_index
                                            val to = (from + (drag_offset / row_height_px).roundToInt())
                                                .coerceIn(0, children.lastIndex)
                                            if (from in children.indices && from != to) {
                                                val moved = children.toMutableList()
                                                moved.add(to, moved.removeAt(from))
                                                live_parent?.let { on_reorder(it, moved) }
                                            }
                                            dragging_index = -1
                                            drag_offset = 0f
                                        },
                                        onDragCancel = {
                                            dragging_index = -1
                                            drag_offset = 0f
                                        }
                                    )
                                }
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "拖拽",
                                    tint = colors.dialog_hint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(tag_cn(item.tag), fontSize = 13.sp, color = colors.dialog_text, maxLines = 1)
                                item.id.takeIf { it.isNotBlank() }?.let {
                                    Text("  #$it", fontSize = 10.sp, color = colors.dialog_hint, maxLines = 1)
                                }
                                Spacer(Modifier.weight(1f))
                                // 容器且有子组件：显示数量并允许进入该容器继续排序
                                if (item.is_container && item.children.isNotEmpty()) {
                                    Text("${item.children.size} 项", fontSize = 10.sp, color = colors.dialog_hint)
                                    IconButton(onClick = { current_parent = item }, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "进入该容器排序",
                                            tint = colors.dialog_hint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
