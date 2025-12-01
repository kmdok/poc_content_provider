package com.example.authconsumer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.authconsumer.domain.repository.AuthKeyRepository

class AuthKeyViewModelFactory(
    private val repository: AuthKeyRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthKeyViewModel::class.java)) {
            return AuthKeyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
