package com.example.authprovider.domain.model

/**
 * 認証キーを表すドメインモデル
 *
 * このクラスはアプリ間で共有される認証キーの情報を保持する。
 * ContentProviderを通じてConsumerアプリに提供される。
 *
 * @property id キーの一意識別子（UUID形式）
 * @property key 実際の認証キー文字列（ダミー実装では "dummy_XXXXXXXX_timestamp" 形式）
 * @property createdAt キー生成時刻（Unix timestamp ミリ秒）
 * @property expiresAt キー有効期限（Unix timestamp ミリ秒）
 */
data class AuthKey(
    val id: String,
    val key: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + DEFAULT_EXPIRATION_MS
) {
    companion object {
        /** デフォルトの有効期限: 1時間（ミリ秒） */
        const val DEFAULT_EXPIRATION_MS = 60 * 60 * 1000L
    }

    /**
     * キーが有効期限切れかどうかを判定
     * @return true: 期限切れ、false: 有効
     */
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    /**
     * 有効期限までの残り時間（ミリ秒）
     * 期限切れの場合は0を返す
     */
    val remainingTimeMs: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}
