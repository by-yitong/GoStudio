package com.xc.code.toolchain

import com.xc.code.project.project_manager
import com.xc.code.core.logging.logger_manager

import java.io.File
import java.util.Properties

data class ndk_toolchain_info(
    val version: String,
    val aliases: Set<String>,
    val host_dir: File,
    val proot_dir: String,
    val llvm_bin_host_dir: File,
    val llvm_bin_proot_dir: String,
    val cmake_toolchain_file: String?
)

data class project_toolchain_environment(
    val environment: Map<String, String>,
    val ndk: ndk_toolchain_info?,
    val missing: List<String>
)

object toolchain_manager {

    private const val PROOT_XCODE_HOME = "/home/xcode"

    private val base_proot_path = listOf(
        "/home/.local/bin",
        "/usr/local/sbin",
        "/usr/local/bin",
        "/bin",
        "/usr/bin",
        "/sbin",
        "/usr/sbin",
        "/usr/games",
        "/usr/local/games"
    )

    private val removed_toolchain_env_blocks = listOf(
        "# >>> XCode toolchain environment >>>" to "# <<< XCode toolchain environment <<<",
        "# >>> xcode toolchain environment >>>" to "# <<< xcode toolchain environment <<<"
    )

    fun cleanup_removed_toolchain_environment() {
        listOf(".bashrc", ".profile").forEach { file_name ->
            val file = File(toolchain_runtime_provider.paths().home_dir, file_name)
            if (!file.exists()) return@forEach

            val current_content = file.readText()
            val cleaned_content = removed_toolchain_env_blocks.fold(current_content) { content, markers ->
                remove_removed_toolchain_environment_block(content, markers.first, markers.second)
            }
            if (cleaned_content != current_content) {
                file.writeText(cleaned_content.trimEnd() + "\n")
            }
        }
    }

    private fun remove_removed_toolchain_environment_block(
        content: String,
        start_marker: String,
        end_marker: String
    ): String {
        val start = content.indexOf(start_marker)
        val end = content.indexOf(end_marker)
        if (start < 0 || end < start) return content

        val after_end = end + end_marker.length
        val cleaned = (content.substring(0, start).trimEnd() + "\n" + content.substring(after_end).trimStart()).trimEnd()
        return remove_removed_toolchain_environment_block(cleaned, start_marker, end_marker)
    }

    fun proot_path(extra_paths: List<String> = emptyList()): String {
        return (extra_paths + base_proot_path)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(":")
    }

    fun available_cmake_versions(): List<String> {
        return installed_cmake_dirs().map { it.name }.distinct()
    }

    private fun installed_cmake_dirs(): List<File> {
        val cmake_root = File(toolchain_runtime_provider.paths().xcode_dir, "cmake")
        return cmake_root.listFiles()
            ?.filter { candidate -> File(candidate, "bin/cmake").isFile && File(candidate, "bin/ninja").isFile }
            ?.sortedWith { left, right -> compare_versions(right.name, left.name) }
            ?: emptyList()
    }

    private fun installed_cmake_bin_paths(requested_version: String = ""): List<String> {
        val cmakes = installed_cmake_dirs()
        val selected = if (requested_version.isBlank()) cmakes.firstOrNull() else select_cmake(cmakes, requested_version)
        return selected?.let { listOf(to_proot_xcode_path(File(it, "bin"))) }.orEmpty()
    }

    fun installed_ndks(): List<ndk_toolchain_info> {
        val ndk_root = File(toolchain_runtime_provider.paths().xcode_dir, "ndk")
        return ndk_root.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { candidate -> create_ndk_info(candidate) }
            ?.sortedWith { left, right -> compare_versions(right.version, left.version) }
            ?: emptyList()
    }

    fun available_ndk_versions(): List<String> {
        return installed_ndks().map { it.version }.distinct()
    }

    fun installed_ndk_version_keys(): Set<String> {
        return installed_ndks().flatMap { info -> info.aliases + info.version + info.host_dir.name }.toSet()
    }

    fun is_ndk_installed(): Boolean {
        return installed_ndks().isNotEmpty()
    }

    fun project_environment(project_path: String): project_toolchain_environment {
        val project_info = project_manager.get_project_info(project_path)
        val ndks = installed_ndks()
        val requested_ndk = project_info?.ndk_version.orEmpty()
        val requested_cmake = project_info?.cmake_version.orEmpty()
        val ndk = select_ndk(ndks, requested_ndk)
        val cmake = select_cmake(installed_cmake_dirs(), requested_cmake)
        val missing = mutableListOf<String>()

        if (requested_ndk.isBlank()) {
            missing += "项目未配置 NDK"
        } else if (ndk == null) {
            missing += "NDK $requested_ndk 未安装或结构无效"
        }
        if (requested_cmake.isBlank()) {
            missing += "项目未配置 CMake"
        } else if (cmake == null) {
            missing += "CMake $requested_cmake 未安装或结构无效"
        }
        val path_entries = mutableListOf<String>()
        cmake?.let { path_entries += to_proot_xcode_path(File(it, "bin")) }
        ndk?.let { path_entries += it.llvm_bin_proot_dir }

        val environment = linkedMapOf(
            "XCODE_HOME" to PROOT_XCODE_HOME,
            "PATH" to proot_path(path_entries)
        )

        ndk?.let { info ->
            environment["ANDROID_NDK_HOME"] = info.proot_dir
            environment["ANDROID_NDK_ROOT"] = info.proot_dir
        }
        return project_toolchain_environment(
            environment = environment,
            ndk = ndk,
            missing = missing
        )
    }

