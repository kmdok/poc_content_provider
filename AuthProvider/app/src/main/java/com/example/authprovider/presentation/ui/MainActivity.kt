package com.example.authprovider.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.authprovider.data.repository.AuthKeyRepositoryImpl
import com.example.authprovider.data.source.AuthKeyDataSource
import com.example.authprovider.presentation.ui.theme.AuthProviderTheme
import com.example.authprovider.presentation.viewmodel.AuthKeyViewModel
import com.example.authprovider.presentation.viewmodel.AuthKeyViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AuthKeyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataSource = AuthKeyDataSource()
        val repository = AuthKeyRepositoryImpl(dataSource)
        val factory = AuthKeyViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AuthKeyViewModel::class.java]

        setContent {
            AuthProviderTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
