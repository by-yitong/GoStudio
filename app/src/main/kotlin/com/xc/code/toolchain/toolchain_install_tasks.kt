package com.xc.code.toolchain

import android.os.Environment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun install_cmake_tool(
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    return install_cmake_from_url(
        version = "4.3.0",
        url = "https://github.com/Kitware/CMake/releases/download/v4.3.0/cmake-4.3.0-linux-aarch64.tar.gz",
        sha256 = "26fe3011f497eb9398115dcabcc094685e634b1841f7c01dc01c5a89b8b0ea0d",
        on_log = on_log,
        on_progress = on_progress
    )
}

suspend fun install_cmake_from_url(
    version: String,
    url: String,
    sha256: String,
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    val trimmed_url = url.trim()
    if (trimmed_url.isBlank()) {
        emit_log(on_log, "错误: CMake 下载地址为空")
        return false
    }

    val target_name = normalize_cmake_install_name(version).ifBlank { strip_archive_extension(archive_name_from_url(trimmed_url)) }
    val archive_name = archive_name_from_url(trimmed_url).ifBlank { "cmake-$target_name-linux-aarch64.tar.gz" }
    val archive_path = "/home/xcode/cache/$archive_name"
    return run_cmake_install_task(
        command = create_cmake_install_command(
            archive_path = archive_path,
            download_url = trimmed_url,
            sha256 = sha256
        ),
        on_log = on_log,
        on_progress = on_progress
    )
}

suspend fun install_cmake_from_archive(
    archive_path: String,
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    val trimmed_path = archive_path.trim()
    if (trimmed_path.isBlank()) {
        emit_log(on_log, "错误: 压缩包路径为空")
        return false
    }

    val host_file = File(trimmed_path)
    if (!trimmed_path.startsWith("/home/") && !host_file.isFile) {
        emit_log(on_log, "错误: 压缩包不存在: $trimmed_path")
        return false
    }

    return run_cmake_install_task(
        command = create_cmake_install_command(
            archive_path = host_path_to_proot_path(trimmed_path),
            download_url = null
        ),
        on_log = on_log,
        on_progress = on_progress
    )
}

suspend fun uninstall_cmake_tool(
    version: String,
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    val key = version.trim()
    val target = if (key.isBlank()) "/home/xcode/cmake" else "/home/xcode/cmake/${normalize_cmake_install_name(key)}"
    val command = """
        set -e
        TARGET=${shell_quote(target)}
        case "@D@TARGET" in /home/xcode/cmake|/home/xcode/cmake/*) ;; *) echo "错误: 拒绝删除非 CMake 目录: @D@TARGET"; exit 1 ;; esac
        if [ ! -d "@D@TARGET" ]; then
          echo "CMake 未安装: @D@TARGET"
          exit 0
        fi
        if [ "@D@TARGET" = "/home/xcode/cmake" ]; then
          FOUND=0
          while IFS= read -r candidate; do
            case "@D@candidate" in /home/xcode/cmake/*) ;; *) echo "错误: 拒绝删除非 CMake 目录: @D@candidate"; exit 1 ;; esac
            if [ -f "@D@candidate/bin/cmake" ]; then
              echo "删除 CMake: @D@candidate"
              rm -rf "@D@candidate"
              FOUND=1
            fi
          done < <(find "@D@TARGET" -mindepth 1 -maxdepth 1 -type d)
          rmdir "@D@TARGET" 2>/dev/null || true
          if [ "@D@FOUND" = "1" ]; then echo "CMake 卸载完成"; else echo "CMake 未安装: @D@TARGET"; fi
        else
          if [ -f "@D@TARGET/bin/cmake" ]; then
            echo "删除 CMake: @D@TARGET"
            rm -rf "@D@TARGET"
            rmdir /home/xcode/cmake 2>/dev/null || true
            echo "CMake 卸载完成"
          else
            echo "CMake 目录结构无效: @D@TARGET"
            exit 1
          fi
        fi
    """.trimIndent().with_shell_dollar()

    return run_proot_toolchain_task(
        start_log = if (key.isBlank()) "开始卸载 CMake" else "开始卸载 CMake $key",
        command = command,
        success_log = "CMake 卸载完成",
        on_log = on_log,
        on_progress = on_progress
    )
}

