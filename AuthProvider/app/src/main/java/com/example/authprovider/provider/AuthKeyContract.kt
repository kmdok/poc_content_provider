package com.example.authprovider.provider

import android.net.Uri

/**
 * ContentProviderのコントラクト（契約）定義
 *
 * Consumerアプリとの通信仕様を定義する。
 * ContentProviderとConsumerの両方で同じ定数を使用し、一貫性を保つ。
 *
 * 利用可能なエンドポイント:
 * - content://com.example.authprovider/authkeys     → 全キー取得
 * - content://com.example.authprovider/authkeys/{id} → ID指定で取得
 * - content://com.example.authprovider/current      → 有効なキー取得（自動更新）
 */
object AuthKeyContract {
    /** ContentProviderのAuthority（AndroidManifestと一致させること） */
    const val AUTHORITY = "com.example.authprovider"

    /** 全キー取得用パス */
    const val PATH_AUTHKEYS = "authkeys"

    /** 現在有効なキー取得用パス（期限切れなら自動再生成） */
    const val PATH_CURRENT = "current"

    /** ベースURI */
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

    /** 全キー取得URI: content://com.example.authprovider/authkeys */
    val CONTENT_URI_AUTHKEYS: Uri = Uri.parse("content://$AUTHORITY/$PATH_AUTHKEYS")

    /** 現在有効キー取得URI: content://com.example.authprovider/current */
    val CONTENT_URI_CURRENT: Uri = Uri.parse("content://$AUTHORITY/$PATH_CURRENT")

    /**
     * Cursor列名の定義
     * ContentProviderが返すCursorの列名を定義
     */
    object Columns {
        /** キーID（UUID文字列） */
        const val ID = "id"

        /** 認証キー本体 */
        const val KEY = "key"

        /** 作成日時（Unix timestamp ミリ秒） */
        const val CREATED_AT = "created_at"

        /** 有効期限（Unix timestamp ミリ秒） */
        const val EXPIRES_AT = "expires_at"

        /** 期限切れフラグ（0: 有効、1: 期限切れ） */
        const val IS_EXPIRED = "is_expired"
    }
}
