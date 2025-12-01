package com.example.authconsumer.data.source

import com.example.authconsumer.domain.model.AuthKey

/**
 * 認証キーリモートデータソースのインターフェース
 *
 * 外部からの認証キー取得を抽象化。
 * 現在の実装: ContentProviderAuthKeyDataSource
 *
 * 将来的な実装例:
 * - RestApiAuthKeyDataSource（REST API経由）
 * - GrpcAuthKeyDataSource（gRPC経由）
 */
interface AuthKeyRemoteDataSource {
    /**
     * 全認証キーを取得
     * @return 認証キーリスト
     * @throws SecurityException 未許可アプリからのアクセス時
     */
    suspend fun fetchAuthKeys(): List<AuthKey>

    /**
     * 現在有効な認証キーを取得
     * @return 有効な認証キー（存在しない場合null）
     */
    suspend fun fetchCurrentValidKey(): AuthKey?

    /**
     * ID指定で認証キーを取得
     * @param id キーID
     * @return 認証キー（存在しない場合null）
     */
    suspend fun fetchAuthKeyById(id: String): AuthKey?
}
