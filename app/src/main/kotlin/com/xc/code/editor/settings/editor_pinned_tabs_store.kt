package com.xc.code.editor.settings

import android.content.Context
import java.io.File

private const val pinned_tabs_prefs_name = "editor_pinned_tabs"

internal fun load_pinned_tab_paths(context: Context, project_dir: File): List<String> {
    val raw_paths = context.getSharedPreferences(pinned_tabs_prefs_name, Context.MODE_PRIVATE)
        .getString(pinned_tabs_key(project_dir), "")
        .orEmpty()

    return raw_paths
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()
}

internal fun save_pinned_tab_paths(context: Context, project_dir: File, paths: List<String>) {
    val pinned_paths = paths
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("\n")
    val prefs_editor = context.getSharedPreferences(pinned_tabs_prefs_name, Context.MODE_PRIVATE).edit()
    if (pinned_paths.isBlank()) {
        prefs_editor.remove(pinned_tabs_key(project_dir))
    } else {
        prefs_editor.putString(pinned_tabs_key(project_dir), pinned_paths)
    }
    prefs_editor.apply()
}

private fun pinned_tabs_key(project_dir: File): String {
    return runCatching { project_dir.canonicalPath }.getOrDefault(project_dir.absolutePath)
}
