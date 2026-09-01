package com.jmwl.gostudio.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.jmwl.gostudio.gostudio_application

class keep_alive_service : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "keep_alive_channel"
    }

    private var is_first_start = true
    private var wake_lock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        create_notification_channel()
        startForeground(NOTIFICATION_ID, create_notification().build())
        acquire_wake_lock()
        gostudio_application.instance.keep_alive_service_ = this
        if (is_first_start) {
            hide_notification()
            is_first_start = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, start_id: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        wake_lock?.release()
        gostudio_application.instance.keep_alive_service_ = null
        super.onDestroy()
    }

    fun hide_notification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun show_notification() {
        startForeground(NOTIFICATION_ID, create_notification().build())
    }

    /**
     * 安装模式下保持真正的前台服务身份（可见通知）。
     *
     * 平时 onCreate 里 startForeground 后立即 stopForeground（静默保活），但这会把
     * 前台身份一并取消——进程优先级跌回普通后台，在 vivo/OPPO 等激进查杀的 ROM 上
     * 锁屏或切后台数秒内就被杀，环境安装永远跑不完（表现为每次打开都"重新安装"）。
     * 安装期间改用带可见通知的前台身份，系统与用户都能感知，查杀阈值大幅提高。
     */
    fun show_install_notification() {
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("正在安装 Linux 环境")
                .setContentText("下载/配置 Go 工具链中，请勿清理 GoStudio 后台")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
        )
        // 安装可能超过普通 wake lock 的 10 分钟窗口，重新计时
        wake_lock?.let { runCatching { it.release() } }
        val power_manager = getSystemService(POWER_SERVICE) as PowerManager
        wake_lock = power_manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GoStudio:install")
            .also { it.acquire(20 * 60 * 1000L) }
    }

    private fun create_notification_channel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GoStudio",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun create_notification(): NotificationCompat.Builder {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending_intent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GoStudio")
            .setContentText("点按通知以打开编辑器")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pending_intent)
            .setAutoCancel(false)
    }

    private fun acquire_wake_lock() {
        val power_manager = getSystemService(POWER_SERVICE) as PowerManager
        wake_lock = power_manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GoStudio")
        wake_lock?.acquire(10 * 60 * 1000L)
    }
}