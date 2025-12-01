package com.example.authprovider.data.source

/**
 * 許可アプリリストのデータソースインターフェース
 *
 * ContentProviderへのアクセスを許可するアプリ（パッケージ名）を管理する。
 *
 * 設計意図:
 * - AndroidManifestのpermissionではなく、動的にアクセス制御
 * - 許可リストをリモートサーバーから取得可能にするため抽象化
 *
 * 現在の実装: MockAllowedAppsDataSource（ハードコード）
 * 将来の実装: RemoteAllowedAppsDataSource（APIから取得）
 */
interface AllowedAppsDataSource {
    /**
     * 許可されているパッケージ名のリストを取得
     * @return 許可パッケージ名のリスト
     */
    fun getAllowedPackages(): List<String>

    /**
     * 指定パッケージがアクセス許可されているか判定
     * @param packageName 判定対象のパッケージ名
     * @return true: 許可、false: 拒否
     */
    fun isPackageAllowed(packageName: String): Boolean
}
