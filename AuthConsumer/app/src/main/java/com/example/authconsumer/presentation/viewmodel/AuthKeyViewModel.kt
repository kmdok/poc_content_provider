package com.example.authconsumer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import com.example.authconsumer.domain.repository.AuthKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 認証キー画面のViewModel
 *
 * MVVM パターンにおけるViewModelの役割:
 * - UIとビジネスロジック（Repository）の橋渡し
 * - UIの状態（State）を管理
 * - ライフサイクルに応じたデータ保持
 *
 * Hilt統合:
 * - @HiltViewModelでHiltによるインスタンス管理
 * - Repositoryはコンストラクタで注入
 *
 * @param repository 認証キーリポジトリ（DIで注入）
 */
@HiltViewModel
class AuthKeyViewModel @Inject constructor(
    private val repository: AuthKeyRepository
) : ViewModel() {

    /**
     * 全キー取得結果
     * UIはこのStateFlowを監視（collectAsState）してリアクティブに更新
     */
    val fetchResult: StateFlow<FetchResult<List<AuthKey>>> = repository.fetchResult

    /**
     * 現在有効キー取得結果
     * 「Get Current Key」ボタン押下時の結果を保持
     */
    val currentKeyResult: StateFlow<FetchResult<AuthKey?>> = repository.currentKeyResult

    /**
     * 全認証キーを取得
     *
     * ボタン押下時に呼ばれる。
     * viewModelScope内でCoroutineを起動し、Repository経由でContentProviderにアクセス。
     */
    fun fetchAuthKeys() {
        viewModelScope.launch {
            repository.fetchAuthKeys()
        }
    }

    /**
     * 現在有効な認証キーを取得
     *
     * ContentProviderの /current エンドポイントにアクセス。
     * 期限切れの場合、Provider側で自動再生成される。
     */
    fun fetchCurrentValidKey() {
        viewModelScope.launch {
            repository.fetchCurrentValidKey()
        }
    }
}
