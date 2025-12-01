package com.example.authprovider.data.source

import com.example.authprovider.domain.model.AuthKey
import kotlinx.coroutines.flow.StateFlow

/**
 * 認証キーデータソースのインターフェース
 *
 * データの永続化方法を抽象化する。
 * 現在の実装: EncryptedAuthKeyDataSource（暗号化SharedPreferences）
 *
 * 将来的に以下の実装に置き換え可能:
 * - Room Database
 * - Remote API
 * - KeyStore
 */
interface AuthKeyDataSource {
    /** 全認証キーを監視するためのStateFlow（UIの自動更新用） */
    val authKeys: StateFlow<List<AuthKey>>

    /** 新しいキーを生成して保存 */
    fun generateNewKey(): AuthKey

    /** キーを保存（リストに追加） */
    fun saveKey(authKey: AuthKey)

    /** 現在有効なキーを取得（期限切れでない最初のキー） */
    fun getCurrentKey(): AuthKey?

    /** ID指定でキーを取得 */
    fun getKeyById(id: String): AuthKey?

    /** 全キーのリストを取得 */
    fun getAllKeys(): List<AuthKey>

    /** ID指定でキーを削除。削除成功でtrue */
    fun deleteKey(id: String): Boolean

    /** 期限切れのキーを全て削除（定期クリーンアップ用） */
    fun deleteExpiredKeys()

    /** 全キーを削除（テスト用/リセット用） */
    fun clearAllKeys()
}
