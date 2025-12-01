# Jetpack DataStore への移行検討

## 結論

| 項目 | 回答 |
|------|------|
| DataStoreへの移行 | **可能** |
| 暗号化 | **追加実装が必要**（DataStoreには暗号化機能がない） |
| Consumer側の変更 | **不要**（ContentProvider経由のため影響なし） |

---

## 現状の構成

```
EncryptedAuthKeyDataSource
         │
         │ uses
         ▼
EncryptedSharedPreferences ──→ AES256-GCM で暗号化保存
         │
         ▼
shared_prefs/auth_keys_prefs.xml
```

---

## DataStoreとは

Jetpack DataStoreは、SharedPreferencesの後継として設計されたデータ保存ライブラリ。

### 2種類のDataStore

| 種類 | 用途 | データ形式 |
|------|------|-----------|
| **Preferences DataStore** | Key-Value形式 | 型安全なPreferences |
| **Proto DataStore** | 構造化データ | Protocol Buffers |

### SharedPreferencesとの比較

| 特性 | SharedPreferences | DataStore |
|------|-------------------|-----------|
| 非同期処理 | commit()はブロッキング | 完全非同期（Flow/suspend） |
| 型安全 | なし（Any型） | あり |
| エラーハンドリング | 例外がUI threadでクラッシュ | Flowでエラーを伝播 |
| トランザクション | なし | あり |
| 暗号化 | EncryptedSharedPreferences | **なし（要追加実装）** |

---

## 暗号化DataStoreの実装方法

DataStoreには組み込みの暗号化機能がないため、以下の方法で実装する必要がある。

### 方法1: Google Tink + Proto DataStore（推奨）

Google Tinkは暗号化ライブラリで、DataStoreと組み合わせて使用できる。

```kotlin
// build.gradle.kts
dependencies {
    implementation("androidx.datastore:datastore:1.0.0")
    implementation("com.google.crypto.tink:tink-android:1.7.0")
}
```

```kotlin
// 暗号化Serializer
class EncryptedAuthKeySerializer(
    private val aead: Aead  // Tinkの暗号化インターフェース
) : Serializer<AuthKeyList> {

    override val defaultValue: AuthKeyList = AuthKeyList.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AuthKeyList {
        val encryptedBytes = input.readBytes()
        val decryptedBytes = aead.decrypt(encryptedBytes, null)
        return AuthKeyList.parseFrom(decryptedBytes)
    }

    override suspend fun writeTo(t: AuthKeyList, output: OutputStream) {
        val bytes = t.toByteArray()
        val encryptedBytes = aead.encrypt(bytes, null)
        output.write(encryptedBytes)
    }
}
```

### 方法2: encrypted-datastore ライブラリ

サードパーティの暗号化DataStoreライブラリを使用。

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.nicholaswong:encrypted-datastore:1.0.0")
}
```

```kotlin
val encryptedDataStore = EncryptedPreferencesDataStore(
    context = context,
    fileName = "auth_keys_encrypted"
)
```

### 方法3: 手動暗号化 + Preferences DataStore

値を手動で暗号化してから保存。

```kotlin
class ManualEncryptedDataSource(
    private val dataStore: DataStore<Preferences>,
    private val cipher: Cipher  // 暗号化インスタンス
) {
    suspend fun saveKeys(keys: List<AuthKey>) {
        val json = gson.toJson(keys)
        val encrypted = cipher.encrypt(json.toByteArray())
        val base64 = Base64.encodeToString(encrypted, Base64.DEFAULT)

        dataStore.edit { preferences ->
            preferences[AUTH_KEYS_KEY] = base64
        }
    }
}
```

---

## 推奨: Tink + Proto DataStore

### なぜProto DataStoreか

| 理由 | 説明 |
|------|------|
| 型安全 | Protocol Buffersでスキーマ定義 |
| 効率的 | バイナリ形式で小さい |
| 暗号化と相性◎ | バイト配列として扱いやすい |
| バージョン管理 | スキーマ進化をサポート |

### 実装例

#### 1. Protocol Buffers定義

```protobuf
// auth_key.proto
syntax = "proto3";

option java_package = "com.example.authprovider.data.proto";

message AuthKeyProto {
    string id = 1;
    string key = 2;
    int64 created_at = 3;
    int64 expires_at = 4;
}

message AuthKeyListProto {
    repeated AuthKeyProto keys = 1;
}
```

#### 2. 暗号化DataStore実装

```kotlin
@Singleton
class EncryptedDataStoreAuthKeyDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthKeyDataSource {

