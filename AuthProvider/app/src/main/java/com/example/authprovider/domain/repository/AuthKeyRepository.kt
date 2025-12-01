package com.example.authprovider.domain.repository

import com.example.authprovider.domain.model.AuthKey
import kotlinx.coroutines.flow.StateFlow

interface AuthKeyRepository {
    val authKeys: StateFlow<List<AuthKey>>
    fun generateNewKey(): AuthKey
    fun getKeyById(id: String): AuthKey?
    fun getAllKeys(): List<AuthKey>
    fun deleteKey(id: String): Boolean
    fun clearAllKeys()
}
