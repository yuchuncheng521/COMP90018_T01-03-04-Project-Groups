package com.knot.app.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.knot.app.model.UserAccount
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/**
 * Everything [AuthRepository] needs from Firebase Authentication, behind an interface so the
 * repository's orchestration logic (validation, error mapping, "also write a Firestore profile
 * after sign-up") can be unit-tested against a hand-written fake instead of the real SDK.
 */
interface FirebaseAuthDataSource {
    val currentUser: UserAccount?
    val isLoggedIn: Boolean

    /** Emits the signed-in user (or null) now, and again whenever Firebase's auth state changes. */
    fun authStateFlow(): Flow<UserAccount?>

    suspend fun signIn(email: String, password: String): UserAccount
    suspend fun signUp(email: String, password: String, displayName: String): UserAccount
    fun signOut()
    suspend fun sendPasswordResetEmail(email: String)
}

/** Real implementation backed by [FirebaseAuth]. Throws [AuthException] on any failure. */
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
        val user = result.user ?: throw IllegalStateException("Login failed: no user returned")
        user.toUserAccount()
    }

    override suspend fun signUp(email: String, password: String, displayName: String): UserAccount = wrapAuthErrors {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Sign up failed: no user returned")

        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        // Best-effort: a failure here shouldn't fail the whole sign-up, the display name is
        // also stored on the Firestore profile document created right after.
        runCatching { user.updateProfile(profileUpdate).await() }

        UserAccount(uid = user.uid, displayName = displayName, email = user.email ?: email)
    }

    override fun signOut() = auth.signOut()

    override suspend fun sendPasswordResetEmail(email: String) {
        // Block body (not `= wrapAuthErrors { ... }`) on purpose: Task<Void>.await() resolves to
        // the Java platform type Void!, which doesn't line up with this interface method's Unit
        // return type as an expression body. A block body just discards the awaited result.
        wrapAuthErrors { auth.sendPasswordResetEmail(email).await() }
    }

    /** Runs [block], converting any Firebase auth/network exception into a friendly [AuthException]. */
    private inline fun <T> wrapAuthErrors(block: () -> T): T {
        try {
            return block()
        } catch (e: FirebaseAuthException) {
            throw AuthErrorMapper.map(e.errorCode, e.message, e)
        } catch (e: FirebaseNetworkException) {
            throw AuthErrorMapper.map("ERROR_NETWORK_REQUEST_FAILED", e.message, e)
        }
    }

    private fun FirebaseUser.toUserAccount() = UserAccount(
        uid = uid,
        displayName = displayName ?: email?.substringBefore("@") ?: "You",
        email = email ?: "",
        photoUrl = photoUrl?.toString()
    )
}

/** Everything [AuthRepository] needs to keep the "users" Firestore collection in sync. */
interface UserProfileDataSource {
    /** Creates (or merges into) the Firestore profile document for a newly-signed-up user. */
    suspend fun createProfile(uid: String, displayName: String, email: String)
}

/** Real implementation backed by Firestore's "users/{uid}" documents. */
class FirestoreUserProfileDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserProfileDataSource {

    override suspend fun createProfile(uid: String, displayName: String, email: String) {
        val profile = hashMapOf(
            "uid" to uid,
            "displayName" to displayName,
            "email" to email,
            "groupIds" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        // merge = true: if this ever runs twice for the same uid (e.g. a retried sign-up call)
        // it won't clobber groupIds the user may have picked up in between.
        firestore.collection("users").document(uid).set(profile, SetOptions.merge()).await()
    }
}
