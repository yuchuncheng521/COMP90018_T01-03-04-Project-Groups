package com.knot.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NetworkTransport {
    WIFI,
    CELLULAR,
    OFFLINE
}

object NetworkMonitor {

    fun getNetworkTransport(context: Context): NetworkTransport {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkTransport.OFFLINE
        val activeNetwork = cm.activeNetwork ?: return NetworkTransport.OFFLINE
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkTransport.OFFLINE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> NetworkTransport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> NetworkTransport.CELLULAR
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> NetworkTransport.WIFI
            else -> NetworkTransport.OFFLINE
        }
    }

    fun observeNetworkTransport(context: Context): Flow<NetworkTransport> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(NetworkTransport.OFFLINE)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getNetworkTransport(context))
            }

            override fun onLost(network: Network) {
                trySend(getNetworkTransport(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(getNetworkTransport(context))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching { cm.registerNetworkCallback(request, callback) }
        trySend(getNetworkTransport(context))

        awaitClose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }
}
