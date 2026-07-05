package com.xc.code.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

object uri_utils {
    
    fun get_path_from_uri(context: Context, uri: Uri): String {
        var path = ""
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val doc_id = DocumentsContract.getDocumentId(uri)
                val split = doc_id.split(":")
                val type = split[0]
                val sub_path = if (split.size > 1) split[1] else ""
                path = when (type.lowercase()) {
                    "primary" -> {
                        android.os.Environment.getExternalStorageDirectory().absolutePath + "/" + sub_path
                    }
                    else -> {
                        "/storage/" + type + "/" + sub_path
                    }
                }
            } else {
                path = uri.path ?: uri.toString()
            }
            if (path.startsWith("/tree/")) {
                path = path.substring(6)
            }
            if (path.startsWith("primary:")) {
                path = android.os.Environment.getExternalStorageDirectory().absolutePath + "/" + path.substring(8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            path = uri.path ?: uri.toString()
        }
        return path
    }
}