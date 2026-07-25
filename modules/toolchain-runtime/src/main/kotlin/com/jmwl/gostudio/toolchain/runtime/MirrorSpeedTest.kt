package com.jmwl.gostudio.toolchain.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 一个 apt 镜像源候选。
 *
 * @param name 显示名（"清华" / "阿里云" / "中科大" / "官方"）
 * @param host 镜像主机名，用于替换 ubuntu.sources 里的 archive.ubuntu.com / security.ubuntu.com
 * @param probeUrl 测速 URL（指向稳定存在的小文件，如 noble 的 Release 文件）
 */
data class MirrorCandidate(
    val name: String,
    val host: String,
    val probeUrl: String
)

/**
 * 镜像测速结果：耗时(ms)；null 表示不可用（超时/非 2xx/异常）。
 */
data class MirrorSpeedResult(
    val candidate: MirrorCandidate,
    val latencyMs: Long?
)

/**
 * 国内 Ubuntu 镜像测速器（纯 Kotlin，无 Android 依赖）。
 *
 * 对候选镜像**并发**测速（HEAD/GET probeUrl），记录从连接到响应的耗时，
 * 返回按耗时升序排序的列表（可用且最快在前，不可用置末）。
 * 进度通过 [onProgress] 实时回调（每完成一个镜像通知一次）。
 */
object MirrorSpeedTest {

    /**
     * 默认候选列表（顺序也是兜底回退顺序）：
     * 清华 → 阿里云 → 中科大 → 官方。
     * probe 用 noble 的 Release 文件（稳定存在，~几十 KB）。
     */
    val DEFAULT_CANDIDATES: List<MirrorCandidate> = listOf(
        MirrorCandidate("清华", "mirrors.tuna.tsinghua.edu.cn", "https://mirrors.tuna.tsinghua.edu.cn/ubuntu/dists/noble/Release"),
        MirrorCandidate("阿里云", "mirrors.aliyun.com", "https://mirrors.aliyun.com/ubuntu/dists/noble/Release"),
        MirrorCandidate("中科大", "mirrors.ustc.edu.cn", "https://mirrors.ustc.edu.cn/ubuntu/dists/noble/Release"),
        MirrorCandidate("官方", "archive.ubuntu.com", "http://archive.ubuntu.com/ubuntu/dists/noble/Release"),
    )

    /**
     * 并发测速并排序。
     *
     * @param timeoutMs 单镜像连接+读取超时，默认 5s
     * @param onProgress 每个镜像测完后回调（候选, 耗时ms, 是否可用）
     * @return 按耗时升序（可用在前，不可用置末）的结果列表
     */
    suspend fun testAndSort(
        candidates: List<MirrorCandidate> = DEFAULT_CANDIDATES,
        timeoutMs: Long = 5_000,
        onProgress: suspend (MirrorCandidate, Long?, Boolean) -> Unit = { _, _, _ -> }
    ): List<MirrorSpeedResult> = withContext(Dispatchers.IO) {
        // 并发测速
        val results = coroutineScope {
            candidates.map { candidate ->
                async { testOne(candidate, timeoutMs) }
            }.awaitAll()
        }
        // 实时进度（按完成顺序）
        results.forEach { onProgress(it.candidate, it.latencyMs, it.latencyMs != null) }
        // 排序：可用在前（按耗时升序），不可用置末（保持原相对顺序）
        results.sortedWith(
            compareBy<MirrorSpeedResult> { it.latencyMs == null }
                .thenBy { it.latencyMs ?: Long.MAX_VALUE }
        )
    }

    /**
     * 测单个镜像：HTTP GET（小范围）probeUrl，记录连接到响应的耗时。
     * 用 GET 而非 HEAD——部分镜像对 HEAD 返回 405；只读响应头即断开，不下载 body。
     */
    private suspend fun testOne(candidate: MirrorCandidate, timeoutMs: Long): MirrorSpeedResult {
        var connection: HttpURLConnection? = null
        return try {
            val start = System.currentTimeMillis()
            connection = (URL(candidate.probeUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs.toInt()
                readTimeout = timeoutMs.toInt()
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "GoStudio/1.0")
                // 只关心响应码，不读 body
                useCaches = false
            }
            val code = connection.responseCode
            connection.disconnect()
            if (code in 200..399) {
                MirrorSpeedResult(candidate, System.currentTimeMillis() - start)
            } else {
                MirrorSpeedResult(candidate, null)
            }
        } catch (_: Exception) {
            runCatching { connection?.disconnect() }
            MirrorSpeedResult(candidate, null)
        }
    }
}
