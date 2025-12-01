# AuthConsumer

ContentProvider経由でAuthProviderから認証キーを取得・表示するAndroidアプリ。

## アーキテクチャ概要

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AuthConsumer App                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Presentation Layer                               │   │
│  │  ┌──────────────┐    ┌─────────────────────┐    ┌───────────────┐   │   │
│  │  │ MainActivity │───→│  AuthKeyViewModel   │───→│  MainScreen   │   │   │
│  │  │   (Activity) │    │   (@HiltViewModel)  │    │  (Composable) │   │   │
│  │  └──────────────┘    └─────────────────────┘    └───────────────┘   │   │
│  │                               │                                      │   │
│  │                               │ observes StateFlow                   │   │
│  │                               ▼                                      │   │
│  │                      FetchResult<T>                                  │   │
│  │                      ├─ Idle                                         │   │
│  │                      ├─ Loading                                      │   │
│  │                      ├─ Success(data)                                │   │
│  │                      └─ Error(message)                               │   │
│  └───────────────────────────────┼──────────────────────────────────────┘   │
│                                  │ calls (suspend)                          │
│  ┌───────────────────────────────▼──────────────────────────────────────┐   │
│  │                       Domain Layer                                    │   │
│  │  ┌──────────────────────────────────────────────────────────────┐    │   │
│  │  │              AuthKeyRepository <<interface>>                  │    │   │
│  │  │                                                               │    │   │
│  │  │  + fetchResult: StateFlow<FetchResult<List<AuthKey>>>         │    │   │
│  │  │  + currentKeyResult: StateFlow<FetchResult<AuthKey?>>         │    │   │
│  │  │                                                               │    │   │
│  │  │  + suspend fetchAuthKeys()         // 全キー取得              │    │   │
│  │  │  + suspend fetchCurrentValidKey()  // 有効キー取得            │    │   │
│  │  │  + suspend fetchAuthKeyById(id)    // ID指定取得              │    │   │
│  │  └──────────────────────────────────────────────────────────────┘    │   │
│  │                               ▲                                      │   │
│  │                               │ implements                           │   │
│  └───────────────────────────────┼──────────────────────────────────────┘   │
│                                  │                                          │
│  ┌───────────────────────────────┼──────────────────────────────────────┐   │
│  │                       Data Layer                                      │   │
│  │  ┌────────────────────────────┴─────────────────────────────────┐    │   │
│  │  │               AuthKeyRepositoryImpl                           │    │   │
│  │  │                                                               │    │   │
│  │  │  - _fetchResult: MutableStateFlow                             │    │   │
│  │  │  - _currentKeyResult: MutableStateFlow                        │    │   │
│  │  │                                                               │    │   │
│  │  │  状態遷移: Idle → Loading → Success/Error                     │    │   │
│  │  └───────────────────────────────────────────────────────────────┘    │   │
│  │                               │                                      │   │
│  │                               │ uses                                 │   │
│  │                               ▼                                      │   │
│  │  ┌───────────────────────────────────────────────────────────────┐   │   │
│  │  │         AuthKeyRemoteDataSource <<interface>>                  │   │   │
│  │  │                                                                │   │   │
│  │  │  + suspend fetchAuthKeys(): List<AuthKey>                      │   │   │
│  │  │  + suspend fetchCurrentValidKey(): AuthKey?                    │   │   │
│  │  │  + suspend fetchAuthKeyById(id): AuthKey?                      │   │   │
│  │  └───────────────────────────────────────────────────────────────┘   │   │
│  │                               ▲                                      │   │
│  │                               │ implements                           │   │
│  │  ┌────────────────────────────┴──────────────────────────────────┐   │   │
│  │  │           ContentProviderAuthKeyDataSource                     │   │   │
│  │  │                                                                │   │   │
│  │  │  ContentResolver を使用してProviderにアクセス                   │   │   │
│  │  │                                                                │   │   │
│  │  │  URI_AUTHKEYS = "content://com.example.authprovider/authkeys"  │   │   │
│  │  │  URI_CURRENT  = "content://com.example.authprovider/current"   │   │   │
│  │  └────────────────────────────────────────────────────────────────┘   │   │
│  │                               │                                       │   │
│  │                               │ query()                               │   │
│  │                               ▼                                       │   │
│  │  ┌────────────────────────────────────────────────────────────────┐  │   │
│  │  │                    ContentResolver                              │  │   │
│  │  │               (システムコンポーネント)                           │  │   │
│  │  └────────────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ IPC (Binder)
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AuthProvider App                                     │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                   AuthKeyContentProvider                               │ │
│  │                                                                        │ │
│  │  content://com.example.authprovider/authkeys     → getAllKeys()        │ │
│  │  content://com.example.authprovider/authkeys/{id} → getKeyById(id)     │ │
│  │  content://com.example.authprovider/current      → getOrRefresh()      │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
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
AuthKeyRemoteDataSource ◁──────── ContentProviderAuthKeyDataSource
                                         │
                                         │ uses
                                         ▼
                                  ContentResolver (Android System)
