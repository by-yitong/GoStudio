package com.xc.code.ui.screens.editor

import com.xc.code.project_file_tree.R

fun editor_file_icon_res(name: String): Int {
    val lower_name = name.lowercase()
    return when {
        lower_name == "cmakelists.txt" -> R.drawable.ic_file_cmake
        lower_name.endsWith(".json") -> R.drawable.ic_file_json
        lower_name.endsWith(".cpp") || lower_name.endsWith(".cc") || lower_name.endsWith(".cxx") -> R.drawable.ic_file_cpp
        lower_name.endsWith(".c") -> R.drawable.ic_file_cpp
        lower_name.endsWith(".h") || lower_name.endsWith(".hpp") || lower_name.endsWith(".hh") || lower_name.endsWith(".hxx") -> R.drawable.ic_file_h
        else -> R.drawable.ic_file_generic
    }
}
