package com.example.authconsumer.domain.repository

import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import kotlinx.coroutines.flow.StateFlow

interface AuthKeyRepository {
    val fetchResult: StateFlow<FetchResult<List<AuthKey>>>
    fun fetchAuthKeys()
    fun fetchAuthKeyById(id: String): AuthKey?
}
