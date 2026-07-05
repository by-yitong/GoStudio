package com.xc.code.editor.core

import java.io.File

fun replace_path_prefix(path: String, old_prefix: String, new_prefix: String): String {
    val normalized_old = old_prefix.trimEnd(File.separatorChar)
    return when {
        path == normalized_old -> new_prefix
        path.startsWith(normalized_old + File.separator) -> new_prefix + path.removePrefix(normalized_old)
        else -> path
    }
}

fun is_same_or_child_path(path: String, parent_path: String): Boolean {
    val normalized_parent = parent_path.trimEnd(File.separatorChar)
    return path == normalized_parent || path.startsWith(normalized_parent + File.separator)
}

fun is_readable_project_file(project_dir: File, file: File): Boolean {
    if (!file.exists() || !file.isFile) return false
    return runCatching {
        val root_path = project_dir.canonicalFile.toPath()
        val file_path = file.canonicalFile.toPath()
        file_path.startsWith(root_path)
    }.getOrDefault(false)
}

fun is_c_family_file(file_path: String?): Boolean {
    if (file_path.isNullOrBlank()) return true
    val name = File(file_path).name
    return name.endsWith(".c", ignoreCase = true) ||
        name.endsWith(".cc", ignoreCase = true) ||
        name.endsWith(".cpp", ignoreCase = true) ||
        name.endsWith(".cxx", ignoreCase = true) ||
        name.endsWith(".h", ignoreCase = true) ||
        name.endsWith(".hh", ignoreCase = true) ||
        name.endsWith(".hpp", ignoreCase = true) ||
        name.endsWith(".hxx", ignoreCase = true)
}