suspend fun install_ndk_from_url(
    version: String,
    url: String,
    sha256: String = "",
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    val target_name = normalize_ndk_install_name(version)
    val archive_name = archive_name_from_url(url).ifBlank { "$target_name.tar.xz" }
    val archive_path = "/home/xcode/cache/$archive_name"
    return run_ndk_install_task(
        command = create_ndk_install_command(
            archive_path = archive_path,
            target_name = target_name,
            download_url = url,
            sha256 = sha256
        ),
        on_log = on_log,
        on_progress = on_progress
    )
}

suspend fun install_ndk_from_archive(
    archive_path: String,
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    val trimmed_path = archive_path.trim()
    if (trimmed_path.isBlank()) {
        emit_log(on_log, "错误: 压缩包路径为空")
        return false
    }

    val host_file = File(trimmed_path)
    if (!trimmed_path.startsWith("/home/") && !host_file.isFile) {
        emit_log(on_log, "错误: 压缩包不存在: $trimmed_path")
        return false
    }

    val archive_name = if (trimmed_path.startsWith("/home/")) {
        trimmed_path.substringAfterLast('/')
    } else {
        host_file.name
    }
    val target_name = normalize_ndk_install_name(strip_archive_extension(archive_name.ifBlank { "custom-ndk" }))
    return run_ndk_install_task(
        command = create_ndk_install_command(
            archive_path = host_path_to_proot_path(trimmed_path),
            target_name = target_name,
            download_url = null
        ),
        on_log = on_log,
        on_progress = on_progress
    )
}

suspend fun uninstall_ndk_tool(
    version: String,
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    val key = version.trim()
    if (key.isBlank()) {
        emit_log(on_log, "错误: NDK 版本为空")
        return false
    }

    val requested = normalize_version_key(key)
    val installed = withContext(Dispatchers.IO) {
        toolchain_manager.installed_ndks().firstOrNull { info ->
            info.aliases.any { alias -> normalize_version_key(alias) == requested } ||
                normalize_version_key(info.version) == requested ||
                normalize_version_key(info.host_dir.name) == requested
        }
    }
    val target = installed?.proot_dir ?: "/home/xcode/ndk/${normalize_ndk_install_name(key)}"
    val command = """
        set -e
        TARGET=${shell_quote(target)}
        case "@D@TARGET" in /home/xcode/ndk/*) ;; *) echo "错误: 拒绝删除非 NDK 目录: @D@TARGET"; exit 1 ;; esac
        if [ -d "@D@TARGET" ]; then
          echo "删除 NDK: @D@TARGET"
          rm -rf "@D@TARGET"
          echo "NDK 卸载完成"
        else
          echo "NDK 未安装: @D@TARGET"
        fi
    """.trimIndent().with_shell_dollar()

    emit_progress(on_progress, 0)
    emit_log(on_log, "开始卸载 NDK $key")
    emit_progress(on_progress, 20)
    val success = proot_manager.execute_command(command) { line -> on_log(line) }
    emit_progress(on_progress, if (success) 100 else 90)
    return success
}

