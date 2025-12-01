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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.authconsumer.domain.model.AuthKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 認証キー表示用カードコンポーネント
 *
 * Jetpack Composeで実装された再利用可能なUIコンポーネント。
 *
 * 表示内容:
 * - キーID（先頭8文字）
 * - 認証キー本体（モノスペースフォント）
 * - 作成日時
 * - 有効期限
 * - 有効期限バッジ（Valid / Xmin left / EXPIRED）
 *
 * 視覚的フィードバック:
 * - 有効: 通常カラー（surfaceVariant）
 * - 期限切れ: 赤系カラー（errorContainer）
 *
 * @param authKey 表示する認証キー
 * @param modifier 外部から適用するModifier
 */
@Composable
fun AuthKeyItem(
    authKey: AuthKey,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // 期限切れかどうかでカード背景色を変更
    val containerColor = if (authKey.isExpired) {
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
                ExpirationBadge(authKey = authKey)
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
                    text = "Created: ${dateFormat.format(Date(authKey.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Expires: ${dateFormat.format(Date(authKey.expiresAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 有効期限バッジコンポーネント
 *
 * 残り時間に応じて色分けして視覚的に状態を伝える。
 *
 * 色分けルール:
 * - 緑（#4CAF50）: 30分以上 → "Valid"
 * - 黄（#FFC107）: 5〜30分 → "Xmin left"
 * - 橙（#FF9800）: 5分以下 → "Xmin left"（警告）
 * - 赤（error）: 期限切れ → "EXPIRED"
 *
 * @param authKey 判定対象の認証キー
 */
@Composable
private fun ExpirationBadge(authKey: AuthKey) {
    val (text, color) = if (authKey.isExpired) {
        "EXPIRED" to MaterialTheme.colorScheme.error
    } else {
        val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(authKey.remainingTimeMs)
        when {
            remainingMinutes <= 5 -> "${remainingMinutes}min left" to Color(0xFFFF9800)   // 橙: 5分以下
            remainingMinutes <= 30 -> "${remainingMinutes}min left" to Color(0xFFFFC107)  // 黄: 30分以下
            else -> "Valid" to Color(0xFF4CAF50)  // 緑: 30分以上
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
