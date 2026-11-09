package com.jinatra.wishplay.di

import com.jinatra.wishplay.data.remote.api.WishPlayApiService
import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindWishPlayApiService(
        impl: WishPlayApiServiceImpl
    ): WishPlayApiService

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
