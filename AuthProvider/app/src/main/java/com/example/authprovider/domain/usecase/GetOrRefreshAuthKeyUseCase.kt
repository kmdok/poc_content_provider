package com.example.authprovider.domain.usecase

import com.example.authprovider.domain.model.AuthKey
import com.example.authprovider.domain.repository.AuthKeyRepository
import javax.inject.Inject

class GetOrRefreshAuthKeyUseCase @Inject constructor(
    private val repository: AuthKeyRepository,
    private val generateAuthKey: GenerateAuthKeyUseCase
) {
    /**
     * 有効なキーを取得。期限切れまたは存在しない場合は再生成して返す。
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