```

### 呼び出し関係

```
MainActivity
    │
    │ @Inject (Hilt)
    ▼
AuthKeyViewModel
    │
    │ calls (viewModelScope.launch)
    ▼
AuthKeyRepository (interface)
    │
    │ (実際は AuthKeyRepositoryImpl)
    │
    │ 状態管理: Idle → Loading → Success/Error
    ▼
AuthKeyRemoteDataSource (interface)
    │
    │ (実際は ContentProviderAuthKeyDataSource)
    ▼
ContentResolver.query(uri, ...)
    │
    │ IPC via Binder
    ▼
AuthProvider の AuthKeyContentProvider
```

## ContentProviderへのアクセス方法

### AndroidManifest.xml での宣言

```xml
<!-- Android 11+ で他アプリのContentProviderにアクセスするための宣言 -->
<queries>
    <package android:name="com.example.authprovider" />
</queries>
```

| 要素 | 説明 |
|------|------|
| `<queries>` | Android 11 (API 30) 以降で必須 |
| `<package>` | アクセスしたいアプリのパッケージ名 |

### ContentResolver を使ったアクセス

```kotlin
// URI定義
val URI_AUTHKEYS = Uri.parse("content://com.example.authprovider/authkeys")
val URI_CURRENT = Uri.parse("content://com.example.authprovider/current")

// クエリ実行
contentResolver.query(
    URI_CURRENT,    // 接続先URI
    null,           // projection (列指定、nullは全列)
    null,           // selection (WHERE句)
    null,           // selectionArgs (WHERE引数)
    null            // sortOrder (ORDER BY)
)?.use { cursor ->
    if (cursor.moveToFirst()) {
        // Cursorからデータ取得
        val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        val key = cursor.getString(cursor.getColumnIndexOrThrow("key"))
        // ...
    }
}
```

### アクセスフロー詳細

```
AuthConsumer                  System                    AuthProvider
    │                           │                            │
    │ contentResolver.query()   │                            │
    │──────────────────────────→│                            │
    │                           │                            │
    │                           │ Binder.transact()          │
    │                           │───────────────────────────→│
    │                           │                            │
    │                           │            ┌───────────────┴───────────────┐
    │                           │            │ AuthKeyContentProvider        │
    │                           │            │                               │
    │                           │            │ 1. validateCallingPackage()   │
    │                           │            │    ├─ Binder.getCallingUid()  │
    │                           │            │    ├─ getPackagesForUid(uid)  │
    │                           │            │    └─ isPackageAllowed(pkg)   │
    │                           │            │                               │
    │                           │            │ 2. URIマッチング               │
    │                           │            │    └─ /current → CODE_CURRENT │
    │                           │            │                               │
    │                           │            │ 3. データ取得                  │
    │                           │            │    └─ getOrRefreshAuthKey()   │
    │                           │            │                               │
    │                           │            │ 4. Cursor生成                  │
    │                           │            │    └─ MatrixCursor + addRow() │
    │                           │            └───────────────┬───────────────┘
    │                           │                            │
    │                           │ Cursor (via Parcel)        │
    │                           │←───────────────────────────│
    │                           │                            │
    │ Cursor                    │                            │
    │←──────────────────────────│                            │
    │                           │                            │
    │ cursor.moveToFirst()      │                            │
    │ cursor.getString(...)     │                            │
    │ cursor.close()            │                            │
