package com.xc.code.core.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

object logger_manager {

    private const val TAG = "XCode"
    private var log_dir: File? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val file_date_format = SimpleDateFormat("yyyy-MM-dd_HH", Locale.US)
    private val date_format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    var is_debug = false
    var enable_file_log = true
    var errors_only = true

    fun init(context: Context) {
        log_dir = File(context.cacheDir, "logs")
        if (!log_dir!!.exists()) {
            log_dir!!.mkdirs()
        }
        if (!errors_only) i("logger", "Log initialized")
    }

    fun d(tag: String, msg: String) {
        if (!is_debug || errors_only) return
        Log.d(tag, msg)
        write_to_file("D/$tag: $msg")
    }

    fun i(tag: String, msg: String) {
        if (errors_only) return
        Log.i(tag, msg)
        write_to_file("I/$tag: $msg")
    }

    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
        write_to_file("E/$tag: $msg")
    }

    fun w(tag: String, msg: String) {
        if (errors_only) return
        Log.w(tag, msg)
        write_to_file("W/$tag: $msg")
    }

    fun e(tag: String, msg: String, tr: Throwable?) {
        Log.e(tag, msg, tr)
        write_to_file("E/$tag: $msg\n${Log.getStackTraceString(tr)}")
    }

    private fun write_to_file(log: String) {
        if (!enable_file_log || log_dir == null) return

        executor.execute {
            try {
                val now = Date()
                val hour_key = file_date_format.format(now)
                val log_file = File(log_dir, "log_$hour_key.txt")

                FileWriter(log_file, true).use { writer ->
                    writer.write("${date_format.format(now)} $log\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log: ${e.message}")
            }
        }
    }
}