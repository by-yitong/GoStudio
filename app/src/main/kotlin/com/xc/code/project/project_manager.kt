package com.xc.code.project

import com.xc.code.toolchain.toolchain_runtime_provider
import com.xc.code.toolchain.toolchain_manager

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object project_manager {
    private const val max_recent_projects = 20
    private const val project_config_dir_name = ".xcode"
    private const val project_config_file_name = ".xcode-project.json"
    private val json = GsonBuilder().setPrettyPrinting().create()
    private val valid_project_name = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
    private val valid_android_platform = Regex("^android-(2[1-9]|3[0-9]|4[0-9])$")
    private val supported_cpp_standards = setOf("11", "14", "17", "20", "23", "26")
    private val supported_build_abis = setOf("x86_64", "arm64-v8a", "x86", "armeabi-v7a")
    private val supported_build_types = setOf("Debug", "Release")
    private val default_clang_format = """
        ---
        Language: Cpp
        BasedOnStyle: LLVM
        Standard: Latest
        IndentWidth: 4
        TabWidth: 4
        UseTab: Never
        ColumnLimit: 165
        AccessModifierOffset: -4
        ConstructorInitializerIndentWidth: 4
        ContinuationIndentWidth: 4
        BreakBeforeBraces: Attach
        AllowShortBlocksOnASingleLine: Never
        AllowShortFunctionsOnASingleLine: Empty
        AllowShortIfStatementsOnASingleLine: WithoutElse
        AllowShortLoopsOnASingleLine: false
        BinPackArguments: true
        BinPackParameters: BinPack
        PointerAlignment: Right
        DerivePointerAlignment: false
        SpaceBeforeParens: ControlStatements
        SpacesInAngles: Never
        SpacesInParens: Never
        SpacesInSquareBrackets: false
        Cpp11BracedListStyle: true
        SortIncludes: CaseSensitive
        IncludeBlocks: Preserve
        IndentCaseLabels: true
        IndentCaseBlocks: false
        IndentPPDirectives: BeforeHash
        MaxEmptyLinesToKeep: 1
        KeepEmptyLines:
          AtStartOfFile: true
          AtStartOfBlock: true
          AtEndOfFile: false
        FixNamespaceComments: true
        ReflowComments: Always
        InsertNewlineAtEOF: false
    """.trimIndent() + "\n"

    suspend fun create_project(
        name: String,
        path: String,
        template_id: String,
        ndk_version: String,
        cmake_version: String,
        android_platform: String,
        cpp_standard: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val project_name = name.trim()
            val parent_dir = File(path.trim())
            val selected_platform = android_platform.trim()
            val selected_cpp_standard = cpp_standard.trim()

            if (!valid_project_name.matches(project_name)) {
                return@withContext Result.failure(IllegalArgumentException("项目名称只能包含字母、数字和下划线，且不能以数字开头"))
            }

            if (path.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("项目路径不能为空"))
            }

            if (ndk_version.isBlank()) {
                return@withContext Result.failure(IllegalStateException("未检测到可用 NDK，无法创建项目"))
            }

            if (cmake_version.isBlank()) {
                return@withContext Result.failure(IllegalStateException("未检测到可用 CMake/Ninja，无法创建项目"))
            }

            if (!valid_android_platform.matches(selected_platform)) {
                return@withContext Result.failure(IllegalArgumentException("Android Platform 必须为 android-21 到 android-49"))
            }

            if (selected_cpp_standard !in supported_cpp_standards) {
                return@withContext Result.failure(IllegalArgumentException("不支持的 C++ 标准: C++$selected_cpp_standard"))
            }

            if (ndk_version !in toolchain_manager.available_ndk_versions()) {
                return@withContext Result.failure(IllegalStateException("NDK " + ndk_version + " 未安装或项目结构无效"))
            }

            if (cmake_version !in toolchain_manager.available_cmake_versions()) {
                return@withContext Result.failure(IllegalStateException("CMake " + cmake_version + " 未安装或结构无效"))
            }

            val project_dir = File(parent_dir, project_name)
            if (project_dir.exists()) {
                return@withContext Result.failure(IllegalStateException("项目已存在"))
            }

            if (!project_dir.mkdirs()) {
                return@withContext Result.failure(IllegalStateException("无法创建项目目录"))
            }

            File(project_dir, "src").mkdirs()
            File(project_dir, "include").mkdirs()

            when (template_id) {
                "executable" -> create_executable_project(project_dir, project_name)
                "static_lib" -> create_static_library_project(project_dir, project_name)
                "dynamic_lib" -> create_dynamic_library_project(project_dir, project_name)
                else -> return@withContext Result.failure(IllegalArgumentException("未知项目模板"))
            }

            create_cmake_lists(project_dir, project_name, template_id)
            write_project_config(
                dir = project_dir,
                name = project_name,
                ndk_version = ndk_version,
                cmake_version = cmake_version,
                template_id = template_id,
                build = project_build_config(
                    abi = "arm64-v8a",
                    platform = selected_platform,
                    cpp_standard = selected_cpp_standard,
                    build_type = "Debug"
                )
            )
            Result.success(project_dir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun create_project_entry(
        project_path: String,
        parent_path: String,
        name: String,
        directory: Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val entry_name = normalize_project_entry_name(name)
            require(entry_name.isNotBlank()) { "名称不能为空" }

            val root_path = File(project_path).canonicalFile.toPath()
            val parent_dir = File(parent_path).canonicalFile
            val parent_dir_path = parent_dir.toPath()
            require(parent_dir.exists() && parent_dir.isDirectory && parent_dir_path.startsWith(root_path)) {
                "目标文件夹不存在或不在项目中"
            }

            val target = File(parent_dir, entry_name)
            val target_path = target.canonicalFile.toPath()
            require(target_path.startsWith(root_path)) { "路径不能超出项目目录" }
            require(!target.exists()) { if (directory) "文件夹已存在" else "文件已存在" }

            if (directory) {
                if (!target.mkdirs()) {
                    throw IllegalStateException("无法创建文件夹")
                }
            } else {
                target.parentFile?.mkdirs()
                if (!target.createNewFile()) {
                    throw IllegalStateException("无法创建文件")
                }
            }
            target
        }
    }

    suspend fun rename_project_entry(
        project_path: String,
        path: String,
        new_name: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val entry_name = normalize_project_entry_name(new_name)
            require(entry_name.isNotBlank()) { "名称不能为空" }
            require(!entry_name.contains('/') && !entry_name.contains('\\')) { "名称不能包含路径分隔符" }

            val root = File(project_path).canonicalFile
            val source = File(path).canonicalFile
            val source_path = source.toPath()
            require(source.exists() && source_path.startsWith(root.toPath())) { "节点不存在或不在项目中" }
            require(source_path != root.toPath()) { "不能重命名项目根目录" }

            val parent = source.parentFile ?: throw IllegalStateException("无法读取父目录")
            val target = File(parent, entry_name).canonicalFile
            require(target.toPath().startsWith(root.toPath())) { "路径不能超出项目目录" }
            require(!target.exists()) { "同名文件或文件夹已存在" }
            if (!source.renameTo(target)) {
                throw IllegalStateException("重命名失败")
            }
            source.absolutePath to target.absolutePath
        }
    }

    suspend fun resolve_project_entry_for_delete(project_path: String, path: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val root = File(project_path).canonicalFile
            val source = File(path).canonicalFile
            val source_path = source.toPath()
            require(source.exists() && source_path.startsWith(root.toPath())) { "节点不存在或不在项目中" }
            require(source_path != root.toPath()) { "不能删除项目根目录" }
            source
        }
    }

    suspend fun delete_project_entry(project_path: String, path: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val source = resolve_project_entry_for_delete(project_path, path).getOrThrow()
            val source_path = source.absolutePath
            val parent_path = source.parentFile?.absolutePath ?: File(project_path).absolutePath
            val deleted = if (source.isDirectory) source.deleteRecursively() else source.delete()
            require(deleted) { "删除失败" }
            source_path to parent_path
        }
    }

    private fun normalize_project_entry_name(name: String): String {
        return name.trim().trim('/', '\\')
    }

    private fun create_executable_project(dir: File, name: String) {
        File(dir, "src/main.cpp").writeText("""
        #include <iostream>

        int main(int argc, char* argv[]) {
            std::cout << "Hello from $name!" << std::endl;
            std::cout << "XCode Project Created Successfully!" << std::endl;
            return 0;
        }
        """.trimIndent())
    }

    private fun create_static_library_project(dir: File, name: String) {
        File(dir, "include/${name}.hpp").writeText(library_header(name, export_macro = false))
        File(dir, "src/${name}.cpp").writeText("""
        #include <iostream>
        #include "${name}.hpp"

        int ${name}_add(int a, int b) {
            return a + b;
        }

        void ${name}_init(void) {
            std::cout << "$name static library initialized" << std::endl;
        }

        void ${name}_cleanup(void) {
            std::cout << "$name static library cleaned up" << std::endl;
        }
        """.trimIndent())
    }

    private fun create_dynamic_library_project(dir: File, name: String) {
        val macro = "${name.uppercase()}_API"
        File(dir, "include/${name}.hpp").writeText(library_header(name, export_macro = true))
        File(dir, "src/${name}.cpp").writeText("""
        #include <iostream>
        #include "${name}.hpp"

        $macro int ${name}_add(int a, int b) {
            return a + b;
        }

        $macro void ${name}_init(void) {
            std::cout << "$name dynamic library initialized" << std::endl;
        }

        $macro void ${name}_cleanup(void) {
            std::cout << "$name dynamic library cleaned up" << std::endl;
        }
        """.trimIndent())
    }

    private fun library_header(name: String, export_macro: Boolean): String {
        val guard = "${name.uppercase()}_HPP"
        val macro = "${name.uppercase()}_API"
        val export_block = if (export_macro) {
            """
            #if defined(_WIN32)
                #ifdef ${name.uppercase()}_EXPORTS
                    #define $macro __declspec(dllexport)
                #else
                    #define $macro __declspec(dllimport)
                #endif
            #else
                #define $macro __attribute__((visibility("default")))
            #endif

            """.trimIndent() + "\n"
        } else {
            ""
        }
        val prefix = if (export_macro) "$macro " else ""

        return """
        #ifndef $guard
        #define $guard

        #ifdef __cplusplus
        extern "C" {
        #endif

        $export_block${prefix}int ${name}_add(int a, int b);
        ${prefix}void ${name}_init(void);
        ${prefix}void ${name}_cleanup(void);

        #ifdef __cplusplus
        }
        #endif

        #endif
        """.trimIndent()
    }

    private fun create_cmake_lists(
        dir: File,
        name: String,
        template_id: String
    ) {
        val cmake_ver = "3.28.3"
        val content = when (template_id) {
            "executable" -> """
            cmake_minimum_required(VERSION $cmake_ver)

            set(XCODE_SUPPORTED_ABIS x86_64 arm64-v8a x86 armeabi-v7a CACHE STRING "Supported Android ABIs")

            if(ANDROID_ABI AND NOT ANDROID_ABI IN_LIST XCODE_SUPPORTED_ABIS)
                message(FATAL_ERROR "Unsupported Android ABI: ${'$'}{ANDROID_ABI}")
            endif()

            project($name CXX)

            add_executable($name src/main.cpp)
            target_include_directories($name PRIVATE include)

            set_target_properties($name PROPERTIES
                RUNTIME_OUTPUT_DIRECTORY "${'$'}{CMAKE_BINARY_DIR}/bin"
            )

            if(CMAKE_BUILD_TYPE STREQUAL "Release")
                add_custom_command(TARGET $name POST_BUILD
                    COMMAND ${'$'}{CMAKE_STRIP} --strip-all "${'$'}<TARGET_FILE:$name>"
                )
            endif()
            """.trimIndent()

            "static_lib" -> """
            cmake_minimum_required(VERSION $cmake_ver)

            set(XCODE_SUPPORTED_ABIS x86_64 arm64-v8a x86 armeabi-v7a CACHE STRING "Supported Android ABIs")

            if(ANDROID_ABI AND NOT ANDROID_ABI IN_LIST XCODE_SUPPORTED_ABIS)
                message(FATAL_ERROR "Unsupported Android ABI: ${'$'}{ANDROID_ABI}")
            endif()

            project($name CXX)

            add_library($name STATIC src/${name}.cpp)
            target_include_directories($name PUBLIC include)

            set_target_properties($name PROPERTIES
                ARCHIVE_OUTPUT_DIRECTORY "${'$'}{CMAKE_BINARY_DIR}/lib"
            )

            if(CMAKE_BUILD_TYPE STREQUAL "Release")
                add_custom_command(TARGET $name POST_BUILD
                    COMMAND ${'$'}{CMAKE_STRIP} --strip-all "${'$'}<TARGET_FILE:$name>"
                )
            endif()
            """.trimIndent()

            "dynamic_lib" -> """
            cmake_minimum_required(VERSION $cmake_ver)

            set(XCODE_SUPPORTED_ABIS x86_64 arm64-v8a x86 armeabi-v7a CACHE STRING "Supported Android ABIs")

            if(ANDROID_ABI AND NOT ANDROID_ABI IN_LIST XCODE_SUPPORTED_ABIS)
                message(FATAL_ERROR "Unsupported Android ABI: ${'$'}{ANDROID_ABI}")
            endif()

            project($name CXX)
            set(CMAKE_POSITION_INDEPENDENT_CODE ON)

            add_library($name SHARED src/${name}.cpp)
            target_include_directories($name PUBLIC include)
            target_compile_definitions($name PRIVATE ${name.uppercase()}_EXPORTS)

            set_target_properties($name PROPERTIES
                LIBRARY_OUTPUT_DIRECTORY "${'$'}{CMAKE_BINARY_DIR}/lib"
                RUNTIME_OUTPUT_DIRECTORY "${'$'}{CMAKE_BINARY_DIR}/bin"
            )

            if(CMAKE_BUILD_TYPE STREQUAL "Release")
                add_custom_command(TARGET $name POST_BUILD
                    COMMAND ${'$'}{CMAKE_STRIP} --strip-all "${'$'}<TARGET_FILE:$name>"
                )
            endif()
            """.trimIndent()

            else -> ""
        }
        File(dir, "CMakeLists.txt").writeText(content)
    }

    private fun project_config_file(project_dir: File): File {
        return File(File(project_dir, project_config_dir_name), project_config_file_name)
    }

    fun ensure_project_clang_format(path: String): Result<Unit> {
        return runCatching {
            val project_dir = File(path)
            if (!project_dir.isDirectory) return@runCatching

            val clang_format_file = File(project_dir, ".clang-format")
            if (!clang_format_file.exists()) {
                clang_format_file.writeText(default_clang_format)
            }
        }
    }

    fun ensure_project_config(path: String): Result<Unit> {
        return runCatching {
            val project_dir = File(path)
            require(project_dir.exists() && project_dir.isDirectory) { "项目目录不存在" }
            require(File(project_dir, "CMakeLists.txt").isFile) { "不是 CMake 项目" }

            val config_file = project_config_file(project_dir)
            if (config_file.isFile) return@runCatching

            write_project_config(
                dir = project_dir,
                name = project_dir.name.ifBlank { "CMakeProject" },
                ndk_version = "",
                cmake_version = "",
                template_id = "imported",
                build = infer_imported_project_build_config(project_dir)
            )
        }
    }

    private fun write_project_config(
        dir: File,
        name: String,
        ndk_version: String,
        cmake_version: String,
        template_id: String,
        build: project_build_config = project_build_config()
    ) {
        val normalized_build = normalize_project_build_config(build)
        val config = project_config(
            name = name,
            ndk_version = ndk_version,
            cmake_version = cmake_version,
            template = template_id,
            created = System.currentTimeMillis(),
            build = normalized_build
        )
        project_config_file(dir).apply {
            parentFile?.mkdirs()
            writeText(json.toJson(config) + "\n")
        }
    }

    fun read_project_build_config(path: String): project_build_config {
        return read_project_ide_config(path).build
    }

    fun read_project_ide_config(path: String): project_ide_config {
        return try {
            val config_file = project_config_file(File(path))
            val config = json.fromJson(config_file.readText(), project_config::class.java)
            project_ide_config(
                ndk_version = config?.ndk_version.orEmpty(),
                cmake_version = config?.cmake_version.orEmpty(),
                build = normalize_project_build_config(config?.build ?: project_build_config())
            )
        } catch (_: Exception) {
            project_ide_config()
        }
    }

    fun save_project_ide_config(path: String, ide_config: project_ide_config): Result<Unit> {
        return runCatching {
            val selected_ndk = ide_config.ndk_version.trim()
            val selected_cmake = ide_config.cmake_version.trim()
            require(selected_ndk.isNotBlank()) { "NDK 不能为空" }
            require(selected_ndk in toolchain_manager.available_ndk_versions()) { "NDK $selected_ndk 未安装或结构无效" }
            require(selected_cmake.isNotBlank()) { "CMake 不能为空" }
            require(selected_cmake in toolchain_manager.available_cmake_versions()) { "CMake $selected_cmake 未安装或结构无效" }

            val project_dir = File(path)
            val config_file = project_config_file(project_dir)
            val config = json.fromJson(config_file.readText(), project_config::class.java)
                ?: throw IllegalStateException("项目配置文件损坏")
            val normalized_build = normalize_project_build_config(ide_config.build)
            config_file.writeText(json.toJson(config.copy(ndk_version = selected_ndk, cmake_version = selected_cmake, build = normalized_build)) + "\n")
        }
    }

    private fun infer_imported_project_build_config(project_dir: File): project_build_config {
        val cmake_file = File(project_dir, "CMakeLists.txt")
        if (!cmake_file.isFile) return project_build_config()

        val content = cmake_file.readText()
        val abi = infer_cmake_android_abi(content) ?: project_build_config().abi
        val platform = infer_cmake_android_platform(content) ?: project_build_config().platform
        val cpp_standard = infer_cmake_cpp_standard(content) ?: project_build_config().cpp_standard
        val build_type = infer_cmake_build_type(content) ?: project_build_config().build_type
        return normalize_project_build_config(
            project_build_config(
                abi = abi,
                platform = platform,
                cpp_standard = cpp_standard,
                build_type = build_type
            )
        )
    }

    private fun infer_cmake_android_abi(content: String): String? {
        if ("CMAKE_ANDROID_ARCH_ABI" !in content && "ANDROID_ABI" !in content) return null
        Regex("set\\s*\\(\\s*ANDROID_ABI\\s+([^\\s)]+)", RegexOption.IGNORE_CASE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it in supported_build_abis }
            ?.let { return it }
        return Regex("ANDROID_ABI\\s+STREQUAL\\s+\"(arm64-v8a|armeabi-v7a|x86_64|x86)\"", RegexOption.IGNORE_CASE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun infer_cmake_android_platform(content: String): String? {
        val explicit_platform = if ("ANDROID_PLATFORM" in content || "CMAKE_SYSTEM_VERSION" in content) {
            Regex("(?<![A-Za-z0-9_-])(?:android-)?(?:2[1-9]|3[0-9]|4[0-9])(?![A-Za-z0-9_-])")
                .findAll(content)
                .map { it.value.let { value -> if (value.startsWith("android-")) value else "android-$value" } }
                .toSet()
                .singleOrNull()
        } else {
            null
        }
        if (explicit_platform != null) return explicit_platform
        return if (uses_android_vulkan(content)) "android-24" else null
    }

    private fun infer_cmake_cpp_standard(content: String): String? {
        return Regex("set\\s*\\(\\s*CMAKE_CXX_STANDARD\\s+([0-9]+)", RegexOption.IGNORE_CASE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it in supported_cpp_standards }
    }

    private fun infer_cmake_build_type(content: String): String? {
        return Regex("set\\s*\\(\\s*CMAKE_BUILD_TYPE\\s+(Debug|Release)", RegexOption.IGNORE_CASE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun uses_android_vulkan(content: String): Boolean {
        return Regex("find_library\\s*\\([^)]*\\bvulkan\\b", RegexOption.IGNORE_CASE).containsMatchIn(content) ||
            Regex("target_link_libraries\\s*\\([^)]*\\bvulkan\\b", RegexOption.IGNORE_CASE).containsMatchIn(content)
    }

    private fun normalize_project_build_config(build: project_build_config): project_build_config {
        val abi = build.abi.takeIf { it in supported_build_abis } ?: "arm64-v8a"
        val platform = build.platform.takeIf { valid_android_platform.matches(it) } ?: "android-24"
        val cpp_standard = build.cpp_standard.takeIf { it in supported_cpp_standards } ?: "20"
        val build_type = build.build_type.takeIf { it in supported_build_types } ?: "Debug"
        val parallel_jobs = build.parallel_jobs.coerceIn(0, 8)
        val extra_cmake_args = build.extra_cmake_args.orEmpty().trim()
        return project_build_config(
            abi = abi,
            platform = platform,
            cpp_standard = cpp_standard,
            build_type = build_type,
            parallel_jobs = parallel_jobs,
            extra_cmake_args = extra_cmake_args
        )
    }

    fun get_project_last_opened(path: String): String {
        return try {
            val last_modified = File(path).takeIf { it.exists() }?.lastModified() ?: System.currentTimeMillis()
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(last_modified))
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }

    fun update_project_opened_time(path: String) {
        try {
            val project_dir = File(path)
            if (project_dir.exists()) {
                project_dir.setLastModified(System.currentTimeMillis())
            }
        } catch (_: Exception) {
        }
    }

    fun get_project_info(path: String): project_info? {
        return try {
            require_xcode_project_info(path)
        } catch (_: Exception) {
            null
        }
    }

    private fun require_xcode_project_info(path: String): project_info {
        val project_dir = File(path)
        require(project_dir.exists() && project_dir.isDirectory) { "项目目录不存在" }
        require(File(project_dir, "CMakeLists.txt").isFile) { "不是 CMake 项目" }

        val config_file = project_config_file(project_dir)
        require(config_file.isFile) { "不是 XCode 项目，缺少 .xcode/.xcode-project.json" }

        val config = try {
            json.fromJson(config_file.readText(), project_config::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("项目配置文件损坏")
        } ?: throw IllegalStateException("项目配置文件损坏")

        require(config.name.isNotBlank()) { "项目配置缺少名称" }
        require(config.template.isNotBlank()) { "项目配置缺少模板类型" }

        return project_info(
            name = config.name,
            path = project_dir.absolutePath,
            ndk_version = config.ndk_version,
            cmake_version = config.cmake_version,
            template = config.template
        )
    }

    suspend fun get_recent_projects(): List<recent_project_info> = withContext(Dispatchers.IO) {
        val records = load_recent_project_records()
            .filter { runCatching { require_xcode_project_info(it.path) }.isSuccess }
            .sortedByDescending { it.opened_at }
            .take(max_recent_projects)
        write_recent_project_records(records)
        records.map { create_recent_project_info(it) }
    }

    suspend fun check_project_toolchain(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val info = require_xcode_project_info(path)
            val project_dir = File(info.path)
            val environment = toolchain_manager.project_environment(project_dir.absolutePath)
            require(environment.missing.isEmpty()) { environment.missing.joinToString("；") }
        }
    }

    suspend fun add_recent_project(path: String): Result<recent_project_info> = withContext(Dispatchers.IO) {
        try {
            val project_path = normalize_project_path(path)
            if (project_path.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("项目路径不能为空"))
            }

            val project_dir = File(project_path)
            if (!project_dir.exists()) {
                return@withContext Result.failure(IllegalArgumentException("项目目录不存在"))
            }
            if (!project_dir.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("项目路径不是目录"))
            }

            val now = System.currentTimeMillis()
            val info = require_xcode_project_info(project_path)
            val record = recent_project_record(
                name = info.name,
                path = project_path,
                cmake_version = info.cmake_version,
                ndk_version = info.ndk_version,
                template = info.template,
                opened_at = now
            )

            val records = read_recent_project_records()
                .filterNot { normalize_project_path(it.path) == project_path }
                .toMutableList()
            records.add(0, record)
            write_recent_project_records(records.take(max_recent_projects))
            runCatching { project_dir.setLastModified(now) }

            Result.success(create_recent_project_info(record))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun remove_recent_project(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val project_path = normalize_project_path(path)
            val records = load_recent_project_records()
                .filterNot { normalize_project_path(it.path) == project_path }
            write_recent_project_records(records)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun recent_projects_file(): File {
        return File(toolchain_runtime_provider.paths().home_dir.parentFile, "recent_projects.json")
    }

    private fun load_recent_project_records(): MutableList<recent_project_record> {
        val file = recent_projects_file()
        if (file.exists()) {
            return read_recent_project_records()
        }

        val discovered_records = discover_project_records()
            .sortedByDescending { it.opened_at }
            .take(max_recent_projects)
        write_recent_project_records(discovered_records)
        return discovered_records.toMutableList()
    }

    private fun read_recent_project_records(): MutableList<recent_project_record> {
        return try {
            val file = recent_projects_file()
            if (!file.exists()) return mutableListOf()

            val type = object : TypeToken<List<recent_project_record>>() {}.type
            val records = json.fromJson<List<recent_project_record>>(file.readText(), type) ?: emptyList()
            records
                .filter { it.path.isNotBlank() }
                .distinctBy { normalize_project_path(it.path) }
                .toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun write_recent_project_records(records: List<recent_project_record>) {
        val file = recent_projects_file()
        file.parentFile?.mkdirs()
        file.writeText(json.toJson(records.take(max_recent_projects)))
    }

    private fun discover_project_records(): List<recent_project_record> {
        return try {
            val root_dir = File(toolchain_runtime_provider.paths().external_storage_dir ?: File("/sdcard"), "XCodeProjects")
            val projects = root_dir.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedByDescending { it.lastModified() }
                ?.take(max_recent_projects)
                ?: emptyList()

            projects.mapNotNull { project_dir ->
                val info = runCatching { require_xcode_project_info(project_dir.absolutePath) }.getOrNull()
                    ?: return@mapNotNull null
                recent_project_record(
                    name = info.name,
                    path = normalize_project_path(project_dir.absolutePath),
                    cmake_version = info.cmake_version,
                    ndk_version = info.ndk_version,
                    template = info.template,
                    opened_at = project_dir.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun normalize_project_path(path: String): String {
        val trimmed_path = path.trim()
        if (trimmed_path.isBlank()) return ""

        return try {
            File(trimmed_path).canonicalPath
        } catch (_: Exception) {
            File(trimmed_path).absolutePath
        }
    }

    private fun create_recent_project_info(record: recent_project_record): recent_project_info {
        val project_info = get_project_info(record.path)
        val project_dir = File(record.path)

        return recent_project_info(
            name = project_info?.name ?: record.name.ifBlank { project_dir.name },
            path = record.path,
            cmake_version = project_info?.cmake_version ?: record.cmake_version,
            ndk_version = project_info?.ndk_version ?: record.ndk_version,
            template = project_info?.template ?: record.template.ifBlank { "executable" },
            last_opened = format_last_opened(record.opened_at),
            opened_at = record.opened_at
        )
    }

    private fun format_last_opened(opened_at: Long): String {
        val diff = (System.currentTimeMillis() - opened_at).coerceAtLeast(0L)
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "刚刚"
            diff < hour -> "${diff / minute}分钟前"
            diff < day -> "${diff / hour}小时前"
            diff < 2 * day -> "昨天"
            diff < 7 * day -> "${diff / day}天前"
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(opened_at))
        }
    }

}

data class project_info(
    val name: String,
    val path: String,
    val ndk_version: String,
    val cmake_version: String,
    val template: String
)

data class project_build_config(
    val abi: String = "arm64-v8a",
    val platform: String = "android-24",
    val cpp_standard: String = "20",
    val build_type: String = "Debug",
    val parallel_jobs: Int = 0,
    val extra_cmake_args: String = ""
)

data class project_ide_config(
    val ndk_version: String = "",
    val cmake_version: String = "",
    val build: project_build_config = project_build_config()
)

private data class project_config(
    val name: String = "",
    val ndk_version: String = "",
    val cmake_version: String = "",
    val template: String = "",
    val created: Long = 0L,
    val build: project_build_config = project_build_config()
)

data class recent_project_info(
    val name: String,
    val path: String,
    val cmake_version: String,
    val ndk_version: String,
    val template: String,
    val last_opened: String,
    val opened_at: Long
)

private data class recent_project_record(
    val name: String = "",
    val path: String = "",
    val cmake_version: String = "",
    val ndk_version: String = "",
    val template: String = "",
    val opened_at: Long = 0L
)
