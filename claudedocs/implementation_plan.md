# ContentProvider実装計画

## 概要

AuthProviderアプリが持つ認証キーを、ContentProviderを通じてAuthConsumerアプリから取得できるようにする。

### 技術的課題

1. **動的アクセス制御**: 許可アプリリストがリモートから動的に取得されるため、AndroidManifestでの静的permission定義が不可
2. **セキュア保存**: 認証キーをEncryptedSharedPreferencesで暗号化保存
3. **アプリ間通信**: ContentProviderによるセキュアなデータ共有
4. **DI**: Hiltによる依存性注入
5. **有効期限管理**: 認証キーの有効期限チェックと自動再生成

### 認証キー仕様

- **生成**: ダミー実装（UUID + タイムスタンプベース）
- **有効期限**: 生成から一定時間（デフォルト: 1時間）
- **再認証**: 期限切れ時に自動再生成

---

## Phase 0: 両プロジェクト - Hilt導入

### 0.1 プロジェクトレベル設定

**ファイル**: `AuthProvider/build.gradle.kts` & `AuthConsumer/build.gradle.kts`

```kotlin
plugins {
    // 追加
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
```

**ファイル**: `gradle/libs.versions.toml` (両プロジェクト)

```toml
[versions]
hilt = "2.51.1"
ksp = "2.0.21-1.0.27"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

[plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 0.2 アプリレベル設定

**ファイル**: `*/app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}
```

### 0.3 Applicationクラス

**新規ファイル**: `AuthProvider/.../AuthProviderApplication.kt`

```kotlin
@HiltAndroidApp
class AuthProviderApplication : Application()
```

**新規ファイル**: `AuthConsumer/.../AuthConsumerApplication.kt`

```kotlin
@HiltAndroidApp
class AuthConsumerApplication : Application()
```

### 0.4 MainActivity更新

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // ViewModelはhiltViewModel()で取得
}
```

---

## Phase 1: AuthProvider - ドメインモデル拡張

### 1.1 AuthKeyモデル更新

**更新ファイル**: `AuthProvider/.../domain/model/AuthKey.kt`

```kotlin
data class AuthKey(
    val id: String,
    val key: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + DEFAULT_EXPIRATION_MS
) {
    companion object {
        const val DEFAULT_EXPIRATION_MS = 60 * 60 * 1000L  // 1時間
    }

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    val remainingTimeMs: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}
```

### 1.2 ダミーキー生成ロジック

**新規ファイル**: `AuthProvider/.../domain/usecase/GenerateAuthKeyUseCase.kt`

```kotlin
class GenerateAuthKeyUseCase @Inject constructor() {
    operator fun invoke(expirationMs: Long = AuthKey.DEFAULT_EXPIRATION_MS): AuthKey {
        val timestamp = System.currentTimeMillis()
        return AuthKey(
            id = UUID.randomUUID().toString(),
            key = "dummy_${UUID.randomUUID().toString().take(8)}_$timestamp",
            createdAt = timestamp,
            expiresAt = timestamp + expirationMs
        )
    }
}
```

### 1.3 有効期限チェック＆再生成UseCase

**新規ファイル**: `AuthProvider/.../domain/usecase/GetOrRefreshAuthKeyUseCase.kt`

```kotlin
class GetOrRefreshAuthKeyUseCase @Inject constructor(
    private val repository: AuthKeyRepository,
    private val generateAuthKey: GenerateAuthKeyUseCase
) {
    /**
     * 有効なキーを取得。期限切れの場合は再生成して返す。
     */
    operator fun invoke(): AuthKey {
        val currentKey = repository.getCurrentKey()

        return if (currentKey == null || currentKey.isExpired) {
            val newKey = generateAuthKey()
            repository.saveKey(newKey)
            newKey
        } else {
            currentKey
        }
    }
}
```

---

## Phase 2: AuthProvider - EncryptedSharedPreferences導入

### 2.1 依存関係追加

**ファイル**: `AuthProvider/app/build.gradle.kts`

