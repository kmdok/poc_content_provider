package com.example.authconsumer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import com.example.authconsumer.domain.repository.AuthKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthKeyViewModel @Inject constructor(
    private val repository: AuthKeyRepository
) : ViewModel() {

    val fetchResult: StateFlow<FetchResult<List<AuthKey>>> = repository.fetchResult
    val currentKeyResult: StateFlow<FetchResult<AuthKey?>> = repository.currentKeyResult

    fun fetchAuthKeys() {
        viewModelScope.launch {
            repository.fetchAuthKeys()
        }
    }

    fun fetchCurrentValidKey() {
        viewModelScope.launch {
            repository.fetchCurrentValidKey()
        }
    }
}
