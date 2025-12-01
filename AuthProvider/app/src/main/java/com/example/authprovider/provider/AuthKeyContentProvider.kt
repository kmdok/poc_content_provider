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

/**
 * 認証キーを他アプリに提供するContentProvider
 *
 * 機能:
 * - 認証キーをConsumerアプリに提供（読み取り専用）
 * - 動的なアクセス制御（パッケージ名ベース）
 * - 有効期限切れ時の自動再生成
 *
 * エンドポイント:
 * - /authkeys     → 全キー取得（CODE_AUTHKEYS）
 * - /authkeys/{id} → ID指定で取得（CODE_AUTHKEY_ID）
 * - /current      → 有効キー取得、期限切れなら再生成（CODE_CURRENT）
 *
 * セキュリティ:
 * - Binder.getCallingUid()で呼び出し元を特定
 * - 許可リスト（AllowedAppsDataSource）でアクセス制御
 * - 未許可アプリにはSecurityExceptionをスロー
 *
 * Hilt統合:
 * - ContentProviderはHiltのコンストラクタインジェクションに非対応
 * - @EntryPointパターンで依存性を取得
 */
class AuthKeyContentProvider : ContentProvider() {

    /**
     * Hilt EntryPoint定義
     *
     * ContentProviderはライフサイクルが特殊でコンストラクタインジェクション不可。
     * EntryPointを使ってHiltのDIグラフから依存性を取得する。
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthKeyContentProviderEntryPoint {
        fun authKeyRepository(): AuthKeyRepository
        fun allowedAppsDataSource(): AllowedAppsDataSource
        fun getOrRefreshAuthKeyUseCase(): GetOrRefreshAuthKeyUseCase
    }

    /** EntryPointへのアクセサ（遅延初期化） */
    private val entryPoint: AuthKeyContentProviderEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context!!.applicationContext,
            AuthKeyContentProviderEntryPoint::class.java
        )
    }

    companion object {
        // URIマッチング用コード
        private const val CODE_AUTHKEYS = 1    // /authkeys
        private const val CODE_AUTHKEY_ID = 2  // /authkeys/{id}
        private const val CODE_CURRENT = 3     // /current

        /**
         * URIマッチャー: リクエストURIを解析してコードに変換
         */
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AuthKeyContract.AUTHORITY, AuthKeyContract.PATH_AUTHKEYS, CODE_AUTHKEYS)
            addURI(AuthKeyContract.AUTHORITY, "${AuthKeyContract.PATH_AUTHKEYS}/*", CODE_AUTHKEY_ID)
            addURI(AuthKeyContract.AUTHORITY, AuthKeyContract.PATH_CURRENT, CODE_CURRENT)
        }
    }

    override fun onCreate(): Boolean = true

    /**
     * データ取得メソッド（読み取り専用）
     *
     * 処理フロー:
     * 1. 呼び出し元パッケージの検証
     * 2. URIに応じたデータ取得
     * 3. Cursorとして結果を返却
     */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        // セキュリティチェック: 許可されたアプリかどうか検証
        validateCallingPackage()

        return when (uriMatcher.match(uri)) {
            CODE_CURRENT -> {
                // /current: 有効なキーを取得（期限切れなら自動再生成）
                val key = entryPoint.getOrRefreshAuthKeyUseCase()()
                createCursor(listOf(key))
            }
            CODE_AUTHKEYS -> {
                // /authkeys: 全キーを取得
                val keys = entryPoint.authKeyRepository().getAllKeys()
                createCursor(keys)
            }
            CODE_AUTHKEY_ID -> {
                // /authkeys/{id}: ID指定で取得
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

    /**
     * 呼び出し元パッケージの検証
     *
     * 動的アクセス制御の実装:
     * 1. Binder.getCallingUid()で呼び出し元のUIDを取得
     * 2. PackageManagerでUIDからパッケージ名を逆引き
     * 3. AllowedAppsDataSourceで許可リストと照合
     *
     * なぜAndroidManifestのpermissionを使わないのか:
     * - 許可リストをリモートから動的に更新したい
     * - アプリ更新なしで許可アプリを追加/削除したい
     *
     * @throws SecurityException 許可されていないパッケージからのアクセス時
     */
    private fun validateCallingPackage() {
        // 呼び出し元プロセスのUIDを取得
        val callingUid = Binder.getCallingUid()

        // UIDからパッケージ名を取得（1つのUIDに複数パッケージの可能性あり）
        val packages = context!!.packageManager.getPackagesForUid(callingUid)

        // いずれかのパッケージが許可リストに含まれているか確認
        val isAllowed = packages?.any { packageName ->
            entryPoint.allowedAppsDataSource().isPackageAllowed(packageName)
        } ?: false

        if (!isAllowed) {
            val packageList = packages?.joinToString() ?: "unknown"
            throw SecurityException("Package not allowed to access auth keys: $packageList")
        }
    }

    /**
     * AuthKeyリストからCursorを生成
     *
     * @param keys 認証キーリスト
     * @return MatrixCursor（メモリ上のCursor実装）
     */
    private fun createCursor(keys: List<AuthKey>): Cursor {
        // 列定義
        val cursor = MatrixCursor(
            arrayOf(
                AuthKeyContract.Columns.ID,
                AuthKeyContract.Columns.KEY,
                AuthKeyContract.Columns.CREATED_AT,
                AuthKeyContract.Columns.EXPIRES_AT,
                AuthKeyContract.Columns.IS_EXPIRED
            )
        )

        // 各キーを行として追加
        keys.forEach { key ->
            cursor.addRow(
                arrayOf(
                    key.id,
                    key.key,
                    key.createdAt,
                    key.expiresAt,
                    if (key.isExpired) 1 else 0  // Boolean → Int変換
                )
            )
        }

        return cursor
    }

    /**
     * MIMEタイプを返す
     * - dir: 複数行（/authkeys）
     * - item: 単一行（/authkeys/{id}, /current）
     */
    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_AUTHKEYS -> "vnd.android.cursor.dir/vnd.${AuthKeyContract.AUTHORITY}.authkey"
            CODE_AUTHKEY_ID -> "vnd.android.cursor.item/vnd.${AuthKeyContract.AUTHORITY}.authkey"
            CODE_CURRENT -> "vnd.android.cursor.item/vnd.${AuthKeyContract.AUTHORITY}.authkey"
            else -> null
        }
    }

    // 以下のメソッドは読み取り専用のため非サポート
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
