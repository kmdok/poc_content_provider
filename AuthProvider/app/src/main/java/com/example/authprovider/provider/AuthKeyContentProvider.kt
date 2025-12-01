package com.example.authprovider.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import com.example.authprovider.data.source.AllowedAppsDataSource
import com.example.authprovider.domain.repository.AuthKeyRepository
import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.usecase.GetOrRefreshAuthKeyUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class AuthKeyContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthKeyContentProviderEntryPoint {
        fun authKeyRepository(): AuthKeyRepository
        fun allowedAppsDataSource(): AllowedAppsDataSource
        fun getOrRefreshAuthKeyUseCase(): GetOrRefreshAuthKeyUseCase
    }

    private val entryPoint: AuthKeyContentProviderEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context!!.applicationContext,
            AuthKeyContentProviderEntryPoint::class.java
        )
    }

    companion object {
        private const val CODE_AUTHKEYS = 1
        private const val CODE_AUTHKEY_ID = 2
        private const val CODE_CURRENT = 3

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AuthKeyContract.AUTHORITY, AuthKeyContract.PATH_AUTHKEYS, CODE_AUTHKEYS)
            addURI(AuthKeyContract.AUTHORITY, "${AuthKeyContract.PATH_AUTHKEYS}/*", CODE_AUTHKEY_ID)
            addURI(AuthKeyContract.AUTHORITY, AuthKeyContract.PATH_CURRENT, CODE_CURRENT)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        validateCallingPackage()

        return when (uriMatcher.match(uri)) {
            CODE_CURRENT -> {
                val key = entryPoint.getOrRefreshAuthKeyUseCase()()
                createCursor(listOf(key))
            }
            CODE_AUTHKEYS -> {
                val keys = entryPoint.authKeyRepository().getAllKeys()
                createCursor(keys)
            }
            CODE_AUTHKEY_ID -> {
                val id = uri.lastPathSegment
                val key = id?.let { entryPoint.authKeyRepository().getKeyById(it) }
                if (key != null) {
                    createCursor(listOf(key))
                } else {
                    createCursor(emptyList())
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun validateCallingPackage() {
        val callingUid = Binder.getCallingUid()
        val packages = context!!.packageManager.getPackagesForUid(callingUid)

        val isAllowed = packages?.any { packageName ->
            entryPoint.allowedAppsDataSource().isPackageAllowed(packageName)
        } ?: false

        if (!isAllowed) {
            val packageList = packages?.joinToString() ?: "unknown"
            throw SecurityException("Package not allowed to access auth keys: $packageList")
        }
    }

    private fun createCursor(keys: List<AuthKey>): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                AuthKeyContract.Columns.ID,
                AuthKeyContract.Columns.KEY,
                AuthKeyContract.Columns.CREATED_AT,
                AuthKeyContract.Columns.EXPIRES_AT,
                AuthKeyContract.Columns.IS_EXPIRED
            )
        )

        keys.forEach { key ->
            cursor.addRow(
                arrayOf(
                    key.id,
                    key.key,
                    key.createdAt,
                    key.expiresAt,
                    if (key.isExpired) 1 else 0
                )
            )
        }

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_AUTHKEYS -> "vnd.android.cursor.dir/vnd.${AuthKeyContract.AUTHORITY}.authkey"
            CODE_AUTHKEY_ID -> "vnd.android.cursor.item/vnd.${AuthKeyContract.AUTHORITY}.authkey"
            CODE_CURRENT -> "vnd.android.cursor.item/vnd.${AuthKeyContract.AUTHORITY}.authkey"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Insert not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Delete not supported")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("Update not supported")
    }
}
