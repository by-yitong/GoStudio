package com.xc.code.editor.core

import com.xc.code.editor.model.editor_file_node

import java.io.File

fun relative_project_path(project_dir: File, file: File): String {
    return runCatching {
        project_dir.canonicalFile.toPath().relativize(file.canonicalFile.toPath()).toString()
    }.getOrDefault(file.name).ifBlank { file.name }
}

fun load_file_tree_directory(dir: File): List<editor_file_node> {
    return dir.listFiles()
        ?.filterNot { it.name.startsWith(".") }
        ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        ?.map { child ->
            editor_file_node(
                name = child.name,
                path = child.absolutePath,
                is_directory = child.isDirectory,
                depth = 0,
                child_count = if (child.isDirectory) child.visible_child_count() else 0,
                file_size = if (child.isFile) child.length() else 0L,
                is_loaded = !child.isDirectory
            )
        }
        ?: emptyList()
}

fun build_lazy_visible_file_nodes(
    root: File,
    expanded_paths: Set<String>,
    children_cache: Map<String, List<editor_file_node>>
): List<editor_file_node> {
    if (!root.exists() || !root.isDirectory) return emptyList()
    val nodes = mutableListOf<editor_file_node>()

    fun append_dir(dir: File, depth: Int, guides: List<Boolean>, source_node: editor_file_node? = null) {
        val path = dir.absolutePath
        val loaded = path in children_cache
        val children = children_cache[path].orEmpty()
        nodes.add(
            editor_file_node(
                name = source_node?.name ?: dir.name.ifBlank { dir.absolutePath },
                path = path,
                is_directory = true,
                depth = depth,
                child_count = if (loaded) children.size else source_node?.child_count ?: dir.visible_child_count(),
                tree_guides = guides,
                is_loaded = loaded
            )
        )

        if (path !in expanded_paths) return

        children.forEachIndexed { index, child ->
            val child_guides = guides + (index < children.lastIndex)
            if (child.is_directory) {
                append_dir(File(child.path), depth + 1, child_guides, child)
            } else {
                nodes.add(child.copy(depth = depth + 1, tree_guides = child_guides))
            }
        }
    }

    append_dir(root, 0, emptyList())
    return nodes
}

private fun File.visible_child_count(): Int {
    return listFiles()?.count { !it.name.startsWith(".") } ?: 0
}
