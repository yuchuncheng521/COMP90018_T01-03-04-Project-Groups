package com.knot.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.knot.app.MainActivity
import com.knot.app.R

class KnotFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.saveTokenForCurrentUser(token)
        println("FCM token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (!NotificationPreferences.isPushEnabled(this)) {
            return
        }

        val title =
            message.notification?.title
                ?: message.data["title"]
                ?: "Knot"

        val body =
            message.notification?.body
                ?: message.data["body"]
                ?: "You have a new update."

        showNotification(title, body)
    }

    private fun showNotification(
        title: String,
        body: String
    ) {
        createNotificationChannel()

        val openAppIntent =
            Intent(this, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            notification
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Knot updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description =
                        "Notifications for prompts, replies, and Knot updates"
                }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "knot_updates"
    }
}
