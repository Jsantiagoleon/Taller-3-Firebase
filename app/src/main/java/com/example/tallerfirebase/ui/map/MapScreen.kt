package com.example.tallerfirebase.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tallerfirebase.R
import com.example.tallerfirebase.ui.components.PrimaryButton

// Pantalla temporal: en la Fase 3 se reemplaza por el mapa
@Composable
fun MapScreen(onLogout: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.sesion_iniciada),
                style = MaterialTheme.typography.headlineSmall
            )
            PrimaryButton(
                text = stringResource(R.string.menu_cerrar_sesion),
                onClick = onLogout
            )
        }
    }
}
