package com.jmwl.gostudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jmwl.gostudio.ui.theme.app_theme_provider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 一条全局搜索结果（file:line:预览）。 */
data class editor_project_search_hit(
    val path: String,
    /** 相对项目根的展示路径。 */
    val relative_path: String,
    val file_name: String,
    /** 1 起始行号。 */
    val line: Int,
    val preview: String
)

/**
 * 全局搜索面板（参考 CodeAssist SearchScreen）：
 * 输入关键字（400ms 防抖，回车立即）→ 调 on_search（proot grep 实现）→
 * 结果按文件分组展示，点击跳转对应文件行。搜索逻辑由外部注入，面板只管状态与展示。
 */
@Composable
internal fun editor_search_project_panel(
    project_root_path: String,
    on_search: suspend (String) -> List<editor_project_search_hit>,
    on_open_hit: (editor_project_search_hit) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = app_theme_provider.colors
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var hits by remember { mutableStateOf<List<editor_project_search_hit>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var debounce_job by remember { mutableStateOf<Job?>(null) }

    fun run_search(keyword: String) {
        val keyword = keyword.trim()
        if (keyword.length < 2 || project_root_path.isBlank()) {
            hits = emptyList()
            searched = false
            return
        }
        debounce_job?.cancel()
        debounce_job = scope.launch {
            searching = true
            error = null
            val result = runCatching { on_search(keyword) }
                .getOrElse { error = it.message ?: "搜索失败"; emptyList() }
            searching = false
            searched = true
            hits = result
        }
    }

    Column(modifier.fillMaxSize()) {
        // 搜索输入框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .background(colors.editor_button_bg, RoundedCornerShape(10.dp))
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.editor_hint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    debounce_job?.cancel()
                    debounce_job = scope.launch {
                        delay(400)
                        run_search(query)
                    }
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = colors.editor_text, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.editor_icon),
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = {
                    query = ""
                    hits = emptyList()
                    searched = false
                    error = null
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "清空", tint = colors.editor_hint, modifier = Modifier.size(16.dp))
                }
            }
        }

        when {
            searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("搜索中...", color = colors.editor_hint, fontSize = 13.sp)
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = colors.danger, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            !searched -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "输入至少 2 个字符\n在整个项目中搜索",
                    color = colors.editor_hint,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            hits.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("没有找到「$query」", color = colors.editor_hint, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            else -> {
                val grouped = remember(hits) { hits.groupBy { it.path } }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    grouped.forEach { (path, file_hits) ->
                        item(key = "file-$path") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.editor_sidebar_selected_bg.copy(alpha = 0.4f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = file_hits.first().relative_path,
                                    color = colors.editor_text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${file_hits.size} 处",
                                    color = colors.editor_hint,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        items(file_hits, key = { "hit-${it.path}-${it.line}" }) { hit ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { on_open_hit(hit) }
                                    .padding(start = 24.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                Text(
                                    text = "行 ${hit.line}",
                                    color = colors.editor_hint,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = hit.preview.trim(),
                                    color = colors.editor_text,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
