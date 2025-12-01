package com.example.authconsumer.data.repository

import com.example.authconsumer.data.source.AuthKeyRemoteDataSource
import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import com.example.authconsumer.domain.repository.AuthKeyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthKeyRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthKeyRemoteDataSource
) : AuthKeyRepository {

    private val _fetchResult = MutableStateFlow<FetchResult<List<AuthKey>>>(FetchResult.Idle)
    override val fetchResult: StateFlow<FetchResult<List<AuthKey>>> = _fetchResult.asStateFlow()

    private val _currentKeyResult = MutableStateFlow<FetchResult<AuthKey?>>(FetchResult.Idle)
    override val currentKeyResult: StateFlow<FetchResult<AuthKey?>> = _currentKeyResult.asStateFlow()

    override suspend fun fetchAuthKeys() {
        _fetchResult.value = FetchResult.Loading
        try {
            val keys = remoteDataSource.fetchAuthKeys()
            _fetchResult.value = FetchResult.Success(keys)
        } catch (e: SecurityException) {
            _fetchResult.value = FetchResult.Error("Access denied: ${e.message}")
        } catch (e: Exception) {
            _fetchResult.value = FetchResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun fetchCurrentValidKey() {
        _currentKeyResult.value = FetchResult.Loading
        try {
            val key = remoteDataSource.fetchCurrentValidKey()
            _currentKeyResult.value = FetchResult.Success(key)
        } catch (e: SecurityException) {
            _currentKeyResult.value = FetchResult.Error("Access denied: ${e.message}")
        } catch (e: Exception) {
            _currentKeyResult.value = FetchResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun fetchAuthKeyById(id: String): AuthKey? {
        return try {
            remoteDataSource.fetchAuthKeyById(id)
        } catch (e: Exception) {
            null
        }
    }
}
