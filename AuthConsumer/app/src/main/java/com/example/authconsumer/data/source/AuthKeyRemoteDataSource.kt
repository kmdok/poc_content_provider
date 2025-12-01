package com.example.authconsumer.data.source

import com.example.authconsumer.domain.model.AuthKey

interface AuthKeyRemoteDataSource {
    fun fetchAuthKeys(): List<AuthKey>
    fun fetchAuthKeyById(id: String): AuthKey?
}
