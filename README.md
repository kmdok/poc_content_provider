# Android ContentProvider PoC

ContentProviderを使用したアプリ間認証キー共有のPoC（Proof of Concept）プロジェクト。

## このプロジェクトは何？

**2つの別々のAndroidアプリ間で、認証キーを安全に共有するデモ**です。

```
┌─────────────────┐                    ┌─────────────────┐
│  AuthProvider   │  ← ContentProvider → │  AuthConsumer   │
│  （キーを作る側）│      経由でアクセス   │  （キーを使う側）│
└─────────────────┘                    └─────────────────┘
```

### 実際のユースケース例

- メインアプリで発行した認証トークンを、サブアプリで利用
- シングルサインオン（SSO）の簡易実装
- アプリ間でのセッション共有

---

## 画面の操作方法

### Auth Provider（青いアイコンのアプリ）

認証キーを**生成・管理**するアプリです。

```
┌──────────────────────────────────────┐
│         Auth Provider                │ ← アプリ名
├──────────────────────────────────────┤
│  [Generate Key]    [Clear All]       │ ← ボタン
├──────────────────────────────────────┤
│  Generated Keys (1)                  │ ← 生成済みキーの数
│  ┌────────────────────────────────┐  │
│  │ ID: 09a57305...             ✕  │  │ ← キーID（UUIDの先頭8文字）
│  │ dummy_4e481108_1764580876364   │  │ ← 認証キー本体
│  │ Created: 2025-12-01 18:21:16   │  │ ← 作成日時
│  │ Expires in: 00:56:33           │  │ ← 残り有効時間
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

#### ボタンの説明

| ボタン | 動作 | 用途 |
|--------|------|------|
| **Generate Key** | 新しい認証キーを生成 | キーが必要なときに押す |
| **Clear All** | 全てのキーを削除 | リセットしたいときに押す |
| **✕**（カード右上） | そのキーだけ削除 | 特定のキーを消したいとき |

#### カードの色の意味

| 色 | 状態 | 意味 |
|----|------|------|
| グレー | 有効 | このキーは使える |
| 赤系 | 期限切れ | このキーはもう使えない |

#### 残り時間の色分け

| 色 | 残り時間 | 緊急度 |
|----|----------|--------|
| 緑 | 30分以上 | 余裕あり |
| 黄 | 5〜30分 | そろそろ更新 |
| 橙 | 5分以下 | まもなく期限切れ |
| 赤 | 期限切れ | 使用不可 |

---

### Auth Consumer（緑のアイコンのアプリ）

認証キーを**取得・表示**するアプリです。

```
┌──────────────────────────────────────┐
│         Auth Consumer                │ ← アプリ名
├──────────────────────────────────────┤
│  [Get Current Key]  [Fetch All Keys] │ ← ボタン
├──────────────────────────────────────┤
│  Current Valid Key                   │ ← 現在有効なキー
│  ┌────────────────────────────────┐  │
│  │ ID: 09a57305...        Valid   │  │
│  │ dummy_4e481108_1764580876364   │  │
│  │ Created: ...    Expires: ...   │  │
│  └────────────────────────────────┘  │
├──────────────────────────────────────┤
│  All Keys                            │ ← 全キー一覧
│  Found 2 key(s)                      │
│  ┌────────────────────────────────┐  │
│  │ ... キーカード ...              │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

#### ボタンの説明

| ボタン | 動作 | いつ使う？ |
|--------|------|-----------|
| **Get Current Key** | 有効なキーを1つ取得 | 普段使いはこれ！期限切れなら自動で新規発行 |
| **Fetch All Keys** | 全キーを一覧取得 | 履歴確認・デバッグ用 |

#### 表示状態の意味

| 表示 | 意味 |
|------|------|
| `Tap 'Get Current Key' to fetch` | まだキーを取得していない（初期状態） |
| プログレス（ぐるぐる） | 取得中... |
| キーカード | 取得成功！ |
| `Error: ...` | エラー発生（後述のトラブルシューティング参照） |

---

## 実際に動かしてみよう

### Step 1: Auth Provider でキーを作成

1. **Auth Provider** アプリを起動
2. **「Generate Key」** ボタンをタップ
3. キーが生成され、カードに表示される

```
✓ ID: 09a57305...
  dummy_4e481108_1764580876364
  Created: 2025-12-01 18:21:16
  Expires in: 00:59:45
```

### Step 2: Auth Consumer でキーを取得

1. **Auth Consumer** アプリを起動
2. **「Get Current Key」** ボタンをタップ
3. Provider で作成したキーが表示される！

```
✓ Current Valid Key
  ID: 09a57305...    Valid
  dummy_4e481108_1764580876364
```

### Step 3: 動作を確認

両方のアプリで **同じキー** が表示されていれば成功！

```
Provider側: dummy_4e481108_1764580876364
Consumer側: dummy_4e481108_1764580876364  ← 一致！
```

---

## 仕組みの解説

### なぜ別アプリ間でデータ共有できるの？

Androidの **ContentProvider** という仕組みを使っています。

```
┌──────────────┐      query()       ┌──────────────────┐
│ AuthConsumer │ ─────────────────→ │ ContentProvider  │
│              │                    │ (AuthProvider内) │
│              │ ←───────────────── │                  │
└──────────────┘   Cursor（結果）   └──────────────────┘
                                            │
                                            ↓
                                    ┌──────────────────┐
                                    │ 暗号化ストレージ │
                                    │ (SharedPrefs)    │
                                    └──────────────────┘
```

### セキュリティはどうなってる？

1. **暗号化保存**: キーはAES256-GCMで暗号化されて保存
2. **アクセス制御**: 許可されたアプリ（パッケージ名）のみアクセス可能
3. **呼び出し元検証**: `Binder.getCallingUid()` でアプリを識別

```kotlin
// 許可されていないアプリからのアクセス
Consumer (unknown.app) → Provider
                       → SecurityException: Access denied
```

---

## エラーが出たときは

### よくあるエラーと対処法

| エラーメッセージ | 原因 | 対処法 |
|------------------|------|--------|
| `Access denied for package` | アクセス権限がない | Provider側で許可設定を確認 |
| `No valid key available` | キーが存在しない | Provider側で「Generate Key」を押す |
| `Provider not found` | Providerアプリ未インストール | AuthProviderをインストール |
| `EXPIRED` 表示 | キーの有効期限切れ | 「Get Current Key」で自動更新 |

### デバッグ方法

```bash
# ログを確認
adb logcat | grep -E "(AuthProvider|AuthConsumer)"

# インストール確認
adb shell pm list packages | grep example
```

---

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

1. **AuthProvider**を起動 → 「Generate Key」でキー生成
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

- デフォルト: 30秒（`DEFAULT_EXPIRATION_MS = 30000`）
- `/current`エンドポイントは期限切れ時に自動再生成
- Consumer UIで残り時間を色分け表示（緑→黄→橙→赤）

## ライセンス

MIT License
