package com.example.authconsumer.data.source

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import com.example.authconsumer.domain.model.AuthKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ContentProviderを使用した認証キーデータソース
 *
 * AuthProviderアプリのContentProviderに接続し、認証キーを取得する。
 * ContentResolverを使ってプロセス間通信（IPC）を行う。
 *
 * 接続先:
 * - Authority: com.example.authprovider
 * - エンドポイント: /authkeys, /authkeys/{id}, /current
 *
 * セキュリティ:
 * - AuthConsumerが許可リストに含まれている必要あり
 * - 未許可の場合、SecurityExceptionがスロー
 *
 * @param contentResolver システムのContentResolver（DIで注入）
 */
@Singleton
class ContentProviderAuthKeyDataSource @Inject constructor(
    private val contentResolver: ContentResolver
) : AuthKeyRemoteDataSource {

    companion object {
        /** 接続先ContentProviderのAuthority */
        private const val AUTHORITY = "com.example.authprovider"

        /** 全キー取得URI */
        private val URI_AUTHKEYS = Uri.parse("content://$AUTHORITY/authkeys")

        /** 現在有効キー取得URI（期限切れなら自動再生成） */
        private val URI_CURRENT = Uri.parse("content://$AUTHORITY/current")

        // Cursor列名（AuthProviderのAuthKeyContract.Columnsと一致させる）
        private const val COLUMN_ID = "id"
        private const val COLUMN_KEY = "key"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_EXPIRES_AT = "expires_at"
    }

    /**
     * 現在有効な認証キーを取得
     *
     * /current エンドポイントにアクセス。
     * Provider側で期限切れ判定し、必要に応じて新規生成して返す。
     */
    override suspend fun fetchCurrentValidKey(): AuthKey? = withContext(Dispatchers.IO) {
        contentResolver.query(URI_CURRENT, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursorToAuthKey(cursor)
            } else {
                null
            }
        }
    }

    /**
     * 全認証キーを取得
     *
     * /authkeys エンドポイントにアクセス。
     * Provider側に保存されている全キー（期限切れ含む）を取得。
     */
    override suspend fun fetchAuthKeys(): List<AuthKey> = withContext(Dispatchers.IO) {
        contentResolver.query(URI_AUTHKEYS, null, null, null, null)?.use { cursor ->
            val keys = mutableListOf<AuthKey>()
            // Cursorの全行を処理
            while (cursor.moveToNext()) {
                keys.add(cursorToAuthKey(cursor))
            }
            keys
        } ?: emptyList()
    }

    /**
     * ID指定で認証キーを取得
     *
     * /authkeys/{id} エンドポイントにアクセス。
     */
    override suspend fun fetchAuthKeyById(id: String): AuthKey? = withContext(Dispatchers.IO) {
        val uri = Uri.parse("content://$AUTHORITY/authkeys/$id")
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursorToAuthKey(cursor)
            } else {
                null
            }
        }
    }

    /**
     * CursorからAuthKeyオブジェクトに変換
     *
     * @param cursor ContentProviderから返されたCursor
     * @return AuthKeyオブジェクト
     */
    private fun cursorToAuthKey(cursor: Cursor): AuthKey {
        return AuthKey(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
            expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPIRES_AT))
        )
    }
}
