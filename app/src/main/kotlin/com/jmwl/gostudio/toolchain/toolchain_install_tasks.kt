package com.jmwl.gostudio.toolchain

import com.jmwl.gostudio.toolchain.runtime.MirrorSpeedTest
import com.jmwl.gostudio.toolchain.runtime.AptMirrorConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Go 工具链安装任务（替代 XCode 的 cmake/ndk 安装）。
 *
 * 通过 apt 在 proot Ubuntu rootfs 内安装 golang / gopls / git。
 * 安装前先测速国内镜像源，选最快的配置 apt 源；apt 失败自动切换下一个镜像重试。
 *
 * 所有命令经 [proot_manager] 在 proot 内执行，输出流式回调到 [onLog]。
 */

/** 默认安装的 Go 工具链包：编译器、语言服务器、版本控制、CA 证书。 */
private const val GO_PACKAGES = "golang gopls git ca-certificates"

/**
 * 安装完整 Go 工具链（go + gopls + git）。测速选镜像后 apt 安装，失败回退。
 *
 * @param onLog 进度日志回调
 * @param onProgress 进度百分比回调（0-100，apt 无精确进度，用阶段值）
 */
suspend fun install_go_toolchain(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    onLog("测速国内镜像源...")
    onProgress(5)
    val ranked = MirrorSpeedTest.testAndSort { candidate, ms, ok ->
        onLog("  ${candidate.name}: ${if (ok) "${ms}ms" else "不可用"}")
    }
    onLog("排序: ${ranked.joinToString { res -> "${res.candidate.name}(${res.latencyMs?.let { "${it}ms" } ?: "不可用"})" }}")
    onProgress(15)

    // 按排序逐个镜像尝试 apt 安装
    for (result in ranked) {
        val mirror = result.candidate
        if (result.latencyMs == null) {
            onLog("跳过不可用的 ${mirror.name}")
            continue
        }
        onLog("使用 ${mirror.name} 配置 apt 源...")
        AptMirrorConfig.applyMirror(toolchain_runtime_provider.paths().ubuntu_base_dir, mirror)
        onProgress(20)
        val ok = apt_update_and_install_go(onLog, onProgress)
        if (ok) {
            onLog("✅ ${mirror.name} 安装成功")
            return@withContext true
        }
        onLog("❌ ${mirror.name} 失败，切换下一个镜像源重试...")
    }
    onLog("所有镜像源均失败，请检查网络后重试")
    false
}

/** 单独安装/重装 Go（golang 包）。 */
suspend fun install_go(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = apt_install_with_best_mirror("golang", onLog, onProgress)

/** 单独安装/重装 gopls。 */
suspend fun install_gopls(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = apt_install_with_best_mirror("gopls", onLog, onProgress)

/** 单独安装/重装 git。 */
suspend fun install_git(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = apt_install_with_best_mirror("git", onLog, onProgress)

/**
 * 安装 garble（Go 代码混淆器，通过 go install）。
 */
suspend fun install_garble(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    onLog("安装 garble...")
    proot_manager.execute_command_with_environment(
        command = "export GOPROXY=https://goproxy.cn,direct && go install mvdan.cc/garble@latest",
        working_dir = "/home",
        on_log = { onLog(it) }
    )
}

/**
 * 测速选最快镜像 → 配置源 → apt update + install [packages]。
 */
private suspend fun apt_install_with_best_mirror(
    packages: String,
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean {
    onProgress(10)
    val ranked = MirrorSpeedTest.testAndSort()
    val best = ranked.firstOrNull { it.latencyMs != null } ?: ranked.firstOrNull()
    if (best != null) {
        onLog("使用 ${best.candidate.name} 配置 apt 源...")
        AptMirrorConfig.applyMirror(toolchain_runtime_provider.paths().ubuntu_base_dir, best.candidate)
    }
    onProgress(20)
    return proot_manager.execute_command_with_environment(
        command = "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y $packages",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
}

/** apt update + install 完整 Go 工具链（用当前已配置的源）。 */
private suspend fun apt_update_and_install_go(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean {
    onLog("更新 apt 索引...")
    onProgress(30)
    val updateOk = proot_manager.execute_command_with_environment(
        command = "apt-get update",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
    if (!updateOk) return false
    onLog("安装 Go / gopls / git（可能需要数分钟）...")
    onProgress(40)
    return proot_manager.execute_command_with_environment(
        command = "DEBIAN_FRONTEND=noninteractive apt-get install -y $GO_PACKAGES",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
}

// ──────────────────────────────────────────────────────────────────────────
// 旧 cmake/ndk 接口桩（兼容 main_activity / main_tools_screen 调用方，
// 阶段 5/6 改 UI 时会彻底替换这些调用）。GoStudio 不支持 cmake/ndk，统一返回失败。
// ──────────────────────────────────────────────────────────────────────────

suspend fun install_cmake_from_url(
    version: String, url: String, sha256: String,
    onLog: (String) -> Unit, onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) { onLog("GoStudio 不支持 CMake"); false }

suspend fun install_cmake_from_archive(
    archivePath: String,
    onLog: (String) -> Unit, onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) { onLog("GoStudio 不支持 CMake"); false }

suspend fun uninstall_cmake_tool(
    version: String,
    onLog: (String) -> Unit, onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) { onLog("GoStudio 不支持 CMake"); false }

suspend fun install_ndk_from_url(
    version: String, url: String, sha256: String,
    onLog: (String) -> Unit, onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) { onLog("GoStudio 不支持 NDK"); false }

suspend fun install_ndk_from_archive(
    archivePath: String,
    onLog: (String) -> Unit, onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) { onLog("GoStudio 不支持 NDK"); false }

suspend fun uninstall_ndk_tool(
    version: String,
    onLog: (String) -> Unit, onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) { onLog("GoStudio 不支持 NDK"); false }
