package com.example.authprovider.domain.usecase

import com.example.authprovider.domain.model.AuthKey
import java.util.UUID
import javax.inject.Inject

class GenerateAuthKeyUseCase @Inject constructor() {

    operator fun invoke(expirationMs: Long = AuthKey.DEFAULT_EXPIRATION_MS): AuthKey {
        val timestamp = System.currentTimeMillis()
        return AuthKey(
            id = UUID.randomUUID().toString(),
            key = "dummy_${UUID.randomUUID().toString().take(8)}_$timestamp",
            createdAt = timestamp,
            expiresAt = timestamp + expirationMs
        )
    }
}
