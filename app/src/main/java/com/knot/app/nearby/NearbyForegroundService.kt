package com.knot.app.nearby

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.knot.app.KnotApplication
import com.knot.app.R

class NearbyForegroundService : Service() {

    private lateinit var nearbyManager: NearbyManager
    private var nearbyStarted = false

    override fun onCreate() {
        super.onCreate()
        nearbyManager = (application as KnotApplication).nearbyManager
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val userName =
            intent?.getStringExtra(EXTRA_USER_NAME)
                ?.takeIf { it.isNotBlank() }
                ?: "KnotUser"

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Knot P2P alerts active")
                .setContentText("Looking for nearby group members.")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        if (!nearbyStarted) {
            nearbyStarted = true
            nearbyManager.startAdvertising(userName)
            nearbyManager.startDiscovery()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        nearbyStarted = false
        nearbyManager.stopNearby()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "P2P proximity detection",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps nearby member detection active"
                }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_USER_NAME = "extra_user_name"

        private const val CHANNEL_ID = "nearby_p2p"
        private const val NOTIFICATION_ID = 1001
    }
}
