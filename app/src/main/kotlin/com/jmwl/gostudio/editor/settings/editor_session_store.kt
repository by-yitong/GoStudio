package com.jmwl.gostudio.editor.settings

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val editor_session_prefs_name = "editor_session"
private const val editor_session_tabs_key = "tabs"
private const val editor_session_active_key = "active_path"

internal data class editor_session_tab(
    val file_path: String,
    val cursor_line: Int = 0,
    val cursor_column: Int = 0,
    val pinned: Boolean = false
)

internal data class editor_session_state(
    val tabs: List<editor_session_tab> = emptyList(),
    val active_path: String? = null
)

internal fun load_editor_session(context: Context, project_dir: File): editor_session_state? {
    return runCatching {
        val raw = context.getSharedPreferences(editor_session_prefs_name, Context.MODE_PRIVATE)
            .getString(editor_session_key(project_dir), null)
            ?: return@runCatching null
        val root = JSONObject(raw)
        val tabs_json = root.optJSONArray(editor_session_tabs_key) ?: JSONArray()
        val tabs = buildList {
            for (index in 0 until tabs_json.length()) {
                val item = tabs_json.optJSONObject(index) ?: continue
                val path = item.optString("path")
                if (path.isBlank()) continue
                add(
                    editor_session_tab(
                        file_path = path,
                        cursor_line = item.optInt("cursorLine", 0).coerceAtLeast(0),
                        cursor_column = item.optInt("cursorColumn", 0).coerceAtLeast(0),
                        pinned = item.optBoolean("pinned", false)
                    )
                )
            }
        }
        editor_session_state(
            tabs = tabs.distinctBy { it.file_path },
            active_path = root.optString(editor_session_active_key).takeIf { it.isNotBlank() }
        )
    }.getOrNull()
}

internal fun save_editor_session(
    context: Context,
    project_dir: File,
    tabs: List<editor_session_tab>,
    active_path: String?
) {
    val root = JSONObject()
    val tabs_json = JSONArray()
    tabs
        .filter { it.file_path.isNotBlank() }
        .distinctBy { it.file_path }
        .forEach { tab ->
            tabs_json.put(
                JSONObject()
                    .put("path", tab.file_path)
                    .put("cursorLine", tab.cursor_line)
                    .put("cursorColumn", tab.cursor_column)
                    .put("pinned", tab.pinned)
            )
        }
    root.put(editor_session_tabs_key, tabs_json)
    root.put(editor_session_active_key, active_path.orEmpty())

    context.getSharedPreferences(editor_session_prefs_name, Context.MODE_PRIVATE)
        .edit()
        .putString(editor_session_key(project_dir), root.toString())
        .commit()
}

private fun editor_session_key(project_dir: File): String {
    return runCatching { project_dir.canonicalPath }.getOrDefault(project_dir.absolutePath)
}
