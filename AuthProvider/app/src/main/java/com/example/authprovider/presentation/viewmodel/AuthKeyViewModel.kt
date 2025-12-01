package com.example.authprovider.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.repository.AuthKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthKeyViewModel @Inject constructor(
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
