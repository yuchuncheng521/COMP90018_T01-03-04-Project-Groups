package com.knot.app.data

/**
 * UI-facing categories for auth failures. Screens can switch on [AuthException.code] when they
 * need to react differently (e.g. show a "Resend verification" button), and always have
 * [AuthException.message] as a ready-to-display string either way.
 */
enum class AuthErrorCode {
    INVALID_EMAIL,
    WRONG_PASSWORD,
    USER_NOT_FOUND,
    USER_DISABLED,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    NETWORK_ERROR,
    REQUIRES_RECENT_LOGIN,
    UNKNOWN
}

/** A friendly, already-safe-to-display auth error. [message] is what the UI should show as-is. */
class AuthException(
    val code: AuthErrorCode,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Maps FirebaseAuth's `errorCode` string constants (see [com.google.firebase.auth.FirebaseAuthException.getErrorCode])
 * to a friendly [AuthException].
 *
 * This is deliberately a pure function over plain strings rather than over the Firebase SDK's
 * exception *types* (FirebaseAuthInvalidCredentialsException, FirebaseAuthWeakPasswordException,
 * etc.). Those classes are constructed internally by the Firebase SDK and aren't meant to be
 * instantiated from app code, which makes them awkward to exercise directly in a JVM unit test.
 * Keeping the mapping keyed on the errorCode string means [AuthErrorMapperTest] can test every
 * branch with plain string literals -- no Firebase SDK, no mocking, no Robolectric needed.
 */
object AuthErrorMapper {

    fun map(errorCode: String?, fallbackMessage: String?, cause: Throwable): AuthException {
        val (code, message) = messageFor(errorCode, fallbackMessage)
        return AuthException(code, message, cause)
    }

    /** Exposed separately from [map] so tests can assert on the (code, message) pair directly. */
    fun messageFor(errorCode: String?, fallbackMessage: String? = null): Pair<AuthErrorCode, String> =
        when (errorCode) {
            "ERROR_INVALID_EMAIL" ->
                AuthErrorCode.INVALID_EMAIL to "That email address doesn't look right."

            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                AuthErrorCode.WRONG_PASSWORD to "Incorrect email or password."

            "ERROR_USER_NOT_FOUND" ->
                AuthErrorCode.USER_NOT_FOUND to "No account found with that email."

            "ERROR_USER_DISABLED" ->
                AuthErrorCode.USER_DISABLED to "This account has been disabled. Contact support for help."

            "ERROR_EMAIL_ALREADY_IN_USE" ->
                AuthErrorCode.EMAIL_ALREADY_IN_USE to "That email is already registered. Try logging in instead."

            "ERROR_WEAK_PASSWORD" ->
                AuthErrorCode.WEAK_PASSWORD to "Password is too weak. Use at least 6 characters, mixing letters and numbers."

            "ERROR_NETWORK_REQUEST_FAILED" ->
                AuthErrorCode.NETWORK_ERROR to "Network error. Check your connection and try again."

            "ERROR_REQUIRES_RECENT_LOGIN" ->
                AuthErrorCode.REQUIRES_RECENT_LOGIN to "Please sign in again to continue."

            else ->
                AuthErrorCode.UNKNOWN to (fallbackMessage?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again.")
        }
}
