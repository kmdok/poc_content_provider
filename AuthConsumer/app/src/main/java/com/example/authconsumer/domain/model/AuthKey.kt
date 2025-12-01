package com.example.authconsumer.domain.model

data class AuthKey(
    val id: String,
    val key: String,
    val createdAt: Long,
    val expiresAt: Long
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    val remainingTimeMs: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}
