package com.example.authconsumer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import com.example.authconsumer.domain.repository.AuthKeyRepository
import kotlinx.coroutines.flow.StateFlow

class AuthKeyViewModel(
    private val repository: AuthKeyRepository
) : ViewModel() {

    val fetchResult: StateFlow<FetchResult<List<AuthKey>>> = repository.fetchResult

    fun fetchAuthKeys() {
        repository.fetchAuthKeys()
    }
}
