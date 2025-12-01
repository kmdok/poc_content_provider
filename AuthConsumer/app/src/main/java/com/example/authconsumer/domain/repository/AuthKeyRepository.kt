package com.example.authconsumer.domain.repository

import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import kotlinx.coroutines.flow.StateFlow

interface AuthKeyRepository {
    val fetchResult: StateFlow<FetchResult<List<AuthKey>>>
    val currentKeyResult: StateFlow<FetchResult<AuthKey?>>
    suspend fun fetchAuthKeys()
    suspend fun fetchCurrentValidKey()
    suspend fun fetchAuthKeyById(id: String): AuthKey?
}