```

## Hilt DI構成

### DataModule.kt のバインディング

```
@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    // インターフェース → 実装 のバインディング
    @Binds AuthKeyRepository        ← AuthKeyRepositoryImpl
    @Binds AuthKeyRemoteDataSource  ← ContentProviderAuthKeyDataSource

    // インスタンス生成
    @Provides ContentResolver       → context.contentResolver
}
```

### 依存性注入の流れ

```
                    Hilt SingletonComponent
                            │
            ┌───────────────┼───────────────┐
            │               │               │
            ▼               ▼               ▼
    ContentResolver  AuthKeyRepository  AuthKeyRemoteDataSource
            │               │               │
            │               │               │
            └───────────────┴───────────────┘
                            │
                            │ inject
                            ▼
               ContentProviderAuthKeyDataSource
                            │
                            │ inject
                            ▼
                  AuthKeyRepositoryImpl
                            │
                            │ inject
                            ▼
                    AuthKeyViewModel
                            │
                            │ inject
                            ▼
                       MainActivity
```

## 状態管理 (FetchResult)

```kotlin
sealed class FetchResult<out T> {
    object Idle : FetchResult<Nothing>()      // 初期状態
    object Loading : FetchResult<Nothing>()   // 取得中
    data class Success<T>(val data: T) : FetchResult<T>()  // 成功
    data class Error(val message: String) : FetchResult<Nothing>()  // 失敗
}
```

### 状態遷移

```
    ┌──────────────────────────────────────────┐
    │                                          │
    │         ┌─────────────────────┐          │
    │         │                     │          │
    │         ▼                     │          │
    │      ┌──────┐            ┌────┴────┐     │
    └─────→│ Idle │───────────→│ Loading │     │
           └──────┘    fetch    └────┬────┘     │
                                     │          │
                        ┌────────────┼──────────┘
                        │            │
                        ▼            ▼
                  ┌─────────┐  ┌─────────┐
                  │ Success │  │  Error  │
                  └─────────┘  └─────────┘
```

## ファイル一覧

```
app/src/main/java/com/example/authconsumer/
├── AuthConsumerApplication.kt       # Hiltアプリケーション
├── di/
│   └── DataModule.kt                # DIバインディング定義
├── domain/
│   ├── model/
│   │   ├── AuthKey.kt               # 認証キーモデル
│   │   └── FetchResult.kt           # 取得状態モデル
│   └── repository/
│       └── AuthKeyRepository.kt     # リポジトリIF
├── data/
│   ├── repository/
│   │   └── AuthKeyRepositoryImpl.kt # リポジトリ実装
│   └── source/
│       ├── AuthKeyRemoteDataSource.kt          # データソースIF
│       └── ContentProviderAuthKeyDataSource.kt # ContentProvider接続
└── presentation/
    ├── ui/
    │   ├── MainActivity.kt          # エントリーポイント
    │   └── MainScreen.kt            # メイン画面
    ├── viewmodel/
    │   └── AuthKeyViewModel.kt      # 画面状態管理
    └── components/
        └── AuthKeyItem.kt           # キーカードUI
```

## Provider未インストール時のエラー

```
// ContentResolver.query() が null を返す
// または SecurityException がスロー

try {
    val keys = remoteDataSource.fetchAuthKeys()
    _fetchResult.value = FetchResult.Success(keys)
} catch (e: SecurityException) {
    // 許可されていない場合
    _fetchResult.value = FetchResult.Error("Access denied: ${e.message}")
} catch (e: Exception) {
    // Provider未インストール等
    _fetchResult.value = FetchResult.Error(e.message ?: "Unknown error")
}
```

## ビルド・実行

```bash
cd AuthConsumer
./gradlew installDebug
```

## 動作確認

1. **AuthProvider**をインストール・起動し、キーを生成
2. **AuthConsumer**をインストール・起動
3. 「有効キー取得」をタップ
4. Provider側で生成されたキーが表示される
