package com.jmwl.gostudio.runtime

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * 编译产物导出：写入公共下载目录下的 gostudio/ 子目录，
 * 让用户在文件管理器里直接取到交叉编译出的二进制。
 */
object build_exporter {

    /**
     * 把产物复制到公共 Download/gostudio/<subdir>；同名覆盖旧版本，避免堆 "(1)" 副本。
     * 返回供日志展示的可读路径。
     */
    fun export_to_downloads(
        context: Context,
        source: File,
        display_name: String,
        subdir: String = "bin"
    ): Result<String> = runCatching {
        val export_dir = "Download/gostudio/$subdir"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            export_via_media_store(context, source, display_name, export_dir)
        } else {
            export_via_legacy_storage(source, display_name, export_dir)
        }
    }

    private fun export_via_media_store(context: Context, source: File, display_name: String, export_dir: String): String {
        val resolver = context.contentResolver
        // 先找同名条目复用（只可能是本应用之前写入的），没有再新建挂起条目。
        val existing_id = resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf("$export_dir/", display_name),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

        val uri = if (existing_id != null) {
            ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, existing_id)
        } else {
            resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, display_name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, export_dir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            ) ?: throw IllegalStateException("无法在 $export_dir 创建导出条目")
        }

        resolver.openOutputStream(uri, "w")?.use { output ->
            source.inputStream().use { it.copyTo(output) }
        } ?: throw IllegalStateException("无法打开导出条目写入流")
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
            null,
            null
        )
        return "$export_dir/$display_name"
    }

    private fun export_via_legacy_storage(source: File, display_name: String, export_dir: String): String {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            export_dir.removePrefix("Download/")
        )
        if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("无法创建 ${dir.absolutePath}")
        val target = File(dir, display_name)
        source.copyTo(target, overwrite = true)
        return target.absolutePath
    }
}
