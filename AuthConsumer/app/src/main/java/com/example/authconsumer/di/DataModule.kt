package com.example.authconsumer.di

import android.content.ContentResolver
import android.content.Context
import com.example.authconsumer.data.repository.AuthKeyRepositoryImpl
import com.example.authconsumer.data.source.AuthKeyRemoteDataSource
import com.example.authconsumer.data.source.ContentProviderAuthKeyDataSource
import com.example.authconsumer.domain.repository.AuthKeyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DIモジュール（Consumer側）
 *
 * 依存性注入の設定を定義する。
 *
 * DIグラフの構成:
 * - AuthKeyRepository ← AuthKeyRepositoryImpl
 * - AuthKeyRemoteDataSource ← ContentProviderAuthKeyDataSource
 * - ContentResolver ← context.contentResolver
 *
 * スコープ:
 * - 全てSingletonComponent（アプリ全体で1インスタンス）
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     * AuthKeyRepositoryのバインディング
     * インターフェース → 実装クラスの紐付け
     */
    @Binds
    @Singleton
    abstract fun bindAuthKeyRepository(
        impl: AuthKeyRepositoryImpl
    ): AuthKeyRepository

    /**
     * AuthKeyRemoteDataSourceのバインディング
     * ContentProvider経由の実装にバインド
     */
    @Binds
    @Singleton
    abstract fun bindAuthKeyRemoteDataSource(
        impl: ContentProviderAuthKeyDataSource
    ): AuthKeyRemoteDataSource

    companion object {
        /**
         * ContentResolverを提供
         *
         * ContentProviderへのアクセスに必要。
         * システムから取得したContentResolverをDIで配布。
         */
        @Provides
        @Singleton
        fun provideContentResolver(
            @ApplicationContext context: Context
        ): ContentResolver = context.contentResolver
    }
}
