package com.nuvetrix.wishplay.data.remote.update

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val minSupportedVersionCode: Int = 1,
    val releaseNotes: String = "",
    val publishedAt: String = ""
)

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class UpToDate(val currentVersion: String) : UpdateStatus
    data class Available(
        val manifest: UpdateManifest,
        val isForced: Boolean
    ) : UpdateStatus
    data class Downloading(val progressPercent: Int) : UpdateStatus
    data class ReadyToInstall(val apkFile: File) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

class InstallPermissionRequiredException(
    message: String = "Install unknown apps permission is required to update"
) : Exception(message)
