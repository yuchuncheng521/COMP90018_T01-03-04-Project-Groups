package com.knot.app.data

import com.google.firebase.auth.FirebaseAuth
import com.knot.app.model.UserAccount
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Authentication (email/password) behind a small suspend-friendly API.
 *
 * This is a STUB: it talks to the real FirebaseAuth SDK, but until a Firebase project +
 * google-services.json is wired up, calls will throw. Screens catch that and surface a
 * friendly error message rather than crashing, so the UI is fully navigable either way.
 */
class AuthRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val currentUser: UserAccount?
        get() = auth.currentUser?.let {
            UserAccount(
                uid = it.uid,
                displayName = it.displayName ?: it.email?.substringBefore("@") ?: "You",
                email = it.email ?: "",
                photoUrl = it.photoUrl?.toString()
            )
        }

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String): Result<UserAccount> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Login failed: no user returned")
        UserAccount(
            uid = user.uid,
            displayName = user.displayName ?: email.substringBefore("@"),
            email = user.email ?: email
        )
    }

    suspend fun signUp(displayName: String, email: String, password: String): Result<UserAccount> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Sign up failed: no user returned")

        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        runCatching { user.updateProfile(profileUpdates).await() }

        UserAccount(uid = user.uid, displayName = displayName, email = email)
    }

    fun signOut() {
        auth.signOut()
    }
}
