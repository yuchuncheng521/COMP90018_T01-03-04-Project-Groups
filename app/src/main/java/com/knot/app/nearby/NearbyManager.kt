package com.knot.app.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution

class NearbyManager(
    context: Context
) {

    private val connectionsClient =
        Nearby.getConnectionsClient(context)

    private val _nearbyMembers =
        MutableStateFlow<List<NearbyMember>>(emptyList())

    val nearbyMembers: StateFlow<List<NearbyMember>> =
        _nearbyMembers.asStateFlow()

    private val _connectedMembers =
        MutableStateFlow<List<NearbyMember>>(emptyList())

    val connectedMembers: StateFlow<List<NearbyMember>> =
        _connectedMembers.asStateFlow()

    private val serviceId = "com.knot.app"

    private val strategy = Strategy.P2P_CLUSTER

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {
                val member = NearbyMember(
                    endpointId = endpointId,
                    endpointName = info.endpointName
                )

                val updatedList =
                    _nearbyMembers.value
                        .filterNot {
                            it.endpointId == endpointId
                        } + member

                _nearbyMembers.value = updatedList

                connectionsClient.requestConnection(
                    "KnotUser",
                    endpointId,
                    connectionLifecycleCallback
                )
            }

            override fun onEndpointLost(
                endpointId: String
            ) {
                _nearbyMembers.value =
                    _nearbyMembers.value.filterNot {
                        it.endpointId == endpointId
                    }
            }
        }

    fun startDiscovery() {

        val options =
            com.google.android.gms.nearby.connection.DiscoveryOptions
                .Builder()
                .setStrategy(strategy)
                .build()

        connectionsClient
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                options
            )
            .addOnSuccessListener {
                println("Nearby discovery started")
                _errorMessage.value = null
            }
            .addOnFailureListener { exception ->
                _errorMessage.value =
                    "Nearby discovery failed: ${exception.message}"
            }
    }
    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {

                connectionsClient.acceptConnection(
                    endpointId,
                    object : com.google.android.gms.nearby.connection.PayloadCallback() {
                        override fun onPayloadReceived(
                            endpointId: String,
                            payload: com.google.android.gms.nearby.connection.Payload
                        ) {
                        }

                        override fun onPayloadTransferUpdate(
                            endpointId: String,
                            update: com.google.android.gms.nearby.connection.PayloadTransferUpdate
                        ) {
                        }
                    }
                )
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                if (result.status.isSuccess) {

                    val member =
                        _nearbyMembers.value.find {
                            it.endpointId == endpointId
                        }

                    if (member != null) {
                        _connectedMembers.value =
                            _connectedMembers.value
                                .filterNot {
                                    it.endpointId == endpointId
                                } + member
                    }

                    println("Nearby connection successful: $endpointId")

                } else {
                    println("Nearby connection failed: $endpointId")
                }
            }

            override fun onDisconnected(
                endpointId: String
            ) {
                _connectedMembers.value =
                    _connectedMembers.value.filterNot {
                        it.endpointId == endpointId
                    }
            }
        }
    fun startAdvertising(
        userName: String
    ) {
        val options =
            AdvertisingOptions.Builder()
                .setStrategy(strategy)
                .build()

        connectionsClient
            .startAdvertising(
                userName,
                serviceId,
                connectionLifecycleCallback,
                options
            )
            .addOnSuccessListener {
                println("Nearby advertising started")
                _errorMessage.value = null
            }
            .addOnFailureListener { exception ->
                _errorMessage.value =
                    "Nearby advertising failed: ${exception.message}"
            }
    }
    fun stopNearby() {
        connectionsClient.stopDiscovery()
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()

        _nearbyMembers.value = emptyList()
        _connectedMembers.value = emptyList()
    }
}