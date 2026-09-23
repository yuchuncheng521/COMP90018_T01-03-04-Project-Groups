package com.knot.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionManager {

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    fun hasNearbyPermissions(context: Context): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val scanGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

            val connectGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

            val advertiseGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) == PackageManager.PERMISSION_GRANTED

            val nearbyWifiGranted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

            return scanGranted &&
                    connectGranted &&
                    advertiseGranted &&
                    nearbyWifiGranted
        }

        return true
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager

        return bluetoothManager.adapter?.isEnabled == true
    }
}