```kotlin
dependencies {
    implementation(libs.androidx.security.crypto)
    implementation(libs.gson)
}
```

**ファイル**: `AuthProvider/gradle/libs.versions.toml`

```toml
[libraries]
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version = "1.1.0-alpha06" }
gson = { group = "com.google.code.gson", name = "gson", version = "2.10.1" }
```

### 2.2 データソースインターフェース

**更新ファイル**: `AuthProvider/.../data/source/AuthKeyDataSource.kt`

```kotlin
interface AuthKeyDataSource {
    val authKeys: StateFlow<List<AuthKey>>
    fun saveKey(authKey: AuthKey)
    fun getCurrentKey(): AuthKey?
    fun getKeyById(id: String): AuthKey?
    fun getAllKeys(): List<AuthKey>
    fun deleteKey(id: String): Boolean
    fun deleteExpiredKeys()
    fun clearAllKeys()
}
```

### 2.3 暗号化ストレージ実装

**新規ファイル**: `AuthProvider/.../data/source/EncryptedAuthKeyDataSource.kt`

```kotlin
class EncryptedAuthKeyDataSource @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : AuthKeyDataSource {

    private val _authKeys = MutableStateFlow<List<AuthKey>>(emptyList())
    override val authKeys: StateFlow<List<AuthKey>> = _authKeys.asStateFlow()

    init {
        loadKeys()
    }

    override fun getCurrentKey(): AuthKey? {
        return getAllKeys().firstOrNull { !it.isExpired }
    }

    override fun deleteExpiredKeys() {
        val validKeys = getAllKeys().filter { !it.isExpired }
        saveAllKeys(validKeys)
    }

    // ... その他の実装
}
```

### 2.4 DIモジュール

**新規ファイル**: `AuthProvider/.../di/DataModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "auth_keys_prefs",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideAuthKeyDataSource(
        sharedPreferences: SharedPreferences,
        gson: Gson
    ): AuthKeyDataSource = EncryptedAuthKeyDataSource(sharedPreferences, gson)

    @Provides
    @Singleton
    fun provideAllowedAppsDataSource(): AllowedAppsDataSource = MockAllowedAppsDataSource()

    @Provides
    @Singleton
    fun provideAuthKeyRepository(
        dataSource: AuthKeyDataSource
    ): AuthKeyRepository = AuthKeyRepositoryImpl(dataSource)
}
```

---

## Phase 3: AuthProvider - ContentProvider実装

### 3.1 許可アプリ管理

**新規ファイル**: `AuthProvider/.../data/source/AllowedAppsDataSource.kt`

```kotlin
interface AllowedAppsDataSource {
    suspend fun getAllowedPackages(): List<String>
    fun isPackageAllowed(packageName: String): Boolean
}
```

**新規ファイル**: `AuthProvider/.../data/source/MockAllowedAppsDataSource.kt`

```kotlin
class MockAllowedAppsDataSource : AllowedAppsDataSource {
    private val allowedPackages = listOf("com.example.authconsumer")

    override suspend fun getAllowedPackages() = allowedPackages
    override fun isPackageAllowed(packageName: String) = packageName in allowedPackages
}
```

### 3.2 Contract定義

**新規ファイル**: `AuthProvider/.../provider/AuthKeyContract.kt`

```kotlin
object AuthKeyContract {
    const val AUTHORITY = "com.example.authprovider"
    const val PATH_AUTHKEYS = "authkeys"
    const val PATH_CURRENT = "current"  // 有効なキーを取得（期限切れなら再生成）

    object Columns {
        const val ID = "id"
        const val KEY = "key"
        const val CREATED_AT = "created_at"
        const val EXPIRES_AT = "expires_at"
        const val IS_EXPIRED = "is_expired"
    }
}
```

#### URI設計

| URI | 操作 | 説明 |
|-----|------|------|
| `content://com.example.authprovider/authkeys` | query | 全キー取得 |
| `content://com.example.authprovider/authkeys/{id}` | query | 特定キー取得 |
| `content://com.example.authprovider/current` | query | **有効なキーを取得（期限切れなら再生成）** |

