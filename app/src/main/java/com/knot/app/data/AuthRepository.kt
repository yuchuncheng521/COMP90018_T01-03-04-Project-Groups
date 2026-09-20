package com.knot.app.data

import com.knot.app.model.UserAccount
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
        authDataSource.signIn(email.trim(), password)
    }

    suspend fun signUp(displayName: String, email: String, password: String): Result<UserAccount> = runCatching {
        val user = authDataSource.signUp(displayName.trim(), email.trim(), password)
        profileDataSource.createProfile(user)
        user
    }

    fun signOut() {
        authDataSource.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        authDataSource.sendPasswordResetEmail(email.trim())
    }
}