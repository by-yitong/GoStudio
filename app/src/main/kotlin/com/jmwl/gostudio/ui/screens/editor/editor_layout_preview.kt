package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jmwl.gostudio.runtime.runtime_layout_loader
import com.jmwl.gostudio.ui.theme.app_theme_provider
import java.io.File

/**
 * layout.xml 可视化预览：
 * - 实时渲染当前编辑器里的 XML（复用运行时布局加载器）
 * - 选中控件查看/修改常用属性，改完回调写回编辑器
 */
@Composable
fun editor_layout_preview(
    xml_content: String,
    on_close: () -> Unit,
    on_content_change: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = app_theme_provider.colors
    var render_revision by remember { mutableIntStateOf(0) }
    var parse_error by remember { mutableStateOf<String?>(null) }
    var selected_id by remember { mutableStateOf<String?>(null) }

    // 渲染当前 XML（每次 render_revision 变化或内容变化重新渲染）
    var render_result by remember(xml_content, render_revision) {
        mutableStateOf<runtime_layout_loader.Result?>(null)
    }
    LaunchedEffect(xml_content, render_revision) {
        parse_error = null
        render_result = null
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val temp = File.createTempFile("preview", ".xml", context.cacheDir)
                temp.writeText(xml_content)
                val result = runtime_layout_loader(context).load(temp)
                temp.delete()
                result
            }
        }.fold(
            onSuccess = { render_result = it },
            onFailure = { parse_error = it.message }
        )
    }

    Column(modifier = modifier.fillMaxSize().background(colors.editor_bg)) {
        // 顶栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "布局预览",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.editor_text
            )
            Spacer(modifier = Modifier.width(12.dp))
            parse_error?.let { error ->
                Text(
                    text = "解析错误: $error",
                    fontSize = 10.sp,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f)
                )
            } ?: Text(
                text = "选中控件编辑属性",
                fontSize = 11.sp,
                color = colors.editor_hint,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { render_revision++ }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = colors.editor_icon, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = on_close, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.editor_icon, modifier = Modifier.size(18.dp))
            }
        }

        Divider(color = colors.editor_divider.copy(alpha = 0.5f))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            render_result?.let { result ->
                AndroidView(
                    factory = { ctx ->
                        val frame = android.widget.FrameLayout(ctx)
                        frame.setBackgroundColor(0xFFF7F7FA.toInt())
                        frame
                    },
                    update = { frame ->
                        frame.removeAllViews()
                        (frame.parent as? android.view.ViewGroup)?.removeView(result.root)
                        frame.addView(
                            result.root,
                            android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        // 给所有带 id 控件绑点击选中
                        result.views.forEach { (id, view) ->
                            view.setOnClickListener {
                                selected_id = id
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 属性面板（选中控件后显示）
        selected_id?.let { vid ->
            render_result?.views?.get(vid)?.let { view ->
                Divider(color = colors.editor_divider.copy(alpha = 0.5f))
                layout_property_panel(
                    view_id = vid,
                    view = view,
                    xml_content = xml_content,
                    on_change = on_content_change
                )
            }
        }
    }
}

@Composable
private fun layout_property_panel(
    view_id: String,
    view: android.view.View,
    xml_content: String,
    on_change: (String) -> Unit
) {
    val colors = app_theme_provider.colors
    var text_value by remember(view_id) {
        mutableStateOf((view as? android.widget.TextView)?.text?.toString() ?: "")
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(4.dp), color = colors.editor_panel_overlay) {
                Text(
                    text = " id: $view_id ",
                    fontSize = 11.sp,
                    color = colors.editor_icon,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = view.javaClass.simpleName,
                fontSize = 10.sp,
                color = colors.editor_hint
            )
        }

        // text 属性
        OutlinedTextField(
            value = text_value,
            onValueChange = { text_value = it },
            label = { Text("text", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
        )

        Button(
            onClick = {
                on_change(update_xml_attribute(xml_content, view_id, "text", text_value))
            },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("应用", fontSize = 13.sp)
        }
    }
}

/** 在 XML 中把指定 id 控件的某个属性值替换为新值（正则处理，保持格式）。 */
private fun update_xml_attribute(xml: String, view_id: String, attr: String, new_value: String): String {
    // 找到 id="$view_id" 所在的标签块，替换其中 attr="..."
    val escaped = Regex.escape(view_id)
    val pattern = Regex("""(<[A-Za-z][^>]*\bid="$escaped"[^>]*?)""", RegexOption.DOT_MATCHES_ALL)
    return pattern.replace(xml) { match ->
        val tag = match.groupValues[1]
        val attr_pattern = Regex("""$attr\s*=\s*"[^"]*"""")
        if (attr_pattern.containsMatchIn(tag)) {
            val new_tag = attr_pattern.replaceFirst(tag, """$attr="$new_value"""")
            match.value.replace(tag, new_tag)
        } else {
            // 属性不存在则追加
            val new_tag = tag.trimEnd() + """ $attr="$new_value"""" 
            match.value.replace(tag, new_tag)
        }
    }
}
