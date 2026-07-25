package com.jmwl.gostudio.toolchain

import com.jmwl.gostudio.toolchain.runtime.MirrorSpeedTest
import com.jmwl.gostudio.toolchain.runtime.AptMirrorConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Go 工具链安装任务。
 *
 * 通过 apt 在 proot Ubuntu rootfs 内安装 golang / gopls / git。
 * 安装前先测速国内镜像源，选最快的配置 apt 源；apt 失败自动切换下一个镜像重试。
 *
 * 所有命令经 [proot_manager] 在 proot 内执行，输出流式回调到 [onLog]。
 */

/**
 * 默认 apt 安装的 Go 工具链包：编译器、版本控制、CA 证书。
 * 注意：gopls 不走 apt（apt 源里的版本太旧，与 go 不匹配会崩溃），改用
 * `go install golang.org/x/tools/gopls@latest` 装最新版（见 install_latest_gopls）。
 */
private const val GO_PACKAGES = "golang git ca-certificates"

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

/**
 * 单独安装/重装 gopls（经 go install）。
 *
 * 关键：固定使用 v0.16.2（与 apt 装的 go 1.22 匹配），不用 @latest。
 * 原因：gopls @latest 要求 go 1.26+，会触发 go 的 GOTOOLCHAIN 自动下载新工具链，
 * 而 proot 跑 toolchain 切换会段错误崩溃。GOTOOLCHAIN=local 禁止自动下载，强制用本地 go。
 */
suspend fun install_gopls(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    onProgress(10)
    onLog("安装 gopls v0.16.2（匹配 go 1.22）...")
    proot_manager.execute_command_with_environment(
        command = "export GOPROXY=https://goproxy.cn,direct GOTOOLCHAIN=local && go install golang.org/x/tools/gopls@v0.16.2",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
}

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

/** apt update + install 完整 Go 工具链（用当前已配置的源），再用 go install 装最新 gopls。 */
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
    onLog("安装 Go / git（可能需要数分钟）...")
    onProgress(40)
    val apt_ok = proot_manager.execute_command_with_environment(
        command = "DEBIAN_FRONTEND=noninteractive apt-get install -y $GO_PACKAGES",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
    if (!apt_ok) return false

    // apt 源里的 gopls 版本太旧（与 go 不匹配会触发 nil 崩溃），用 go install 装匹配版本。
    // 固定 v0.16.2（匹配 go 1.22），不用 @latest：@latest 要求 go 1.26+，会触发 GOTOOLCHAIN
    // 自动下载，而 proot 跑 toolchain 切换会段错误。GOTOOLCHAIN=local 禁止自动下载。
    onLog("安装 gopls v0.16.2（匹配 go 1.22，可能需要一两分钟）...")
    onProgress(80)
    val gopls_ok = proot_manager.execute_command_with_environment(
        command = "export GOPROXY=https://goproxy.cn,direct GOTOOLCHAIN=local && go install golang.org/x/tools/gopls@v0.16.2",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
    if (!gopls_ok) {
        onLog("⚠️ gopls 安装失败，补全/诊断暂不可用，可稍后在「开发工具」页重试")
    }
    return true
}