### 3.3 ContentProvider本体

**新規ファイル**: `AuthProvider/.../provider/AuthKeyContentProvider.kt`

```kotlin
class AuthKeyContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthKeyContentProviderEntryPoint {
        fun authKeyRepository(): AuthKeyRepository
        fun allowedAppsDataSource(): AllowedAppsDataSource
        fun getOrRefreshAuthKeyUseCase(): GetOrRefreshAuthKeyUseCase
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context!!.applicationContext,
            AuthKeyContentProviderEntryPoint::class.java
        )
    }

    override fun query(...): Cursor? {
        // 1. アクセス元パッケージ検証
        validateCallingPackage()

        // 2. URIに応じた処理
        return when (uriMatcher.match(uri)) {
            CODE_CURRENT -> {
                // 有効なキーを取得（期限切れなら再生成）
                val key = entryPoint.getOrRefreshAuthKeyUseCase()()
                createCursor(listOf(key))
            }
            CODE_AUTHKEYS -> {
                val keys = entryPoint.authKeyRepository().getAllKeys()
                createCursor(keys)
            }
            // ...
        }
    }

    private fun validateCallingPackage() {
        val callingUid = Binder.getCallingUid()
        val packages = context!!.packageManager.getPackagesForUid(callingUid)
        val isAllowed = packages?.any {
            entryPoint.allowedAppsDataSource().isPackageAllowed(it)
        } ?: false

        if (!isAllowed) {
            throw SecurityException("Package not allowed to access auth keys")
        }
    }
}
```

### 3.4 AndroidManifest更新

**ファイル**: `AuthProvider/app/src/main/AndroidManifest.xml`

```xml
<application
    android:name=".AuthProviderApplication"
    ...>

    <provider
        android:name=".provider.AuthKeyContentProvider"
        android:authorities="com.example.authprovider"
        android:exported="true"
        android:grantUriPermissions="false" />
</application>
```

---

## Phase 4: AuthConsumer - ContentProvider接続

### 4.1 AuthKeyモデル（Consumer側）

**更新ファイル**: `AuthConsumer/.../domain/model/AuthKey.kt`

```kotlin
data class AuthKey(
    val id: String,
    val key: String,
    val createdAt: Long,
    val expiresAt: Long
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt

    val remainingTimeMs: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}
```

### 4.2 DIモジュール

**新規ファイル**: `AuthConsumer/.../di/DataModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideContentResolver(
        @ApplicationContext context: Context
    ): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    fun provideAuthKeyRemoteDataSource(
        contentResolver: ContentResolver
    ): AuthKeyRemoteDataSource = ContentProviderAuthKeyDataSource(contentResolver)

    @Provides
    @Singleton
    fun provideAuthKeyRepository(
        remoteDataSource: AuthKeyRemoteDataSource
    ): AuthKeyRepository = AuthKeyRepositoryImpl(remoteDataSource)
}
```

### 4.3 ContentProvider DataSource

**更新ファイル**: `AuthConsumer/.../data/source/AuthKeyRemoteDataSource.kt`

```kotlin
interface AuthKeyRemoteDataSource {
    suspend fun fetchAuthKeys(): List<AuthKey>
    suspend fun fetchCurrentValidKey(): AuthKey?  // 有効なキー取得（再認証含む）
}
```

**新規ファイル**: `AuthConsumer/.../data/source/ContentProviderAuthKeyDataSource.kt`

```kotlin
class ContentProviderAuthKeyDataSource @Inject constructor(
    private val contentResolver: ContentResolver
) : AuthKeyRemoteDataSource {

    override suspend fun fetchCurrentValidKey(): AuthKey? = withContext(Dispatchers.IO) {
        // content://com.example.authprovider/current にアクセス
        // → Provider側で期限切れチェック＆再生成
        val uri = Uri.parse("content://com.example.authprovider/current")
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursorToAuthKey(cursor)
            } else null
        }
    }

    override suspend fun fetchAuthKeys(): List<AuthKey> = withContext(Dispatchers.IO) {
        val uri = Uri.parse("content://com.example.authprovider/authkeys")
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val keys = mutableListOf<AuthKey>()
            while (cursor.moveToNext()) {
                keys.add(cursorToAuthKey(cursor))
            }
            keys
        } ?: emptyList()
    }

    private fun cursorToAuthKey(cursor: Cursor): AuthKey {
        return AuthKey(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            key = cursor.getString(cursor.getColumnIndexOrThrow("key")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow("expires_at"))
        )
    }
}
```

