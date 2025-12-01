package com.example.authconsumer.data.repository

import com.example.authconsumer.data.source.AuthKeyRemoteDataSource
import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import com.example.authconsumer.domain.repository.AuthKeyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthKeyRepositoryの実装クラス（Consumer側）
 *
 * ContentProviderからのデータ取得とUI向け状態管理を担当。
 *
 * 責務:
 * - DataSourceへのアクセスをカプセル化
 * - 取得状態（Loading/Success/Error）をStateFlowで管理
 * - 例外をFetchResult.Errorに変換してUIに通知
 *
 * エラーハンドリング:
 * - SecurityException: 未許可アプリからのアクセス
 * - その他Exception: ネットワークエラー、パース失敗等
 *
 * @param remoteDataSource 認証キーデータソース（DIで注入）
 */
@Singleton
class AuthKeyRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthKeyRemoteDataSource
) : AuthKeyRepository {

    /** 全キー取得結果の状態 */
    private val _fetchResult = MutableStateFlow<FetchResult<List<AuthKey>>>(FetchResult.Idle)
    override val fetchResult: StateFlow<FetchResult<List<AuthKey>>> = _fetchResult.asStateFlow()

    /** 現在有効キー取得結果の状態 */
    private val _currentKeyResult = MutableStateFlow<FetchResult<AuthKey?>>(FetchResult.Idle)
    override val currentKeyResult: StateFlow<FetchResult<AuthKey?>> = _currentKeyResult.asStateFlow()

    /**
     * 全認証キーを取得
     *
     * 状態遷移: Idle → Loading → Success/Error
     */
    override suspend fun fetchAuthKeys() {
        _fetchResult.value = FetchResult.Loading
        try {
            val keys = remoteDataSource.fetchAuthKeys()
            _fetchResult.value = FetchResult.Success(keys)
        } catch (e: SecurityException) {
            // ContentProviderのアクセス拒否
            _fetchResult.value = FetchResult.Error("Access denied: ${e.message}")
        } catch (e: Exception) {
            // その他のエラー（Provider未インストール等）
            _fetchResult.value = FetchResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * 現在有効な認証キーを取得
     *
     * 状態遷移: Idle → Loading → Success/Error
     */
    override suspend fun fetchCurrentValidKey() {
        _currentKeyResult.value = FetchResult.Loading
        try {
            val key = remoteDataSource.fetchCurrentValidKey()
            _currentKeyResult.value = FetchResult.Success(key)
        } catch (e: SecurityException) {
            _currentKeyResult.value = FetchResult.Error("Access denied: ${e.message}")
        } catch (e: Exception) {
            _currentKeyResult.value = FetchResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * ID指定で認証キーを取得
     *
     * エラー時はnullを返す（状態管理なし）
     */
    override suspend fun fetchAuthKeyById(id: String): AuthKey? {
        return try {
            remoteDataSource.fetchAuthKeyById(id)
        } catch (e: Exception) {
            null
        }
    }
}
