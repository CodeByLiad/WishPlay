package com.jinatra.wishplay

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jinatra.wishplay.data.local.db.WishPlayDatabase
import com.jinatra.wishplay.data.local.entity.GameEntity
import com.jinatra.wishplay.data.local.entity.WishlistItemEntity
import com.jinatra.wishplay.data.remote.api.WishPlayApiServiceImpl
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CatalogRegressionTest {
    @Test fun refreshPreservesWishlistAndAlertSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, WishPlayDatabase::class.java).build()
        try {
            val dao = db.wishlistDao()
            dao.insertGame(GameEntity("test", "Before"))
            dao.insertWishlistItem(WishlistItemEntity("test", alertEnabled = false, orderIndex = 7))
            dao.insertGame(GameEntity("test", "After", platformsJson = "{\"PS5\":null}"))
            assertEquals(1, dao.getWishlistCount())
            assertEquals(false, dao.getWishlistItem("test")?.alertEnabled)
            assertEquals(7, dao.getWishlistItem("test")?.orderIndex)
            assertEquals("After", dao.getGameById("test")?.game?.title)
        } finally { db.close() }
    }

    @Test fun platformDatesAndMissingImagesAreMappedWithoutInventingData() {
        val api = WishPlayApiServiceImpl(OkHttpClient())
        val dto = api.mapRawgGameToDto(JSONObject("""
            {"id":12,"name":"Ports","released":"2020-01-01","background_image":null,
             "platforms":[
               {"platform":{"slug":"pc","name":"PC"},"released_at":"2020-01-01"},
               {"platform":{"slug":"android","name":"Android"},"released_at":"2027-03-01"},
               {"platform":{"slug":"playstation5","name":"PlayStation 5"},"released_at":null},
               {"platform":{"slug":"playstation4","name":"PlayStation 4"},"released_at":null}
             ]}
        """))
        assertEquals("2020-01-01", dto.platforms["PC"])
        assertEquals("2027-03-01", dto.platforms["Android"])
        assertTrue(dto.platforms.containsKey("PS5"))
        assertNull(dto.platforms["PS5"])
        assertTrue(dto.platforms.containsKey("PS4"))
        assertNull(dto.coverUrl)
        assertNull(dto.price)
        assertNull(dto.expectedYear)
        assertNull(dto.igdbId)
    }
}