private fun create_cmake_install_command(
    archive_path: String,
    download_url: String?,
    sha256: String = ""
): String {
    val download_block = create_download_block(download_url ?: "", sha256)

    val script = """
        set -e
        set -o pipefail
        CACHE_DIR=/home/xcode/cache
        CMAKE_ROOT=/home/xcode/cmake
        ARCHIVE=${shell_quote(archive_path)}
        TEMP_DIR="@D@CACHE_DIR/cmake-install-@D@@D@"
        mkdir -p "@D@CACHE_DIR" "@D@CMAKE_ROOT"

        $download_block

        rm -rf "@D@TEMP_DIR"
        mkdir -p "@D@TEMP_DIR"
        echo "开始解压: @D@ARCHIVE"
        case "@D@ARCHIVE" in
          *.tar.xz|*.txz) tar -xJvf "@D@ARCHIVE" -C "@D@TEMP_DIR" >/dev/null ;;
          *.tar.gz|*.tgz) tar -xzvf "@D@ARCHIVE" -C "@D@TEMP_DIR" >/dev/null ;;
          *.tar) tar -xvf "@D@ARCHIVE" -C "@D@TEMP_DIR" >/dev/null ;;
          *.zip) command -v unzip >/dev/null 2>&1 || { echo "错误: Ubuntu 中未安装 unzip，请先安装 unzip"; exit 1; }; unzip -o "@D@ARCHIVE" -d "@D@TEMP_DIR" >/dev/null ;;
          *) echo "错误: 不支持的压缩格式: @D@ARCHIVE"; exit 1 ;;
        esac

        FOUND=""
        while IFS= read -r cmake_bin; do
          dir="@D@(dirname "@D@(dirname "@D@cmake_bin")")"
          if [ -d "@D@dir/bin" ]; then
            FOUND="@D@dir"
            break
          fi
        done < <(find "@D@TEMP_DIR" -maxdepth 5 -type f -path '*/bin/cmake')

        [ -n "@D@FOUND" ] || { echo "错误: 未找到有效 CMake 目录"; rm -rf "@D@TEMP_DIR"; exit 1; }
        CMAKE_VERSION="@D@("@D@FOUND/bin/cmake" --version | head -n 1 | awk '{print @D@3}')"
        [ -n "@D@CMAKE_VERSION" ] || { echo "错误: 无法读取 CMake 版本"; rm -rf "@D@TEMP_DIR"; exit 1; }
        printf '%s' "@D@CMAKE_VERSION" | grep -Eq '^[0-9]+(\.[0-9]+)+([A-Za-z0-9._-]*)?@D@' || { echo "错误: CMake 版本无效: @D@CMAKE_VERSION"; rm -rf "@D@TEMP_DIR"; exit 1; }
        SAFE_VERSION="@D@(printf '%s' "@D@CMAKE_VERSION" | sed 's/[^A-Za-z0-9._-]/_/g; s/^[._-]*//; s/[._-]*@D@//')"
        [ -n "@D@SAFE_VERSION" ] || { echo "错误: CMake 版本无效: @D@CMAKE_VERSION"; rm -rf "@D@TEMP_DIR"; exit 1; }
        TARGET="@D@CMAKE_ROOT/@D@SAFE_VERSION"
        echo "发现 CMake: @D@FOUND"
        echo "CMake 版本: @D@CMAKE_VERSION"
        echo "CMake 目标目录: @D@TARGET"
        if [ -f "@D@TARGET/bin/cmake" ]; then
          echo "CMake 已安装: @D@TARGET"
          rm -rf "@D@TEMP_DIR"
          echo "CMAKE_TARGET=@D@TARGET"
          exit 0
        fi
        rm -rf "@D@TARGET"
        mkdir -p "@D@TARGET"
        cp -a "@D@FOUND"/. "@D@TARGET"/
        chmod -R u+rwX "@D@TARGET" || true
        rm -rf "@D@TEMP_DIR"
        echo "CMake 安装完成: @D@TARGET"
        echo "CMAKE_TARGET=@D@TARGET"
    """.trimIndent()
    return script.with_shell_dollar()
}

private suspend fun run_cmake_install_task(
    command: String,
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    emit_progress(on_progress, 0)
    emit_log(on_log, "开始安装 CMake 构建工具")
    emit_progress(on_progress, 10)
    var cmake_target = ""
    val success = proot_manager.execute_command(command) { line ->
        on_log(line)
        val marker = "CMAKE_TARGET="
        val index = line.indexOf(marker)
        if (index >= 0) cmake_target = line.substring(index + marker.length).trim()
    }
    if (!success) {
        emit_progress(on_progress, 90)
        emit_log(on_log, "命令执行失败")
        return false
    }
    val copied = install_bundled_ninja(cmake_target, on_log)
    emit_progress(on_progress, if (copied) 100 else 90)
    emit_log(on_log, if (copied) "CMake 构建工具安装完成" else "Ninja 安装失败")
    return copied
}

