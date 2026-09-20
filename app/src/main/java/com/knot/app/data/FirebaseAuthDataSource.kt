package com.knot.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.knot.app.model.UserAccount
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/** Talks to the real Firebase Auth SDK. Kept behind an interface so it can be faked in tests. */
interface FirebaseAuthDataSource {
    val currentUser: UserAccount?
    val isLoggedIn: Boolean
    fun authStateFlow(): Flow<UserAccount?>
    suspend fun signIn(email: String, password: String): UserAccount
    suspend fun signUp(displayName: String, email: String, password: String): UserAccount
    fun signOut()
    suspend fun sendPasswordResetEmail(email: String)
}

/** Writes the Firestore "users/{uid}" profile document. Kept behind an interface for testing. */
interface UserProfileDataSource {
    suspend fun createProfile(user: UserAccount)
}

class FirebaseAuthDataSourceImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : FirebaseAuthDataSource {

    override val currentUser: UserAccount?
        get() = auth.currentUser?.toUserAccount()

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override fun authStateFlow(): Flow<UserAccount?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toUserAccount())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): UserAccount = wrapAuthErrors {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw AuthException(AuthErrorCode.UNKNOWN, "Login failed. Please try again.")
        user.toUserAccount()
    }

    override suspend fun signUp(displayName: String, email: String, password: String): UserAccount = wrapAuthErrors {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw AuthException(AuthErrorCode.UNKNOWN, "Sign up failed. Please try again.")
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        runCatching { user.updateProfile(profileUpdates).await() }
        UserAccount(uid = user.uid, displayName = displayName, email = user.email ?: email)
    }

    override fun signOut() {
        auth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        wrapAuthErrors { auth.sendPasswordResetEmail(email).await() }
    }

    private fun FirebaseUser.toUserAccount(): UserAccount = UserAccount(
        uid = uid,
        displayName = displayName ?: email?.substringBefore("@") ?: "You",
        email = email ?: "",
        photoUrl = photoUrl?.toString()
    )

    private inline fun <T> wrapAuthErrors(block: () -> T): T {
        return try {
            block()
        } catch (e: FirebaseAuthException) {
            throw AuthErrorMapper.map(e.errorCode, e.message, e)
        } catch (e: FirebaseNetworkException) {
            throw AuthErrorMapper.map("ERROR_NETWORK_REQUEST_FAILED", e.message, e)
        }
    }
}

class FirestoreUserProfileDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserProfileDataSource {
    override suspend fun createProfile(user: UserAccount) {
        val data = mapOf(
            "uid" to user.uid,
            "displayName" to user.displayName,
            "email" to user.email,
            "photoUrl" to user.photoUrl,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .await()
    }
}