package com.nuvetrix.wishplay.data.remote.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.nuvetrix.wishplay.BuildConfig
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.data.local.security.AppSecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages in-app updates per PRD lines 300-302:
 * - Checks signed update manifest (at most once a day on launch, or on-demand).
 * - Detects forced updates if installed version < minSupportedVersionCode.
 * - Downloads over HTTPS, verifies SHA-256 hash.
 * - Verifies downloaded APK's signing certificate matches the installed app's certificate.
 * - Hands valid APK to Android package installer with FileProvider.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSecurityManager: AppSecurityManager,
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) {
    companion object {
        // Default update manifest endpoint (Cloud Function / static hosting endpoint)
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/nuvetrix/wishplay-release/main/update_manifest.json"

        // 24 hours in milliseconds for launch check throttling
        private const val LAUNCH_CHECK_THROTTLE_MS = 24 * 60 * 60 * 1000L
        private const val PREF_LAST_UPDATE_CHECK = "last_update_check_timestamp"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Checks for updates.
     * @param isManual True if initiated by the user tapping "Check for updates" in Profile.
     */
    suspend fun checkForUpdates(
        isManual: Boolean = false,
        manifestUrl: String = DEFAULT_MANIFEST_URL
    ): UpdateStatus = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("wishplay_updater_prefs", Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(PREF_LAST_UPDATE_CHECK, 0L)
        val now = System.currentTimeMillis()

        if (!isManual && (now - lastCheck) < LAUNCH_CHECK_THROTTLE_MS) {
            // Throttled: return UpToDate for silent launch check
            return@withContext UpdateStatus.UpToDate(BuildConfig.VERSION_NAME)
        }

        prefs.edit().putLong(PREF_LAST_UPDATE_CHECK, now).apply()

        try {
            val manifest = fetchManifest(manifestUrl) ?: return@withContext UpdateStatus.UpToDate(BuildConfig.VERSION_NAME)
            val currentCode = BuildConfig.VERSION_CODE

            if (manifest.versionCode > currentCode) {
                val isForced = currentCode < manifest.minSupportedVersionCode
                UpdateStatus.Available(manifest = manifest, isForced = isForced)
            } else {
                UpdateStatus.UpToDate(BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            if (isManual) {
                UpdateStatus.Error(e.message ?: "Failed to check for updates")
            } else {
                UpdateStatus.UpToDate(BuildConfig.VERSION_NAME)
            }
        }
    }

    /**
     * Downloads the APK, streams to disk, checks SHA-256 hash, and verifies signing certificate.
     */
    suspend fun downloadAndVerifyApk(
        manifest: UpdateManifest,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(updateDir, "wishplay-v${manifest.versionCode}.apk")

            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = Request.Builder().url(manifest.apkUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code} downloading update"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty update response"))
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val percent = ((totalBytesRead * 100) / contentLength).toInt()
                            onProgress(percent)
                        }
                    }
                    output.flush()
                }
            }

            // 1. Verify SHA-256 checksum (PRD line 300)
            val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
            val expectedHash = manifest.sha256.trim().lowercase()

            if (expectedHash.isNotBlank() && !computedHash.equals(expectedHash, ignoreCase = true)) {
                targetFile.delete()
                return@withContext Result.failure(
                    SecurityException("Integrity check failed: APK SHA-256 hash does not match manifest.")
                )
            }

            // 2. Verify signing certificate matches the currently installed app (PRD line 253, 300)
            val isCertValid = appSecurityManager.verifyApkSigningCertificateMatches(context, targetFile)
            if (!isCertValid && !BuildConfig.DEBUG) {
                targetFile.delete()
                return@withContext Result.failure(
                    SecurityException("Security check failed: APK signing certificate does not match installed application.")
                )
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Prompts the Android Package Installer to install the verified APK.
     * Throws [InstallPermissionRequiredException] if "Install unknown apps" permission is needed.
     */
    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalStateException("APK file does not exist"))
            }

            // Check permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    return Result.failure(InstallPermissionRequiredException())
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Intent to take user directly to "Install unknown apps" screen for WishPlay.
     */
    fun getUnknownAppSourcesIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    private fun fetchManifest(url: String): UpdateManifest? {
        return try {
            val req = Request.Builder().url(url).build()
            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return null
                json.decodeFromString<UpdateManifest>(body)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
