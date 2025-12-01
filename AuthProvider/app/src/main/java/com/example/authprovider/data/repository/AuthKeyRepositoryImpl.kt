package com.example.authprovider.data.repository

import com.example.authprovider.data.source.AuthKeyDataSource
import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.repository.AuthKeyRepository
import kotlinx.coroutines.flow.StateFlow

class AuthKeyRepositoryImpl(
    private val dataSource: AuthKeyDataSource
) : AuthKeyRepository {

    override val authKeys: StateFlow<List<AuthKey>>
        get() = dataSource.authKeys

    override fun generateNewKey(): AuthKey {
        return dataSource.generateNewKey()
    }

    override fun getKeyById(id: String): AuthKey? {
        return dataSource.getKeyById(id)
    }

    override fun getAllKeys(): List<AuthKey> {
        return dataSource.getAllKeys()
    }

    override fun deleteKey(id: String): Boolean {
        return dataSource.deleteKey(id)
    }

    override fun clearAllKeys() {
        dataSource.clearAllKeys()
    }
}
