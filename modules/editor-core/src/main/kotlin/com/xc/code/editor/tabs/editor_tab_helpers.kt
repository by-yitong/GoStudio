package com.xc.code.editor.tabs

import com.xc.code.editor.session.editor_open_tab

fun find_dirty_closable_tab_index(
    tabs: List<editor_open_tab>,
    keep_file_path: String? = null
): Int {
    return tabs.indexOfFirst { tab ->
        !tab.pinned && tab.has_changes && (keep_file_path == null || tab.file_path != keep_file_path)
    }
}

fun ordered_pinned_first_tabs(tabs: List<editor_open_tab>): List<editor_open_tab> {
    return tabs.filter { it.pinned } + tabs.filter { !it.pinned }
}

fun remaining_tabs_after_close_others(
    tabs: List<editor_open_tab>,
    keep_file_path: String
): List<editor_open_tab> {
    return tabs.filter { it.pinned || it.file_path == keep_file_path }
}

fun closing_paths_after_close_others(
    tabs: List<editor_open_tab>,
    keep_file_path: String
): List<String> {
    val remaining_tabs = remaining_tabs_after_close_others(tabs, keep_file_path).toSet()
    return tabs.filterNot { tab -> tab in remaining_tabs }.map { it.file_path }
}

fun pinned_tabs(tabs: List<editor_open_tab>): List<editor_open_tab> {
    return tabs.filter { it.pinned }
}

fun closable_tab_paths(tabs: List<editor_open_tab>): List<String> {
    return tabs.filterNot { it.pinned }.map { it.file_path }
}

fun find_dirty_tab_index(tabs: List<editor_open_tab>): Int {
    return tabs.indexOfFirst { it.has_changes }
}

fun pinned_tab_paths(tabs: List<editor_open_tab>): List<String> {
    return tabs.filter { it.pinned }.map { it.file_path }
}
