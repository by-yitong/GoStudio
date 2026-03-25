package com.termux.app.gostudio.lsp

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * LSP 客户端 - JSON-RPC over stdio 通信
 * 注意：LSP 暂未适配 proot 后端
 */
class LspClient(
    private val goplsPath: String
) {
    companion object {
        private const val TAG = "LspClient"
    }

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private val requestId = AtomicInteger(0)
    private val pendingRequests = ConcurrentHashMap<Int, (JsonRpcResponse) -> Unit>()
    private var notificationHandler: ((String, JSONObject?) -> Unit)? = null
    @Volatile
    private var isRunning = false
    private var readerThread: Thread? = null

    fun start(): Boolean {
        if (isRunning) return true

        return try {
            val command = "$goplsPath"
            Log.i(TAG, "启动 gopls: $command")

            val pb = ProcessBuilder(
                com.termux.shared.termux.TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash", "-c", command
            )
            pb.environment()["PREFIX"] = com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH
            pb.environment()["HOME"] = com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH
            pb.environment()["PATH"] = com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin:" +
                com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH + "/go/bin:/system/bin:/system/xbin"
            pb.redirectErrorStream(true)

            process = pb.start()
            try {
                val pidField = process?.javaClass?.getDeclaredMethod("pid")
                Log.i(TAG, "gopls started")
            } catch (_: Exception) {}
            writer = OutputStreamWriter(process!!.outputStream)
            reader = BufferedReader(InputStreamReader(process!!.inputStream))

            isRunning = true

            readerThread = Thread({ readLoop() }, "LspReader").apply {
                isDaemon = true
                start()
            }

            Log.i(TAG, "gopls 进程已启动")
            true
        } catch (e: Exception) {
            Log.e(TAG, "启动 gopls 失败", e)
            isRunning = false
            false
        }
    }

    fun sendRequest(method: String, params: JSONObject?): JsonRpcResponse {
        val id = requestId.incrementAndGet()
        val request = JsonRpcRequest(id, method, params)
        sendMessage(request.toJson())
        return waitForResponse(id)
    }

    fun sendRequestAsync(method: String, params: JSONObject?, callback: (JsonRpcResponse) -> Unit) {
        val id = requestId.incrementAndGet()
        pendingRequests[id] = callback
        val request = JsonRpcRequest(id, method, params)
        sendMessage(request.toJson())
    }

    fun sendNotification(method: String, params: JSONObject?) {
        val notification = JsonRpcNotification(method, params)
        sendMessage(notification.toJson())
    }

    fun setNotificationHandler(handler: (String, JSONObject?) -> Unit) {
        notificationHandler = handler
    }

    fun shutdown() {
        if (!isRunning) return
        try {
            val id = requestId.incrementAndGet()
            sendMessage(JsonRpcRequest(id, "shutdown", null).toJson())
            waitForResponse(id)
        } catch (e: Exception) {
            Log.w(TAG, "LSP shutdown 异常", e)
        }
        try { sendNotification("exit", null) } catch (_: Exception) {}
        try { process?.waitFor() } catch (_: Exception) {}
        cleanup()
    }

    fun isAlive(): Boolean = isRunning && process?.let {
        try { it.inputStream != null } catch (_: Exception) { false }
    } ?: false

    private fun sendMessage(json: String) {
        val w = writer ?: throw IllegalStateException("LSP 未启动")
        val header = "Content-Length: ${json.toByteArray(Charsets.UTF_8).size}\r\n\r\n"
        synchronized(w) {
            w.write(header)
            w.write(json)
            w.flush()
        }
        Log.d(TAG, "LSP sent: ${json.take(200)}")
    }

    private fun waitForResponse(id: Int): JsonRpcResponse {
        val result = java.util.concurrent.CountDownLatch(1)
        var response: JsonRpcResponse? = null
        pendingRequests[id] = { resp ->
            response = resp
            result.countDown()
        }
        result.await()
        return response ?: throw IllegalStateException("等待 LSP 响应超时 (id=$id)")
    }

    private fun readLoop() {
        val stream = process?.inputStream ?: return
        Log.i(TAG, "LSP readLoop 已启动")
        val lineBuffer = java.io.ByteArrayOutputStream()

        try {
            while (isRunning) {
                // 读取直到遇到 \r\n\r\n（消息头结束）
                lineBuffer.reset()
                var foundEmptyLine = false
                while (!foundEmptyLine && isRunning) {
                    val b = stream.read()
                    if (b < 0) return
                    lineBuffer.write(b)
                    val bytes = lineBuffer.toByteArray()
                    val len = bytes.size
                    if (len >= 4 && bytes[len - 4].toInt() == '\r'.code && bytes[len - 3].toInt() == '\n'.code
                        && bytes[len - 2].toInt() == '\r'.code && bytes[len - 1].toInt() == '\n'.code) {
                        foundEmptyLine = true
                    }
                }

                val headerStr = lineBuffer.toString("UTF-8")
                val bodyLength = parseContentLength(headerStr)
                Log.d(TAG, "LSP body length: $bodyLength bytes")

                if (bodyLength > 0) {
                    // 按字节数读取 body，避免多字节字符导致读偏
                    val bodyBytes = ByteArray(bodyLength)
                    var totalRead = 0
                    while (totalRead < bodyLength && isRunning) {
                        val read = stream.read(bodyBytes, totalRead, bodyLength - totalRead)
                        if (read <= 0) break
                        totalRead += read
                    }

                    if (totalRead > 0) {
                        val bodyStr = String(bodyBytes, 0, totalRead, Charsets.UTF_8)
                        Log.i(TAG, "LSP received: ${bodyStr.take(200)}")
                        handleMessage(bodyStr)
                    }
                }
            }
        } catch (e: Exception) {
            if (isRunning) Log.e(TAG, "LSP 读取循环异常", e)
        }

        Log.i(TAG, "LSP readLoop 已退出")
        isRunning = false
    }

    private fun parseContentLength(headers: String): Int {
        val regex = Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE)
        return regex.find(headers)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun handleMessage(body: String) {
        try {
            val json = JSONObject(body)
            if (json.has("id") && (json.has("result") || json.has("error"))) {
                val response = JsonRpcResponse.fromJson(json)
                val callback = pendingRequests.remove(response.id)
                callback?.invoke(response)
            } else if (json.has("method")) {
                val method = json.getString("method")
                val params = json.optJSONObject("params")
                notificationHandler?.invoke(method, params)
            }
        } catch (e: Exception) {
            Log.w(TAG, "解析 LSP 消息失败: $body", e)
        }
    }

    private fun cleanup() {
        isRunning = false
        try { writer?.close() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { process?.destroyForcibly() } catch (_: Exception) {}
        writer = null
        reader = null
        process = null
        pendingRequests.clear()
    }
}
