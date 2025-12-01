package com.example.authprovider.data.repository

import com.example.authprovider.data.source.AuthKeyDataSource
import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.repository.AuthKeyRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthKeyRepositoryの実装クラス
 *
 * リポジトリパターンの実装:
 * - ドメイン層のインターフェース（AuthKeyRepository）を実装
 * - データソースへのアクセスをカプセル化
 * - 将来的に複数のデータソース（ローカル/リモート）を統合可能
 *
 * 現在の構成:
 * UseCase → Repository(この クラス) → DataSource → EncryptedSharedPreferences
 *
 * @param dataSource 認証キーデータソース（DIで注入）
 */
@Singleton
class AuthKeyRepositoryImpl @Inject constructor(
    private val dataSource: AuthKeyDataSource
) : AuthKeyRepository {

    override val authKeys: StateFlow<List<AuthKey>>
        get() = dataSource.authKeys

    override fun generateNewKey(): AuthKey {
        return dataSource.generateNewKey()
    }

    override fun saveKey(authKey: AuthKey) {
        dataSource.saveKey(authKey)
    }

    override fun getCurrentKey(): AuthKey? {
        return dataSource.getCurrentKey()
    }

    override fun getKeyById(id: String): AuthKey? {
        return dataSource.getKeyById(id)
    }

    override fun getAllKeys(): List<AuthKey> {
        return dataSource.getAllKeys()
    }

    override fun deleteKey(id: String): Boolean {
        return dataSource.deleteKey(id)
    }

    override fun deleteExpiredKeys() {
        dataSource.deleteExpiredKeys()
    }

    override fun clearAllKeys() {
        dataSource.clearAllKeys()
    }
}
