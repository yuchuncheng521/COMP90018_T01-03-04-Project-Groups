package com.knot.app.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenManager {

    private const val TAG = "FCM_DEBUG"

    fun syncCurrentToken() {
        Log.d(TAG, "FCM sync started")

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "FCM sync skipped: no current Firebase user")
            return
        }

        val uid = currentUser.uid
        Log.d(TAG, "FCM current user uid = $uid")

        FirebaseMessaging.getInstance()
            .token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token received = $token")
                saveToken(uid, token)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get FCM token", exception)
            }
    }

    fun saveTokenForCurrentUser(token: String) {
        Log.d(TAG, "FCM onNewToken callback received")

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "FCM token not saved: no current Firebase user")
            return
        }

        saveToken(currentUser.uid, token)
    }

    private fun saveToken(
        uid: String,
        token: String
    ) {
        Log.d(TAG, "Saving FCM token for uid = $uid")

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token saved for user = $uid")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save FCM token for uid = $uid", exception)
            }
    }
}
