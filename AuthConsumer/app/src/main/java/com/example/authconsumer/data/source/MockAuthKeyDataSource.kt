package com.example.authconsumer.data.source

import com.example.authconsumer.domain.model.AuthKey

class MockAuthKeyDataSource : AuthKeyRemoteDataSource {

    override fun fetchAuthKeys(): List<AuthKey> {
        return emptyList()
    }

    override fun fetchAuthKeyById(id: String): AuthKey? {
        return null
    }
}