    fun is_cmake_installed(): Boolean {
        return installed_cmake_bin_paths().isNotEmpty()
    }

    private fun shell_quote(value: String): String {
        if (value.isEmpty()) return "''"
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private fun create_ndk_info(candidate: File): ndk_toolchain_info? {
        val ndk_dir = find_ndk_dir(candidate) ?: return null
        val llvm_bin = find_ndk_llvm_bin(ndk_dir) ?: return null
        val version = read_ndk_revision(ndk_dir).ifBlank {
            extract_version(candidate.name) ?: candidate.name
        }
        val aliases = build_version_aliases(version, candidate.name, ndk_dir.name)
        val cmake_toolchain = File(ndk_dir, "build/cmake/android.toolchain.cmake")
            .takeIf { it.isFile }
            ?.let { to_proot_xcode_path(it) }

        return ndk_toolchain_info(
            version = version,
            aliases = aliases,
            host_dir = ndk_dir,
            proot_dir = to_proot_xcode_path(ndk_dir),
            llvm_bin_host_dir = llvm_bin,
            llvm_bin_proot_dir = to_proot_xcode_path(llvm_bin),
            cmake_toolchain_file = cmake_toolchain
        )
    }

    private fun find_ndk_dir(candidate: File): File? {
        if (is_valid_ndk_dir(candidate)) return candidate
        return candidate.listFiles()
            ?.filter { it.isDirectory }
            ?.firstOrNull { is_valid_ndk_dir(it) }
    }

    private fun is_valid_ndk_dir(dir: File): Boolean {
        return find_ndk_llvm_bin(dir) != null && File(dir, "build/cmake/android.toolchain.cmake").isFile
    }

    private fun find_ndk_llvm_bin(ndk_dir: File): File? {
        val prebuilt_dir = File(ndk_dir, "toolchains/llvm/prebuilt")
        return prebuilt_dir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { File(it, "bin") }
            ?.firstOrNull { bin ->
                bin.isDirectory && (File(bin, "clang").isFile || File(bin, "clang++").isFile)
            }
    }

    private fun read_ndk_revision(ndk_dir: File): String {
        val source_properties = File(ndk_dir, "source.properties")
        if (!source_properties.isFile) return ""

        return runCatching {
            val properties = Properties()
            source_properties.inputStream().use { stream -> properties.load(stream) }
            properties.getProperty("Pkg.Revision").orEmpty().trim()
        }.getOrDefault("")
    }

    private fun select_cmake(cmakes: List<File>, requested_version: String): File? {
        if (requested_version.isBlank()) return null
        val requested_key = normalize_version_key(requested_version)
        return cmakes.firstOrNull { cmake -> normalize_version_key(cmake.name) == requested_key }
    }

    private fun select_ndk(ndks: List<ndk_toolchain_info>, requested_version: String): ndk_toolchain_info? {
        if (requested_version.isBlank()) return null
        val requested_key = normalize_version_key(requested_version)
        return ndks.firstOrNull { info ->
            info.aliases.any { alias -> normalize_version_key(alias) == requested_key }
        }
    }

    private fun build_version_aliases(vararg values: String): Set<String> {
        val aliases = values
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()

        values.forEach { value ->
            val major = Regex("\\d+").find(value)?.value
            if (!major.isNullOrBlank()) {
                aliases += major
                aliases += "r$major"
            }
            extract_version(value)?.let { aliases += it }
        }

        val revision_aliases = mapOf(
            "29.0.14206865" to "r29"
        )
        aliases.toList().forEach { alias ->
            revision_aliases[alias]?.let { ndk_release ->
                aliases += ndk_release
                aliases += "android-ndk-$ndk_release"
            }
        }

        return aliases
    }

    private fun extract_version(value: String): String? {
        return Regex("\\d+(?:\\.\\d+)+(?:[a-zA-Z]?)").find(value)?.value
            ?: Regex("r\\d+[a-zA-Z]?").find(value)?.value
    }

    private fun normalize_version_key(value: String): String {
        return value.trim()
            .lowercase()
            .removePrefix("android-ndk-")
            .removePrefix("cmake-")
            .removePrefix("cmake ")
            .removePrefix("cmake_")
    }

    private fun to_proot_xcode_path(file: File): String {
        val base_path = toolchain_runtime_provider.paths().xcode_dir.absolutePath.trimEnd('/')
        val target_path = file.absolutePath.trimEnd('/')
        val relative_path = target_path.removePrefix(base_path).trimStart('/')
        return if (relative_path.isBlank()) PROOT_XCODE_HOME else "$PROOT_XCODE_HOME/$relative_path"
    }

    private fun compare_versions(left: String, right: String): Int {
        val left_numbers = Regex("\\d+").findAll(left).mapNotNull { it.value.toIntOrNull() }.toList()
        val right_numbers = Regex("\\d+").findAll(right).mapNotNull { it.value.toIntOrNull() }.toList()
        val max_size = maxOf(left_numbers.size, right_numbers.size)

        for (index in 0 until max_size) {
            val left_value = left_numbers.getOrElse(index) { 0 }
            val right_value = right_numbers.getOrElse(index) { 0 }
            if (left_value != right_value) return left_value.compareTo(right_value)
        }

        return left.compareTo(right)
    }
}