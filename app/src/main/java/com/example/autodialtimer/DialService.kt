package com.example.autodialtimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class DialService : Service() {

    private val handler   = Handler(Looper.getMainLooper())
    private var callCount = 0
    private var maxCalls  = 0
    private var ussd      = ""
    private var interval  = 30L

    companion object {
        const val CHANNEL_ID   = "dial_channel"
        const val NOTIF_ID     = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ussd     = intent?.getStringExtra("USSD")      ?: "*806*9299367*4#"
        interval = intent?.getLongExtra("INTERVAL", 30L) ?: 30L
        maxCalls = intent?.getIntExtra("MAX_CALLS", 0)  ?: 0
        callCount = 0

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Starting auto-dial…"))
        scheduleDial()
        return START_NOT_STICKY
    }

    private fun scheduleDial() {
        handler.postDelayed({
            if (shouldContinue()) {
                dial()
                callCount++
                updateNotification("Calls made: $callCount")
                scheduleDial()
            } else {
                stopSelf()
            }
        }, interval * 1_000L)
    }

    private fun shouldContinue() = maxCalls == 0 || callCount < maxCalls

    private fun dial() {
        val encoded = Uri.encode(ussd, "*#+")   // keep USSD special chars
        val dialUri = Uri.parse("tel:$encoded")
        val callIntent = Intent(Intent.ACTION_CALL, dialUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(callIntent)
        } catch (e: SecurityException) {
            updateNotification("Permission error – cannot dial")
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Auto Dial", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, StopReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Dial Timer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
