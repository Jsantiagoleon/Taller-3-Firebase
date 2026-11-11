package com.example.tallerfirebase.ui.map

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tallerfirebase.R
import com.example.tallerfirebase.ui.components.ConnectionSwitch
import com.example.tallerfirebase.ui.components.OnlineUsersCard
import com.example.tallerfirebase.util.LOCATION_PERMISSIONS
import com.example.tallerfirebase.util.hasLocationPermission
import com.example.tallerfirebase.util.isLocationEnabled
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    val permisoDenegadoMsg = stringResource(R.string.permiso_ubicacion_denegado)
    val gpsDesactivadoMsg = stringResource(R.string.gps_desactivado)

    LaunchedEffect(Unit) { viewModel.bind() }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun connect() {
        if (!context.isLocationEnabled()) showMessage(gpsDesactivadoMsg)
        viewModel.startTracking()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) connect() else showMessage(permisoDenegadoMsg)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mapa_titulo)) },
                actions = {
                    ConnectionSwitch(
                        isConnected = state.isTracking,
                        onToggle = { enabled ->
                            if (enabled) {
                                if (context.hasLocationPermission()) connect()
                                else permissionLauncher.launch(LOCATION_PERMISSIONS)
                            } else {
                                viewModel.stopTracking()
                            }
                        }
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.cd_menu)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_editar_perfil)) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToProfile()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_cerrar_sesion)) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.logout(onLoggedOut = onLogout)
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OsmMap(
                currentUser = state.currentUser,
                myLocation = state.myLocation,
                myRoute = state.myRoute,
                otherUsers = state.otherUsers,
                otherRoutes = state.otherRoutes,
                modifier = Modifier.fillMaxSize()
            )
            OnlineUsersCard(
                onlineCount = state.otherUsers.size,
                isConnected = state.isTracking,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}