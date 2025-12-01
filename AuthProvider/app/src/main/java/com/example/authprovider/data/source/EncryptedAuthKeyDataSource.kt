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
import androidx.core.content.edit

/**
 * EncryptedSharedPreferencesを使用した認証キーデータソース
 *
 * セキュリティ特性:
 * - キー暗号化: AES256_SIV
 * - 値暗号化: AES256_GCM
 * - マスターキー: Android Keystore で保護
 *
 * データ構造:
 * - JSON形式でキーリストを保存
 * - StateFlowでリアクティブに変更を通知
 *
 * @param sharedPreferences EncryptedSharedPreferencesのインスタンス（DIで注入）
 * @param gson JSONシリアライズ用
 * @param generateAuthKeyUseCase キー生成UseCase
 */
@Singleton
class EncryptedAuthKeyDataSource @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val generateAuthKeyUseCase: GenerateAuthKeyUseCase
) : AuthKeyDataSource {

    /** メモリ上のキーリスト（StateFlowで変更を監視可能） */
    private val _authKeys = MutableStateFlow<List<AuthKey>>(emptyList())
    override val authKeys: StateFlow<List<AuthKey>> = _authKeys.asStateFlow()

    init {
        // インスタンス生成時にストレージからキーを読み込み
        loadKeys()
    }

    /**
     * ストレージからキーリストを読み込み
     * アプリ起動時に呼ばれ、以前保存したキーを復元する
     */
    private fun loadKeys() {
        val json = sharedPreferences.getString(KEY_AUTH_KEYS, null)
        if (json != null) {
            // Gsonでデシリアライズ（List<AuthKey>型を明示）
            val type = object : TypeToken<List<AuthKey>>() {}.type
            val keys: List<AuthKey> = gson.fromJson(json, type)
            _authKeys.value = keys
        }
    }

    /**
     * 全キーをストレージに保存
     * JSON形式でシリアライズしてSharedPreferencesに書き込む
     *
     * @param keys 保存するキーリスト
     */
    private fun saveAllKeys(keys: List<AuthKey>) {
        val json = gson.toJson(keys)
        // apply()で非同期書き込み（UIブロックしない）
        sharedPreferences.edit { putString(KEY_AUTH_KEYS, json) }
        // メモリ上のリストも更新（StateFlow経由で監視者に通知）
        _authKeys.value = keys
    }

    override fun generateNewKey(): AuthKey {
        val newKey = generateAuthKeyUseCase()
        // 既存リストに追加して保存
        val updatedKeys = _authKeys.value + newKey
        saveAllKeys(updatedKeys)
        return newKey
    }

    override fun saveKey(authKey: AuthKey) {
        val updatedKeys = _authKeys.value + authKey
        saveAllKeys(updatedKeys)
    }

    override fun getCurrentKey(): AuthKey? {
        // 期限切れでない最初のキーを返す
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
            return true // 削除成功
        }
        return false // 該当キーなし
    }

    override fun deleteExpiredKeys() {
        // 有効なキーのみ残す
        val validKeys = _authKeys.value.filter { !it.isExpired }
        saveAllKeys(validKeys)
    }

    override fun clearAllKeys() {
        saveAllKeys(emptyList())
    }

    companion object {
        /** SharedPreferences内でキーリストを保存するキー名 */
        private const val KEY_AUTH_KEYS = "auth_keys"
    }
}
