package com.example.authconsumer.domain.model

sealed class FetchResult<out T> {
    data class Success<T>(val data: T) : FetchResult<T>()
    data class Error(val message: String) : FetchResult<Nothing>()
    data object Loading : FetchResult<Nothing>()
    data object Idle : FetchResult<Nothing>()
}
