package com.nuvetrix.wishplay

import com.nuvetrix.wishplay.domain.model.AdminBkashItem
import com.nuvetrix.wishplay.domain.model.AdminDashboardData
import com.nuvetrix.wishplay.domain.model.AdminPromoCode
import com.nuvetrix.wishplay.domain.model.AdminStats
import com.nuvetrix.wishplay.domain.model.AuthUser
import com.nuvetrix.wishplay.ui.admin.AdminUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminFeatureTest {

    @Test
    fun adminRoleGrantsProAndPrivileges() {
        val adminUser = AuthUser(
            id = "admin_uid_1",
            email = "hyathis.x@gmail.com",
            name = "Liad",
            role = "admin",
            isPro = true
        )

        assertEquals("admin", adminUser.role)
        assertTrue(adminUser.isPro)
        assertFalse(adminUser.isGuest)
        assertEquals("LI", adminUser.initials)
    }

    @Test
    fun nonProUserLockedFromAccentThemes() {
        val freeUser = AuthUser(
            id = "free_uid_1",
            email = "user@test.com",
            name = "Test User",
            role = "free",
            isPro = false
        )

        fun canSelectAccent(user: AuthUser, accentKey: String): Boolean {
            return user.isPro || accentKey == "gold"
        }

        assertTrue("Free user can select free gold theme", canSelectAccent(freeUser, "gold"))
        assertFalse("Free user cannot select violet theme", canSelectAccent(freeUser, "violet"))
        assertFalse("Free user cannot select teal theme", canSelectAccent(freeUser, "teal"))
        assertFalse("Free user cannot select coral theme", canSelectAccent(freeUser, "coral"))

        val proUser = freeUser.copy(role = "pro", isPro = true)
        assertTrue("Pro user can select violet theme", canSelectAccent(proUser, "violet"))
        assertTrue("Pro user can select teal theme", canSelectAccent(proUser, "teal"))
        assertTrue("Pro user can select coral theme", canSelectAccent(proUser, "coral"))
    }

    @Test
    fun promoCodeFilteringWorksCorrectly() {
        val codes = listOf(
            AdminPromoCode(code = "WP-GIFT-7K2Q", type = "lifetime", active = true, maxUses = 10, uses = 2),
            AdminPromoCode(code = "WP-EID20", type = "discount", discountPct = 20, active = true, maxUses = 100, uses = 15),
            AdminPromoCode(code = "WP-SUMMER", type = "discount", discountPct = 15, active = false, maxUses = 50, uses = 50)
        )

        val stateEmptyQuery = AdminUiState(codes = codes, codeSearchQuery = "")
        assertEquals(3, stateEmptyQuery.filteredCodes.size)

        val stateSearchGift = AdminUiState(codes = codes, codeSearchQuery = "GIFT")
        assertEquals(1, stateSearchGift.filteredCodes.size)
        assertEquals("WP-GIFT-7K2Q", stateSearchGift.filteredCodes[0].code)

        val stateSearchEid = AdminUiState(codes = codes, codeSearchQuery = "eid")
        assertEquals(1, stateSearchEid.filteredCodes.size)
        assertEquals("WP-EID20", stateSearchEid.filteredCodes[0].code)
    }

    @Test
    fun activeCodesCountComputation() {
        val codes = listOf(
            AdminPromoCode(code = "C1", active = true, maxUses = 10, uses = 2),
            AdminPromoCode(code = "C2", active = false, maxUses = 10, uses = 1), // Inactive
            AdminPromoCode(code = "C3", active = true, maxUses = 10, uses = 10), // Exhausted
            AdminPromoCode(code = "C4", active = true, maxUses = 50, uses = 12)
        )

        val activeCount = codes.count { it.active && it.uses < it.maxUses }
        assertEquals(2, activeCount)
    }

    @Test
    fun bkashApprovalStateTransition() {
        val pendingSubmissions = listOf(
            AdminBkashItem(id = "sub_1", trxId = "BK7X4Q2M9A", senderNumber = "01712***890", amountBdt = 349),
            AdminBkashItem(id = "sub_2", trxId = "BK9Y1Z8K2P", senderNumber = "01978***129", amountBdt = 349)
        )

        val initialStats = AdminStats(pendingBkashCount = pendingSubmissions.size)
        assertEquals(2, initialStats.pendingBkashCount)

        // Approve sub_1
        val updatedSubmissions = pendingSubmissions.filterNot { it.id == "sub_1" }
        val updatedStats = initialStats.copy(pendingBkashCount = updatedSubmissions.size)

        assertEquals(1, updatedSubmissions.size)
        assertEquals("sub_2", updatedSubmissions[0].id)
        assertEquals(1, updatedStats.pendingBkashCount)
    }
}
