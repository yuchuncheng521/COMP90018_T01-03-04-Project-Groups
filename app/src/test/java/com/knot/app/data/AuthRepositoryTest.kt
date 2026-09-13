package com.knot.app.data

import com.knot.app.model.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AuthRepository]'s orchestration logic: trimming input, turning data-source
 * failures into [Result.failure], and making sure sign-up also creates the Firestore profile
 * (and fails loudly if that write fails).
 *
 * These use hand-written fakes for [FirebaseAuthDataSource] / [UserProfileDataSource] instead of
 * mocking the Firebase SDK -- AuthRepository never touches FirebaseAuth/FirebaseFirestore
 * directly (see FirebaseAuthDataSource.kt), so no Firebase classes, no Robolectric, and no
 * mocking library are needed here at all.
 */
class AuthRepositoryTest {

    private lateinit var authDataSource: FakeAuthDataSource
    private lateinit var profileDataSource: FakeUserProfileDataSource
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        authDataSource = FakeAuthDataSource()
        profileDataSource = FakeUserProfileDataSource()
        repository = AuthRepository(authDataSource, profileDataSource)
    }

    @Test
    fun `signIn trims the email before calling the data source`() = runTest {
        authDataSource.signInResult = { email, _ -> UserAccount(uid = "u1", email = email, displayName = "Test") }

        repository.signIn("  test@example.com  ", "password123")

        assertEquals("test@example.com", authDataSource.signInCalls.single().first)
    }

    @Test
    fun `signIn success returns the account wrapped in Result success`() = runTest {
        val account = UserAccount(uid = "u1", email = "test@example.com", displayName = "Test")
        authDataSource.signInResult = { _, _ -> account }

        val result = repository.signIn("test@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals(account, result.getOrNull())
    }

    @Test
    fun `signIn failure (wrong password) surfaces as Result failure with the friendly message`() = runTest {
        val wrongPassword = AuthErrorMapper.map("ERROR_WRONG_PASSWORD", null, RuntimeException())
        authDataSource.signInResult = { _, _ -> throw wrongPassword }

        val result = repository.signIn("test@example.com", "wrong")

        assertTrue(result.isFailure)
        assertEquals("Incorrect email or password.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signUp trims displayName and email before calling the data source`() = runTest {
        authDataSource.signUpResult = { email, _, displayName ->
            UserAccount(uid = "u1", email = email, displayName = displayName)
        }

        repository.signUp("  Alice  ", "  alice@example.com  ", "password123")

        val call = authDataSource.signUpCalls.single()
        assertEquals("alice@example.com", call.first)
        assertEquals("Alice", call.third)
    }

    @Test
    fun `signUp creates a Firestore profile with the new user's uid, name and email`() = runTest {
        authDataSource.signUpResult = { email, _, displayName ->
            UserAccount(uid = "new-uid", email = email, displayName = displayName)
        }

        repository.signUp("Alice", "alice@example.com", "password123")

        val profileCall = profileDataSource.createProfileCalls.single()
        assertEquals("new-uid", profileCall.first)
        assertEquals("Alice", profileCall.second)
        assertEquals("alice@example.com", profileCall.third)
    }

    @Test
    fun `signUp does not attempt to create a profile if account creation fails`() = runTest {
        val emailTaken = AuthErrorMapper.map("ERROR_EMAIL_ALREADY_IN_USE", null, RuntimeException())
        authDataSource.signUpResult = { _, _, _ -> throw emailTaken }

        val result = repository.signUp("Alice", "alice@example.com", "password123")

        assertTrue(result.isFailure)
        assertEquals("That email is already registered. Try logging in instead.", result.exceptionOrNull()?.message)
        assertTrue(profileDataSource.createProfileCalls.isEmpty())
    }

    @Test
    fun `signUp fails overall if the Auth account is created but the Firestore profile write fails`() = runTest {
        authDataSource.signUpResult = { email, _, displayName ->
            UserAccount(uid = "new-uid", email = email, displayName = displayName)
        }
        profileDataSource.shouldThrow = IllegalStateException("Firestore is unreachable")

        val result = repository.signUp("Alice", "alice@example.com", "password123")

        // A user with an Auth account but no profile document can't join/create groups later,
        // so this must NOT look like a successful sign-up to the caller.
        assertTrue(result.isFailure)
    }

    @Test
    fun `signOut delegates to the auth data source`() {
        repository.signOut()

        assertTrue(authDataSource.signOutCalled)
    }

    @Test
    fun `sendPasswordResetEmail trims the email and returns success`() = runTest {
        val result = repository.sendPasswordResetEmail("  alice@example.com  ")

        assertTrue(result.isSuccess)
        assertEquals("alice@example.com", authDataSource.sendPasswordResetEmailCalls.single())
    }

    @Test
    fun `sendPasswordResetEmail for an unregistered address surfaces the friendly error`() = runTest {
        val notFound = AuthErrorMapper.map("ERROR_USER_NOT_FOUND", null, RuntimeException())
        authDataSource.sendPasswordResetEmailResult = { throw notFound }

        val result = repository.sendPasswordResetEmail("nobody@example.com")

        assertFalse(result.isSuccess)
        assertEquals("No account found with that email.", result.exceptionOrNull()?.message)
    }
}

/** Hand-written fake -- no mocking library needed since [FirebaseAuthDataSource] is a small interface. */
private class FakeAuthDataSource : FirebaseAuthDataSource {

    override var currentUser: UserAccount? = null
    override val isLoggedIn: Boolean get() = currentUser != null

    var authStateFlowValue: Flow<UserAccount?> = flowOf(null)
    override fun authStateFlow(): Flow<UserAccount?> = authStateFlowValue

    val signInCalls = mutableListOf<Pair<String, String>>()
    var signInResult: (email: String, password: String) -> UserAccount = { _, _ -> error("signIn not stubbed") }
    override suspend fun signIn(email: String, password: String): UserAccount {
        signInCalls += email to password
        return signInResult(email, password)
    }

    val signUpCalls = mutableListOf<Triple<String, String, String>>() // email, password, displayName
    var signUpResult: (email: String, password: String, displayName: String) -> UserAccount =
        { _, _, _ -> error("signUp not stubbed") }
    override suspend fun signUp(email: String, password: String, displayName: String): UserAccount {
        signUpCalls += Triple(email, password, displayName)
        return signUpResult(email, password, displayName)
    }

    var signOutCalled = false
    override fun signOut() {
        signOutCalled = true
    }

    val sendPasswordResetEmailCalls = mutableListOf<String>()
    var sendPasswordResetEmailResult: (email: String) -> Unit = {}
    override suspend fun sendPasswordResetEmail(email: String) {
        sendPasswordResetEmailCalls += email
        sendPasswordResetEmailResult(email)
    }
}

/** Hand-written fake for the Firestore "users" profile write. */
private class FakeUserProfileDataSource : UserProfileDataSource {
    val createProfileCalls = mutableListOf<Triple<String, String, String>>() // uid, displayName, email
    var shouldThrow: Throwable? = null

    override suspend fun createProfile(uid: String, displayName: String, email: String) {
        createProfileCalls += Triple(uid, displayName, email)
        shouldThrow?.let { throw it }
    }
}
