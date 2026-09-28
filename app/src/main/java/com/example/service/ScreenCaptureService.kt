package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.diagnostic.AppDiagnostics

class ScreenCaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "peerlink_screen_share"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        const val EXTRA_TARGET_IP = "EXTRA_TARGET_IP"
        const val EXTRA_TARGET_NAME = "EXTRA_TARGET_NAME"

        var onMediaProjectionReadyListener: ((MediaProjection, String, String) -> Unit)? = null
        var onServiceStoppedListener: (() -> Unit)? = null
        var pendingData: Intent? = null
        var pendingResultCode: Int = Activity.RESULT_CANCELED
        var isServiceRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AppDiagnostics.log("ScreenCaptureService", "ACTION_STOP received")
            isServiceRunning = false
            onServiceStoppedListener?.invoke()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isServiceRunning = true
            AppDiagnostics.log("ScreenCaptureService", "Foreground service started with MEDIA_PROJECTION")
        } catch (e: Throwable) {
            AppDiagnostics.log("ScreenCaptureService", "Failed to startForeground: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        // On Android 14+, getMediaProjection MUST be called only after startForeground
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData: Intent? = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }) ?: pendingData
        val effectiveResultCode = if (resultCode != Activity.RESULT_CANCELED) resultCode else pendingResultCode
        val targetIp = intent?.getStringExtra(EXTRA_TARGET_IP) ?: ""
        val targetName = intent?.getStringExtra(EXTRA_TARGET_NAME) ?: "Peer"

        if (effectiveResultCode == Activity.RESULT_OK && resultData != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            try {
                val mp = projectionManager?.getMediaProjection(effectiveResultCode, resultData)
                if (mp != null) {
                    AppDiagnostics.log("ScreenCaptureService", "MediaProjection successfully obtained")
                    onMediaProjectionReadyListener?.invoke(mp, targetIp, targetName)
                } else {
                    AppDiagnostics.log("ScreenCaptureService", "getMediaProjection returned null")
                    stopSelf()
                }
            } catch (e: Throwable) {
                AppDiagnostics.log("ScreenCaptureService", "getMediaProjection failed: ${e.message}", e)
                stopSelf()
            } finally {
                pendingData = null
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        onServiceStoppedListener?.invoke()
    }

    private fun createNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (launchIntent != null) {
            android.app.PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PeerLink Screen Sharing")
            .setContentText("Screen is actively being shared to nearby peer")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PeerLink Screen Share Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when screen sharing is in progress"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