private suspend fun install_bundled_ninja(
    cmake_target: String,
    on_log: (String) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    val source = File(toolchain_runtime_provider.paths().native_library_dir, "libninja_exec.so")
    if (!source.isFile) {
        emit_log(on_log, "错误: 未找到内置 Ninja: ${source.absolutePath}")
        return@withContext false
    }
    if (!cmake_target.startsWith("/home/xcode/cmake/")) {
        emit_log(on_log, "错误: CMake 目标目录无效: $cmake_target")
        return@withContext false
    }
    val relative_path = cmake_target.removePrefix("/home/xcode").trimStart('/')
    if (relative_path.isBlank()) {
        emit_log(on_log, "错误: CMake 目标目录无效: $cmake_target")
        return@withContext false
    }
    val target_dir = File(toolchain_runtime_provider.paths().xcode_dir, relative_path)
    val bin_dir = File(target_dir, "bin")
    if (!File(bin_dir, "cmake").isFile) {
        emit_log(on_log, "错误: CMake 目标目录缺少 bin/cmake: ${target_dir.absolutePath}")
        return@withContext false
    }
    val target = File(bin_dir, "ninja")
    runCatching {
        source.copyTo(target, overwrite = true)
        target.setExecutable(true, false)
        target.setReadable(true, false)
        target.setWritable(true, true)
    }.onFailure { error ->
        emit_log(on_log, "错误: 安装 Ninja 失败: ${error.message}")
        return@withContext false
    }
    emit_log(on_log, "Ninja 安装完成: ${target.absolutePath}")
    true
}

private suspend fun run_proot_toolchain_task(
    start_log: String,
    command: String,
    success_log: String,
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    emit_progress(on_progress, 0)
    emit_log(on_log, start_log)
    emit_progress(on_progress, 10)
    val success = proot_manager.execute_command(command) { line -> on_log(line) }
    emit_progress(on_progress, if (success) 100 else 90)
    emit_log(on_log, if (success) success_log else "命令执行失败")
    return success
}

private suspend fun run_ndk_install_task(
    command: String,
    on_log: (String) -> Unit,
    on_progress: (Int) -> Unit
): Boolean {
    emit_progress(on_progress, 0)
    emit_log(on_log, "开始安装 NDK")
    emit_progress(on_progress, 8)
    val success = proot_manager.execute_command(command) { line ->
        on_log(line)
        update_ndk_progress_from_log(line, on_progress)
    }
    emit_progress(on_progress, if (success) 100 else 90)
    emit_log(on_log, if (success) "NDK 安装完成" else "NDK 安装失败")
    return success
}

private fun create_download_block(
    url: String,
    sha256: String
): String {
    if (url.isBlank()) {
        return """
            [ -f "@D@ARCHIVE" ] || { echo "错误: 压缩包不存在: @D@ARCHIVE"; exit 1; }
        """.trimIndent()
    }
    val checksum = sha256.takeIf { it.isNotBlank() }
    val sha256_block = if (checksum != null) {
        """
            echo "校验 SHA256..."
            ACTUAL_SHA256="@D@(sha256sum "@D@ARCHIVE" | awk '{print @D@1}')"
            if [ "@D@ACTUAL_SHA256" != "@D@EXPECTED_SHA256" ]; then
              echo "错误: SHA256 不匹配！"
              echo "  期望: @D@EXPECTED_SHA256"
              echo "  实际: @D@ACTUAL_SHA256"
              rm -f "@D@ARCHIVE"
              exit 1
            fi
            echo "SHA256 校验通过"
        """.trimIndent()
    } else ""
    return """
        URL=${shell_quote(url)}
        EXPECTED_SHA256=${shell_quote(sha256)}
        if [ -s "@D@ARCHIVE" ]; then
          echo "使用缓存: @D@ARCHIVE"
        else
          command -v wget >/dev/null 2>&1 || { echo "错误: 未安装 wget"; exit 1; }
          echo "开始下载: @D@URL"
          wget -c --show-progress --progress=bar:force:noscroll -O "@D@ARCHIVE" "@D@URL"
        fi
        $sha256_block
    """.trimIndent()
}

