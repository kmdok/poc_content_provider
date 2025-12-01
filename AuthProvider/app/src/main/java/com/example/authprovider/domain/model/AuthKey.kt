package com.example.authprovider.domain.model

data class AuthKey(
    val id: String,
    val key: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + DEFAULT_EXPIRATION_MS
) {
    companion object {
        const val DEFAULT_EXPIRATION_MS = 60 * 60 * 1000L  // 1時間
    }

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    val remainingTimeMs: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}
