package com.example.authprovider.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.authprovider.data.repository.AuthKeyRepositoryImpl
import com.example.authprovider.data.source.AuthKeyDataSource
import com.example.authprovider.data.source.EncryptedAuthKeyDataSource
import com.example.authprovider.domain.repository.AuthKeyRepository
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthKeyRepository(
        impl: AuthKeyRepositoryImpl
    ): AuthKeyRepository

    @Binds
    @Singleton
    abstract fun bindAuthKeyDataSource(
        impl: EncryptedAuthKeyDataSource
    ): AuthKeyDataSource

    companion object {
        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()

        @Provides
        @Singleton
        fun provideEncryptedSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                "auth_keys_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
