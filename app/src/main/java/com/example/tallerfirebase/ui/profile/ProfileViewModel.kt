package com.example.tallerfirebase.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallerfirebase.data.model.User
import com.example.tallerfirebase.data.repository.AuthRepository
import com.example.tallerfirebase.data.repository.StorageRepository
import com.example.tallerfirebase.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val currentUser: User? = null,
    val nombre: String = "",
    val identificacion: String = "",
    val telefono: String = "",
    val fotoUrl: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                if (user != null) {
                    _uiState.update { state ->
                        state.copy(
                            currentUser = user,
                            nombre = if (state.nombre.isEmpty()) user.nombre else state.nombre,
                            identificacion = if (state.identificacion.isEmpty()) user.identificacion else state.identificacion,
                            telefono = if (state.telefono.isEmpty()) user.telefono else state.telefono,
                            fotoUrl = user.fotoUrl
                        )
                    }
                }
            }
        }
    }

    fun onNombreChanged(value: String) {
        _uiState.update { it.copy(nombre = value, errorMessage = null) }
    }

    fun onIdentificacionChanged(value: String) {
        _uiState.update { it.copy(identificacion = value, errorMessage = null) }
    }

    fun onTelefonoChanged(value: String) {
        _uiState.update { it.copy(telefono = value, errorMessage = null) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        val uid = authRepository.currentUid ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val url = storageRepository.uploadProfileImageBitmap(uid, bitmap)
                userRepository.updateProfile(
                    uid = uid,
                    nombre = _uiState.value.nombre.ifBlank { _uiState.value.currentUser?.nombre ?: "" },
                    identificacion = _uiState.value.identificacion.ifBlank { _uiState.value.currentUser?.identificacion ?: "" },
                    telefono = _uiState.value.telefono.ifBlank { _uiState.value.currentUser?.telefono ?: "" },
                    fotoUrl = url
                )
                url
            }.onSuccess { url ->
                _uiState.update { it.copy(isLoading = false, fotoUrl = url, isSuccess = true) }
            }.onFailure { ex ->
                _uiState.update { it.copy(isLoading = false, errorMessage = ex.localizedMessage ?: "Error al subir la foto") }
            }
        }
    }

    fun onPhotoSelected(context: Context, uri: Uri) {
        val uid = authRepository.currentUid ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val url = storageRepository.uploadProfileImageUri(context, uid, uri)
                userRepository.updateProfile(
                    uid = uid,
                    nombre = _uiState.value.nombre.ifBlank { _uiState.value.currentUser?.nombre ?: "" },
                    identificacion = _uiState.value.identificacion.ifBlank { _uiState.value.currentUser?.identificacion ?: "" },
                    telefono = _uiState.value.telefono.ifBlank { _uiState.value.currentUser?.telefono ?: "" },
                    fotoUrl = url
                )
                url
            }.onSuccess { url ->
                _uiState.update { it.copy(isLoading = false, fotoUrl = url, isSuccess = true) }
            }.onFailure { ex ->
                _uiState.update { it.copy(isLoading = false, errorMessage = ex.localizedMessage ?: "Error al subir la foto") }
            }
        }
    }

    fun updateProfile() {
        val state = _uiState.value
        val uid = authRepository.currentUid ?: return

        if (state.nombre.isBlank() || state.identificacion.isBlank() || state.telefono.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Completa todos los campos") }
            return
        }

        if (state.newPassword.isNotEmpty() && state.newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching {
                userRepository.updateProfile(
                    uid = uid,
                    nombre = state.nombre.trim(),
                    identificacion = state.identificacion.trim(),
                    telefono = state.telefono.trim(),
                    fotoUrl = state.fotoUrl.ifBlank { null }
                )
                if (state.newPassword.isNotBlank()) {
                    authRepository.updatePassword(state.newPassword)
                }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true, newPassword = "") }
            }.onFailure { ex ->
                _uiState.update { it.copy(isLoading = false, errorMessage = ex.localizedMessage ?: "Error al actualizar") }
            }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}
