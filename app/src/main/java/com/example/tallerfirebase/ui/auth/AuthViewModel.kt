package com.example.tallerfirebase.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallerfirebase.R
import com.example.tallerfirebase.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val nombre: String = "",
    val identificacion: String = "",
    val correo: String = "",
    val password: String = "",
    val telefono: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val authSuccess: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun isLoggedIn(): Boolean = repository.isLoggedIn()

    fun onNombreChange(value: String) {
        _uiState.update { it.copy(nombre = value, errorRes = null) }
    }

    fun onIdentificacionChange(value: String) {
        _uiState.update { it.copy(identificacion = value, errorRes = null) }
    }

    fun onCorreoChange(value: String) {
        _uiState.update { it.copy(correo = value, errorRes = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorRes = null) }
    }

    fun onTelefonoChange(value: String) {
        _uiState.update { it.copy(telefono = value, errorRes = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorRes = null) }
    }

    fun consumeAuthSuccess() {
        _uiState.update { it.copy(authSuccess = false) }
    }

    fun login() {
        val s = _uiState.value
        val error = when {
            s.correo.isBlank() || s.password.isBlank() -> R.string.error_campos_vacios
            !Patterns.EMAIL_ADDRESS.matcher(s.correo.trim()).matches() -> R.string.error_correo_invalido
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorRes = error) }
            return
        }
        runAuth { repository.login(s.correo.trim(), s.password) }
    }

    fun register() {
        val s = _uiState.value
        val error = when {
            listOf(s.nombre, s.identificacion, s.correo, s.password, s.telefono).any { it.isBlank() } ->
                R.string.error_campos_vacios
            !s.identificacion.trim().all { it.isDigit() } -> R.string.error_identificacion_invalida
            !Patterns.EMAIL_ADDRESS.matcher(s.correo.trim()).matches() -> R.string.error_correo_invalido
            s.password.length < 6 -> R.string.error_password_corta
            s.telefono.trim().count { it.isDigit() } < 7 -> R.string.error_telefono_invalido
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorRes = error) }
            return
        }
        runAuth {
            repository.register(
                nombre = s.nombre.trim(),
                identificacion = s.identificacion.trim(),
                correo = s.correo.trim(),
                password = s.password,
                telefono = s.telefono.trim()
            )
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState()
    }

    private fun runAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorRes = null) }
            try {
                block()
                _uiState.value = AuthUiState(authSuccess = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorRes = mapError(e)) }
            }
        }
    }

    @StringRes
    private fun mapError(e: Exception): Int = when (e) {
        is FirebaseAuthUserCollisionException -> R.string.error_correo_existe
        is FirebaseAuthWeakPasswordException -> R.string.error_password_corta
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> R.string.error_credenciales
        else -> R.string.error_generico
    }
}
