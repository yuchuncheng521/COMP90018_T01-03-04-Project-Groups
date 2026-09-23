package com.knot.app.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenManager {

    fun syncCurrentToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance()
            .token
            .addOnSuccessListener { token ->
                saveToken(uid, token)
            }
            .addOnFailureListener { exception ->
                println("Failed to get FCM token: ${exception.message}")
            }
    }

    fun saveTokenForCurrentUser(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveToken(uid, token)
    }

    private fun saveToken(
        uid: String,
        token: String
    ) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener {
                println("FCM token saved for user: $uid")
            }
            .addOnFailureListener { exception ->
                println("Failed to save FCM token: ${exception.message}")
            }
    }
}
