package com.jmwl.gostudio.project

import java.io.File

enum class project_kind {
    GO,
    CMAKE,
    UNKNOWN
}

data class detected_project(
    val root_path: String,
    val kind: project_kind,
    val build_file_path: String? = null,
    val build_dir: String? = null,
    val compile_commands_path: String? = null
) {
    val is_go: Boolean
        get() = kind == project_kind.GO

    val is_cmake: Boolean
        get() = kind == project_kind.CMAKE
}

object project_detector {
    private const val go_mod_file_name = "go.mod"
    private const val cmake_file_name = "CMakeLists.txt"
    private const val default_cmake_build_dir = "build"
    private const val default_go_build_dir = "bin"
    private const val compile_commands_file_name = "compile_commands.json"

    fun detect_project(project_path: String): detected_project {
        val root = File(project_path).absoluteFile
        val root_path = root.absolutePath
        if (!root.isDirectory) {
            return detected_project(
                root_path = root_path,
                kind = project_kind.UNKNOWN
            )
        }

        val go_mod_file = File(root, go_mod_file_name)
        if (go_mod_file.isFile) {
            return detected_project(
                root_path = root_path,
                kind = project_kind.GO,
                build_file_path = go_mod_file.absolutePath,
                build_dir = File(root, default_go_build_dir).absolutePath
            )
        }

        val cmake_file = File(root, cmake_file_name)
        if (cmake_file.isFile) {
            val build_dir = File(root, default_cmake_build_dir)
            return detected_project(
                root_path = root_path,
                kind = project_kind.CMAKE,
                build_file_path = cmake_file.absolutePath,
                build_dir = build_dir.absolutePath,
                compile_commands_path = File(build_dir, compile_commands_file_name).absolutePath
            )
        }

        return detected_project(
            root_path = root_path,
            kind = project_kind.UNKNOWN
        )
    }
}
