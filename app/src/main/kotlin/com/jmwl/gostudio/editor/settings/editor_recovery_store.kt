package com.jmwl.gostudio.editor.settings

import android.content.Context
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

private const val editor_recovery_dir_name = "editor_recovery"

/**
 * 未保存代码恢复草稿。
 *
 * 草稿存放在应用私有目录，不写回项目文件；使用临时文件 + fsync + 原子替换，
 * 避免进程被杀时留下半份内容。
 */
internal fun save_editor_recovery_draft(
    context: Context,
    project_dir: File,
    file_path: String,
    content: String
): Result<Unit> {
    return runCatching {
        val target = editor_recovery_file(context, project_dir, file_path)
        val directory = target.parentFile ?: error("恢复目录不存在")
        if (!directory.exists() && !directory.mkdirs()) error("创建恢复目录失败")

        val temp = File.createTempFile("draft-", ".tmp", directory)
        try {
            FileOutputStream(temp).use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                output.flush()
                output.fd.sync()
            }
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            sync_directory(directory)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}

internal fun load_editor_recovery_draft(
    context: Context,
    project_dir: File,
    file_path: String
): String? {
    val file = editor_recovery_file(context, project_dir, file_path)
    if (!file.isFile) return null
    return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
}

internal fun remove_editor_recovery_draft(
    context: Context,
    project_dir: File,
    file_path: String
): Boolean {
    val file = editor_recovery_file(context, project_dir, file_path)
    return file.delete()
}

internal fun rename_editor_recovery_draft(
    context: Context,
    project_dir: File,
    old_file_path: String,
    new_file_path: String
): Boolean {
    val content = load_editor_recovery_draft(context, project_dir, old_file_path) ?: return false
    val saved = save_editor_recovery_draft(context, project_dir, new_file_path, content).isSuccess
    if (saved) remove_editor_recovery_draft(context, project_dir, old_file_path)
    return saved
}

private fun editor_recovery_file(
    context: Context,
    project_dir: File,
    file_path: String
): File {
    val project_key = canonical_path(project_dir)
    val digest = MessageDigest.getInstance("SHA-256")
        .digest((project_key + '\u0000' + canonical_path(File(file_path))).toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
    return File(File(context.filesDir, editor_recovery_dir_name), digest)
}

private fun canonical_path(file: File): String {
    return runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
}

private fun sync_directory(directory: File) {
    runCatching {
        val descriptor = Os.open(directory.absolutePath, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }
}
