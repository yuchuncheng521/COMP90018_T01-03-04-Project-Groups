package com.knot.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.knot.app.data.AuthRepository
import com.knot.app.ui.activities.ActivitiesScreen
import com.knot.app.ui.activities.ActivitiesViewModel
import com.knot.app.ui.activities.ActivityDetailScreen
import com.knot.app.ui.activities.CreateActivityScreen
import com.knot.app.ui.auth.AuthViewModel
import com.knot.app.ui.auth.LoginScreen
import com.knot.app.ui.auth.SignUpScreen
import com.knot.app.ui.components.KnotBottomNavBar
import com.knot.app.ui.groups.GroupDetailScreen
import com.knot.app.ui.groups.GroupsScreen
import com.knot.app.ui.groups.GroupsViewModel
import com.knot.app.ui.settings.AccountSettingsScreen
import com.knot.app.ui.settings.AccountSettingsViewModel
import com.knot.app.ui.settings.AppPreferencesScreen
import com.knot.app.ui.settings.DeleteAccountScreen
import com.knot.app.ui.settings.EditProfileScreen
import com.knot.app.ui.groups.MonthDetailScreen

@Composable
fun KnotNavHost() {
    val navController = rememberNavController()

    val startDestination = if (AuthRepository().isLoggedIn) RootGraph.MAIN else RootGraph.AUTH

    NavHost(navController = navController, startDestination = startDestination) {

        // ---- Auth flow: Login / Sign up, no bottom nav ----
        composable(RootGraph.AUTH) {
            AuthNavHost(
                onAuthenticated = {
                    navController.navigate(RootGraph.MAIN) {
                        popUpTo(RootGraph.AUTH) { inclusive = true }
                    }
                },
            )
        }

        // ---- Main app flow: Groups / Activities / Account, with bottom nav ----
        composable(RootGraph.MAIN) {
            MainNavHost(
                onSignedOut = {
                    navController.navigate(RootGraph.AUTH) {
                        popUpTo(RootGraph.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}

@Composable
private fun AuthNavHost(onAuthenticated: () -> Unit) {
    val authNavController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = authNavController, startDestination = AuthScreen.Login.route) {
        composable(AuthScreen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { onAuthenticated() },
                onNavigateToSignUp = {
                    authViewModel.clearError()
                    authNavController.navigate(AuthScreen.SignUp.route)
                }
            )
        }
        composable(AuthScreen.SignUp.route) {
            SignUpScreen(
                viewModel = authViewModel,
                onSignUpSuccess = { onAuthenticated() },
                onNavigateToLogin = {
                    authViewModel.clearError()
                    authNavController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun MainNavHost(onSignedOut: () -> Unit) {
    val mainNavController: NavHostController = rememberNavController()

    Scaffold(
        bottomBar = { KnotBottomNavBar(mainNavController) }
    ) { padding ->
        NavHost(
            navController = mainNavController,
            startDestination = MainScreen.Groups.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(MainScreen.Groups.route) {
                val groupsViewModel: GroupsViewModel = viewModel()
                GroupsScreen(
                    viewModel = groupsViewModel,
                    onGroupClick = { group ->
                        mainNavController.navigate("groupDetail/${group.id}")
                    }
                )
            }
            composable(
                route = "groupDetail/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupDetailScreen(
                    groupId = groupId,
                    onBackClick = { mainNavController.popBackStack() },
                    onAlbumClick = { album ->
                        mainNavController.navigate("monthDetail/${album}")
                    }
                )
            }
            composable(MainScreen.Activities.route) {
                val activitiesViewModel: ActivitiesViewModel = viewModel()
                ActivitiesScreen(
                    viewModel = activitiesViewModel,
                    onActivityClick = { activity ->
                        mainNavController.navigate("activityDetail/${activity.id}")
                    },
                    onCreateClick = {
                        mainNavController.navigate("createActivity")
                    }
                )
            }
            composable(
                route = "activityDetail/{activityId}",
                arguments = listOf(navArgument("activityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId") ?: return@composable
                // Reuse the ViewModel from the Activities screen to share state
                val parentEntry = remember(backStackEntry) {
                    mainNavController.getBackStackEntry(MainScreen.Activities.route)
                }
                val activitiesViewModel: ActivitiesViewModel = viewModel(parentEntry)
                
                ActivityDetailScreen(
                    activityId = activityId,
                    viewModel = activitiesViewModel,
                    onBackClick = { mainNavController.popBackStack() }
                )
            }
            composable(
                route = "monthDetail/{monthLabel}",
                arguments = listOf(navArgument("monthLabel") { type = NavType.StringType })
            ) { backStackEntry ->
                val monthLabel = backStackEntry.arguments?.getString("monthLabel") ?: return@composable
                MonthDetailScreen(
                    monthLabel = monthLabel,
                    onBackClick = { mainNavController.popBackStack() }
                )
            }
            composable("createActivity") {
                CreateActivityScreen(
                    onBackClick = { mainNavController.popBackStack() }
                )
            }
            composable(MainScreen.Settings.route) {
                val settingsViewModel: AccountSettingsViewModel = viewModel()
                AccountSettingsScreen(
                    viewModel = settingsViewModel,
                    onSignedOut = onSignedOut,
                    onProfileClick = { mainNavController.navigate("editProfile") },
                    onAppPreferencesClick = { mainNavController.navigate("appPreferences") },
                    onDeleteAccountClick = { mainNavController.navigate("deleteAccount") }
                )
            }
            composable("editProfile") {
                val settingsViewModel: AccountSettingsViewModel = viewModel()
                EditProfileScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { mainNavController.popBackStack() }
                )
            }
            composable("appPreferences") {
                val settingsViewModel: AccountSettingsViewModel = viewModel()
                AppPreferencesScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { mainNavController.popBackStack() }
                )
            }
            composable("deleteAccount") {
                val settingsViewModel: AccountSettingsViewModel = viewModel()
                DeleteAccountScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { mainNavController.popBackStack() },
                    onDeleted = { onSignedOut() }
                )
            }
        }
    }
}