    // Tinkの初期化
    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "auth_keyset", "auth_keyset_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://auth_master_key")
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    // 暗号化DataStore
    private val dataStore: DataStore<AuthKeyListProto> = DataStoreFactory.create(
        serializer = EncryptedAuthKeySerializer(aead),
        produceFile = { context.dataStoreFile("auth_keys.pb") }
    )

    // StateFlowに変換
    override val authKeys: StateFlow<List<AuthKey>> = dataStore.data
        .map { proto -> proto.keysList.map { it.toDomain() } }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override suspend fun saveKey(authKey: AuthKey) {
        dataStore.updateData { current ->
            current.toBuilder()
                .addKeys(authKey.toProto())
                .build()
        }
    }

    // ... 他のメソッド
}
```

---

## Consumer側への影響

### 結論: 影響なし

```
┌─────────────────────────────────────────────────────────────────┐
│                        AuthProvider                              │
│                                                                  │
│  ┌──────────────────────┐      ┌────────────────────────────┐   │
│  │ AuthKeyContentProvider│ ←── │ AuthKeyRepository          │   │
│  │                      │      │         │                  │   │
│  │  query() を公開      │      │         ▼                  │   │
│  └──────────┬───────────┘      │ AuthKeyDataSource          │   │
│             │                  │    <<interface>>           │   │
│             │                  │         ▲                  │   │
│             │                  │         │                  │   │
│             │                  │   ┌─────┴─────┐            │   │
│             │                  │   │           │            │   │
│             │          ┌───────┴───┴─┐   ┌────┴────────┐    │   │
│             │          │ Encrypted   │   │ DataStore   │    │   │
│             │          │ SharedPrefs │   │ (新実装)     │    │   │
│             │          └─────────────┘   └─────────────┘    │   │
│             │                  どちらでもOK                   │   │
└─────────────┼───────────────────────────────────────────────────┘
              │
              │ IPC (Binder)
              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AuthConsumer                              │
│                                                                  │
│  ContentResolver.query("content://com.example.authprovider/...") │
│                                                                  │
│  → Cursorでデータ受信（内部実装には依存しない）                    │
└─────────────────────────────────────────────────────────────────┘
```

### なぜ影響がないのか

1. **ContentProviderが抽象化レイヤー**
   - ConsumerはContentProvider経由でアクセス
   - 内部ストレージの実装詳細は隠蔽されている

2. **インターフェースは変わらない**
   - `AuthKeyDataSource` インターフェースは同じ
   - Repository/UseCase/ContentProvider のコードは変更不要

3. **データ形式も同じ**
   - ContentProviderはCursorで返す
   - Cursorの列定義は変わらない

---

## 移行手順

### Phase 1: 準備

```kotlin
// 1. 依存関係追加
// build.gradle.kts
dependencies {
    implementation("androidx.datastore:datastore:1.0.0")
    implementation("com.google.protobuf:protobuf-javalite:3.21.7")
    implementation("com.google.crypto.tink:tink-android:1.7.0")
}

// 2. Protobufプラグイン設定
plugins {
    id("com.google.protobuf") version "0.9.4"
}
```

### Phase 2: 新DataSource実装

```kotlin
// 新しいDataSource実装
@Singleton
class DataStoreAuthKeyDataSource @Inject constructor(
    // ...
) : AuthKeyDataSource {
    // DataStore実装
}
```

### Phase 3: DIモジュール切り替え

```kotlin
// DataModule.kt
@Binds
@Singleton
abstract fun bindAuthKeyDataSource(
    // 旧: impl: EncryptedAuthKeyDataSource
    impl: DataStoreAuthKeyDataSource  // 新
): AuthKeyDataSource
```

### Phase 4: データ移行（オプション）

```kotlin
// 既存データの移行が必要な場合
class MigrationHelper @Inject constructor(
    private val oldDataSource: EncryptedAuthKeyDataSource,
    private val newDataSource: DataStoreAuthKeyDataSource
) {
    suspend fun migrate() {
        val existingKeys = oldDataSource.getAllKeys()
        existingKeys.forEach { key ->
            newDataSource.saveKey(key)
        }
        // 旧データ削除
        oldDataSource.clearAllKeys()
    }
}
```

---

## 比較表

| 観点 | EncryptedSharedPreferences | 暗号化DataStore |
|------|---------------------------|-----------------|
| Google公式 | ✅ | ✅ (DataStore) + Tink |
| 暗号化 | 組み込み | 追加実装 |
| 非同期 | apply()のみ | 完全非同期 |
| 型安全 | ❌ | ✅ (Proto) |
| Coroutine対応 | ❌ | ✅ |
| 複雑さ | 低 | 中〜高 |
| 移行コスト | - | 中 |

---

## 推奨事項

### 現状維持が適切なケース

- データ量が少ない
- 複雑な型が不要
- 既存実装で問題がない

### DataStore移行が適切なケース

- ✅ 認証情報を扱う（セキュリティ重視）
- ✅ 将来的にデータ構造が複雑化する可能性
- ✅ Coroutineベースのアーキテクチャ
- ✅ 型安全性を重視

### このプロジェクトの場合

現状の **EncryptedSharedPreferences で十分** と考えられる理由:

1. データ構造がシンプル（AuthKeyのリスト）
2. 暗号化が組み込みで確実
3. 動作に問題がない
4. PoCプロジェクトとして十分

ただし、本番環境への移行時には **Proto DataStore + Tink** を検討する価値あり。

---

## 参考リンク

- [Jetpack DataStore 公式ドキュメント](https://developer.android.com/topic/libraries/architecture/datastore)
- [Google Tink 暗号化ライブラリ](https://github.com/google/tink)
- [DataStore Codelab](https://developer.android.com/codelabs/android-datastore)
- [Proto DataStore ガイド](https://developer.android.com/topic/libraries/architecture/datastore#proto-datastore)
