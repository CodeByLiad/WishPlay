package com.nuvetrix.wishplay.di

import com.nuvetrix.wishplay.data.remote.api.WishPlayApiService
import com.nuvetrix.wishplay.data.remote.api.WishPlayApiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
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
        fun provideCertificatePinner(): CertificatePinner {
            return CertificatePinner.Builder()
                // Google Trust Services Root R1 (Cloud Functions)
                .add("*.cloudfunctions.net", "sha256/hxqRlPTuQEkOL9CrOcxBrTUl6+enRg0EfcqqbxjmqGs=")
                // DigiCert Global Root G2 (Cloud Functions Backup)
                .add("*.cloudfunctions.net", "sha256/i7WTqTvh0OioIruIfFR4kRqhVtUGF1UvvW9FnKUGuOI=")
                // Cloudflare Inc ECC CA-3 (Workers)
                .add("*.nuvetrix.workers.dev", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
                // ISRG Root X1 Backup
                .add("*.nuvetrix.workers.dev", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=")
                .build()
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(pinner: CertificatePinner): OkHttpClient {
            return OkHttpClient.Builder()
                .certificatePinner(pinner)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
