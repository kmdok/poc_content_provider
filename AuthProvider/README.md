# AuthProvider

認証キーを生成・管理し、ContentProviderで他アプリに提供するAndroidアプリ。

## アーキテクチャ概要

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AuthProvider App                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Presentation Layer                               │   │
│  │  ┌──────────────┐    ┌─────────────────────┐    ┌───────────────┐   │   │
│  │  │ MainActivity │───→│  AuthKeyViewModel   │───→│  MainScreen   │   │   │
│  │  │   (Activity) │    │   (@HiltViewModel)  │    │  (Composable) │   │   │
│  │  └──────────────┘    └─────────────────────┘    └───────────────┘   │   │
│  │                               │                                      │   │
│  └───────────────────────────────┼──────────────────────────────────────┘   │
│                                  │ calls                                    │
│  ┌───────────────────────────────▼──────────────────────────────────────┐   │
│  │                       Domain Layer                                    │   │
│  │  ┌──────────────────────────┐    ┌──────────────────────────────┐    │   │
│  │  │  AuthKeyRepository       │    │ GetOrRefreshAuthKeyUseCase   │    │   │
│  │  │     <<interface>>        │    │                              │    │   │
│  │  │  + authKeys: StateFlow   │    │ + invoke(): AuthKey          │    │   │
│  │  │  + generateNewKey()      │←───│   (有効キー取得/自動再生成)   │    │   │
│  │  │  + getCurrentKey()       │    └──────────────────────────────┘    │   │
│  │  │  + getAllKeys()          │                    │                   │   │
│  │  │  + deleteKey(id)         │    ┌──────────────────────────────┐    │   │
│  │  │  + clearAllKeys()        │    │ GenerateAuthKeyUseCase       │    │   │
│  │  └──────────────────────────┘    │                              │    │   │
│  │               ▲                  │ + invoke(): AuthKey          │    │   │
│  │               │ implements       │   (ダミーキー生成)            │    │   │
│  └───────────────┼──────────────────┴──────────────────────────────────┘   │
│                  │                                                          │
│  ┌───────────────┼──────────────────────────────────────────────────────┐   │
│  │               │           Data Layer                                  │   │
│  │  ┌────────────┴─────────────┐    ┌──────────────────────────────┐    │   │
│  │  │ AuthKeyRepositoryImpl    │    │ AuthKeyDataSource            │    │   │
│  │  │                          │───→│    <<interface>>             │    │   │
│  │  │ implements               │    │ + authKeys: StateFlow        │    │   │
│  │  │ AuthKeyRepository        │    │ + generateNewKey()           │    │   │
│  │  └──────────────────────────┘    │ + getCurrentKey()            │    │   │
│  │                                  │ + getAllKeys()               │    │   │
│  │                                  └──────────────────────────────┘    │   │
│  │                                               ▲                      │   │
│  │                                               │ implements           │   │
│  │                                  ┌────────────┴─────────────────┐    │   │
│  │                                  │EncryptedAuthKeyDataSource    │    │   │
│  │                                  │                              │    │   │
│  │                                  │ (AES256暗号化保存)            │    │   │
│  │                                  └──────────────────────────────┘    │   │
│  │                                               │                      │   │
│  │                                               ▼                      │   │
│  │                                  ┌──────────────────────────────┐    │   │
│  │                                  │ EncryptedSharedPreferences   │    │   │
│  │                                  │ (Android Security Crypto)    │    │   │
│  │                                  └──────────────────────────────┘    │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                     Provider Layer (外部公開)                         │   │
│  │  ┌──────────────────────────────────────────────────────────────┐    │   │
│  │  │              AuthKeyContentProvider                           │    │   │
│  │  │                  extends ContentProvider                      │    │   │
│  │  │                                                               │    │   │
│  │  │  query(uri) ─────┬─→ /authkeys     → getAllKeys()             │    │   │
│  │  │                  ├─→ /authkeys/{id} → getKeyById(id)          │    │   │
│  │  │                  └─→ /current      → getOrRefreshAuthKey()    │    │   │
│  │  │                                                               │    │   │
│  │  │  validateCallingPackage() ───→ AllowedAppsDataSource          │    │   │
│  │  │                                   <<interface>>               │    │   │
│  │  │                                        ▲                      │    │   │
│  │  │                                        │ implements           │    │   │
│  │  │                               MockAllowedAppsDataSource       │    │   │
│  │  │                               (許可パッケージリスト)            │    │   │
│  │  └──────────────────────────────────────────────────────────────┘    │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## クラス関係図

### インターフェースと実装

```
<<interface>>                      <<class>>
AuthKeyRepository    ◁─────────── AuthKeyRepositoryImpl
     │                                   │
     │ uses                              │ uses
     ▼                                   ▼
<<interface>>                      <<class>>
AuthKeyDataSource    ◁─────────── EncryptedAuthKeyDataSource
                                         │
                                         │ uses
                                         ▼
                               EncryptedSharedPreferences


<<interface>>                      <<class>>
AllowedAppsDataSource ◁────────── MockAllowedAppsDataSource
```

### 呼び出し関係

```
MainActivity
    │
    │ @Inject (Hilt)
    ▼
AuthKeyViewModel
    │
    │ calls
    ▼
AuthKeyRepository (interface)
    │
    │ (実際は AuthKeyRepositoryImpl)
    ▼
AuthKeyDataSource (interface)
    │
    │ (実際は EncryptedAuthKeyDataSource)
    ▼
EncryptedSharedPreferences (JSONで保存)
```

