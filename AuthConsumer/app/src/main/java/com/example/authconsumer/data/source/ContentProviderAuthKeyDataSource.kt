package com.example.authconsumer.data.source

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import com.example.authconsumer.domain.model.AuthKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentProviderAuthKeyDataSource @Inject constructor(
    private val contentResolver: ContentResolver
) : AuthKeyRemoteDataSource {

    companion object {
        private const val AUTHORITY = "com.example.authprovider"
        private val URI_AUTHKEYS = Uri.parse("content://$AUTHORITY/authkeys")
        private val URI_CURRENT = Uri.parse("content://$AUTHORITY/current")

        private const val COLUMN_ID = "id"
        private const val COLUMN_KEY = "key"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_EXPIRES_AT = "expires_at"
    }

    override suspend fun fetchCurrentValidKey(): AuthKey? = withContext(Dispatchers.IO) {
        contentResolver.query(URI_CURRENT, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursorToAuthKey(cursor)
            } else {
                null
            }
        }
    }

    override suspend fun fetchAuthKeys(): List<AuthKey> = withContext(Dispatchers.IO) {
        contentResolver.query(URI_AUTHKEYS, null, null, null, null)?.use { cursor ->
            val keys = mutableListOf<AuthKey>()
            while (cursor.moveToNext()) {
                keys.add(cursorToAuthKey(cursor))
            }
            keys
        } ?: emptyList()
    }

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

    private fun cursorToAuthKey(cursor: Cursor): AuthKey {
        return AuthKey(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
            expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPIRES_AT))
        )
    }
}
