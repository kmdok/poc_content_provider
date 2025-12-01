package com.example.authprovider.domain.usecase

import com.example.authprovider.domain.model.AuthKey
import java.util.UUID
import javax.inject.Inject

/**
 * 新しい認証キーを生成するUseCase
 *
 * このクラスはダミーの認証キーを生成する。
 * 本番環境では、セキュアな認証サーバーからキーを取得する実装に置き換える。
 *
 * 生成されるキーの形式:
 * - ID: UUID（例: "550e8400-e29b-41d4-a716-446655440000"）
 * - Key: "dummy_XXXXXXXX_timestamp"（例: "dummy_a1b2c3d4_1701234567890"）
 */
class GenerateAuthKeyUseCase @Inject constructor() {

    /**
     * 認証キーを生成する
     *
     * @param expirationMs 有効期限（ミリ秒）。デフォルトは1時間
     * @return 生成された認証キー
     */
    operator fun invoke(expirationMs: Long = AuthKey.DEFAULT_EXPIRATION_MS): AuthKey {
        val timestamp = System.currentTimeMillis()
        return AuthKey(
            id = UUID.randomUUID().toString(),
            // ダミー実装: UUID先頭8文字 + タイムスタンプでキーを生成
            key = "dummy_${UUID.randomUUID().toString().take(8)}_$timestamp",
            createdAt = timestamp,
            expiresAt = timestamp + expirationMs
        )
    }
}
