package com.example.authprovider.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.authprovider.presentation.ui.theme.AuthProviderTheme
import com.example.authprovider.presentation.viewmodel.AuthKeyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AuthProviderTheme {
                val viewModel: AuthKeyViewModel = hiltViewModel()
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
