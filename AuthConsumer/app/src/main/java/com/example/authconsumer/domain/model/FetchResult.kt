package com.example.authconsumer.domain.model

/**
 * データ取得結果を表すsealed class
 *
 * 非同期処理の状態をUIに伝えるために使用。
 * StateFlowと組み合わせてリアクティブなUI更新を実現。
 *
 * 状態遷移:
 * Idle → Loading → Success / Error
 *       ↑__________________________|
 *       （再取得時）
 *
 * @param T 成功時のデータ型
 */
sealed class FetchResult<out T> {
    /**
     * 取得成功
     * @property data 取得したデータ
     */
    data class Success<T>(val data: T) : FetchResult<T>()

    /**
     * 取得失敗
     * @property message エラーメッセージ（UIに表示）
     */
    data class Error(val message: String) : FetchResult<Nothing>()

    /** 取得中（ローディング表示用） */
    data object Loading : FetchResult<Nothing>()

    /** 初期状態（まだ取得していない） */
    data object Idle : FetchResult<Nothing>()
}
