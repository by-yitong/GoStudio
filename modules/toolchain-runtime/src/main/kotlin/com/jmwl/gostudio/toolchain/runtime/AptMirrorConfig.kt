package com.jmwl.gostudio.toolchain.runtime

import java.io.File

/**
 * rootfs apt 镜像源切换器（host 侧直接读写文件，无需 proot）。
 *
 * Ubuntu 24.04 用 DEB822 格式的 `/etc/apt/sources.list.d/ubuntu.sources`（含
 * `archive.ubuntu.com` 与 `security.ubuntu.com` 两类 URI）。本对象把这两个官方域名
 * 替换为镜像 host，并把 `http://` 升级为 `https://`（镜像均支持）。
 *
 * 若 rootfs 用旧格式（`/etc/apt/sources.list`），同样替换。
 * 幂等：替换前先备份（`.bak.gostudio`），已切换则跳过备份。
 */
object AptMirrorConfig {

    /** 官方主仓库域名 */
    private const val OFFICIAL_ARCHIVE = "archive.ubuntu.com"
    /** 官方安全更新域名 */
    private const val OFFICIAL_SECURITY = "security.ubuntu.com"

    /**
     * 把 [rootfsDir] 的 apt 源切换到 [mirror]。
     *
     * @return true 文件存在并已写入；false 找不到 sources 文件
     */
    fun applyMirror(rootfsDir: File, mirror: MirrorCandidate): Boolean {
        // 优先 DEB822 新格式
        val deb822 = File(rootfsDir, "etc/apt/sources.list.d/ubuntu.sources")
        if (deb822.isFile) {
            return rewriteFile(deb822, mirror)
        }
        // 回退传统 sources.list
        val legacy = File(rootfsDir, "etc/apt/sources.list")
        if (legacy.isFile) {
            return rewriteFile(legacy, mirror)
        }
        // 都没有：尝试生成一个最小的 DEB822（noble）
        return writeMinimalDeb822(deb822, mirror)
    }

    /**
     * 重写 sources 文件：把官方域名替换为镜像 host，http→https。
     * 替换前备份一次（仅当未备份过）。
     */
    private fun rewriteFile(file: File, mirror: MirrorCandidate): Boolean {
        val backup = File(file.parentFile, "${file.name}.bak.gostudio")
        if (!backup.exists()) file.copyTo(backup, overwrite = false)

        val original = file.readText()
        var rewritten = original
            // 官方主仓库 → 镜像
            .replace(OFFICIAL_ARCHIVE, mirror.host)
            // 官方安全更新 → 镜像（清华/阿里/中科大都镜像了 security）
            .replace(OFFICIAL_SECURITY, mirror.host)

        // http → https（仅对该镜像域名的行；官方 archive 保留 http 以免国外 https 慢）
        if (mirror.host != OFFICIAL_ARCHIVE) {
            rewritten = rewritten.replace("http://$mirror.host", "https://$mirror.host")
        }

        if (rewritten != original) {
            file.writeText(rewritten)
        }
        return true
    }

    /**
     * 当 sources 文件都不存在时，生成一个最小 noble DEB822 配置指向 [mirror]。
     */
    private fun writeMinimalDeb822(file: File, mirror: MirrorCandidate): Boolean {
        file.parentFile?.mkdirs()
        val scheme = if (mirror.host == OFFICIAL_ARCHIVE) "http" else "https"
        val base = "$scheme://${mirror.host}/ubuntu/"
        file.writeText(
            buildString {
                appendLine("Types: deb")
                appendLine("URIs: $base")
                appendLine("Suites: noble noble-updates noble-backports")
                appendLine("Components: main restricted universe multiverse")
                appendLine("Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg")
                appendLine()
                appendLine("Types: deb")
                appendLine("URIs: $base")
                appendLine("Suites: noble-security")
                appendLine("Components: main restricted universe multiverse")
                appendLine("Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg")
            }
        )
        return true
    }
}
