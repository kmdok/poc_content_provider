package com.example.authprovider.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.authprovider.data.repository.AuthKeyRepositoryImpl
import com.example.authprovider.data.source.AllowedAppsDataSource
import com.example.authprovider.data.source.AuthKeyDataSource
import com.example.authprovider.data.source.EncryptedAuthKeyDataSource
import com.example.authprovider.data.source.MockAllowedAppsDataSource
import com.example.authprovider.domain.repository.AuthKeyRepository
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DIモジュール（データ層）
 *
 * 依存性注入の設定を定義する。
 * インターフェースと実装のバインディング、インスタンス生成のファクトリを提供。
 *
 * DIグラフの構成:
 * - AuthKeyRepository ← AuthKeyRepositoryImpl
 * - AuthKeyDataSource ← EncryptedAuthKeyDataSource
 * - AllowedAppsDataSource ← MockAllowedAppsDataSource
 * - SharedPreferences ← EncryptedSharedPreferences
 * - Gson ← Gsonインスタンス
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
     * AuthKeyDataSourceのバインディング
     * 暗号化SharedPreferencesを使用する実装にバインド
     */
    @Binds
    @Singleton
    abstract fun bindAuthKeyDataSource(
        impl: EncryptedAuthKeyDataSource
    ): AuthKeyDataSource

    /**
     * AllowedAppsDataSourceのバインディング
     * 本番ではRemote実装に置き換える
     */
    @Binds
    @Singleton
    abstract fun bindAllowedAppsDataSource(
        impl: MockAllowedAppsDataSource
    ): AllowedAppsDataSource

    companion object {
        /**
         * Gsonインスタンスを提供
         * JSON <-> オブジェクト変換用
         */
        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()

        /**
         * EncryptedSharedPreferencesインスタンスを提供
         *
         * セキュリティ設定:
         * - MasterKey: AES256_GCM（Android Keystoreで保護）
         * - キー暗号化: AES256_SIV
         * - 値暗号化: AES256_GCM
         *
         * ファイル名: "auth_keys_prefs"
         * 保存場所: /data/data/{package}/shared_prefs/auth_keys_prefs.xml
         * （ただし暗号化されているため直接読めない）
         */
        @Provides
        @Singleton
        fun provideEncryptedSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences {
            // マスターキーを作成（Android Keystoreで保護）
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // 暗号化SharedPreferencesを作成
            return EncryptedSharedPreferences.create(
                context,
                "auth_keys_prefs",  // ファイル名
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,  // キー暗号化方式
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM // 値暗号化方式
            )
        }
    }
}
