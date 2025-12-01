package com.example.authprovider.data.source

import android.content.SharedPreferences
import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.usecase.GenerateAuthKeyUseCase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedAuthKeyDataSource @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val generateAuthKeyUseCase: GenerateAuthKeyUseCase
) : AuthKeyDataSource {

    private val _authKeys = MutableStateFlow<List<AuthKey>>(emptyList())
    override val authKeys: StateFlow<List<AuthKey>> = _authKeys.asStateFlow()

    init {
        loadKeys()
    }

    private fun loadKeys() {
        val json = sharedPreferences.getString(KEY_AUTH_KEYS, null)
        if (json != null) {
            val type = object : TypeToken<List<AuthKey>>() {}.type
            val keys: List<AuthKey> = gson.fromJson(json, type)
            _authKeys.value = keys
        }
    }

    private fun saveAllKeys(keys: List<AuthKey>) {
        val json = gson.toJson(keys)
        sharedPreferences.edit().putString(KEY_AUTH_KEYS, json).apply()
        _authKeys.value = keys
    }

    override fun generateNewKey(): AuthKey {
        val newKey = generateAuthKeyUseCase()
        val updatedKeys = _authKeys.value + newKey
        saveAllKeys(updatedKeys)
        return newKey
    }

    override fun saveKey(authKey: AuthKey) {
        val updatedKeys = _authKeys.value + authKey
        saveAllKeys(updatedKeys)
    }

    override fun getCurrentKey(): AuthKey? {
        return _authKeys.value.firstOrNull { !it.isExpired }
    }

    override fun getKeyById(id: String): AuthKey? {
        return _authKeys.value.find { it.id == id }
    }

    override fun getAllKeys(): List<AuthKey> {
        return _authKeys.value
    }

    override fun deleteKey(id: String): Boolean {
        val initialSize = _authKeys.value.size
        val updatedKeys = _authKeys.value.filter { it.id != id }
        if (updatedKeys.size < initialSize) {
            saveAllKeys(updatedKeys)
            return true
        }
        return false
    }

    override fun deleteExpiredKeys() {
        val validKeys = _authKeys.value.filter { !it.isExpired }
        saveAllKeys(validKeys)
    }

    override fun clearAllKeys() {
        saveAllKeys(emptyList())
    }

    companion object {
        private const val KEY_AUTH_KEYS = "auth_keys"
    }
}
