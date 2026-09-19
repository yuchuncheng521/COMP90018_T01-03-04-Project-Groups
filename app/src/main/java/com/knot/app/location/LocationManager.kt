package com.knot.app.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

data class CurrentLocation(
    val latitude: Double,
    val longitude: Double
)

class LocationManager(
    context: Context
) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): CurrentLocation? {

        val cancellationTokenSource = CancellationTokenSource()

        val location = fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
            .await()

        return location?.let {
            CurrentLocation(
                latitude = it.latitude,
                longitude = it.longitude
            )
        }
    }
}