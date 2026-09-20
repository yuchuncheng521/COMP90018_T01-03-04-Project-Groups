package com.knot.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

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

        // Sync to Firestore
        val userAccount = UserAccount(uid = user.uid, displayName = displayName, email = email)
        firestore.collection("users").document(user.uid).set(userAccount).await()

        userAccount
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not logged in")
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()

        // Sync to Firestore
        firestore.collection("users").document(user.uid).update("displayName", name).await()
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not logged in")
        // Note: verifyBeforeUpdateEmail is preferred as it sends a verification email 
        // to the new address before actually changing it.
        user.verifyBeforeUpdateEmail(newEmail).await()

        // Sync to Firestore
        firestore.collection("users").document(user.uid).update("email", newEmail).await()
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not logged in")
        user.updatePassword(newPassword).await()
    }

    suspend fun deleteAccount(email: String, password: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not logged in")
        
        // Re-authenticate before deletion
        val credentials = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credentials).await()
        
        // Delete from Firestore
        firestore.collection("users").document(user.uid).delete().await()
        
        // Delete from Auth
        user.delete().await()
    }
}
