package com.example.tallerfirebase.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tallerfirebase.ui.auth.AuthViewModel
import com.example.tallerfirebase.ui.auth.LoginScreen
import com.example.tallerfirebase.ui.auth.RegisterScreen
import com.example.tallerfirebase.ui.map.MapScreen
import com.example.tallerfirebase.ui.profile.ProfileScreen

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val navController = rememberNavController()
    val startDestination: Any = if (authViewModel.isLoggedIn()) MapRoute else LoginRoute

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<LoginRoute> {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(MapRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                },
                onGoToRegister = {
                    navController.navigate(RegisterRoute)
                }
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(MapRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<MapRoute> {
            MapScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(LoginRoute) {
                        popUpTo<MapRoute> { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(ProfileRoute)
                }
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}