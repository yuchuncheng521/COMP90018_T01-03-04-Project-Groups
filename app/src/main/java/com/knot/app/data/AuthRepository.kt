package com.knot.app.data

import com.knot.app.model.UserAccount
import com.knot.app.notifications.FcmTokenManager
import kotlinx.coroutines.flow.Flow

/**
 * Handles sign in, sign up, sign out, and password reset.
 * This class doesn't talk to Firebase directly — [FirebaseAuthDataSource] and [UserProfileDataSource] do that.
 * Keeping them separate makes it possible to test this class with fake data instead of a real Firebase connection.
 */
class AuthRepository(
    private val authDataSource: FirebaseAuthDataSource = FirebaseAuthDataSourceImpl(),
    private val profileDataSource: UserProfileDataSource = FirestoreUserProfileDataSource()
) {

    val currentUser: UserAccount?
        get() = authDataSource.currentUser

    val isLoggedIn: Boolean
        get() = authDataSource.isLoggedIn

    fun authStateFlow(): Flow<UserAccount?> = authDataSource.authStateFlow()

    suspend fun signIn(email: String, password: String): Result<UserAccount> = runCatching {
        val user = authDataSource.signIn(email.trim(), password)
        FcmTokenManager.syncCurrentToken()
        user
    }

    suspend fun signUp(displayName: String, email: String, password: String): Result<UserAccount> = runCatching {
        val user = authDataSource.signUp(displayName.trim(), email.trim(), password)
        profileDataSource.createProfile(user)
        FcmTokenManager.syncCurrentToken()
        user
    }

    fun signOut() {
        authDataSource.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        authDataSource.sendPasswordResetEmail(email.trim())
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
