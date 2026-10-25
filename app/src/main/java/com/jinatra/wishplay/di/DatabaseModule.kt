package com.jinatra.wishplay.di

import android.content.Context
import androidx.room.Room
import com.jinatra.wishplay.data.local.dao.WishlistDao
import com.jinatra.wishplay.data.local.db.WishPlayDatabase
import com.jinatra.wishplay.data.local.prefs.UserPreferences
import com.jinatra.wishplay.data.local.security.PassphraseManager
import com.jinatra.wishplay.data.repository.WishlistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWishPlayDatabase(@ApplicationContext context: Context): WishPlayDatabase {
        val passphraseManager = PassphraseManager(context)
        val passphrase = passphraseManager.getOrCreatePassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            WishPlayDatabase::class.java,
            "wishplay_secure.db"
        )
            .openHelperFactory(factory)
            .addMigrations(com.jinatra.wishplay.data.local.db.MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideWishlistDao(database: WishPlayDatabase): WishlistDao =
        database.wishlistDao()

    @Provides
    fun provideAlertDao(database: WishPlayDatabase): com.jinatra.wishplay.data.local.dao.AlertDao =
        database.alertDao()

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        UserPreferences(context)

    @Provides
    @Singleton
    fun provideWishlistRepository(
        @ApplicationContext context: Context,
        wishlistDao: WishlistDao,
        alertScheduler: com.jinatra.wishplay.alerts.AlertScheduler,
        userPreferences: UserPreferences
    ): WishlistRepository =
        WishlistRepository(wishlistDao, alertScheduler, userPreferences, context)
}
