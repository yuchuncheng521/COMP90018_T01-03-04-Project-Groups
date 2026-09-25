package com.knot.app.nearby

data class NearbyMember(
    val endpointId: String,
    val endpointName: String,
    val userId: String? = null,
    val sharedGroupId: String? = null
)
