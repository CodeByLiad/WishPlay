package com.nuvetrix.wishplay.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.nuvetrix.wishplay.domain.model.AdminBkashItem
import com.nuvetrix.wishplay.domain.model.AdminDashboardData
import com.nuvetrix.wishplay.domain.model.AdminPromoCode
import com.nuvetrix.wishplay.domain.model.AdminStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getDashboardData(): Result<AdminDashboardData> = withContext(Dispatchers.IO) {
        try {
            // Attempt Cloud Function call first
            val callable = functions.getHttpsCallable("adminGetDashboard")
            val result = callable.call().await()
            val data = result.getData() as? Map<*, *>

            if (data != null && data["success"] == true) {
                val statsMap = data["stats"] as? Map<*, *> ?: emptyMap<String, Any>()
                val stats = AdminStats(
                    activeCodes = (statsMap["activeCodes"] as? Number)?.toInt() ?: 0,
                    redeemed = (statsMap["redeemed"] as? Number)?.toInt() ?: 0,
                    pendingBkashCount = (statsMap["pendingBkashCount"] as? Number)?.toInt() ?: 0
                )

                val pendingListRaw = data["pendingBkash"] as? List<*> ?: emptyList<Any>()
                val pendingBkash = pendingListRaw.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    AdminBkashItem(
                        id = map["id"] as? String ?: "",
                        trxId = map["trxId"] as? String ?: "",
                        userId = map["userId"] as? String ?: "",
                        amountBdt = (map["amountBdt"] as? Number)?.toInt() ?: 349,
                        senderNumber = map["senderNumber"] as? String ?: "",
                        createdAt = map["createdAt"] as? String
                    )
                }

                val codesRaw = data["codes"] as? List<*> ?: emptyList<Any>()
                val codes = codesRaw.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    AdminPromoCode(
                        code = map["code"] as? String ?: "",
                        type = map["type"] as? String ?: "lifetime",
                        discountPct = (map["discountPct"] as? Number)?.toInt() ?: 0,
                        maxUses = (map["maxUses"] as? Number)?.toInt() ?: 10,
                        uses = (map["uses"] as? Number)?.toInt() ?: 0,
                        expiresAt = map["expiresAt"] as? String,
                        active = map["active"] as? Boolean ?: true
                    )
                }

                return@withContext Result.success(AdminDashboardData(stats, pendingBkash, codes))
            }

            // Fallback: Direct Firestore read
            fetchDashboardFromFirestore()
        } catch (e: Exception) {
            // Fallback direct read or prototype mock data if unauthenticated/offline
            fetchDashboardFromFirestore().fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.success(getPrototypeMockData()) }
            )
        }
    }

    suspend fun reviewBkash(submissionId: String, action: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val callable = functions.getHttpsCallable("adminReviewBkash")
            val params = hashMapOf("submissionId" to submissionId, "action" to action)
            val result = callable.call(params).await()
            val data = result.getData() as? Map<*, *>
            val msg = (data?.get("message") as? String)
                ?: if (action == "approve") "Payment approved" else "Payment rejected"
            Result.success(msg)
        } catch (e: Exception) {
            // Firestore direct fallback
            try {
                val subRef = firestore.collection("bkashSubmissions").document(submissionId)
                val snap = subRef.get().await()
                if (snap.exists()) {
                    val uid = snap.getString("userId") ?: ""
                    val trx = snap.getString("trxId") ?: ""
                    val now = com.google.firebase.Timestamp.now()
                    val newStatus = if (action == "approve") "approved" else "rejected"

                    val updateData = mapOf<String, Any>(
                        "status" to newStatus,
                        "reviewedAt" to now,
                        "reviewedBy" to (auth.currentUser?.uid ?: "admin")
                    )
                    subRef.update(updateData).await()

                    if (action == "approve" && uid.isNotBlank()) {
                        firestore.collection("users").document(uid)
                            .collection("entitlements").document("pro")
                            .set(
                                mapOf<String, Any?>(
                                    "plan" to "pro",
                                    "source" to "bkash",
                                    "sourceRef" to trx,
                                    "grantedAt" to now,
                                    "revokedAt" to null
                                )
                            ).await()
                    }
                    Result.success(if (action == "approve") "Payment approved. Pro unlocked." else "Payment rejected.")
                } else {
                    Result.failure(Exception("Submission not found"))
                }
            } catch (fallbackEx: Exception) {
                Result.failure(fallbackEx)
            }
        }
    }

    suspend fun createCode(
        type: String,
        discountPct: Int,
        maxUses: Int,
        expiresAtIso: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val callable = functions.getHttpsCallable("adminCreateCode")
            val params = hashMapOf(
                "type" to type,
                "discountPct" to discountPct,
                "maxUses" to maxUses,
                "expiresAt" to expiresAtIso
            )
            val result = callable.call(params).await()
            val data = result.getData() as? Map<*, *>
            val code = (data?.get("code") as? String) ?: ""
            Result.success(code)
        } catch (e: Exception) {
            // Direct Firestore fallback
            try {
                val code = generatePromoCode()
                val codeType = if (type == "free") "lifetime" else "discount"
                val pct = if (type == "free") 100 else discountPct

                val codeData = mapOf<String, Any>(
                    "code" to code,
                    "type" to codeType,
                    "discountPct" to pct,
                    "maxUses" to maxUses,
                    "uses" to 0,
                    "active" to true,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("promoCodes").document(code).set(codeData).await()

                Result.success(code)
            } catch (fallbackEx: Exception) {
                Result.failure(fallbackEx)
            }
        }
    }

    suspend fun toggleCode(code: String, active: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val callable = functions.getHttpsCallable("adminToggleCode")
            val params = hashMapOf("code" to code, "active" to active)
            callable.call(params).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                firestore.collection("promoCodes").document(code)
                    .update("active", active as Any).await()
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Result.failure(fallbackEx)
            }
        }
    }

    private suspend fun fetchDashboardFromFirestore(): Result<AdminDashboardData> {
        val codesSnap = firestore.collection("promoCodes").get().await()
        var activeCodesCount = 0
        val codes = codesSnap.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val active = data["active"] as? Boolean ?: true
            val uses = (data["uses"] as? Number)?.toInt() ?: 0
            val maxUses = (data["maxUses"] as? Number)?.toInt() ?: 10
            if (active && uses < maxUses) activeCodesCount++
            val expTs = data["expiresAt"] as? com.google.firebase.Timestamp
            AdminPromoCode(
                code = doc.id,
                type = data["type"] as? String ?: "lifetime",
                discountPct = (data["discountPct"] as? Number)?.toInt() ?: 0,
                maxUses = maxUses,
                uses = uses,
                expiresAt = expTs?.toDate()?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) },
                active = active
            )
        }

        val redemptionsSnap = firestore.collection("codeRedemptions").get().await()
        val pendingSnap = firestore.collection("bkashSubmissions")
            .whereEqualTo("status", "pending")
            .get()
            .await()

        val pendingList = pendingSnap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val ts = d["createdAt"] as? com.google.firebase.Timestamp
            val dateStr = ts?.toDate()?.let { SimpleDateFormat("MMM d, h:mm a", Locale.US).format(it) } ?: "Recent"
            AdminBkashItem(
                id = doc.id,
                trxId = d["trxId"] as? String ?: "",
                userId = d["userId"] as? String ?: "",
                amountBdt = (d["amountBdt"] as? Number)?.toInt() ?: 349,
                senderNumber = d["senderNumber"] as? String ?: "",
                createdAt = dateStr
            )
        }

        return Result.success(
            AdminDashboardData(
                stats = AdminStats(
                    activeCodes = activeCodesCount,
                    redeemed = redemptionsSnap.size(),
                    pendingBkashCount = pendingList.size
                ),
                pendingBkash = pendingList,
                codes = codes
            )
        )
    }

    private fun getPrototypeMockData(): AdminDashboardData {
        return AdminDashboardData(
            stats = AdminStats(activeCodes = 2, redeemed = 47, pendingBkashCount = 1),
            pendingBkash = listOf(
                AdminBkashItem(
                    id = "mock_bk_1",
                    trxId = "BK7X4Q2M9A",
                    userId = "user_123",
                    amountBdt = 349,
                    senderNumber = "01712***890",
                    createdAt = "Today, 10:14 AM"
                )
            ),
            codes = listOf(
                AdminPromoCode(
                    code = "WP-GIFT-7K2Q",
                    type = "lifetime",
                    discountPct = 100,
                    maxUses = 25,
                    uses = 18,
                    expiresAt = "Dec 31, 2026",
                    active = true
                ),
                AdminPromoCode(
                    code = "WP-EID20",
                    type = "discount",
                    discountPct = 20,
                    maxUses = 100,
                    uses = 29,
                    expiresAt = "Dec 31, 2026",
                    active = true
                )
            )
        )
    }

    private fun generatePromoCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val p1 = (1..4).map { chars.random() }.joinToString("")
        val p2 = (1..4).map { chars.random() }.joinToString("")
        return "WP-$p1-$p2"
    }
}
