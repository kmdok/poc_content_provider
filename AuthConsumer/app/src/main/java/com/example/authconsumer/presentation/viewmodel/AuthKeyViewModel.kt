package com.example.authconsumer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import com.example.authconsumer.domain.repository.AuthKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthKeyViewModel @Inject constructor(
    private val repository: AuthKeyRepository
) : ViewModel() {

    val fetchResult: StateFlow<FetchResult<List<AuthKey>>> = repository.fetchResult

    fun fetchAuthKeys() {
        repository.fetchAuthKeys()
    }
}
