package com.example.authprovider.domain.repository

import com.example.authprovider.domain.model.AuthKey
import kotlinx.coroutines.flow.StateFlow

/**
 * 認証キーリポジトリのインターフェース
 *
 * ドメイン層で定義し、データ層で実装する（依存性逆転の原則）。
 * これにより、UseCaseはデータソースの実装詳細を知らずに済む。
 */
interface AuthKeyRepository {
    /** 全認証キーを監視するためのStateFlow */
    val authKeys: StateFlow<List<AuthKey>>

    /** 新しいキーを生成して保存し、そのキーを返す */
    fun generateNewKey(): AuthKey

    /** キーを保存する（リストに追加） */
    fun saveKey(authKey: AuthKey)

    /** 現在有効なキーを取得（期限切れでない最初のキー） */
    fun getCurrentKey(): AuthKey?

    /** ID指定でキーを取得 */
    fun getKeyById(id: String): AuthKey?

    /** 全キーのリストを取得 */
    fun getAllKeys(): List<AuthKey>

    /** ID指定でキーを削除。削除成功でtrue */
    fun deleteKey(id: String): Boolean

    /** 期限切れのキーを全て削除 */
    fun deleteExpiredKeys()

    /** 全キーを削除 */
    fun clearAllKeys()
}
