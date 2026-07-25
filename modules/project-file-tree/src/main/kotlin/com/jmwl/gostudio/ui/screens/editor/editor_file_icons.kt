package com.jmwl.gostudio.ui.screens.editor

import com.jmwl.gostudio.project_file_tree.R

fun editor_file_icon_res(name: String): Int {
    val lower_name = name.lowercase()
    return when {
        lower_name.endsWith(".json") -> R.drawable.ic_file_json
        else -> R.drawable.ic_file_generic
    }
}
