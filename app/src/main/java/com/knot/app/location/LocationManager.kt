package com.knot.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class CurrentLocation(
    val latitude: Double,
    val longitude: Double
)

class LocationManager(
    context: Context
) {

    private val appContext = context.applicationContext

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(appContext)

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

    suspend fun getLocationName(
        location: CurrentLocation
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            val address = Geocoder(
                appContext,
                Locale.getDefault()
            )
                .getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                ?.firstOrNull()
                ?: return@runCatching null

            val parts = listOfNotNull(
                address.subLocality?.takeIf { it.isNotBlank() },
                address.locality?.takeIf { it.isNotBlank() },
                address.adminArea?.takeIf { it.isNotBlank() },
                address.countryName?.takeIf { it.isNotBlank() }
            )
                .distinct()

            parts.takeIf { it.isNotEmpty() }
                ?.joinToString(", ")
                ?: address.getAddressLine(0)
        }.getOrNull()
    }
}
