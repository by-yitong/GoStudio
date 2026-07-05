package com.xc.code.editor.core

import java.io.File

data class editor_loaded_file(
    val file: File,
    val content: String
)

fun load_project_file(project_dir: File, file_path: String): Result<editor_loaded_file> {
    return runCatching {
        val file = File(file_path)
        require(is_readable_project_file(project_dir, file)) { "文件不存在或不在项目中" }
        editor_loaded_file(file = file.absoluteFile, content = file.readText())
    }
}

fun load_pinned_project_files(project_dir: File, paths: List<String>): List<editor_loaded_file> {
    return paths.mapNotNull { path ->
        load_project_file(project_dir, path).getOrNull()
    }
}

fun save_project_file(file_path: String, content: String): Result<File> {
    return runCatching {
        val file = File(file_path)
        file.writeText(content)
        file
    }
}
