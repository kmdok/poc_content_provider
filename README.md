# Android ContentProvider PoC

ContentProviderを使用したアプリ間認証キー共有のPoC（Proof of Concept）プロジェクト。

## 概要

2つのAndroidアプリ間で認証キーを安全に共有するシステム:

- **AuthProvider**: 認証キーを生成・保存し、ContentProviderで提供
- **AuthConsumer**: ContentProvider経由で認証キーを取得・表示

## 主な特徴

- **暗号化ストレージ**: EncryptedSharedPreferences（AES256-GCM）で認証キーを保護
- **動的アクセス制御**: パッケージ名ベースで許可アプリを管理（AndroidManifest permission不使用）
- **自動キー更新**: 有効期限切れ時に自動で新規キーを生成
- **MVVM + Repository**: クリーンアーキテクチャベースの設計
- **Hilt DI**: 依存性注入による疎結合な実装

## 技術スタック

| カテゴリ | 技術 |
|---------|------|
| 言語 | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.12.01) |
| DI | Hilt 2.51.1 + KSP |
| 暗号化 | security-crypto 1.1.0-alpha06 |
| シリアライズ | Gson 2.10.1 |
| ビルド | AGP 8.7.3, Gradle 8.9 |

## プロジェクト構成

```
android_contents_provider/
├── AuthProvider/          # Provider側アプリ
│   └── app/src/main/java/com/example/authprovider/
│       ├── domain/        # ドメイン層
│       │   ├── model/     # AuthKey
│       │   ├── repository/# AuthKeyRepository (interface)
│       │   └── usecase/   # GenerateAuthKeyUseCase, GetOrRefreshAuthKeyUseCase
│       ├── data/          # データ層
│       │   ├── repository/# AuthKeyRepositoryImpl
│       │   └── source/    # EncryptedAuthKeyDataSource, AllowedAppsDataSource
│       ├── provider/      # ContentProvider
│       │   ├── AuthKeyContract.kt      # URI・列名定義
│       │   └── AuthKeyContentProvider.kt # ContentProvider実装
│       ├── di/            # Hilt Module
│       └── presentation/  # UI (Compose)
│
├── AuthConsumer/          # Consumer側アプリ
│   └── app/src/main/java/com/example/authconsumer/
│       ├── domain/        # ドメイン層
│       │   ├── model/     # AuthKey, FetchResult
│       │   └── repository/# AuthKeyRepository (interface)
│       ├── data/          # データ層
│       │   ├── repository/# AuthKeyRepositoryImpl
│       │   └── source/    # ContentProviderAuthKeyDataSource
│       ├── di/            # Hilt Module
│       └── presentation/  # UI (Compose)
│           ├── viewmodel/ # AuthKeyViewModel
│           ├── components/# AuthKeyItem
│           └── ui/        # MainScreen, MainActivity
│
└── claudedocs/            # 実装計画書等
```

## ContentProvider API

### Authority
```
com.example.authprovider
```

### エンドポイント

| URI | 説明 |
|-----|------|
| `content://com.example.authprovider/authkeys` | 全キー取得 |
| `content://com.example.authprovider/authkeys/{id}` | ID指定で取得 |
| `content://com.example.authprovider/current` | 有効キー取得（期限切れなら自動再生成） |

### Cursor列

| 列名 | 型 | 説明 |
|------|-----|------|
| `id` | String | キーID（UUID） |
| `key` | String | 認証キー本体 |
| `created_at` | Long | 作成日時（Unix timestamp ms） |
| `expires_at` | Long | 有効期限（Unix timestamp ms） |
| `is_expired` | Int | 期限切れフラグ（0/1） |

## セットアップ

### 1. ビルド

```bash
# AuthProviderをビルド・インストール
cd AuthProvider
./gradlew installDebug

# AuthConsumerをビルド・インストール
cd ../AuthConsumer
./gradlew installDebug
```

### 2. 動作確認

1. **AuthProvider**を起動 → 「Generate New Key」でキー生成
2. **AuthConsumer**を起動 → 「Get Current Key」でキー取得

## アクセス制御

### 許可アプリの管理

`MockAllowedAppsDataSource.kt`で許可パッケージを定義:

```kotlin
private val allowedPackages = listOf(
    "com.example.authconsumer"  // 許可するパッケージ名
)
```

### 本番環境への移行

`AllowedAppsDataSource`の実装を差し替えてリモートAPIから許可リストを取得:

```kotlin
class RemoteAllowedAppsDataSource @Inject constructor(
    private val api: AllowedAppsApi
) : AllowedAppsDataSource {
    override fun isPackageAllowed(packageName: String): Boolean {
        return api.getAllowedPackages().contains(packageName)
    }
}
```

## セキュリティ

### 暗号化

- **MasterKey**: Android Keystore (AES256-GCM)
- **キー暗号化**: AES256-SIV
- **値暗号化**: AES256-GCM

### アクセス制御フロー

```
Consumer → ContentProvider.query()
         → Binder.getCallingUid()
         → PackageManager.getPackagesForUid()
         → AllowedAppsDataSource.isPackageAllowed()
         → 許可/SecurityException
```

## 有効期限

- デフォルト: 1時間（`DEFAULT_EXPIRATION_MS = 3600000`）
- `/current`エンドポイントは期限切れ時に自動再生成
- Consumer UIで残り時間を色分け表示（緑→黄→橙→赤）

## ライセンス

MIT License
