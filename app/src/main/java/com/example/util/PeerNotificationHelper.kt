// PeerLink Production Sync - Active
/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: PeerNotificationHelper.kt
 *
 * Responsibilities:
 * - Manages notification channels for Peer Presence and Chat Reconnection alerts.
 * - Posts high-priority heads-up notifications when an offline peer reconnects so the user can continue chatting.
 * - Configures PendingIntents targeting MainActivity with the peerId extra for direct deep-link resume.
 */

package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object PeerNotificationHelper {
    private const val CHANNEL_ID = "peerlink_chat_reconnect_channel"
    private const val CHANNEL_NAME = "Peer Presence & Chat Alerts"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when an offline contact returns online to resume chatting"
                enableVibration(true)
                setShowBadge(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun notifyPeerBackOnline(context: Context, peerId: String, peerName: String) {
        initNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat_peer_id", peerId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            peerId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🟢 $peerName is back online!")
            .setContentText("Chat history restored. Tap to continue chatting.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🟢 $peerName came back online. Your chat history was saved locally and pending messages are syncing. Tap to continue chatting from where you left off.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                "Resume Chat",
                pendingIntent
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(peerId.hashCode(), notification)
    }

    fun clearNotification(context: Context, peerId: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(peerId.hashCode())
    }
}
