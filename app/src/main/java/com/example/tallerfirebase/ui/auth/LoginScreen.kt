package com.example.tallerfirebase.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tallerfirebase.R
import com.example.tallerfirebase.ui.components.AppTextField
import com.example.tallerfirebase.ui.components.AuthHeader
import com.example.tallerfirebase.ui.components.ErrorText
import com.example.tallerfirebase.ui.components.PrimaryButton

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.authSuccess) {
        if (state.authSuccess) {
            viewModel.consumeAuthSuccess()
            onLoginSuccess()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthHeader(titleRes = R.string.login_titulo, subtitleRes = R.string.login_subtitulo)

            AppTextField(
                value = state.correo,
                onValueChange = viewModel::onCorreoChange,
                labelRes = R.string.campo_correo,
                icon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email
            )
            AppTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                labelRes = R.string.campo_password,
                icon = Icons.Filled.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                imeAction = ImeAction.Done
            )

            state.errorRes?.let { ErrorText(messageRes = it) }

            PrimaryButton(
                text = stringResource(R.string.btn_iniciar_sesion),
                onClick = viewModel::login,
                isLoading = state.isLoading
            )

            TextButton(onClick = {
                viewModel.clearError()
                onGoToRegister()
            }) {
                Text(stringResource(R.string.ir_a_registro))
            }
        }
    }
}
