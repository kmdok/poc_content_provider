package com.example.authconsumer.data.source

import com.example.authconsumer.domain.model.AuthKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAuthKeyDataSource @Inject constructor() : AuthKeyRemoteDataSource {

    override suspend fun fetchAuthKeys(): List<AuthKey> {
        return emptyList()
    }

    override suspend fun fetchCurrentValidKey(): AuthKey? {
        return null
    }

    override suspend fun fetchAuthKeyById(id: String): AuthKey? {
        return null
    }
}
