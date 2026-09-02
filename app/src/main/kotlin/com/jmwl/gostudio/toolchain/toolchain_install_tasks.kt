package com.jmwl.gostudio.toolchain

import com.jmwl.gostudio.toolchain.runtime.MirrorSpeedTest
import com.jmwl.gostudio.toolchain.runtime.ApkMirrorConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Go 工具链安装任务。
 *
 * 通过 apk 在 proot Alpine rootfs 内安装 go / gopls / git。
 * 安装前先测速国内镜像源，选最快的配置 apk 源；apk 失败自动切换下一个镜像重试。
 *
 * 所有命令经 [proot_manager] 在 proot 内执行，输出流式回调到 [onLog]。
 */

/**
 * 默认 apk 安装的 Go 工具链包：编译器、版本控制、CA 证书。
 * 注意：gopls 不走 apk（community 源里有但版本固定，升级节奏跟 go 不同步），改用
 * `go install golang.org/x/tools/gopls@v0.21.1` 装配套版本（见 install_gopls）。
 */
private const val GO_PACKAGES = "go git ca-certificates"

/** gopls 版本，与 Alpine v3.24 community 源的 gopls 0.21.1 / go 1.26.3 配对。 */
private const val GOPLS_VERSION = "v0.21.1"

/**
 * 安装完整 Go 工具链（go + gopls + git）。测速选镜像后 apk 安装，失败回退。
 *
 * @param onLog 进度日志回调
 * @param onProgress 进度百分比回调（0-100，apk 无精确进度，用阶段值）
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

    // 按排序逐个镜像尝试 apk 安装
    for (result in ranked) {
        val mirror = result.candidate
        if (result.latencyMs == null) {
            onLog("跳过不可用的 ${mirror.name}")
            continue
        }
        onLog("使用 ${mirror.name} 配置 apk 源...")
        ApkMirrorConfig.applyMirror(toolchain_runtime_provider.paths().rootfs_dir, mirror)
        onProgress(20)
        val ok = apk_update_and_install_go(onLog, onProgress)
        if (ok) {
            onLog("✅ ${mirror.name} 安装成功")
            toolchain_manager.invalidate_go_probe()
            toolchain_manager.ensure_default_go_env(onLog)
            return@withContext true
        }
        onLog("❌ ${mirror.name} 失败，切换下一个镜像源重试...")
    }
    onLog("所有镜像源均失败，请检查网络后重试")
    false
}

/**
 * 测速并把 rootfs 的 apk 源切到最快镜像。
 *
 * 安装页第一条 apk 命令（基础包）之前也要调用——minirootfs 自带的源指向官方
 * dl-cdn，国内直连不稳定；不先切镜像，基础包就会在官方源上 DNS/超时失败。
 */
suspend fun configure_best_apk_mirror(onLog: (String) -> Unit) {
    val ranked = MirrorSpeedTest.testAndSort()
    val best = ranked.firstOrNull { it.latencyMs != null } ?: return
    onLog("使用 ${best.candidate.name} 配置 apk 源...")
    ApkMirrorConfig.applyMirror(toolchain_runtime_provider.paths().rootfs_dir, best.candidate)
}

