package com.knot.app.model

/** Basic profile info shown on the Account Settings screen. */
data class UserAccount(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null
)
