package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.VideoControlCommand

class MediaPlaybackService : Service() {

    companion object {
        const val ACTION_UPDATE = "com.example.streampay.ACTION_UPDATE"
        const val ACTION_PLAY = "com.example.streampay.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.streampay.ACTION_PAUSE"
        
        const val NOTIFICATION_ID = 9911
        const val CHANNEL_ID = "streampay_playback"

        @Volatile
        private var activeInstance: MediaPlaybackService? = null
        fun getActiveInstance(): MediaPlaybackService? = activeInstance
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    fun updateNotificationState(title: String, pageUrl: String, currentTime: Double, isPlaying: Boolean) {
        showNotification(title, pageUrl, currentTime, isPlaying)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_UPDATE -> {
                val title = intent.getStringExtra("title") ?: "Video StreamPay"
                val pageUrl = intent.getStringExtra("pageUrl") ?: ""
                val currentTime = intent.getDoubleExtra("currentTime", 0.0)
                val isPlaying = intent.getBooleanExtra("isPlaying", false)

                showNotification(title, pageUrl, currentTime, isPlaying)
            }
            ACTION_PLAY -> {
                AppViewModel.getActiveInstance()?.sendVideoControl(VideoControlCommand.PLAY)
            }
            ACTION_PAUSE -> {
                AppViewModel.getActiveInstance()?.sendVideoControl(VideoControlCommand.PAUSE)
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción en Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Control de reproducción de videos de StreamPay"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, pageUrl: String, currentTime: Double, isPlaying: Boolean) {
        val intentOpen = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("page_url", pageUrl)
            putExtra("seek_time", currentTime)
            putExtra("from_notification", true)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingOpen = PendingIntent.getActivity(this, 201, intentOpen, flags)

        val intentPlay = Intent(this, MediaPlaybackService::class.java).apply {
            action = ACTION_PLAY
        }
        val pendingPlay = PendingIntent.getService(this, 202, intentPlay, flags)

        val intentPause = Intent(this, MediaPlaybackService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pendingPause = PendingIntent.getService(this, 203, intentPause, flags)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Reproduciendo en segundo plano" else "Pausado")
            .setOngoing(isPlaying)
            .setContentIntent(pendingOpen)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isPlaying) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pausar", pendingPause)
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Reproducir", pendingPlay)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                builder.build(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
        }
    }
}
