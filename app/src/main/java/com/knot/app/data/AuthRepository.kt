package com.knot.app.data

import com.knot.app.model.UserAccount
import kotlinx.coroutines.flow.Flow

/**
 * Wraps Firebase Authentication (email/password) + the "users" Firestore profile collection
 * behind a small suspend-friendly API that returns [Result], so callers (ViewModels) never have
 * to deal with raw Firebase exceptions.
 *
 * The actual Firebase/Firestore calls live behind [FirebaseAuthDataSource] and
 * [UserProfileDataSource] so this class's orchestration logic -- trimming input, mapping errors,
 * "sign-up also creates a Firestore profile" -- can be unit-tested with fakes. See
 * AuthRepositoryTest.
 */
class AuthRepository(
    private val authDataSource: FirebaseAuthDataSource = FirebaseAuthDataSourceImpl(),
    private val profileDataSource: UserProfileDataSource = FirestoreUserProfileDataSource()
) {

    val currentUser: UserAccount?
        get() = authDataSource.currentUser

    val isLoggedIn: Boolean
        get() = authDataSource.isLoggedIn

    /**
     * Emits the current signed-in user (or null) immediately, then again every time Firebase's
     * auth state changes (sign-in, sign-out, or the session/token being invalidated elsewhere).
     * Collect this instead of only reading [isLoggedIn] once at app launch, so the UI reacts on
     * its own if a session expires while the app is open.
     */
    fun authStateFlow(): Flow<UserAccount?> = authDataSource.authStateFlow()

    suspend fun signIn(email: String, password: String): Result<UserAccount> = runCatching {
        authDataSource.signIn(email.trim(), password)
    }

    suspend fun signUp(displayName: String, email: String, password: String): Result<UserAccount> = runCatching {
        val trimmedName = displayName.trim()
        val trimmedEmail = email.trim()
        val user = authDataSource.signUp(trimmedEmail, password, trimmedName)

        // A user with an Auth account but no Firestore document can't join/create groups later,
        // so a failure here fails the whole signUp() call (rather than being swallowed) -- the UI
        // can then show an error and let the user retry.
        profileDataSource.createProfile(uid = user.uid, displayName = trimmedName, email = trimmedEmail)

        user
    }

    fun signOut() = authDataSource.signOut()

    /**
     * Sends a Firebase password-reset email. Note that Firebase itself fails this with a
     * "no account found" error (mapped to [AuthErrorCode.USER_NOT_FOUND]) when the address isn't
     * registered, so the UI *can* tell the user that -- unlike some other providers, Firebase
     * doesn't hide account existence on this endpoint.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        authDataSource.sendPasswordResetEmail(email.trim())
    }
}
