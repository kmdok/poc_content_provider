package com.example.authprovider.domain.model

data class AuthKey(
    val id: String,
    val key: String,
    val createdAt: Long = System.currentTimeMillis()
)
