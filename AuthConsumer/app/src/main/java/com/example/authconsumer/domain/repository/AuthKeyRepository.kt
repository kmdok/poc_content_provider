package com.example.authconsumer.domain.repository

import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import kotlinx.coroutines.flow.StateFlow

/**
 * 認証キーリポジトリのインターフェース（Consumer側）
 *
 * ContentProviderからの認証キー取得を抽象化。
 * ViewModelはこのインターフェースを通じてデータにアクセスする。
 *
 * 特徴:
 * - StateFlowで結果を監視可能（リアクティブUI更新）
 * - FetchResultで取得状態（Idle/Loading/Success/Error）を管理
 * - suspend関数で非同期処理（Coroutine対応）
 */
interface AuthKeyRepository {
    /** 全キー取得結果を監視するStateFlow */
    val fetchResult: StateFlow<FetchResult<List<AuthKey>>>

    /** 現在有効キー取得結果を監視するStateFlow */
    val currentKeyResult: StateFlow<FetchResult<AuthKey?>>

    /**
     * 全認証キーを取得
     * ContentProvider URI: content://com.example.authprovider/authkeys
     */
    suspend fun fetchAuthKeys()

    /**
     * 現在有効な認証キーを取得
     * ContentProvider URI: content://com.example.authprovider/current
     * 期限切れの場合、Provider側で自動再生成される
     */
    suspend fun fetchCurrentValidKey()

    /**
     * ID指定で認証キーを取得
     * ContentProvider URI: content://com.example.authprovider/authkeys/{id}
     * @param id キーID
     * @return 認証キー（存在しない場合null）
     */
    suspend fun fetchAuthKeyById(id: String): AuthKey?
}
