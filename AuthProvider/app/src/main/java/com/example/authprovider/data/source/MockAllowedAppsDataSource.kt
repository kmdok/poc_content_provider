package com.example.authprovider.data.source

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 許可アプリリストのモック実装
 *
 * 開発・テスト用にハードコードされた許可リストを提供する。
 *
 * 本番環境では以下の実装に置き換える:
 * - RemoteAllowedAppsDataSource: APIから許可リストを取得
 * - CachedAllowedAppsDataSource: リモート + ローカルキャッシュ
 *
 * 現在許可しているアプリ:
 * - com.example.authconsumer（AuthConsumerアプリ）
 */
@Singleton
class MockAllowedAppsDataSource @Inject constructor() : AllowedAppsDataSource {

    /**
     * 許可パッケージリスト
     * TODO: 本番環境ではリモートAPIから取得する実装に置き換え
     */
    private val allowedPackages = listOf(
        "com.example.authconsumer"  // AuthConsumerアプリを許可
    )

    override fun getAllowedPackages(): List<String> = allowedPackages

    override fun isPackageAllowed(packageName: String): Boolean {
        return packageName in allowedPackages
    }
}
