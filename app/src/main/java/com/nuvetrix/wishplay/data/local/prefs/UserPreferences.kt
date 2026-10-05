package com.nuvetrix.wishplay.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wishplay_settings")

class UserPreferences(private val context: Context) {

    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    private val KEY_ROLE = stringPreferencesKey("user_role")
    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_ACCENT = stringPreferencesKey("accent_color")
    private val KEY_PLATFORMS = stringSetPreferencesKey("preferred_platforms")
    private val KEY_REMINDER_LEAD = stringPreferencesKey("reminder_lead")
    private val KEY_NOTIF_ASKED = booleanPreferencesKey("notif_asked")
    private val KEY_NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
    private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches_csv")
    private val KEY_ALERT_RELEASE = booleanPreferencesKey("alert_release")
    private val KEY_ALERT_COUNTDOWN = booleanPreferencesKey("alert_countdown")
    private val KEY_ALERT_CHANGES = booleanPreferencesKey("alert_changes")
    private val KEY_ALERT_PRICE = booleanPreferencesKey("alert_price")
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_USER_AVATAR = stringPreferencesKey("user_avatar_url")
    private val KEY_IS_PRO = booleanPreferencesKey("is_pro")
    private val KEY_LAST_SYNCED = androidx.datastore.preferences.core.longPreferencesKey("last_synced_timestamp")
    private val KEY_SYNC_STATE = stringPreferencesKey("sync_state")

    val alertRelease: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ALERT_RELEASE] ?: true
    }

    val alertCountdown: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ALERT_COUNTDOWN] ?: true
    }

    val alertChanges: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ALERT_CHANGES] ?: true
    }

    val alertPrice: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ALERT_PRICE] ?: true
    }

    val recentSearches: Flow<List<String>> = context.dataStore.data.map {
        val raw = it[KEY_RECENT_SEARCHES] ?: ""
        if (raw.isBlank()) emptyList() else raw.split("|||")
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ONBOARDING_DONE] ?: false
    }

    val userRole: Flow<String> = context.dataStore.data.map {
        it[KEY_ROLE] ?: "guest"
    }

    val themeMode: Flow<String> = context.dataStore.data.map {
        it[KEY_THEME] ?: "system"
    }

    val accentColor: Flow<String> = context.dataStore.data.map {
        it[KEY_ACCENT] ?: "gold"
    }

    val preferredPlatforms: Flow<Set<String>> = context.dataStore.data.map {
        it[KEY_PLATFORMS] ?: emptySet()
    }

    val reminderLead: Flow<String> = context.dataStore.data.map {
        it[KEY_REMINDER_LEAD] ?: "1 week"
    }

    val notificationsAsked: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_NOTIF_ASKED] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_NOTIF_ENABLED] ?: false
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setUserRole(role: String) {
        context.dataStore.edit { it[KEY_ROLE] = role }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun setAccentColor(accent: String) {
        context.dataStore.edit { it[KEY_ACCENT] = accent }
    }

    suspend fun setPreferredPlatforms(platforms: Set<String>) {
        context.dataStore.edit { it[KEY_PLATFORMS] = platforms }
    }

    suspend fun setReminderLead(lead: String) {
        context.dataStore.edit { it[KEY_REMINDER_LEAD] = lead }
    }

    suspend fun setNotificationsAsked(asked: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_ASKED] = asked }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_ENABLED] = enabled }
    }

    suspend fun setAlertRelease(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALERT_RELEASE] = enabled }
    }

    suspend fun setAlertCountdown(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALERT_COUNTDOWN] = enabled }
    }

    suspend fun setAlertChanges(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALERT_CHANGES] = enabled }
    }

    suspend fun setAlertPrice(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALERT_PRICE] = enabled }
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
            val list = if (raw.isBlank()) mutableListOf() else raw.split("|||").toMutableList()
            list.remove(trimmed)
            list.add(0, trimmed)
            val updated = list.take(8).joinToString("|||")
            prefs[KEY_RECENT_SEARCHES] = updated
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
            val list = if (raw.isBlank()) mutableListOf() else raw.split("|||").toMutableList()
            list.remove(query.trim())
            prefs[KEY_RECENT_SEARCHES] = list.joinToString("|||")
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_RECENT_SEARCHES)
        }
    }

    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[KEY_USER_EMAIL] }
    val userName: Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }
    val userAvatarUrl: Flow<String?> = context.dataStore.data.map { it[KEY_USER_AVATAR] }
    val isPro: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_PRO] ?: false }
    val lastSyncedTime: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_SYNCED] ?: 0L }
    val syncState: Flow<String> = context.dataStore.data.map { it[KEY_SYNC_STATE] ?: "IDLE" }

    suspend fun setUserData(
        id: String,
        email: String,
        name: String,
        avatarUrl: String?,
        role: String,
        isProUser: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = id
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_NAME] = name
            if (avatarUrl != null) prefs[KEY_USER_AVATAR] = avatarUrl else prefs.remove(KEY_USER_AVATAR)
            prefs[KEY_ROLE] = role
            prefs[KEY_IS_PRO] = isProUser
        }
    }

    suspend fun setProStatus(isPro: Boolean) {
        context.dataStore.edit { it[KEY_IS_PRO] = isPro }
    }

    suspend fun clearUserData() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_AVATAR)
            prefs[KEY_ROLE] = "guest"
            prefs[KEY_IS_PRO] = false
            prefs[KEY_LAST_SYNCED] = 0L
            prefs[KEY_SYNC_STATE] = "IDLE"
        }
    }

    suspend fun setLastSyncedTime(time: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNCED] = time }
    }

    suspend fun setSyncState(state: String) {
        context.dataStore.edit { it[KEY_SYNC_STATE] = state }
    }
}
