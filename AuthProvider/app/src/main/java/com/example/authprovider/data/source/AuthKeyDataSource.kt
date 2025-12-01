package com.example.authprovider.data.source

import com.example.authprovider.domain.model.AuthKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthKeyDataSource @Inject constructor() {

    private val _authKeys = MutableStateFlow<List<AuthKey>>(emptyList())
    val authKeys: StateFlow<List<AuthKey>> = _authKeys.asStateFlow()

    fun generateNewKey(): AuthKey {
        val newKey = AuthKey(
            id = UUID.randomUUID().toString(),
            key = UUID.randomUUID().toString().replace("-", "")
        )
        _authKeys.value = _authKeys.value + newKey
        return newKey
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

    fun clearAllKeys() {
        _authKeys.value = emptyList()
    }
}
