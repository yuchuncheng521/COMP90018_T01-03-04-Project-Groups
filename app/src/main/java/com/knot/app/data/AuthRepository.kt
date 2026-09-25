package com.knot.app.data

import com.knot.app.model.UserAccount
import com.knot.app.notifications.FcmTokenManager
import kotlinx.coroutines.flow.Flow

/**
 * Handles authentication and account operations through data sources.
 * Keeping Firebase access behind data-source interfaces makes the repository easier to test.
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

    suspend fun signUp(
        displayName: String,
        email: String,
        password: String
    ): Result<UserAccount> = runCatching {
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
        val user = currentUser ?: error("Not logged in")
        val trimmedName = name.trim()

        authDataSource.updateDisplayName(trimmedName)
        profileDataSource.updateDisplayName(user.uid, trimmedName)
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        authDataSource.updateEmail(newEmail.trim())
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        authDataSource.updatePassword(newPassword)
    }

    suspend fun deleteAccount(email: String, password: String): Result<Unit> = runCatching {
        val user = currentUser ?: error("Not logged in")

        authDataSource.reauthenticate(email.trim(), password)
        profileDataSource.deleteProfile(user.uid)
        authDataSource.deleteAccount()
    }
}
