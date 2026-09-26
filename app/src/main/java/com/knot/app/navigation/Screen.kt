package com.knot.app.navigation

import com.knot.app.R

/** Auth flow routes (no bottom nav). */
sealed class AuthScreen(val route: String) {
    object Login : AuthScreen("login")
    object SignUp : AuthScreen("sign_up")
    object ForgotPassword : AuthScreen("forgot_password")

}

/** Main app routes, each shown with the bottom navigation bar. */
sealed class MainScreen(val route: String, val label: String, val iconRes: Int) {
    object Groups : MainScreen("groups", "Groups", R.drawable.nav_ic_groups)
    object Activities : MainScreen("activities", "Activities", R.drawable.nav_ic_activities)
    object Settings : MainScreen("settings", "Account", R.drawable.nav_ic_account)

    companion object {
        val bottomNavItems = listOf(Groups, Activities, Settings)
    }
}

/** Top-level graph roots, used to switch between the auth flow and the main app flow. */
object RootGraph {
    const val AUTH = "auth_graph"
    const val MAIN = "main_graph"
}

/**
 * A group's month list (the "album" covers screen).
 * Reached by tapping a group on the Groups screen.
 */
object GroupDetailScreenRoute {
    const val ROUTE_PATTERN = "groupDetail/{groupId}/{groupName}"
    fun route(groupId: String, groupName: String) = "groupDetail/$groupId/$groupName"
}

/**
 * A group's shared timeline for one month (the weekly spread). Reached by tapping a month "album" on GroupDetailScreen.
 */
object TimelineScreenRoute {
    const val ROUTE_PATTERN = "timeline/{groupId}/{groupName}"
    fun route(groupId: String, groupName: String) = "timeline/$groupId/$groupName"
}