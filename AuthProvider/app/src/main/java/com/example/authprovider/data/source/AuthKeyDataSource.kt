package com.example.authprovider.data.source

import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.usecase.GenerateAuthKeyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthKeyDataSource @Inject constructor(
    private val generateAuthKeyUseCase: GenerateAuthKeyUseCase
) {

    private val _authKeys = MutableStateFlow<List<AuthKey>>(emptyList())
    val authKeys: StateFlow<List<AuthKey>> = _authKeys.asStateFlow()

    fun generateNewKey(): AuthKey {
        val newKey = generateAuthKeyUseCase()
        _authKeys.value = _authKeys.value + newKey
        return newKey
    }

    fun saveKey(authKey: AuthKey) {
        _authKeys.value = _authKeys.value + authKey
    }

    fun getCurrentKey(): AuthKey? {
        return _authKeys.value.firstOrNull { !it.isExpired }
    }

    fun getKeyById(id: String): AuthKey? {
        return _authKeys.value.find { it.id == id }
    }

    fun getAllKeys(): List<AuthKey> {
        return _authKeys.value
    }

    fun deleteKey(id: String): Boolean {
        val initialSize = _authKeys.value.size
        _authKeys.value = _authKeys.value.filter { it.id != id }
        return _authKeys.value.size < initialSize
    }

    fun deleteExpiredKeys() {
        _authKeys.value = _authKeys.value.filter { !it.isExpired }
    }

    fun clearAllKeys() {
        _authKeys.value = emptyList()
    }
}
