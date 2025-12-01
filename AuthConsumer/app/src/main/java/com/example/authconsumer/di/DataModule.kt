package com.example.authconsumer.di

import com.example.authconsumer.data.repository.AuthKeyRepositoryImpl
import com.example.authconsumer.data.source.AuthKeyRemoteDataSource
import com.example.authconsumer.data.source.MockAuthKeyDataSource
import com.example.authconsumer.domain.repository.AuthKeyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
        impl: MockAuthKeyDataSource
    ): AuthKeyRemoteDataSource
}
