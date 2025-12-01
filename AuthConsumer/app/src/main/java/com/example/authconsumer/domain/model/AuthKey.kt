package com.example.authconsumer.domain.model

/**
 * 認証キーを表すドメインモデル（Consumer側）
 *
 * ContentProviderから取得した認証キー情報を保持する。
 * AuthProvider側のAuthKeyと同じ構造だが、Consumer側で独立して定義。
 *
 * @property id キーの一意識別子（UUID形式）
 * @property key 実際の認証キー文字列
 * @property createdAt キー生成時刻（Unix timestamp ミリ秒）
 * @property expiresAt キー有効期限（Unix timestamp ミリ秒）
 */
data class AuthKey(
    val id: String,
    val key: String,
    val createdAt: Long,
    val expiresAt: Long
) {
    /**
     * キーが有効期限切れかどうかを判定
     * @return true: 期限切れ、false: 有効
     */
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    /**
     * 有効期限までの残り時間（ミリ秒）
     * UIで残り時間を表示する際に使用
     */
    val remainingTimeMs: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}
