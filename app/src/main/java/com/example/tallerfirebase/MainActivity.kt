package com.example.tallerfirebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tallerfirebase.ui.navigation.AppNavigation
import com.example.tallerfirebase.ui.theme.TallerFirebaseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TallerFirebaseTheme {
                AppNavigation()
            }
        }
    }
}
