package com.example.authprovider.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.repository.AuthKeyRepository
import kotlinx.coroutines.flow.StateFlow

class AuthKeyViewModel(
    private val repository: AuthKeyRepository
) : ViewModel() {

    val authKeys: StateFlow<List<AuthKey>> = repository.authKeys

    fun generateNewKey() {
        repository.generateNewKey()
    }

    fun deleteKey(id: String) {
        repository.deleteKey(id)
    }

    fun clearAllKeys() {
        repository.clearAllKeys()
    }
}
