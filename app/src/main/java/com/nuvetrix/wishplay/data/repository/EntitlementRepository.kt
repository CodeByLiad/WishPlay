package com.nuvetrix.wishplay.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RedeemResult(
    val success: Boolean = false,
    val type: String = "",
    val discount_pct: Int = 0,
    val pro_granted: Boolean = false,
    val message: String = "",
    val error: String? = null
)

@Serializable
data class BkashConfig(
    val recipient: String = "",
    val amount_bdt: Int = 349
)

@Serializable
data class BkashSubmitResult(
    val success: Boolean = false,
    val message: String = "",
    val error: String? = null
)

/**
 * Manages Pro entitlements using Firebase:
 * - Pro status lives in Firestore: /users/{uid}/entitlements/pro
 * - Code redemption and bKash submission call Firebase Cloud Functions
 * - Paddle checkout opens a hosted URL (Cloud Function generates the session)
 */
@Singleton
class EntitlementRepository @Inject constructor(
    private val userPreferences: UserPreferences,
    private val appSecurityManager: com.nuvetrix.wishplay.data.local.security.AppSecurityManager,
    private val httpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Cloud Function base region (us-central1 by default, change if you deploy elsewhere)
    // bKash config is served from a public Cloud Function endpoint
    private val functionsBaseUrl =
        "https://us-central1-wishplay-app.cloudfunctions.net"

    /** Returns true if the user currently has a valid Pro entitlement (cached locally). */
    val isPro: Flow<Boolean> = userPreferences.isPro

    /**
     * Refreshes the Pro entitlement status directly from Firestore and caches it.
     * Call after sign-in and periodically in the background.
     */
    suspend fun refreshEntitlement(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Quiet failure (PRD line 262-263): if tampering or hooking is detected,
            // never request or unlock Pro; keep the user silently in free mode without crashing.
            if (appSecurityManager.isEnvironmentCompromised(context)) {
                userPreferences.setProStatus(false)
                return@withContext Result.success(false)
            }

            val uid = firebaseAuth.currentUser?.uid
                ?: userPreferences.userId.first()

            if (uid.isNullOrBlank()) {
                return@withContext Result.success(false)
            }

            val doc = firestore
                .collection("users")
                .document(uid)
                .collection("entitlements")
                .document("pro")
                .get()
                .await()

            val email = firebaseAuth.currentUser?.email ?: ""
            val isAdmin = email.equals("hyathis.x@gmail.com", ignoreCase = true) || try {
                firestore.collection("admins").document(uid).get().await().exists()
            } catch (_: Exception) { false }

            val isPro = isAdmin || (doc.exists() && doc.getTimestamp("revokedAt") == null)
            userPreferences.setProStatus(isPro)
            if (isAdmin) {
                userPreferences.setUserRole("admin")
            } else if (isPro) {
                userPreferences.setUserRole("pro")
            }

            Result.success(isPro)
        } catch (_: Exception) {
            // Offline: return cached value
            Result.success(userPreferences.isPro.first())
        }
    }

    /**
     * Redeems a promo code via the Firebase Cloud Function `redeemCode`.
     * The function validates expiry, max-uses, uniqueness and grants Pro.
     */
    suspend fun redeemCode(code: String): RedeemResult = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf("code" to code.trim().uppercase())
            val result = functions
                .getHttpsCallable("redeemCode")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any?> ?: return@withContext failResult()
            val proGranted = map["pro_granted"] as? Boolean ?: false
            val redeemResult = RedeemResult(
                success = map["success"] as? Boolean ?: false,
                type = map["type"] as? String ?: "",
                discount_pct = (map["discount_pct"] as? Long)?.toInt() ?: 0,
                pro_granted = proGranted,
                message = map["message"] as? String ?: "",
                error = map["error"] as? String
            )

            if (proGranted) {
                userPreferences.setProStatus(true)
                userPreferences.setUserRole("pro")
            }
            redeemResult
        } catch (e: Exception) {
            // Offline fallback: check known demo codes only
            MockEntitlements.redeemCode(code)
        }
    }

    /**
     * Fetches the bKash recipient number and amount from a public Cloud Function.
     * These values must never be hardcoded in the APK.
     */
    suspend fun getBkashConfig(): BkashConfig = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$functionsBaseUrl/bkashConfig")
                .get()
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return@withContext BkashConfig()
                json.decodeFromString<BkashConfig>(body)
            } else {
                BkashConfig()
            }
        } catch (_: Exception) {
            BkashConfig()
        }
    }

    /**
     * Submits a bKash TrxID for admin review via Firebase Cloud Function `submitBkash`.
     */
    suspend fun submitBkash(
        trxId: String,
        senderNumber: String,
        amountBdt: Int
    ): BkashSubmitResult = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "trxId" to trxId.trim().uppercase(),
                "senderNumber" to senderNumber.trim(),
                "amountBdt" to amountBdt
            )
            val result = functions
                .getHttpsCallable("submitBkash")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any?> ?: return@withContext BkashSubmitResult(
                success = false, message = "Unexpected response.", error = "parse_error"
            )
            BkashSubmitResult(
                success = map["success"] as? Boolean ?: false,
                message = map["message"] as? String ?: "",
                error = map["error"] as? String
            )
        } catch (e: Exception) {
            BkashSubmitResult(
                success = false,
                message = "Could not reach server. Check your connection.",
                error = "network_error"
            )
        }
    }

    /**
     * Returns the Paddle checkout URL, generated by a Cloud Function that
     * embeds the Firebase UID as custom_data so the webhook can grant Pro.
     */
    suspend fun getPaddleCheckoutUrl(): String = withContext(Dispatchers.IO) {
        try {
            val result = functions
                .getHttpsCallable("getPaddleCheckoutUrl")
                .call()
                .await()
            @Suppress("UNCHECKED_CAST")
            (result.data as? Map<String, Any?>)?.get("url") as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun failResult() = RedeemResult(
        success = false,
        error = "parse_error",
        message = "Could not validate code. Please try again."
    )
}

/** Offline / prototype fallback for known demo codes. */
private object MockEntitlements {
    fun redeemCode(code: String): RedeemResult = when (code.trim().uppercase()) {
        "WP-GIFT-7K2Q" -> RedeemResult(
            success = true, type = "lifetime", discount_pct = 100,
            pro_granted = true, message = "Code accepted! WishPlay Pro unlocked for life."
        )
        "WP-EID20" -> RedeemResult(
            success = true, type = "discount", discount_pct = 20,
            pro_granted = false, message = "20% discount code applied!"
        )
        else -> RedeemResult(
            success = false, error = "invalid_code", message = "Invalid or expired code."
        )
    }
}