## ContentProvider登録方法

### AndroidManifest.xml での宣言

```xml
<provider
    android:name=".provider.AuthKeyContentProvider"
    android:authorities="com.example.authprovider"
    android:exported="true"
    android:grantUriPermissions="false" />
```

| 属性 | 値 | 説明 |
|------|-----|------|
| `name` | `.provider.AuthKeyContentProvider` | ContentProvider実装クラス |
| `authorities` | `com.example.authprovider` | 一意のAuthority（URI識別子） |
| `exported` | `true` | 他アプリからアクセス可能 |
| `grantUriPermissions` | `false` | URI個別権限は使用しない |

### システムへの登録フロー

```
1. アプリインストール時
   └─→ PackageManagerがManifestを解析
       └─→ ContentProviderを登録
           └─→ Authority "com.example.authprovider" を予約

2. アプリ起動時
   └─→ ContentProvider.onCreate() が呼ばれる
       └─→ return true で初期化完了

3. 他アプリからアクセス時
   └─→ ContentResolver.query(uri, ...) を呼び出し
       └─→ システムがAuthorityから該当Providerを検索
           └─→ AuthKeyContentProvider.query() を呼び出し
```

## ContentProviderの仕組み

### エンドポイント設計

```
content://com.example.authprovider/
├── authkeys           全キー取得
├── authkeys/{id}      ID指定で取得
└── current            有効キー取得（自動再生成）
```

### URIマッチング

```kotlin
private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
    addURI("com.example.authprovider", "authkeys", CODE_AUTHKEYS)     // → 1
    addURI("com.example.authprovider", "authkeys/*", CODE_AUTHKEY_ID) // → 2
    addURI("com.example.authprovider", "current", CODE_CURRENT)       // → 3
}
```

### アクセス制御フロー

```
Consumer App                   AuthKeyContentProvider
    │                                    │
    │  query("content://...")            │
    │───────────────────────────────────→│
    │                                    │
    │                          ┌─────────┴─────────┐
    │                          │validateCallingPackage()
    │                          │                   │
    │                          │ Binder.getCallingUid()
    │                          │       │
    │                          │       ▼
    │                          │ PackageManager.getPackagesForUid(uid)
    │                          │       │
    │                          │       ▼
    │                          │ AllowedAppsDataSource.isPackageAllowed(pkg)
    │                          │       │
    │                          │       ├─→ true: 処理続行
    │                          │       └─→ false: SecurityException
    │                          └─────────┬─────────┘
    │                                    │
    │  Cursor (認証キーデータ)           │
    │←───────────────────────────────────│
```

## Hilt DI構成

### DataModule.kt のバインディング

```
@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    // インターフェース → 実装 のバインディング
    @Binds AuthKeyRepository     ← AuthKeyRepositoryImpl
    @Binds AuthKeyDataSource     ← EncryptedAuthKeyDataSource
    @Binds AllowedAppsDataSource ← MockAllowedAppsDataSource

    // インスタンス生成
    @Provides Gson               → Gson()
    @Provides SharedPreferences  → EncryptedSharedPreferences
}
```

### ContentProviderでのHilt利用

ContentProviderはHiltのコンストラクタインジェクションに非対応のため、`@EntryPoint`パターンを使用:

```kotlin
// EntryPoint定義
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthKeyContentProviderEntryPoint {
    fun authKeyRepository(): AuthKeyRepository
    fun allowedAppsDataSource(): AllowedAppsDataSource
    fun getOrRefreshAuthKeyUseCase(): GetOrRefreshAuthKeyUseCase
}

// 依存性取得
private val entryPoint by lazy {
    EntryPointAccessors.fromApplication(
        context!!.applicationContext,
        AuthKeyContentProviderEntryPoint::class.java
    )
}

// 使用
val keys = entryPoint.authKeyRepository().getAllKeys()
```

## ファイル一覧

```
app/src/main/java/com/example/authprovider/
├── AuthProviderApplication.kt      # Hiltアプリケーション
├── di/
│   └── DataModule.kt               # DIバインディング定義
├── domain/
│   ├── model/
│   │   └── AuthKey.kt              # 認証キーモデル
│   ├── repository/
│   │   └── AuthKeyRepository.kt    # リポジトリIF
│   └── usecase/
│       ├── GenerateAuthKeyUseCase.kt    # キー生成
│       └── GetOrRefreshAuthKeyUseCase.kt # 有効キー取得
├── data/
│   ├── repository/
│   │   └── AuthKeyRepositoryImpl.kt     # リポジトリ実装
│   └── source/
│       ├── AuthKeyDataSource.kt          # データソースIF
│       ├── EncryptedAuthKeyDataSource.kt # 暗号化保存
│       ├── AllowedAppsDataSource.kt      # 許可アプリIF
│       └── MockAllowedAppsDataSource.kt  # 許可アプリ実装
├── provider/
│   ├── AuthKeyContract.kt          # URI・列名定義
│   └── AuthKeyContentProvider.kt   # ContentProvider実装
└── presentation/
    ├── ui/
    │   ├── MainActivity.kt         # エントリーポイント
    │   └── MainScreen.kt           # メイン画面
    ├── viewmodel/
    │   └── AuthKeyViewModel.kt     # 画面状態管理
    └── components/
        └── AuthKeyItem.kt          # キーカードUI
```

## ビルド・実行

```bash
cd AuthProvider
./gradlew installDebug
```
