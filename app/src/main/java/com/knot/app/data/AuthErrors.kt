package com.knot.app.data

enum class AuthErrorCode {
    INVALID_EMAIL, WRONG_PASSWORD, USER_NOT_FOUND, USER_DISABLED,
    EMAIL_ALREADY_IN_USE, WEAK_PASSWORD, NETWORK_ERROR, REQUIRES_RECENT_LOGIN, UNKNOWN
}

class AuthException(
    val code: AuthErrorCode,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

object AuthErrorMapper {
    fun map(errorCode: String?, fallbackMessage: String?, cause: Throwable): AuthException {
        val (code, message) = messageFor(errorCode, fallbackMessage)
        return AuthException(code, message, cause)
    }

    fun messageFor(errorCode: String?, fallbackMessage: String? = null): Pair<AuthErrorCode, String> =
        when (errorCode) {
            "ERROR_INVALID_EMAIL" -> AuthErrorCode.INVALID_EMAIL to "The email address is invalid."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> AuthErrorCode.WRONG_PASSWORD to "Incorrect email or password. Please type it again!"
            "ERROR_USER_NOT_FOUND" -> AuthErrorCode.USER_NOT_FOUND to "No account found with that email. Please register with a new account."
            "ERROR_USER_DISABLED" -> AuthErrorCode.USER_DISABLED to "This account has been disabled. Contact support for help."
            "ERROR_EMAIL_ALREADY_IN_USE" -> AuthErrorCode.EMAIL_ALREADY_IN_USE to "That email is already registered. Try logging in instead."
            "ERROR_WEAK_PASSWORD" -> AuthErrorCode.WEAK_PASSWORD to "Password is too weak. Use at least 6 characters, mixing letters and numbers."
            "ERROR_NETWORK_REQUEST_FAILED" -> AuthErrorCode.NETWORK_ERROR to "Network error. Check your connection and try again."
            "ERROR_REQUIRES_RECENT_LOGIN" -> AuthErrorCode.REQUIRES_RECENT_LOGIN to "Please sign in again to continue."
            else -> AuthErrorCode.UNKNOWN to (fallbackMessage?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again.")
        }
}