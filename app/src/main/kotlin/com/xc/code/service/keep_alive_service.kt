package com.xc.code.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.xc.code.xc_application

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
        xc_application.instance.keep_alive_service_ = this
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
        xc_application.instance.keep_alive_service_ = null
        super.onDestroy()
    }

    fun hide_notification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun show_notification() {
        startForeground(NOTIFICATION_ID, create_notification().build())
    }

    private fun create_notification_channel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "XCode",
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
            .setContentTitle("XCode")
            .setContentText("点按通知以打开编辑器")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pending_intent)
            .setAutoCancel(false)
    }

    private fun acquire_wake_lock() {
        val power_manager = getSystemService(POWER_SERVICE) as PowerManager
        wake_lock = power_manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XCode")
        wake_lock?.acquire(10 * 60 * 1000L)
    }
}