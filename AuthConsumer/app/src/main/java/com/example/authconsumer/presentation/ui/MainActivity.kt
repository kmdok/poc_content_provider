package com.example.authconsumer.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.authconsumer.data.repository.AuthKeyRepositoryImpl
import com.example.authconsumer.data.source.MockAuthKeyDataSource
import com.example.authconsumer.presentation.ui.theme.AuthConsumerTheme
import com.example.authconsumer.presentation.viewmodel.AuthKeyViewModel
import com.example.authconsumer.presentation.viewmodel.AuthKeyViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AuthKeyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataSource = MockAuthKeyDataSource()
        val repository = AuthKeyRepositoryImpl(dataSource)
        val factory = AuthKeyViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AuthKeyViewModel::class.java]

        setContent {
            AuthConsumerTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