### 4.4 ViewModel更新

**更新ファイル**: `AuthConsumer/.../presentation/viewmodel/AuthKeyViewModel.kt`

```kotlin
@HiltViewModel
class AuthKeyViewModel @Inject constructor(
    private val repository: AuthKeyRepository
) : ViewModel() {

    private val _fetchResult = MutableStateFlow<FetchResult<AuthKey?>>(FetchResult.Idle)
    val fetchResult: StateFlow<FetchResult<AuthKey?>> = _fetchResult.asStateFlow()

    /**
     * 有効な認証キーを取得（期限切れなら自動再認証）
     */
    fun fetchValidAuthKey() {
        viewModelScope.launch {
            _fetchResult.value = FetchResult.Loading
            try {
                val key = repository.fetchCurrentValidKey()
                _fetchResult.value = FetchResult.Success(key)
            } catch (e: SecurityException) {
                _fetchResult.value = FetchResult.Error("Access denied: ${e.message}")
            } catch (e: Exception) {
                _fetchResult.value = FetchResult.Error("Failed to fetch: ${e.message}")
            }
        }
    }
}
```

### 4.5 AndroidManifest更新

**ファイル**: `AuthConsumer/app/src/main/AndroidManifest.xml`

```xml
<manifest>
    <!-- Android 11+ でContentProviderにアクセスするために必要 -->
    <queries>
        <package android:name="com.example.authprovider" />
    </queries>

    <application
        android:name=".AuthConsumerApplication"
        ...>
    </application>
</manifest>
```

---

## Phase 5: 追加考慮事項

### 5.1 認証フロー図

```text
Consumer                         Provider
   |                                |
   |-- query(current) ------------->|
   |                                |-- getCurrentKey()
   |                                |-- isExpired?
   |                                |   ├─ Yes: generateNewKey() → save
   |                                |   └─ No: return existing
   |<-- Cursor(valid key) ----------|
   |                                |
```

### 5.2 将来のリモートAPI連携（スコープ外）

```text
AllowedAppsDataSource
├── MockAllowedAppsDataSource (現在)
└── RemoteAllowedAppsDataSource (将来)
    └── Retrofit/Ktor でAPIから取得
    └── ローカルキャッシュ戦略
```

### 5.3 エラーハンドリング

| ケース | AuthProvider側 | AuthConsumer側 |
|--------|----------------|----------------|
| 未許可アプリ | SecurityException | FetchResult.Error("Access denied") |
| Provider未インストール | - | FetchResult.Error("Provider not found") |
| キー期限切れ | 自動再生成 | 新しいキーを受信 |
| データなし | 新規生成 | FetchResult.Success(newKey) |

### 5.4 セキュリティ考慮

- EncryptedSharedPreferencesはAES256-GCMで暗号化
- ContentProviderはUID検証による動的アクセス制御
- exported=trueだが、コード内で明示的にパッケージ検証
- 期限切れキーは定期的に削除

---

## 実装順序

