package com.knot.app.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.knot.app.notifications.P2pLocalNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyManager(
    context: Context
) {

    private val appContext = context.applicationContext

    private val connectionsClient =
        Nearby.getConnectionsClient(appContext)

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val _nearbyMembers =
        MutableStateFlow<List<NearbyMember>>(emptyList())

    val nearbyMembers: StateFlow<List<NearbyMember>> =
        _nearbyMembers.asStateFlow()

    private val _connectedMembers =
        MutableStateFlow<List<NearbyMember>>(emptyList())

    val connectedMembers: StateFlow<List<NearbyMember>> =
        _connectedMembers.asStateFlow()

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()

    private val serviceId = "com.knot.app"
    private val strategy = Strategy.P2P_CLUSTER

    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {
                val bytes = payload.asBytes() ?: return
                val peerUid = bytes.toString(Charsets.UTF_8)

                if (peerUid.isBlank()) return

                validateSharedGroup(
                    endpointId = endpointId,
                    peerUid = peerUid
                )
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
                // UID payloads are tiny byte payloads, so no progress UI is needed.
            }
        }

    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {
                val member =
                    NearbyMember(
                        endpointId = endpointId,
                        endpointName = connectionInfo.endpointName
                    )

                _nearbyMembers.value =
                    _nearbyMembers.value
                        .filterNot { it.endpointId == endpointId } + member

                connectionsClient.acceptConnection(
                    endpointId,
                    payloadCallback
                )
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                if (!result.status.isSuccess) {
                    _errorMessage.value =
                        "Nearby connection failed for $endpointId"
                    return
                }

                val myUid = auth.currentUser?.uid

                if (myUid.isNullOrBlank()) {
                    _errorMessage.value =
                        "Sign in before using P2P proximity alerts."
                    connectionsClient.disconnectFromEndpoint(endpointId)
                    return
                }

                // Exchange Firebase UIDs after the Nearby connection succeeds.
                // The peer is NOT treated as a connected group member until
                // Firestore confirms that both users share at least one group.
                connectionsClient
                    .sendPayload(
                        endpointId,
                        Payload.fromBytes(myUid.toByteArray(Charsets.UTF_8))
                    )
                    .addOnFailureListener { exception ->
                        _errorMessage.value =
                            "Could not verify nearby member: ${exception.message}"
                        connectionsClient.disconnectFromEndpoint(endpointId)
                    }
            }

            override fun onDisconnected(
                endpointId: String
            ) {
                _nearbyMembers.value =
                    _nearbyMembers.value.filterNot {
                        it.endpointId == endpointId
                    }

                _connectedMembers.value =
                    _connectedMembers.value.filterNot {
                        it.endpointId == endpointId
                    }
            }
        }

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {
                val member =
                    NearbyMember(
                        endpointId = endpointId,
                        endpointName = info.endpointName
                    )

                _nearbyMembers.value =
                    _nearbyMembers.value
                        .filterNot { it.endpointId == endpointId } + member

                connectionsClient.requestConnection(
                    auth.currentUser?.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: "KnotUser",
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
            DiscoveryOptions.Builder()
                .setStrategy(strategy)
                .build()

        connectionsClient
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                options
            )
            .addOnSuccessListener {
                _errorMessage.value = null
            }
            .addOnFailureListener { exception ->
                _errorMessage.value =
                    "Nearby discovery failed: ${exception.message}"
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
                _errorMessage.value = null
            }
            .addOnFailureListener { exception ->
                _errorMessage.value =
                    "Nearby advertising failed: ${exception.message}"
            }
    }

    private fun validateSharedGroup(
        endpointId: String,
        peerUid: String
    ) {
        val myUid = auth.currentUser?.uid

        if (myUid.isNullOrBlank() || peerUid == myUid) {
            connectionsClient.disconnectFromEndpoint(endpointId)
            return
        }

        val peerUserRef =
            firestore.collection("users").document(peerUid)

        // Use the groups collection as the source of truth instead of users/{uid}.groupIds.
        // A user's cached groupIds can contain stale IDs after a group is deleted, which
        // previously produced a fake "Shared group" option in the P2P picker.
        firestore.collection("groups")
            .whereArrayContains("memberIds", myUid)
            .get()
            .addOnSuccessListener groupsSuccess@{ groupSnapshot ->
                val sharedGroupIds =
                    groupSnapshot.documents
                        .filter { document ->
                            val memberIds =
                                (document.get("memberIds") as? List<*>)
                                    ?.filterIsInstance<String>()
                                    .orEmpty()

                            peerUid in memberIds
                        }
                        .map { it.id }

                if (sharedGroupIds.isEmpty()) {
                    connectionsClient.disconnectFromEndpoint(endpointId)
                    return@groupsSuccess
                }

                peerUserRef.get()
                    .addOnSuccessListener { peerDocument ->
                        val existingMember =
                            _nearbyMembers.value.firstOrNull {
                                it.endpointId == endpointId
                            }

                        val peerName =
                            peerDocument.getString("displayName")
                                ?.takeIf { it.isNotBlank() }
                                ?: existingMember?.endpointName
                                ?: "Group member"

                        val verifiedMember =
                            NearbyMember(
                                endpointId = endpointId,
                                endpointName = peerName,
                                userId = peerUid,
                                sharedGroupId = sharedGroupIds.first(),
                                sharedGroupIds = sharedGroupIds
                            )

                        _nearbyMembers.value =
                            _nearbyMembers.value
                                .filterNot {
                                    it.endpointId == endpointId
                                } + verifiedMember

                        _connectedMembers.value =
                            _connectedMembers.value
                                .filterNot {
                                    it.endpointId == endpointId
                                } + verifiedMember

                        P2pLocalNotificationManager
                            .showNearbyMemberNotification(
                                context = appContext,
                                peerName = peerName
                            )

                        _errorMessage.value = null
                    }
                    .addOnFailureListener { exception ->
                        _errorMessage.value =
                            "Could not load nearby member: ${exception.message}"
                        connectionsClient.disconnectFromEndpoint(endpointId)
                    }
            }
            .addOnFailureListener { exception ->
                _errorMessage.value =
                    "Could not check shared groups: ${exception.message}"
                connectionsClient.disconnectFromEndpoint(endpointId)
            }
    }

    fun stopNearby() {
        connectionsClient.stopDiscovery()
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()

        _nearbyMembers.value = emptyList()
        _connectedMembers.value = emptyList()
        _errorMessage.value = null
    }
}
