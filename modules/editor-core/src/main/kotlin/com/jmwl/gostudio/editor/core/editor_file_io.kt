package com.jmwl.gostudio.editor.core

import java.io.File

data class editor_loaded_file(
    val file: File,
    val content: String
)

fun load_project_file(project_dir: File, file_path: String): Result<editor_loaded_file> {
    return runCatching {
        val file = File(file_path)
        require(is_readable_project_file(project_dir, file)) { "文件不存在或不在项目中" }
        require(!is_probably_binary_file(file)) { "二进制文件不支持用编辑器打开" }
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

/** 常见二进制/资源后缀：命中即直接拒绝，避免 readText 把整个文件读进来卡死。 */
private val binary_extensions = listOf(
    ".apk", ".bin", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".ico", ".svgz",
    ".zip", ".jar", ".7z", ".tar", ".gz", ".xz", ".zst", ".br",
    ".so", ".dex", ".class", ".o", ".a", ".exe", ".dll",
    ".keystore", ".jks", ".bks", ".pem", ".der", ".key",
    ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
    ".mp3", ".mp4", ".wav", ".ogg", ".flac", ".avi", ".mkv", ".webm", ".3gp",
    ".ttf", ".otf", ".woff", ".woff2",
    ".db", ".sqlite", ".sqlite3"
)

/**
 * 判断文件是否大概率是二进制：先看后缀黑名单，再嗅探头部 8KB 是否含 NUL 字节
 * （文本文件几乎不会出现 NUL，二进制文件几乎必有）。UTF-16 文本会误判为二进制，
 * 但代码项目里极少见，误判也比卡死好。
 */
fun is_probably_binary_file(file: File): Boolean {
    val lower_name = file.name.lowercase()
    if (binary_extensions.any { lower_name.endsWith(it) }) return true
    return try {
        file.inputStream().use { input ->
            val head = ByteArray(8 * 1024)
            var read = 0
            while (read < head.size) {
                val n = input.read(head, read, head.size - read)
                if (n < 0) break
                read += n
            }
            head.indexOfFirst { it == 0.toByte() } in 0 until read
        }
    } catch (_: Exception) {
        false
    }
}
