package com.example.authconsumer.data.source

import com.example.authconsumer.domain.model.AuthKey

interface AuthKeyRemoteDataSource {
    suspend fun fetchAuthKeys(): List<AuthKey>
    suspend fun fetchCurrentValidKey(): AuthKey?
    suspend fun fetchAuthKeyById(id: String): AuthKey?
}
