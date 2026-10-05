package com.nuvetrix.wishplay.domain.model

enum class Platform(val displayName: String, val shortName: String) {
    PC("PC", "PC"),
    PS5("PlayStation 5", "PS5"),
    XBOX_SERIES("Xbox Series X/S", "Xbox"),
    SWITCH_2("Nintendo Switch 2", "Switch 2"),
    ANDROID("Android", "Android"),
    IOS("iOS", "iOS");

    companion object {
        fun fromKey(key: String): Platform? = when (key.uppercase()) {
            "PC" -> PC
            "PS5", "PLAYSTATION", "PLAYSTATION 5" -> PS5
            "XBOX", "XBOX SERIES", "XBOX SERIES X/S" -> XBOX_SERIES
            "SWITCH", "SWITCH 2", "NINTENDO SWITCH" -> SWITCH_2
            "ANDROID" -> ANDROID
            "IOS", "APPLE" -> IOS
            else -> entries.find { it.shortName.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) }
        }
    }
}

enum class ReleaseStatus {
    UPCOMING,
    OUT_NOW,
    TBA
}

enum class SortOption(val id: String, val title: String, val subtitle: String) {
    NEAREST_RELEASE("date", "Nearest release", "Out now goes last"),
    RECENTLY_ADDED("added", "Recently added", "Newest on your list first"),
    NAME_A_TO_Z("name", "Name A to Z", "Alphabetical")
}

enum class FilterOption(val id: String, val label: String) {
    ALL("all", "All"),
    UPCOMING("upcoming", "Upcoming"),
    OUT_NOW("out", "Out now"),
    TBA("tba", "Date TBA")
}

data class RequirementsLevel(
    val os: String? = null,
    val cpu: String? = null,
    val gpu: String? = null,
    val ram: String? = null
)

data class SystemRequirements(
    val min: RequirementsLevel? = null,
    val rec: RequirementsLevel? = null
)

data class GamePrice(
    val store: String,
    val now: String,
    val was: String? = null
)

data class Game(
    val id: String,
    val title: String,
    val developer: String? = null,
    val hueHex: String = "#1F7A6E",
    val shapeKey: String = "c9",
    val coverUrl: String? = null,
    val logoUrl: String? = null,
    val platforms: Map<String, String?> = emptyMap(), // platform name -> date "YYYY-MM-DD"
    val about: String? = null,
    val isCustom: Boolean = false,
    val customNotes: String? = null,
    val hasTrailer: Boolean = false,
    val trailerYoutubeId: String? = null,
    val requirements: SystemRequirements? = null,
    val price: GamePrice? = null,
    val movedFromDate: String? = null,
    val expectedYear: String? = null,
    val storageSizes: Map<String, String?> = emptyMap(),
    val progress: Float = 0.5f,
    val igdbId: Long? = null,
    // Wishlist specific fields:
    val inWishlist: Boolean = false,
    val alertEnabled: Boolean = true,
    val priceAlertEnabled: Boolean = false,
    val orderIndex: Int = 0
) {
    val earliestDate: String?
        get() = platforms.values.filterNotNull().sorted().firstOrNull()

    fun status(today: String = "2026-09-30"): ReleaseStatus {
        val d = earliestDate ?: return ReleaseStatus.TBA
        return if (d <= today) ReleaseStatus.OUT_NOW else ReleaseStatus.UPCOMING
    }

    fun daysUntilRelease(today: String = "2026-09-30"): Int? {
        val d = earliestDate ?: return null
        return try {
            val target = java.time.LocalDate.parse(d)
            val current = java.time.LocalDate.parse(today)
            java.time.temporal.ChronoUnit.DAYS.between(current, target).toInt()
        } catch (e: Exception) {
            null
        }
    }
}

data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val role: String = "guest", // "guest", "free", "pro", "admin"
    val isPro: Boolean = false
) {
    val isGuest: Boolean get() = role == "guest"
    val initials: String
        get() {
            if (isGuest) return ""
            val parts = name.trim().split(" ")
            return if (parts.size >= 2) {
                "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
            } else {
                name.take(2).uppercase()
            }
        }
}

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

data class SyncSummary(
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val deletedCount: Int = 0,
    val lastSyncedMillis: Long = System.currentTimeMillis()
)

data class AdminStats(
    val activeCodes: Int = 0,
    val redeemed: Int = 0,
    val pendingBkashCount: Int = 0
)

data class AdminBkashItem(
    val id: String = "",
    val trxId: String = "",
    val userId: String = "",
    val amountBdt: Int = 349,
    val senderNumber: String = "",
    val createdAt: String? = null
)

data class AdminPromoCode(
    val code: String = "",
    val type: String = "lifetime", // "lifetime" or "discount"
    val discountPct: Int = 0,
    val maxUses: Int = 10,
    val uses: Int = 0,
    val expiresAt: String? = null,
    val active: Boolean = true
)

data class AdminDashboardData(
    val stats: AdminStats = AdminStats(),
    val pendingBkash: List<AdminBkashItem> = emptyList(),
    val codes: List<AdminPromoCode> = emptyList()
)

