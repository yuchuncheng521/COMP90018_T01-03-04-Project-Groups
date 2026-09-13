package com.knot.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.knot.app.data.AuthRepository
import com.knot.app.ui.activities.ActivitiesScreen
import com.knot.app.ui.activities.ActivitiesViewModel
import com.knot.app.ui.auth.AuthViewModel
import com.knot.app.ui.auth.ForgotPasswordScreen
import com.knot.app.ui.auth.LoginScreen
import com.knot.app.ui.auth.SignUpScreen
import com.knot.app.ui.components.KnotBottomNavBar
import com.knot.app.ui.groups.GroupsScreen
import com.knot.app.ui.groups.GroupsViewModel
import com.knot.app.ui.settings.AccountSettingsScreen
import com.knot.app.ui.settings.AccountSettingsViewModel

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
                }
            )
        }

        // ---- Main app flow: Groups / Activities / Account, with bottom nav ----
        composable(RootGraph.MAIN) {
            MainNavHost(
                onSignedOut = {
                    navController.navigate(RootGraph.AUTH) {
                        popUpTo(RootGraph.MAIN) { inclusive = true }
                    }
                }
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
                },
                onNavigateToForgotPassword = {
                    authViewModel.clearError()
                    authNavController.navigate(AuthScreen.ForgotPassword.route)
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
        composable(AuthScreen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = {
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
                GroupsScreen(viewModel = groupsViewModel)
            }
            composable(MainScreen.Activities.route) {
                val activitiesViewModel: ActivitiesViewModel = viewModel()
                ActivitiesScreen(viewModel = activitiesViewModel)
            }
            composable(MainScreen.Settings.route) {
                val settingsViewModel: AccountSettingsViewModel = viewModel()
                AccountSettingsScreen(
                    viewModel = settingsViewModel,
                    onSignedOut = onSignedOut
                )
            }
        }
    }
}