private fun create_ndk_install_command(
    archive_path: String,
    target_name: String,
    download_url: String?,
    sha256: String = ""
): String {
    val download_block = create_download_block(download_url ?: "", sha256)

    val script = """
        set -e
        set -o pipefail
        CACHE_DIR=/home/xcode/cache
        NDK_ROOT=/home/xcode/ndk
        ARCHIVE=${shell_quote(archive_path)}
        TARGET_NAME=${shell_quote(target_name)}
        TEMP_DIR="@D@CACHE_DIR/ndk-install-@D@TARGET_NAME-@D@@D@"
        mkdir -p "@D@CACHE_DIR" "@D@NDK_ROOT"

        $download_block

        rm -rf "@D@TEMP_DIR"
        mkdir -p "@D@TEMP_DIR"
        echo "开始解压: @D@ARCHIVE"
        print_extract_entries() {
          "@D@@A@" | while IFS= read -r entry; do
            [ -n "@D@entry" ] && printf '解压: %s\r' "@D@entry"
          done
          status=@D@{PIPESTATUS[0]}
          printf '\n'
          return @D@status
        }
        case "@D@ARCHIVE" in
          *.tar.xz|*.txz) print_extract_entries tar -xJvf "@D@ARCHIVE" -C "@D@TEMP_DIR" ;;
          *.tar.gz|*.tgz) print_extract_entries tar -xzvf "@D@ARCHIVE" -C "@D@TEMP_DIR" ;;
          *.tar) print_extract_entries tar -xvf "@D@ARCHIVE" -C "@D@TEMP_DIR" ;;
          *.zip) command -v unzip >/dev/null 2>&1 || { echo "错误: Ubuntu 中未安装 unzip，请先安装 unzip"; exit 1; }; print_extract_entries unzip -o "@D@ARCHIVE" -d "@D@TEMP_DIR" ;;
          *) echo "错误: 不支持的压缩格式: @D@ARCHIVE"; exit 1 ;;
        esac

        FOUND=""
        while IFS= read -r props; do
          dir="@D@(dirname "@D@props")"
          if [ -d "@D@dir/toolchains/llvm/prebuilt" ] && [ -f "@D@dir/build/cmake/android.toolchain.cmake" ]; then
            FOUND="@D@dir"
            break
          fi
        done < <(find "@D@TEMP_DIR" -maxdepth 6 -type f -name source.properties)

        [ -n "@D@FOUND" ] || { echo "错误: 未找到有效 NDK 目录"; rm -rf "@D@TEMP_DIR"; exit 1; }
        REVISION="@D@(grep -E '^Pkg\.Revision[[:space:]]*=' "@D@FOUND/source.properties" | head -n 1 | cut -d= -f2- | tr -d '[:space:]')"
        [ -n "@D@REVISION" ] || { echo "错误: source.properties 缺少 Pkg.Revision"; rm -rf "@D@TEMP_DIR"; exit 1; }
        SAFE_REVISION="@D@(printf '%s' "@D@REVISION" | sed 's/[^A-Za-z0-9._-]/_/g; s/^[._-]*//; s/[._-]*@D@//')"
        [ -n "@D@SAFE_REVISION" ] || { echo "错误: Pkg.Revision 无效: @D@REVISION"; rm -rf "@D@TEMP_DIR"; exit 1; }
        TARGET="@D@NDK_ROOT/@D@SAFE_REVISION"
        echo "发现 NDK: @D@FOUND"
        echo "Pkg.Revision: @D@REVISION"
        echo "NDK 目标目录: @D@TARGET"
        if [ -f "@D@TARGET/source.properties" ] && [ -d "@D@TARGET/toolchains/llvm/prebuilt" ]; then
          echo "NDK 已安装: @D@TARGET"
          rm -rf "@D@TEMP_DIR"
          exit 0
        fi
        rm -rf "@D@TARGET"
        mkdir -p "@D@TARGET"
        cp -a "@D@FOUND"/. "@D@TARGET"/
        chmod -R u+rwX "@D@TARGET" || true
        rm -rf "@D@TEMP_DIR"
        echo "NDK 安装完成: @D@TARGET"
    """.trimIndent()
    return script.with_shell_dollar()
}

