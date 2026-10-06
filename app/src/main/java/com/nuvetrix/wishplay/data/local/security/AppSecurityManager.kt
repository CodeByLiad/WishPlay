package com.nuvetrix.wishplay.data.local.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.nuvetrix.wishplay.BuildConfig
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App hardening and runtime integrity protection matching PRD line 243, 261-263:
 * - Runtime check of the app's own signing certificate digest.
 * - Verification that downloaded update APKs match the installed signing certificate.
 * - Light detection of rooting and hooking tools.
 * - Quiet failure: never crashes or alerts modders; Pro features stay locked while free mode functions.
 */
@Singleton
class AppSecurityManager @Inject constructor() {

    companion object {
        // Jinatra official release signing certificate SHA-256 digest
        const val OFFICIAL_RELEASE_SHA256 = "64:23:4B:91:38:61:94:0A:68:57:9F:50:5B:73:C0:B0:86:14:4E:51:7A:FA:F8:8C:FE:19:D4:4F:92:4B:27:32"

        // Default debug signing certificate SHA-256 digest (Android SDK debug.keystore)
        const val KNOWN_DEBUG_SHA256 = "A4:0D:A8:0A:59:D1:70:CA:A9:50:CF:15:C1:8C:45:4D:47:A3:9B:26:98:9D:8B:64:0E:CD:74:5B:A7:1B:F5:DC"
    }

    /**
     * Returns the SHA-256 fingerprint of the currently running app's signing certificate.
     */
    fun getInstalledSigningCertificateFingerprint(context: Context): String {
        return try {
            val signatures = getPackageSignatures(context.packageManager, context.packageName)
            if (signatures.isNotEmpty()) {
                computeSha256Fingerprint(signatures[0].toByteArray())
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Verifies that the app's active signature matches the legitimate Jinatra release certificate
     * (or debug certificate in debug builds).
     */
    fun isAppSignatureValid(context: Context): Boolean {
        if (BuildConfig.DEBUG) {
            // In debug mode, allow standard debug builds
            return true
        }

        val fingerprint = getInstalledSigningCertificateFingerprint(context)
        if (fingerprint.isBlank()) return false

        // Compare normalized hex string
        val normalizedExpected = OFFICIAL_RELEASE_SHA256.replace(":", "").uppercase()
        val normalizedActual = fingerprint.replace(":", "").uppercase()
        return normalizedActual == normalizedExpected
    }

    /**
     * Verifies that a downloaded APK file has been signed with the EXACT SAME certificate
     * as the currently installed app (PRD line 253: "the in-app updater only installs APKs
     * signed with the same certificate").
     */
    fun verifyApkSigningCertificateMatches(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) return false

        try {
            val packageManager = context.packageManager
            val archiveSignatures = getArchiveSignatures(packageManager, apkFile.absolutePath)
            if (archiveSignatures.isEmpty()) return false

            val installedSignatures = getPackageSignatures(packageManager, context.packageName)
            if (installedSignatures.isEmpty()) return false

            val archiveFingerprint = computeSha256Fingerprint(archiveSignatures[0].toByteArray())
            val installedFingerprint = computeSha256Fingerprint(installedSignatures[0].toByteArray())

            return archiveFingerprint.isNotBlank() &&
                    archiveFingerprint.equals(installedFingerprint, ignoreCase = true)
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Light detection of hooking tools and root (PRD line 262).
     */
    fun isRootOrHookingDetected(): Boolean {
        return checkRootBinaries() || checkTestKeys() || checkHookingClasses()
    }

    /**
     * Determines whether the environment is compromised (tampered APK, root, hooking).
     * Used for quiet failure: Pro features stay locked, but the free app is never blocked.
     */
    fun isEnvironmentCompromised(context: Context): Boolean {
        if (!isAppSignatureValid(context)) {
            return true
        }
        if (!BuildConfig.DEBUG && isRootOrHookingDetected()) {
            return true
        }
        return false
    }

    private fun checkRootBinaries(): Boolean {
        val rootPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su"
        )
        return rootPaths.any { path ->
            try {
                File(path).exists()
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun checkTestKeys(): Boolean {
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }

    private fun checkHookingClasses(): Boolean {
        val hookingClasses = arrayOf(
            "de.robv.android.xposed.XposedBridge",
            "com.saurik.substrate.MS$2"
        )
        return hookingClasses.any { className ->
            try {
                Class.forName(className)
                true
            } catch (_: ClassNotFoundException) {
                false
            } catch (_: Throwable) {
                false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getPackageSignatures(pm: PackageManager, packageName: String): List<Signature> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo ?: return emptyList()
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.toList()
                } else {
                    signingInfo.signingCertificateHistory.toList()
                }
            } else {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                packageInfo.signatures?.toList() ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private fun getArchiveSignatures(pm: PackageManager, archiveFilePath: String): List<Signature> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo?.signingInfo ?: return emptyList()
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.toList()
                } else {
                    signingInfo.signingCertificateHistory.toList()
                }
            } else {
                val packageInfo = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_SIGNATURES)
                packageInfo?.signatures?.toList() ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun computeSha256Fingerprint(bytes: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}
