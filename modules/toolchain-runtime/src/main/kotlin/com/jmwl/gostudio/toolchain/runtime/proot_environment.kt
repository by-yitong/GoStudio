package com.jmwl.gostudio.toolchain.runtime

data class proot_environment(
    val path: String,
    val extra: Map<String, String> = emptyMap()
) {
    fun as_map(paths: toolchain_runtime_paths): Map<String, String> {
        return linkedMapOf(
            "HOME" to proot_home,
            "LANG" to proot_lang,
            "TERM" to proot_term,
            "PROOT_TMP_DIR" to paths.proot_tmp_dir.absolutePath,
            "PATH" to path
        ).apply {
            put("PROOT_LOADER", paths.proot_loader_file.absolutePath)
            // --android-profile 默认把自己生成的 resolv.conf（写死 8.8.8.8/8.8.4.4/
            // 1.1.1.1）bind 进 guest 的 /etc/resolv.conf，国内网络下 musl 解析全超时，
            // 直接写 rootfs 里的文件会被它盖掉。用此变量让它 bind 我们维护的那份
            // （app 层 sandbox_dns 每次执行前刷新系统 DNS）。
            put("PROOT_ANDROID_RESOLV_CONF", java.io.File(paths.rootfs_dir, "etc/resolv.conf").absolutePath)
            putAll(extra)
        }
    }

    fun as_array(paths: toolchain_runtime_paths): Array<String> {
        return as_map(paths).map { (key, value) -> "$key=$value" }.toTypedArray()
    }
}

internal fun clean_shell_env_args(
    tmpdir: String = "/tmp",
    extra_environment: Map<String, String> = emptyMap()
): List<String> {
    val environment = linkedMapOf(
        "HOME" to proot_home,
        "PATH" to proot_default_path,
        "TERM" to proot_term,
        "LANG" to proot_lang,
        "LC_ALL" to proot_lang,
        "TMPDIR" to tmpdir
    ).apply {
        putAll(extra_environment.filterKeys { it.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")) })
    }
    return listOf("/usr/bin/env", "-i") + environment.map { (key, value) -> "$key=$value" }
}

internal const val proot_home = "/home"
internal const val proot_lang = "C.UTF-8"
internal const val proot_term = "xterm-256color"
internal const val proot_default_path = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
