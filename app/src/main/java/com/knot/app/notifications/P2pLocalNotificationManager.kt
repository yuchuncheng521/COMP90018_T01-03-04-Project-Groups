package com.knot.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.knot.app.MainActivity
import com.knot.app.R

object P2pLocalNotificationManager {

    private const val CHANNEL_ID = "p2p_member_detected"
    private const val CHANNEL_NAME = "Nearby group members"

    fun showNearbyMemberNotification(
        context: Context,
        peerName: String
    ) {
        if (!NotificationPreferences.isPushEnabled(context)) {
            return
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createNotificationChannel(context)

        val openAppIntent =
            Intent(context, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("$peerName is nearby!")
                .setContentText("Capture a memory together?")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$peerName is nearby. Capture a memory together?")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        NotificationManagerCompat.from(context)
            .notify(
                peerName.hashCode(),
                notification
            )
    }

    private fun createNotificationChannel(
        context: Context
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description =
                        "Alerts when a member of one of your Knot groups is nearby"
                }

            context.getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }
    }
}
