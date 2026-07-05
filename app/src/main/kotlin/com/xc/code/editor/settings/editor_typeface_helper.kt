package com.xc.code.editor.settings

import com.xc.code.editor.model.editor_settings_state

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import com.xc.code.R
import java.io.File

internal fun load_editor_typeface(context: Context, settings: editor_settings_state): Typeface {
    if (settings.font_family == "imported") {
        val imported_font = File(settings.imported_font_path)
        if (imported_font.exists() && imported_font.isFile) {
            return runCatching { Typeface.createFromFile(imported_font) }.getOrDefault(Typeface.MONOSPACE)
        }
        return Typeface.MONOSPACE
    }

    val font_res = when (settings.font_family) {
        "roboto" -> R.font.roboto_regular
        "jetbrains_mono" -> R.font.jetbrains_mono_regular
        else -> return Typeface.MONOSPACE
    }
    return ResourcesCompat.getFont(context, font_res) ?: Typeface.MONOSPACE
}

internal fun import_editor_font(context: Context, uri: Uri): String {
    val fonts_dir = File(context.filesDir, "editor_fonts").apply { mkdirs() }
    val target = File(fonts_dir, "imported_editor_font")
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("无法读取字体文件")

    input.use { source ->
        target.outputStream().use { output ->
            source.copyTo(output)
        }
    }

    Typeface.createFromFile(target)
    return target.absolutePath
}