private fun update_ndk_progress_from_log(line: String, on_progress: (Int) -> Unit) {
    val percent = Regex("(\\d{1,3})%").find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (percent != null) {
        on_progress((10 + percent.coerceIn(0, 100) * 55 / 100).coerceIn(10, 65))
        return
    }
    when {
        line.contains("使用缓存") -> on_progress(45)
        line.contains("开始下载") -> on_progress(12)
        line.contains("开始解压") -> on_progress(70)
        line.contains("解压:") -> on_progress(78)
        line.contains("发现 NDK") -> on_progress(84)
        line.contains("NDK 安装完成") -> on_progress(100)
    }
}

private fun host_path_to_proot_path(path: String): String {
    if (path.startsWith("/home/")) return path

    val absolute_path = File(path).absolutePath
    val xcode_path = toolchain_runtime_provider.paths().xcode_dir.absolutePath.trimEnd('/')
    val home_path = toolchain_runtime_provider.paths().home_dir.absolutePath.trimEnd('/')
    val external_path = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
    val sdcard_path = File("/sdcard").absolutePath.trimEnd('/')
    return when {
        absolute_path == xcode_path -> "/home/xcode"
        absolute_path.startsWith("$xcode_path/") -> "/home/xcode/" + absolute_path.removePrefix("$xcode_path/")
        absolute_path == home_path -> "/home"
        absolute_path.startsWith("$home_path/") -> "/home/" + absolute_path.removePrefix("$home_path/")
        absolute_path == sdcard_path -> external_path
        absolute_path.startsWith("$sdcard_path/") -> external_path + "/" + absolute_path.removePrefix("$sdcard_path/")
        else -> absolute_path
    }
}

private fun archive_name_from_url(url: String): String {
    return url.substringBefore('?').substringAfterLast('/').trim()
}

private fun strip_archive_extension(name: String): String {
    val lower_name = name.lowercase()
    val extensions = listOf(".tar.xz", ".tar.gz", ".tgz", ".zip", ".tar", ".xz", ".gz")
    val extension = extensions.firstOrNull { lower_name.endsWith(it) } ?: return name
    return name.dropLast(extension.length)
}

private fun normalize_cmake_install_name(value: String): String {
    return value.trim()
        .replace(Regex("^cmake[-_ ]*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('_', '.', '-')
}

private fun normalize_ndk_install_name(value: String): String {
    val stripped = strip_archive_extension(value.trim())
    return stripped
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('_', '.', '-')
        .ifBlank { "custom-ndk" }
}

private fun normalize_version_key(value: String): String {
    return value.trim().lowercase().removePrefix("android-ndk-")
}

private fun shell_quote(value: String): String {
    if (value.isEmpty()) return "''"
    return "'" + value.replace("'", "'\"'\"'") + "'"
}

private fun String.with_shell_dollar(): String {
    return replace("@D@", "\$").replace("@A@", "@")
}

private suspend fun emit_log(on_log: (String) -> Unit, message: String) {
    withContext(Dispatchers.Main) {
        on_log(message)
    }
}

private suspend fun emit_progress(on_progress: (Int) -> Unit, progress: Int) {
    withContext(Dispatchers.Main) {
        on_progress(progress.coerceIn(0, 100))
    }
}