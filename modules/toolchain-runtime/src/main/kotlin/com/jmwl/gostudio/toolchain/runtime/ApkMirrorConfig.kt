package com.jmwl.gostudio.toolchain.runtime

import java.io.File

/**
 * rootfs apk 镜像源切换器（host 侧直接读写文件，无需 proot）。
 *
 * Alpine 的 /etc/apk/repositories 每行一个仓库 URL（main + community，go 包在
 * community 里）。本对象直接用 [MirrorCandidate] 重新生成该文件：官方源
 * dl-cdn.alpinelinux.org 换成镜像 host，http 升级 https（镜像均支持）。
 *
 * 幂等：首次替换前备份为 repositories.bak.gostudio。
 */
object ApkMirrorConfig {

    /** Alpine 官方 CDN 域名 */
    private const val OFFICIAL_HOST = "dl-cdn.alpinelinux.org"

    /** Alpine 仓库分支（v3.24 对应 Alpine 3.24.x） */
    private const val ALPINE_BRANCH = "v3.24"

    /**
     * 把 [rootfsDir] 的 apk 源切换到 [mirror]。
     *
     * @return true 已写入 repositories
     */
    fun applyMirror(rootfsDir: File, mirror: MirrorCandidate): Boolean {
        val repositories = File(rootfsDir, "etc/apk/repositories")
        repositories.parentFile?.mkdirs()
        if (repositories.isFile) {
            val backup = File(repositories.parentFile, "${repositories.name}.bak.gostudio")
            if (!backup.exists()) repositories.copyTo(backup, overwrite = false)
        }

        val scheme = if (mirror.host == OFFICIAL_HOST) "http" else "https"
        val base = "$scheme://${mirror.host}/alpine/$ALPINE_BRANCH"
        repositories.writeText(
            buildString {
                appendLine("$base/main")
                appendLine("$base/community")
            }
        )
        return true
    }
}
