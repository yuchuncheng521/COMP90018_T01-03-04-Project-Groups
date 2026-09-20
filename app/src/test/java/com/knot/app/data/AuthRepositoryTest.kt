package com.knot.app.data

import com.knot.app.model.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAuthDataSource(
    private var user: UserAccount? = null,
    private val signInResult: Result<UserAccount>? = null,
    private val signUpResult: Result<UserAccount>? = null,
    private val resetShouldFail: Boolean = false
) : FirebaseAuthDataSource {

    var signOutCalled = false
        private set
    var lastSignInEmail: String? = null
    var lastResetEmail: String? = null

    override val currentUser: UserAccount? get() = user
    override val isLoggedIn: Boolean get() = user != null

    override fun authStateFlow(): Flow<UserAccount?> = MutableStateFlow(user)

    override suspend fun signIn(email: String, password: String): UserAccount {
        lastSignInEmail = email
        return signInResult?.getOrThrow() ?: throw AuthException(AuthErrorCode.UNKNOWN, "not configured")
    }

    override suspend fun signUp(displayName: String, email: String, password: String): UserAccount {
        return signUpResult?.getOrThrow() ?: throw AuthException(AuthErrorCode.UNKNOWN, "not configured")
    }

    override fun signOut() {
        signOutCalled = true
        user = null
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        lastResetEmail = email
        if (resetShouldFail) throw AuthException(AuthErrorCode.USER_NOT_FOUND, "No account found with that email.")
    }
}

private class FakeUserProfileDataSource(private val shouldFail: Boolean = false) : UserProfileDataSource {
    var createProfileCalledWith: UserAccount? = null
        private set

    override suspend fun createProfile(user: UserAccount) {
        createProfileCalledWith = user
        if (shouldFail) error("Couldn't write profile")
    }
}

class AuthRepositoryTest {

    private val testUser = UserAccount(uid = "uid-1", displayName = "Alice", email = "alice@example.com")

    @Test
    fun `signIn trims email and returns the user on success`() = runTest {
        val authSource = FakeAuthDataSource(signInResult = Result.success(testUser))
        val repository = AuthRepository(authSource, FakeUserProfileDataSource())

        val result = repository.signIn("  alice@example.com  ", "password123")

        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
        assertEquals("alice@example.com", authSource.lastSignInEmail)
    }

    @Test
    fun `signIn propagates failure from the data source`() = runTest {
        val authSource = FakeAuthDataSource(
            signInResult = Result.failure(AuthException(AuthErrorCode.WRONG_PASSWORD, "Incorrect email or password."))
        )
        val repository = AuthRepository(authSource, FakeUserProfileDataSource())

        val result = repository.signIn("alice@example.com", "wrong")

        assertTrue(result.isFailure)
        assertEquals("Incorrect email or password.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signUp does not create a profile if auth fails`() = runTest {
        val authSource = FakeAuthDataSource(
            signUpResult = Result.failure(AuthException(AuthErrorCode.EMAIL_ALREADY_IN_USE, "That email is already registered. Try logging in instead."))
        )
        val profileSource = FakeUserProfileDataSource()
        val repository = AuthRepository(authSource, profileSource)

        val result = repository.signUp("Alice", "alice@example.com", "password123")

        assertTrue(result.isFailure)
        assertEquals(null, profileSource.createProfileCalledWith)
    }

    @Test
    fun `signUp fails overall if profile creation fails even though auth succeeded`() = runTest {
        val authSource = FakeAuthDataSource(signUpResult = Result.success(testUser))
        val profileSource = FakeUserProfileDataSource(shouldFail = true)
        val repository = AuthRepository(authSource, profileSource)

        val result = repository.signUp("Alice", "alice@example.com", "password123")

        assertTrue(result.isFailure)
        assertEquals(testUser, profileSource.createProfileCalledWith)
    }

    @Test
    fun `signUp succeeds and creates a profile when both steps succeed`() = runTest {
        val authSource = FakeAuthDataSource(signUpResult = Result.success(testUser))
        val profileSource = FakeUserProfileDataSource()
        val repository = AuthRepository(authSource, profileSource)

        val result = repository.signUp("Alice", "alice@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals(testUser, profileSource.createProfileCalledWith)
    }

    @Test
    fun `signOut delegates to the data source`() {
        val authSource = FakeAuthDataSource(user = testUser)
        val repository = AuthRepository(authSource, FakeUserProfileDataSource())

        repository.signOut()

        assertTrue(authSource.signOutCalled)
    }

    @Test
    fun `sendPasswordResetEmail trims the email`() = runTest {
        val authSource = FakeAuthDataSource()
        val repository = AuthRepository(authSource, FakeUserProfileDataSource())

        val result = repository.sendPasswordResetEmail("  alice@example.com  ")

        assertTrue(result.isSuccess)
        assertEquals("alice@example.com", authSource.lastResetEmail)
    }

    @Test
    fun `sendPasswordResetEmail surfaces a failure`() = runTest {
        val authSource = FakeAuthDataSource(resetShouldFail = true)
        val repository = AuthRepository(authSource, FakeUserProfileDataSource())

        val result = repository.sendPasswordResetEmail("nobody@example.com")

        assertFalse(result.isSuccess)
        assertEquals("No account found with that email.", result.exceptionOrNull()?.message)
    }
}