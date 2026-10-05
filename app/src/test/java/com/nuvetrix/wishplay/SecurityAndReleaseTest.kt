package com.nuvetrix.wishplay

import com.nuvetrix.wishplay.data.local.security.AppSecurityManager
import com.nuvetrix.wishplay.data.remote.update.UpdateManifest
import com.nuvetrix.wishplay.data.remote.update.UpdateStatus
import com.nuvetrix.wishplay.di.NetworkModule
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class SecurityAndReleaseTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun testSha256FingerprintComputation() {
        val manager = AppSecurityManager()
        val sampleBytes = "NuvetrixWishPlayReleaseCertificate".toByteArray(Charsets.UTF_8)
        val fingerprint = manager.computeSha256Fingerprint(sampleBytes)

        assertNotNull(fingerprint)
        assertTrue(fingerprint.contains(":"))
        // SHA-256 produces 32 bytes = 32 pairs of hex chars separated by colons (length = 32*3 - 1 = 95)
        assertEquals(95, fingerprint.length)
    }

    @Test
    fun testUpdateManifestParsingAndVersionEvaluation() {
        val rawJson = """
            {
                "versionCode": 5,
                "versionName": "1.2.0",
                "apkUrl": "https://wishplay.app/releases/wishplay-v1.2.0.apk",
                "sha256": "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
                "minSupportedVersionCode": 3,
                "releaseNotes": "Fixes date parsing and adds new calendar themes."
            }
        """.trimIndent()

        val manifest = json.decodeFromString<UpdateManifest>(rawJson)
        assertEquals(5, manifest.versionCode)
        assertEquals("1.2.0", manifest.versionName)
        assertEquals(3, manifest.minSupportedVersionCode)

        // Test normal update available (installed version 4 >= minSupported 3)
        val currentInstalledCode = 4
        val isUpdateAvailable = manifest.versionCode > currentInstalledCode
        val isForced = currentInstalledCode < manifest.minSupportedVersionCode

        assertTrue(isUpdateAvailable)
        assertFalse(isForced)

        // Test critical/forced update (installed version 2 < minSupported 3)
        val oldInstalledCode = 2
        val isOldUpdateAvailable = manifest.versionCode > oldInstalledCode
        val isOldForced = oldInstalledCode < manifest.minSupportedVersionCode

        assertTrue(isOldUpdateAvailable)
        assertTrue(isOldForced)
    }

    @Test
    fun testCertificatePinnerConfiguration() {
        val pinner = NetworkModule.provideCertificatePinner()
        assertNotNull(pinner)
        assertTrue(pinner.pins.isNotEmpty())
        val client = NetworkModule.provideOkHttpClient(pinner)
        assertNotNull(client)
        assertEquals(pinner.pins.size, client.certificatePinner.pins.size)
    }

    @Test
    fun testSha256ChecksumVerification() {
        val testContent = "Test payload for WishPlay APK download verification"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(testContent.toByteArray()).joinToString("") { "%02x".format(it) }

        // Expected SHA-256 hash of testContent
        val expected = "c2373c3036689a548f4fe6fbdd9f9cae867b6db9a18f823d622df2284e2156fb"
        assertEquals(expected, hash)
    }
}
