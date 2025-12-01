package com.example.authprovider.data.source

import com.example.authprovider.domain.model.AuthKey
import kotlinx.coroutines.flow.StateFlow

interface AuthKeyDataSource {
    val authKeys: StateFlow<List<AuthKey>>
    fun generateNewKey(): AuthKey
    fun saveKey(authKey: AuthKey)
    fun getCurrentKey(): AuthKey?
    fun getKeyById(id: String): AuthKey?
    fun getAllKeys(): List<AuthKey>
    fun deleteKey(id: String): Boolean
    fun deleteExpiredKeys()
    fun clearAllKeys()
}
