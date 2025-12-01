package com.example.authprovider.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.authprovider.domain.model.AuthKey
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
    onDelete: (String) -> Unit,
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "ID: ${authKey.id.take(8)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExpired) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = authKey.key,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "作成日時: ${dateFormat.format(Date(authKey.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExpired) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // 残り時間（秒単位でリアルタイム更新）
                val remainingSeconds = remainingMs / 1000
                val (timeText, timeColor) = if (isExpired) {
                    "期限切れ" to MaterialTheme.colorScheme.error
                } else {
                    "残り: ${remainingSeconds}秒" to when {
                        remainingSeconds <= 5 -> Color(0xFFFF5722)  // 赤橙: 5秒以下
                        remainingSeconds <= 10 -> Color(0xFFFF9800) // 橙: 10秒以下
                        remainingSeconds <= 20 -> Color(0xFFFFC107) // 黄: 20秒以下
                        else -> Color(0xFF4CAF50)                    // 緑: 20秒以上
                    }
                }

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = timeColor
                )
            }
            IconButton(onClick = { onDelete(authKey.id) }) {
                Text(text = "X", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
