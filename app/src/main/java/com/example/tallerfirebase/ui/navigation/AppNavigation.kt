package com.example.tallerfirebase.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.tallerfirebase.ui.auth.AuthViewModel
import com.example.tallerfirebase.ui.auth.LoginScreen
import com.example.tallerfirebase.ui.auth.RegisterScreen
import com.example.tallerfirebase.ui.map.MapScreen

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val startRoute: NavKey = if (authViewModel.isLoggedIn()) MapRoute else LoginRoute
    val backStack = rememberNavBackStack(startRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<LoginRoute> {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = { backStack.resetTo(MapRoute) },
                    onGoToRegister = { backStack.add(RegisterRoute) }
                )
            }
            entry<RegisterRoute> {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = { backStack.resetTo(MapRoute) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<MapRoute> {
                MapScreen(
                    onLogout = {
                        authViewModel.logout()
                        backStack.resetTo(LoginRoute)
                    }
                )
            }
        }
    )
}

// Deja la pila de navegación solo con la ruta indicada (para que "atrás" no regrese al login)
private fun MutableList<NavKey>.resetTo(route: NavKey) {
    add(route)
    removeAll { it != route }
}
