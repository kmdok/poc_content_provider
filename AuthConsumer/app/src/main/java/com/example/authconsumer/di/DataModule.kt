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
    abstract fun bindAuthKeyRemoteDataSource(
        impl: ContentProviderAuthKeyDataSource
    ): AuthKeyRemoteDataSource

    companion object {
        @Provides
        @Singleton
        fun provideContentResolver(
            @ApplicationContext context: Context
        ): ContentResolver = context.contentResolver
    }
}
