package com.xc.code.editor.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xc.code.editor.model.editor_file_node
import com.xc.code.editor.model.editor_settings_state
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

class editor_activity_state {
    var project_name by mutableStateOf("项目")
    val open_tabs = mutableStateListOf<editor_open_tab>()
    var selected_tab_index by mutableIntStateOf(-1)
    var current_file_path by mutableStateOf<String?>(null)
    var current_file_name by mutableStateOf("未打开文件")
    var content by mutableStateOf("")
    var cursor_line by mutableIntStateOf(1)
    var cursor_column by mutableIntStateOf(1)
    var cursor_selected by mutableStateOf(false)
    var can_undo by mutableStateOf(false)
    var can_redo by mutableStateOf(false)
    var has_changes by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var read_only by mutableStateOf(false)
    var toolbar_visible by mutableStateOf(true)
    var editor_settings by mutableStateOf(editor_settings_state())
    var status_text by mutableStateOf("请选择左侧文件")
    var file_nodes by mutableStateOf<List<editor_file_node>>(emptyList())
    var expanded_paths by mutableStateOf(emptySet<String>())
    var file_tree_loading by mutableStateOf(false)
    var project_exists by mutableStateOf(false)
    var show_exit_dialog by mutableStateOf(false)
    var show_unsaved_dialog by mutableStateOf(false)
    var pending_action by mutableStateOf<editor_pending_action?>(null)
}

class editor_open_tab(
    initial_file_path: String,
    initial_file_name: String,
    initial_status_text: String,
    initial_content: String
) {
    var file_path by mutableStateOf(initial_file_path)
    var file_name by mutableStateOf(initial_file_name)
    val document = Content(initial_content)
    var content by mutableStateOf(initial_content)
    var status_text by mutableStateOf(initial_status_text)
    var has_changes by mutableStateOf(false)
    var pinned by mutableStateOf(false)
    var editor: CodeEditor? = null
    var cursor_line by mutableIntStateOf(0)
    var cursor_column by mutableIntStateOf(0)
}

sealed class editor_pending_action {
    data class CloseTab(val file_path: String) : editor_pending_action()
    data class CloseOtherTabs(val keep_file_path: String) : editor_pending_action()
    object CloseAllTabs : editor_pending_action()
    object CloseEditor : editor_pending_action()
}