package com.example.tallerfirebase.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallerfirebase.data.location.LocationClient
import com.example.tallerfirebase.data.model.MapPoint
import com.example.tallerfirebase.data.model.User
import com.example.tallerfirebase.data.repository.AuthRepository
import com.example.tallerfirebase.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val currentUser: User? = null,
    val onlineUsers: List<User> = emptyList(),
    val isTracking: Boolean = false,
    val myLocation: MapPoint? = null,
    val myRoute: List<MapPoint> = emptyList(),
    val otherRoutes: Map<String, List<MapPoint>> = emptyMap()
) {
    // Usuarios conectados distintos al usuario actual
    val otherUsers: List<User>
        get() = onlineUsers.filter { it.uid != currentUser?.uid }
}

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val locationClient = LocationClient(application)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var boundUid: String? = null
    private var observeJob: Job? = null
    private var trackingJob: Job? = null

    // Se llama al entrar a la pantalla: empieza a escuchar al usuario actual y a los conectados
    fun bind() {
        val uid = authRepository.currentUid ?: return
        if (uid == boundUid) return
        boundUid = uid
        trackingJob?.cancel()
        observeJob?.cancel()
        _uiState.value = MapUiState()

        observeJob = viewModelScope.launch {
            // Al abrir la app el usuario inicia desconectado hasta que active el switch
            launch { runCatching { userRepository.setConnected(uid, false) } }
            launch {
                userRepository.observeUser(uid)
                    .catch { }
                    .collect { user -> _uiState.update { it.copy(currentUser = user) } }
            }
            launch {
                userRepository.observeOnlineUsers()
                    .catch { emit(emptyList()) }
                    .collect { users ->
                        _uiState.update { currentState ->
                            val currentOnlineOtherUids = users.filter { it.uid != currentState.currentUser?.uid }.map { it.uid }.toSet()
                            val newOtherRoutes = currentState.otherRoutes.filterKeys { it in currentOnlineOtherUids }.toMutableMap()

                            users.filter { it.uid != currentState.currentUser?.uid }.forEach { user ->
                                if (user.latitud != 0.0 || user.longitud != 0.0) {
                                    val p = MapPoint(user.latitud, user.longitud)
                                    val existing = newOtherRoutes[user.uid] ?: emptyList()
                                    if (existing.lastOrNull() != p) {
                                        newOtherRoutes[user.uid] = existing + p
                                    }
                                }
                            }

                            currentState.copy(
                                onlineUsers = users,
                                otherRoutes = newOtherRoutes
                            )
                        }
                    }
            }
        }
    }

    // Enciende el rastreo de posición (locationUpdates) y actualiza Firebase en cada cambio
    fun startTracking() {
        val uid = boundUid ?: return
        if (trackingJob?.isActive == true) return
        _uiState.update { it.copy(isTracking = true) }

        trackingJob = viewModelScope.launch {
            var markedConnected = false
            locationClient.locationUpdates().collect { location ->
                val newPoint = MapPoint(location.latitude, location.longitude)
                _uiState.update { currentState ->
                    val updatedRoute = if (currentState.myRoute.lastOrNull() != newPoint) {
                        currentState.myRoute + newPoint
                    } else {
                        currentState.myRoute
                    }
                    currentState.copy(
                        myLocation = newPoint,
                        myRoute = updatedRoute
                    )
                }
                runCatching { userRepository.updateLocation(uid, location.latitude, location.longitude) }
                // Se marca como conectado después de guardar la primera posición real
                if (!markedConnected) {
                    runCatching { userRepository.setConnected(uid, true) }
                    markedConnected = true
                }
            }
        }
    }

    // Apaga el rastreo y marca al usuario como desconectado
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.update { currentState ->
            currentState.copy(
                isTracking = false,
                myLocation = null,
                myRoute = emptyList()
            )
        }
        val uid = boundUid ?: return
        viewModelScope.launch { runCatching { userRepository.setConnected(uid, false) } }
    }

    // Desconecta al usuario ANTES de cerrar sesión (después ya no tendría permisos para escribir)
    fun logout(onLoggedOut: () -> Unit) {
        trackingJob?.cancel()
        trackingJob = null
        val uid = boundUid
        viewModelScope.launch {
            if (uid != null) runCatching { userRepository.setConnected(uid, false) }
            observeJob?.cancel()
            boundUid = null
            _uiState.value = MapUiState()
            authRepository.logout()
            onLoggedOut()
        }
    }
}
