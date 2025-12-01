# CLAUDE.md - プロジェクト固有の指示

このファイルはClaude Codeがこのプロジェクトで作業する際の指示を提供します。

## プロジェクト概要

Android ContentProviderを使用したアプリ間認証キー共有のPoCプロジェクト。
2つのアプリ（AuthProvider / AuthConsumer）で構成される。

## アーキテクチャ

### レイヤー構成（両プロジェクト共通）

```
presentation/  → UI層（Compose, ViewModel）
    ↓
domain/        → ビジネスロジック（UseCase, Repository Interface, Model）
    ↓
data/          → データアクセス（Repository Impl, DataSource）
    ↓
di/            → 依存性注入（Hilt Module）
```

### データフロー

**AuthProvider**:
```
UI → ViewModel → Repository → DataSource → EncryptedSharedPreferences
                                        ↓
                           ContentProvider → Consumer
```

**AuthConsumer**:
```
UI → ViewModel → Repository → DataSource → ContentResolver → Provider
```

## 重要なファイル

### AuthProvider

| ファイル | 役割 |
|---------|------|
| `provider/AuthKeyContentProvider.kt` | ContentProvider本体。アクセス制御ロジック |
| `provider/AuthKeyContract.kt` | URI・列名の定義 |
| `data/source/EncryptedAuthKeyDataSource.kt` | 暗号化ストレージ |
| `data/source/MockAllowedAppsDataSource.kt` | 許可アプリリスト（本番では差し替え） |
| `domain/usecase/GetOrRefreshAuthKeyUseCase.kt` | キー取得・自動更新ロジック |
| `di/DataModule.kt` | Hilt DI設定 |

### AuthConsumer

| ファイル | 役割 |
|---------|------|
| `data/source/ContentProviderAuthKeyDataSource.kt` | ContentProvider接続 |
| `presentation/ui/MainScreen.kt` | メイン画面 |
| `presentation/viewmodel/AuthKeyViewModel.kt` | 状態管理 |

## 技術詳細

### Hilt + ContentProvider

ContentProviderはHiltのコンストラクタインジェクションに非対応。
`@EntryPoint`パターンで依存性を取得:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthKeyContentProviderEntryPoint {
    fun authKeyRepository(): AuthKeyRepository
}

private val entryPoint by lazy {
    EntryPointAccessors.fromApplication(context!!, AuthKeyContentProviderEntryPoint::class.java)
}
```

### 動的アクセス制御

AndroidManifestのpermissionではなく、実行時にパッケージ名で判定:

```kotlin
val callingUid = Binder.getCallingUid()
val packages = context.packageManager.getPackagesForUid(callingUid)
val isAllowed = packages?.any { allowedAppsDataSource.isPackageAllowed(it) } ?: false
```

### EncryptedSharedPreferences

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

EncryptedSharedPreferences.create(
    context,
    "auth_keys_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

## ビルドコマンド

```bash
# AuthProvider
cd AuthProvider && ./gradlew assembleDebug

# AuthConsumer
cd AuthConsumer && ./gradlew assembleDebug

# 両方インストール
cd AuthProvider && ./gradlew installDebug
cd ../AuthConsumer && ./gradlew installDebug
```

## テスト方法

1. 両アプリをエミュレータ/実機にインストール
2. AuthProviderを起動し「Generate New Key」を押す
3. AuthConsumerを起動し「Get Current Key」を押す
4. 認証キーが表示されれば成功

### エラー時の確認ポイント

- `SecurityException`: 許可リストにパッケージ名が含まれているか
- `NullPointerException`: AuthProviderがインストールされているか
- `queries`宣言: Consumer側AndroidManifestに`<queries>`があるか

## コーディング規約

### コメント

- KDoc形式で日本語コメント
- クラス: 概要、責務、使用方法
- メソッド: 処理内容、パラメータ、戻り値
- 重要な処理にはインラインコメント

### 命名規則

- ファイル名: PascalCase（`AuthKeyViewModel.kt`）
- クラス名: PascalCase
- 関数・変数: camelCase
- 定数: SCREAMING_SNAKE_CASE

### Compose

- `@Composable`関数はPascalCase
- Modifierは最後のパラメータ
- Previewを用意（必要に応じて）

## 拡張ポイント

### 本番環境への移行時

1. **許可アプリリスト**: `MockAllowedAppsDataSource` → リモートAPI実装
2. **認証キー生成**: `GenerateAuthKeyUseCase` → 認証サーバー連携
3. **ストレージ**: SharedPreferences → Room/リモートDB
4. **セキュリティ**: 署名検証、証明書ピンニング追加

### 機能追加時

- 新しいエンドポイント: `AuthKeyContract`に定義追加 → ContentProviderで処理
- 新しいデータ: domain/model に追加 → 各層で対応
- UI変更: presentation層のみ変更