```text
Phase 0: Hilt導入
├── [両方] libs.versions.toml にHilt/KSP追加
├── [両方] build.gradle.kts にplugins追加
├── [両方] Applicationクラス作成 (@HiltAndroidApp)
└── [両方] MainActivity更新 (@AndroidEntryPoint)

Phase 1: AuthProvider - ドメインモデル拡張
├── AuthKey に expiresAt, isExpired 追加
├── GenerateAuthKeyUseCase 実装（ダミー生成）
└── GetOrRefreshAuthKeyUseCase 実装（期限切れ再生成）

Phase 2: AuthProvider - EncryptedSharedPreferences
├── libs.versions.toml に security-crypto/gson追加
├── EncryptedAuthKeyDataSource 実装
├── DataModule でDI設定
└── 既存インメモリDataSourceを置き換え

Phase 3: AuthProvider - ContentProvider
├── AllowedAppsDataSource インターフェース
├── MockAllowedAppsDataSource 実装
├── AuthKeyContract 定義（/current URI追加）
├── AuthKeyContentProvider 実装
└── AndroidManifest 登録

Phase 4: AuthConsumer - ContentProvider接続
├── AuthKey に expiresAt 追加
├── DataModule でDI設定
├── ContentProviderAuthKeyDataSource 実装
├── fetchCurrentValidKey() メソッド追加
├── AndroidManifest に queries追加
└── Mock実装を差し替え
```

---

## ファイル構成（実装後）

### AuthProvider

```text
app/src/main/java/com/example/authprovider/
├── AuthProviderApplication.kt (新規)
├── data/
│   ├── repository/
│   │   └── AuthKeyRepositoryImpl.kt (更新)
│   └── source/
│       ├── AuthKeyDataSource.kt (インターフェース化)
│       ├── EncryptedAuthKeyDataSource.kt (新規)
│       ├── AllowedAppsDataSource.kt (新規)
│       └── MockAllowedAppsDataSource.kt (新規)
├── di/
│   └── DataModule.kt (新規)
├── domain/
│   ├── model/
│   │   └── AuthKey.kt (更新: expiresAt追加)
│   ├── repository/
│   │   └── AuthKeyRepository.kt (更新)
│   └── usecase/
│       ├── GenerateAuthKeyUseCase.kt (新規)
│       └── GetOrRefreshAuthKeyUseCase.kt (新規)
├── presentation/
│   ├── components/
│   │   └── AuthKeyItem.kt (更新: 有効期限表示)
│   ├── ui/
│   │   ├── MainActivity.kt (更新: @AndroidEntryPoint)
│   │   ├── MainScreen.kt
│   │   └── theme/
│   │       └── Theme.kt
│   └── viewmodel/
│       └── AuthKeyViewModel.kt (更新: @HiltViewModel)
└── provider/
    ├── AuthKeyContract.kt (新規)
    └── AuthKeyContentProvider.kt (新規)
```

### AuthConsumer

```text
app/src/main/java/com/example/authconsumer/
├── AuthConsumerApplication.kt (新規)
├── data/
│   ├── repository/
│   │   └── AuthKeyRepositoryImpl.kt (更新)
│   └── source/
│       ├── AuthKeyRemoteDataSource.kt (更新: fetchCurrentValidKey追加)
│       ├── MockAuthKeyDataSource.kt (開発用に残す)
│       └── ContentProviderAuthKeyDataSource.kt (新規)
├── di/
│   └── DataModule.kt (新規)
├── domain/
│   ├── model/
│   │   ├── AuthKey.kt (更新: expiresAt追加)
│   │   └── FetchResult.kt
│   └── repository/
│       └── AuthKeyRepository.kt (更新)
└── presentation/
    ├── components/
    │   └── AuthKeyItem.kt (更新: 有効期限表示)
    ├── ui/
    │   ├── MainActivity.kt (更新: @AndroidEntryPoint)
    │   ├── MainScreen.kt (更新: 有効キー取得ボタン)
    │   └── theme/
    │       └── Theme.kt
    └── viewmodel/
        └── AuthKeyViewModel.kt (更新: @HiltViewModel, fetchValidAuthKey)
```

---

## 確認ポイント

- [ ] Hiltが正しくセットアップされている
- [ ] AuthProviderで認証キーを生成・保存できる
- [ ] アプリ再起動後もキーが永続化されている
- [ ] 認証キーの有効期限が正しく設定される
- [ ] 期限切れキーでアクセス時、自動再生成される
- [ ] AuthConsumerからContentProvider経由でキーを取得できる
- [ ] AuthConsumerで有効期限情報が表示される
- [ ] 許可されていないアプリからのアクセスは拒否される
