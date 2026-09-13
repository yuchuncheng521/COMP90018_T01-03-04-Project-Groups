package com.knot.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Auth flow routes (no bottom nav). */
sealed class AuthScreen(val route: String) {
    object Login : AuthScreen("login")
    object SignUp : AuthScreen("sign_up")
    object ForgotPassword : AuthScreen("forgot_password")
}

/** Main app routes, each shown with the bottom navigation bar. */
sealed class MainScreen(val route: String, val label: String, val icon: ImageVector) {
    object Groups : MainScreen("groups", "Groups", Icons.Filled.Groups)
    object Activities : MainScreen("activities", "Activities", Icons.Filled.Checklist)
    object Settings : MainScreen("settings", "Account", Icons.Filled.Settings)

    companion object {
        val bottomNavItems = listOf(Groups, Activities, Settings)
    }
}

/** Top-level graph roots, used to switch between the auth flow and the main app flow. */
object RootGraph {
    const val AUTH = "auth_graph"
    const val MAIN = "main_graph"
}
