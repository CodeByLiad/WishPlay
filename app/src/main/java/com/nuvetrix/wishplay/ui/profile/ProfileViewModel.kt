package com.nuvetrix.wishplay.ui.profile

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvetrix.wishplay.alerts.SyncWorker
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.data.remote.update.InstallPermissionRequiredException
import com.nuvetrix.wishplay.data.remote.update.UpdateManager
import com.nuvetrix.wishplay.data.remote.update.UpdateManifest
import com.nuvetrix.wishplay.data.remote.update.UpdateStatus
import com.nuvetrix.wishplay.data.repository.AuthRepository
import com.nuvetrix.wishplay.data.repository.BkashConfig
import com.nuvetrix.wishplay.data.repository.EntitlementRepository
import com.nuvetrix.wishplay.data.repository.SyncRepository
import com.nuvetrix.wishplay.domain.model.AuthUser
import com.nuvetrix.wishplay.domain.model.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val user: AuthUser = AuthUser("", "List stored on this phone only", "Guest", null, "guest", false),
    val themeMode: String = "system",
    val accentColor: String = "gold",
    val isNotificationsAllowed: Boolean = true,
    val lastSyncedTime: Long = 0L,
    val syncState: SyncState = SyncState.IDLE,
    val snackMessage: String? = null,
    val isDeleteSheetOpen: Boolean = false,
    val isSignInSheetOpen: Boolean = false,
    val isRedeemSheetOpen: Boolean = false,
    val isPrivacySheetOpen: Boolean = false,
    val isBatterySheetOpen: Boolean = false,
    val isPrimingSheetOpen: Boolean = false,
    val isProSheetOpen: Boolean = false,
    val isUpdateSheetOpen: Boolean = false,
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val updateManifest: UpdateManifest? = null,
    val isUpdateForced: Boolean = false,
    val bkashConfig: BkashConfig = BkashConfig(),
    val isBkashSubmitting: Boolean = false,
    val paddleCheckoutUrl: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val userPreferences: UserPreferences,
    private val entitlementRepository: EntitlementRepository,
    private val updateManager: UpdateManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _notificationsAllowed = MutableStateFlow(
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    )
    private val _snackMessage = MutableStateFlow<String?>(null)
    private val _isDeleteSheetOpen = MutableStateFlow(false)
    private val _isSignInSheetOpen = MutableStateFlow(false)
    private val _isRedeemSheetOpen = MutableStateFlow(false)
    private val _isPrivacySheetOpen = MutableStateFlow(false)
    private val _isBatterySheetOpen = MutableStateFlow(false)
    private val _isPrimingSheetOpen = MutableStateFlow(false)
    private val _isProSheetOpen = MutableStateFlow(false)
    private val _isUpdateSheetOpen = MutableStateFlow(false)
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    private val _updateManifest = MutableStateFlow<UpdateManifest?>(null)
    private val _isUpdateForced = MutableStateFlow(false)
    private var downloadedApkFile: File? = null

    private val _bkashConfig = MutableStateFlow(BkashConfig())
    private val _isBkashSubmitting = MutableStateFlow(false)
    private val _paddleCheckoutUrl = MutableStateFlow("")

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.currentUser,
        userPreferences.themeMode,
        userPreferences.accentColor,
        _notificationsAllowed,
        userPreferences.lastSyncedTime,
        syncRepository.syncState,
        _snackMessage,
        combine(
            _isDeleteSheetOpen,
            _isSignInSheetOpen,
            _isRedeemSheetOpen,
            _isPrivacySheetOpen,
            _isBatterySheetOpen,
            _isPrimingSheetOpen,
            _isProSheetOpen
        ) { sheets -> sheets },
        combine(
            _isUpdateSheetOpen,
            _updateStatus,
            _updateManifest,
            _isUpdateForced
        ) { isSheetOpen, status, manifest, isForced ->
            UpdateGroup(isSheetOpen, status, manifest, isForced)
        },
        combine(
            _bkashConfig,
            _isBkashSubmitting,
            _paddleCheckoutUrl
        ) { config, submitting, url -> Triple(config, submitting, url) }
    ) { args ->
        val user = args[0] as AuthUser
        val theme = args[1] as String
        val accent = args[2] as String
        val notifs = args[3] as Boolean
        val lastSynced = args[4] as Long
        val syncState = args[5] as SyncState
        val snack = args[6] as String?
        val sheets = args[7] as Array<*>
        val update = args[8] as UpdateGroup
        @Suppress("UNCHECKED_CAST")
        val payment = args[9] as Triple<BkashConfig, Boolean, String>

        ProfileUiState(
            user = user,
            themeMode = theme,
            accentColor = accent,
            isNotificationsAllowed = notifs,
            lastSyncedTime = lastSynced,
            syncState = syncState,
            snackMessage = snack,
            isDeleteSheetOpen = sheets[0] as Boolean,
            isSignInSheetOpen = sheets[1] as Boolean,
            isRedeemSheetOpen = sheets[2] as Boolean,
            isPrivacySheetOpen = sheets[3] as Boolean,
            isBatterySheetOpen = sheets[4] as Boolean,
            isPrimingSheetOpen = sheets[5] as Boolean,
            isProSheetOpen = sheets[6] as Boolean,
            isUpdateSheetOpen = update.isSheetOpen,
            updateStatus = update.status,
            updateManifest = update.manifest,
            isUpdateForced = update.isForced,
            bkashConfig = payment.first,
            isBkashSubmitting = payment.second,
            paddleCheckoutUrl = payment.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    init {
        // Automatic daily launch check for updates (PRD lines 300-301)
        checkForUpdates(isManual = false)
    }

    fun refreshNotificationStatus() {
        _notificationsAllowed.value = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            userPreferences.setThemeMode(theme)
        }
    }

    fun setAccent(accent: String) {
        viewModelScope.launch {
            userPreferences.setAccentColor(accent)
        }
    }

    fun signInWithGoogle(activityContext: android.content.Context) {
        viewModelScope.launch {
            _isSignInSheetOpen.value = false
            val result = authRepository.signInWithGoogle(activityContext)
            result.onSuccess {
                _snackMessage.value = "Signed in. Your list is syncing now."
                SyncWorker.schedulePeriodicSync(context)
                entitlementRepository.refreshEntitlement()
            }.onFailure {
                _snackMessage.value = if (it.message == "Sign-in cancelled") null
                else "Sign-in failed: ${it.message}"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _snackMessage.value = "Signed out. Switched to guest mode."
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isDeleteSheetOpen.value = false
            val result = authRepository.deleteAccount()
            result.onSuccess {
                _snackMessage.value = "Account deleted. Wishlist removed from server."
            }.onFailure {
                _snackMessage.value = "Could not delete account: ${it.message}"
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            val user = uiState.value.user
            if (user.isGuest) {
                _snackMessage.value = "Sign in to back up and sync your list."
                return@launch
            }
            _snackMessage.value = "Syncing..."
            val result = syncRepository.syncWishlist()
            result.onSuccess { summary ->
                _snackMessage.value = "Synced just now (${summary.pushedCount} pushed, ${summary.pulledCount} pulled)."
            }.onFailure {
                _snackMessage.value = "Sync failed. Will retry automatically."
            }
        }
    }

    /** Server-side code redemption. */
    fun redeemCode(code: String): Boolean {
        viewModelScope.launch {
            val result = entitlementRepository.redeemCode(code)
            _snackMessage.value = result.message
            if (result.pro_granted) {
                _isRedeemSheetOpen.value = false
            }
        }
        return code.trim().uppercase().let { it == "WP-GIFT-7K2Q" || it == "WP-EID20" }
    }

    // ──────────────────────────── Pro Payment Sheet ─────────────────────────

    fun openProSheet() {
        viewModelScope.launch {
            _isProSheetOpen.value = true
            val bkashResult = entitlementRepository.getBkashConfig()
            _bkashConfig.value = bkashResult

            val checkoutUrl = entitlementRepository.getPaddleCheckoutUrl()
            _paddleCheckoutUrl.value = checkoutUrl
        }
    }

    fun closeProSheet() { _isProSheetOpen.value = false }

    fun submitBkash(trxId: String, senderNumber: String) {
        viewModelScope.launch {
            _isBkashSubmitting.value = true
            val config = _bkashConfig.value
            val result = entitlementRepository.submitBkash(trxId, senderNumber, config.amount_bdt)
            _isBkashSubmitting.value = false
            _snackMessage.value = result.message
            if (result.success) {
                _isProSheetOpen.value = false
            }
        }
    }

    // ──────────────────────────── In-App Updates ────────────────────────────

    fun checkForUpdates(isManual: Boolean = true) {
        viewModelScope.launch {
            if (isManual) {
                _updateStatus.value = UpdateStatus.Checking
            }
            val status = updateManager.checkForUpdates(isManual = isManual)
            _updateStatus.value = status

            when (status) {
                is UpdateStatus.Available -> {
                    _updateManifest.value = status.manifest
                    _isUpdateForced.value = status.isForced
                    _isUpdateSheetOpen.value = true
                }
                is UpdateStatus.UpToDate -> {
                    if (isManual) {
                        _snackMessage.value = "WishPlay is up to date (v${status.currentVersion})."
                    }
                }
                is UpdateStatus.Error -> {
                    if (isManual) {
                        _snackMessage.value = "Check failed: ${status.message}"
                    }
                }
                else -> Unit
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val manifest = _updateManifest.value ?: return
        viewModelScope.launch {
            if (downloadedApkFile != null && downloadedApkFile?.exists() == true) {
                triggerInstall(downloadedApkFile!!)
                return@launch
            }

            _updateStatus.value = UpdateStatus.Downloading(0)
            val result = updateManager.downloadAndVerifyApk(manifest) { percent ->
                _updateStatus.value = UpdateStatus.Downloading(percent)
            }

            result.onSuccess { apkFile ->
                downloadedApkFile = apkFile
                _updateStatus.value = UpdateStatus.ReadyToInstall(apkFile)
                triggerInstall(apkFile)
            }.onFailure { err ->
                _updateStatus.value = UpdateStatus.Error(err.message ?: "Download failed")
                _snackMessage.value = "Update error: ${err.message}"
            }
        }
    }

    private fun triggerInstall(apkFile: File) {
        val result = updateManager.installApk(apkFile)
        result.onFailure { error ->
            if (error is InstallPermissionRequiredException) {
                _snackMessage.value = "Please grant permission to install updates."
                context.startActivity(updateManager.getUnknownAppSourcesIntent())
            } else {
                _snackMessage.value = "Install failed: ${error.message}"
            }
        }
    }

    fun closeUpdateSheet() {
        if (!_isUpdateForced.value) {
            _isUpdateSheetOpen.value = false
        }
    }

    // ──────────────────────────── Sheet controls ────────────────────────────

    fun openDeleteSheet() { _isDeleteSheetOpen.value = true }
    fun closeDeleteSheet() { _isDeleteSheetOpen.value = false }

    fun openSignInSheet() { _isSignInSheetOpen.value = true }
    fun closeSignInSheet() { _isSignInSheetOpen.value = false }

    fun openRedeemSheet() { _isRedeemSheetOpen.value = true }
    fun closeRedeemSheet() { _isRedeemSheetOpen.value = false }

    fun openPrivacySheet() { _isPrivacySheetOpen.value = true }
    fun closePrivacySheet() { _isPrivacySheetOpen.value = false }

    fun openBatterySheet() { _isBatterySheetOpen.value = true }
    fun closeBatterySheet() { _isBatterySheetOpen.value = false }

    fun openPrimingSheet() { _isPrimingSheetOpen.value = true }
    fun closePrimingSheet() { _isPrimingSheetOpen.value = false }

    fun dismissSnack() { _snackMessage.value = null }
}

private data class UpdateGroup(
    val isSheetOpen: Boolean,
    val status: UpdateStatus,
    val manifest: UpdateManifest?,
    val isForced: Boolean
)
