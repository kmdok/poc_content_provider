package com.example.authconsumer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.authconsumer.domain.model.AuthKey
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 認証キー表示用カードコンポーネント
 *
 * 残り時間はリアルタイム（1秒ごと）で更新される。
 */
@Composable
fun AuthKeyItem(
    authKey: AuthKey,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // 残り時間をリアルタイムで更新するための状態
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 1秒ごとに現在時刻を更新
    LaunchedEffect(authKey.id) {
        while (true) {
            delay(1000L)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    // 現在の残り時間を計算
    val remainingMs = (authKey.expiresAt - currentTimeMs).coerceAtLeast(0)
    val isExpired = currentTimeMs > authKey.expiresAt

    // 期限切れかどうかでカード背景色を変更
    val containerColor = if (isExpired) {
        MaterialTheme.colorScheme.errorContainer  // 赤系（警告）
    } else {
        MaterialTheme.colorScheme.surfaceVariant  // 通常
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 上段: ID と有効期限バッジ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${authKey.id.take(8)}...",  // UUIDは長いので先頭8文字のみ表示
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 残り時間バッジ（秒単位でリアルタイム更新）
                ExpirationBadge(remainingMs = remainingMs, isExpired = isExpired)
            }

            // 中段: 認証キー本体
            Text(
                text = authKey.key,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace  // 等幅フォントで可読性向上
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis  // 長い場合は「...」で省略
            )

            // 下段: 作成日時と有効期限
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "作成: ${dateFormat.format(Date(authKey.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "期限: ${dateFormat.format(Date(authKey.expiresAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 有効期限バッジコンポーネント（秒単位でリアルタイム更新）
 *
 * 残り時間に応じて色分けして視覚的に状態を伝える。
 *
 * 色分けルール:
 * - 緑（#4CAF50）: 20秒以上
 * - 黄（#FFC107）: 10〜20秒
 * - 橙（#FF9800）: 5〜10秒
 * - 赤橙（#FF5722）: 5秒以下
 * - 赤（error）: 期限切れ
 */
@Composable
private fun ExpirationBadge(remainingMs: Long, isExpired: Boolean) {
    val remainingSeconds = remainingMs / 1000

    val (text, color) = if (isExpired) {
        "期限切れ" to MaterialTheme.colorScheme.error
    } else {
        "残り: ${remainingSeconds}秒" to when {
            remainingSeconds <= 5 -> Color(0xFFFF5722)   // 赤橙: 5秒以下
            remainingSeconds <= 10 -> Color(0xFFFF9800)  // 橙: 10秒以下
            remainingSeconds <= 20 -> Color(0xFFFFC107)  // 黄: 20秒以下
            else -> Color(0xFF4CAF50)                    // 緑: 20秒以上
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold  // 太字で目立たせる
        ),
        color = color
    )
}
