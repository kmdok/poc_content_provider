package com.example.authprovider.domain.usecase

import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.repository.AuthKeyRepository
import javax.inject.Inject

/**
 * 有効な認証キーを取得または再生成するUseCase
 *
 * ContentProviderの /current エンドポイントで使用される。
 * Consumerアプリが常に有効なキーを取得できるようにする。
 *
 * 動作フロー:
 * 1. 現在のキーを取得
 * 2. キーが存在しない or 期限切れ → 新しいキーを生成して保存
 * 3. 有効なキーが存在 → そのまま返す
 */
class GetOrRefreshAuthKeyUseCase @Inject constructor(
    private val repository: AuthKeyRepository,
    private val generateAuthKey: GenerateAuthKeyUseCase
) {
    /**
     * 有効なキーを取得する
     *
     * 期限切れまたは存在しない場合は自動的に再生成して返す。
     * これにより、Consumerは常に有効なキーを取得できる。
     *
     * @return 有効な認証キー（新規生成または既存）
     */
    operator fun invoke(): AuthKey {
        // 現在保存されているキーを取得
        val currentKey = repository.getCurrentKey()

        return if (currentKey == null || currentKey.isExpired) {
            // キーがない or 期限切れ → 新規生成して保存
            val newKey = generateAuthKey()
            repository.saveKey(newKey)
            newKey
        } else {
            // 有効なキーがある → そのまま返す
            currentKey
        }
    }
}