/** 单独安装/重装 Go（go 包）。 */
suspend fun install_go(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean {
    val ok = apk_install_with_best_mirror("go", onLog, onProgress)
    if (ok) {
        toolchain_manager.invalidate_go_probe()
        toolchain_manager.ensure_default_go_env(onLog)
    }
    return ok
}

/**
 * 单独安装/重装 gopls（经 go install）。
 *
 * 关键：固定使用 $GOPLS_VERSION（与 apk 装的 go 1.26 配对），不用 @latest。
 * 原因：gopls @latest 可能要求更高的 go 版本，会触发 go 的 GOTOOLCHAIN 自动下载新工具链，
 * 而 proot 跑 toolchain 切换会段错误崩溃。GOTOOLCHAIN=local 禁止自动下载，强制用本地 go。
 */
suspend fun install_gopls(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    onProgress(10)
    onLog("安装 gopls $GOPLS_VERSION（匹配 go 1.26）...")
    proot_manager.execute_command_with_environment(
        command = "export GOPROXY=${goproxy_store.current()} GOTOOLCHAIN=local && go install golang.org/x/tools/gopls@$GOPLS_VERSION",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
}

/** 单独安装/重装 git。 */
suspend fun install_git(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = apk_install_with_best_mirror("git", onLog, onProgress)

/**
 * 安装 C/C++ 编译器（gcc / musl-dev / binutils），供 cgo 项目使用。
 *
 * 默认环境不装（保持体积小）：无 C 编译器时 go 自动禁用 cgo，纯 Go 项目零影响；
 * 需要调用 C 库的项目在这里补装后即可编译 cgo 代码。
 */
suspend fun install_gcc(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = apk_install_with_best_mirror("gcc musl-dev binutils", onLog, onProgress)

/**
 * 安装 garble（Go 代码混淆器，通过 go install）。
 */
suspend fun install_garble(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    onLog("安装 garble...")
    proot_manager.execute_command_with_environment(
        command = "export GOPROXY=${goproxy_store.current()} && go install mvdan.cc/garble@latest",
        working_dir = "/home",
        on_log = { onLog(it) }
    )
}

/**
 * 测速选最快镜像 → 配置源 → apk update + add [packages]。
 */
private suspend fun apk_install_with_best_mirror(
    packages: String,
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean {
    onProgress(10)
    val ranked = MirrorSpeedTest.testAndSort()
    val best = ranked.firstOrNull { it.latencyMs != null } ?: ranked.firstOrNull()
    if (best != null) {
        onLog("使用 ${best.candidate.name} 配置 apk 源...")
        ApkMirrorConfig.applyMirror(toolchain_runtime_provider.paths().rootfs_dir, best.candidate)
    }
    onProgress(20)
    return proot_manager.execute_command_with_environment(
        command = "apk update && apk add $packages",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
}

/** apk update + add 完整 Go 工具链（用当前已配置的源），再用 go install 装配套 gopls。 */
private suspend fun apk_update_and_install_go(
    onLog: (String) -> Unit,
    onProgress: (Int) -> Unit
): Boolean {
    onLog("更新 apk 索引...")
    onProgress(30)
    val updateOk = proot_manager.execute_command_with_environment(
        command = "apk update",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
    if (!updateOk) return false
    onLog("安装 Go / git（可能需要数分钟）...")
    onProgress(40)
    val apk_ok = proot_manager.execute_command_with_environment(
        // apk fix go：包数据库标记"已装"但文件缺失时（安装中途进程被杀留下的事务残骸）
        // apk add 视为无操作，fix 会校验并补齐缺失文件。对完好的包是无操作。
        command = "apk add $GO_PACKAGES && apk fix go",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
    if (!apk_ok) return false

    // gopls 用 go install 装固定配套版本（apk 源版本随发行版冻结，与本地 go 升级节奏不同步）。
    // 不用 @latest：@latest 可能要求更高 go 版本，触发 GOTOOLCHAIN 自动下载，
    // 而 proot 跑 toolchain 切换会段错误。GOTOOLCHAIN=local 禁止自动下载。
    onLog("安装 gopls $GOPLS_VERSION（匹配 go 1.26，可能需要一两分钟）...")
    onProgress(80)
    val gopls_ok = proot_manager.execute_command_with_environment(
        command = "export GOPROXY=${goproxy_store.current()} GOTOOLCHAIN=local && go install golang.org/x/tools/gopls@$GOPLS_VERSION",
        working_dir = "/root",
        on_log = { onLog(it) }
    )
    if (!gopls_ok) {
        onLog("⚠️ gopls 安装失败，补全/诊断暂不可用，可稍后在「开发工具」页重试")
    }
    return true
}
