package com.example.authconsumer.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import com.example.authconsumer.presentation.components.AuthKeyItem
import com.example.authconsumer.presentation.viewmodel.AuthKeyViewModel

/**
 * メイン画面のComposable
 *
 * 画面構成:
 * 1. TopAppBar: アプリタイトル
 * 2. ボタン行: 「Get Current Key」「Fetch All Keys」
 * 3. CurrentKeySection: 現在有効なキーの表示
 * 4. 区切り線
 * 5. AllKeysSection: 全キー一覧
 *
 * 状態管理:
 * - ViewModelのStateFlowをcollectAsStateで監視
 * - 状態変化時に自動的に再コンポーズ（リアクティブUI）
 *
 * @param viewModel 認証キー画面のViewModel
 * @param modifier 外部から適用するModifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AuthKeyViewModel,
    modifier: Modifier = Modifier
) {
    // StateFlowを監視（Compose用に変換）
    val fetchResult by viewModel.fetchResult.collectAsState()
    val currentKeyResult by viewModel.currentKeyResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auth Consumer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // アクションボタン行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 主要アクション: 有効なキーを取得（期限切れなら自動更新）
                Button(
                    onClick = { viewModel.fetchCurrentValidKey() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Get Current Key")
                }
                // 補助アクション: 全キー一覧を取得
                OutlinedButton(
                    onClick = { viewModel.fetchAuthKeys() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Fetch All Keys")
                }
            }

            // 現在有効キーセクション
            CurrentKeySection(currentKeyResult = currentKeyResult)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 全キー一覧セクション
            AllKeysSection(fetchResult = fetchResult)
        }
    }
}

/**
 * 現在有効キー表示セクション
 *
 * ContentProviderの /current エンドポイントから取得した
 * 有効なキーを表示する。
 *
 * 表示状態:
 * - Idle: 操作ガイド表示
 * - Loading: プログレスインジケータ
 * - Success: キーカード表示 or 「キーなし」メッセージ
 * - Error: エラーメッセージ（赤字）
 *
 * @param currentKeyResult 現在キー取得結果
 */
@Composable
private fun CurrentKeySection(currentKeyResult: FetchResult<AuthKey?>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Current Valid Key",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // FetchResultの状態に応じて表示を切り替え（when式）
        when (currentKeyResult) {
            is FetchResult.Idle -> {
                // 初期状態: 操作ガイド
                Text(
                    text = "Tap 'Get Current Key' to fetch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is FetchResult.Loading -> {
                // 取得中: インライン進捗表示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp)
                    )
                    Text(
                        text = "Fetching current key...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            is FetchResult.Success -> {
                // 成功: キー表示 or 「なし」メッセージ
                val key = currentKeyResult.data
                if (key != null) {
                    AuthKeyItem(authKey = key)
                } else {
                    Text(
                        text = "No valid key available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is FetchResult.Error -> {
                // エラー: 赤字でメッセージ表示
                Text(
                    text = "Error: ${currentKeyResult.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 全キー一覧表示セクション
 *
 * ContentProviderの /authkeys エンドポイントから取得した
 * 全キー（期限切れ含む）をリスト表示する。
 *
 * LazyColumnで効率的なリスト表示:
 * - 画面に表示されている項目のみレンダリング
 * - key=it.idでアイテム識別（再利用効率化）
 *
 * @param fetchResult 全キー取得結果
 */
@Composable
private fun AllKeysSection(fetchResult: FetchResult<List<AuthKey>>) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "All Keys",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (fetchResult) {
            is FetchResult.Idle -> {
                // 初期状態: 中央に操作ガイド
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap 'Fetch All Keys' to load all keys",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is FetchResult.Loading -> {
                // 取得中: 中央にプログレス
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is FetchResult.Success -> {
                if (fetchResult.data.isEmpty()) {
                    // 空の場合
                    Text(
                        text = "No keys available from provider",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // キー一覧表示
                    Text(
                        text = "Found ${fetchResult.data.size} key(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    // LazyColumn: スクロール可能なリスト（RecyclerView相当）
                    LazyColumn {
                        items(
                            items = fetchResult.data,
                            key = { it.id }  // 一意キーでアイテム識別
                        ) { authKey ->
                            AuthKeyItem(authKey = authKey)
                        }
                    }
                }
            }

            is FetchResult.Error -> {
                // エラー: 中央に赤字表示
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${fetchResult.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
