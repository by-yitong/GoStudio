package com.xc.code.editor.model

data class editor_file_node(
    val name: String,
    val path: String,
    val is_directory: Boolean,
    val depth: Int,
    val child_count: Int,
    val file_size: Long = 0L,
    val tree_guides: List<Boolean> = emptyList(),
    val is_loaded: Boolean = true
)
