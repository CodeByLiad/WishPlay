package com.nuvetrix.wishplay.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvetrix.wishplay.data.repository.AdminRepository
import com.nuvetrix.wishplay.domain.model.AdminBkashItem
import com.nuvetrix.wishplay.domain.model.AdminDashboardData
import com.nuvetrix.wishplay.domain.model.AdminPromoCode
import com.nuvetrix.wishplay.domain.model.AdminStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val stats: AdminStats = AdminStats(),
    val pendingBkash: List<AdminBkashItem> = emptyList(),
    val codes: List<AdminPromoCode> = emptyList(),
    val codeSearchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedCodeType: String = "free", // "free" or "pct"
    val discountPct: Int = 20,
    val maxUses: Int = 10,
    val expiresAt: String = "2026-12-31",
    val snackMessage: String? = null
) {
    val filteredCodes: List<AdminPromoCode>
        get() {
            if (codeSearchQuery.isBlank()) return codes
            val q = codeSearchQuery.trim().lowercase()
            return codes.filter { it.code.lowercase().contains(q) }
        }
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = adminRepository.getDashboardData()
            result.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        stats = data.stats,
                        pendingBkash = data.pendingBkash,
                        codes = data.codes,
                        isLoading = false
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackMessage = "Failed to load dashboard: ${ex.message}"
                    )
                }
            }
        }
    }

    fun approveBkash(item: AdminBkashItem) {
        viewModelScope.launch {
            val result = adminRepository.reviewBkash(item.id, "approve")
            result.onSuccess { msg ->
                _uiState.update { state ->
                    val updatedList = state.pendingBkash.filterNot { it.id == item.id }
                    state.copy(
                        pendingBkash = updatedList,
                        stats = state.stats.copy(pendingBkashCount = updatedList.size),
                        snackMessage = msg
                    )
                }
            }.onFailure { ex ->
                _uiState.update { it.copy(snackMessage = "Approval failed: ${ex.message}") }
            }
        }
    }

    fun rejectBkash(item: AdminBkashItem) {
        viewModelScope.launch {
            val result = adminRepository.reviewBkash(item.id, "reject")
            result.onSuccess { msg ->
                _uiState.update { state ->
                    val updatedList = state.pendingBkash.filterNot { it.id == item.id }
                    state.copy(
                        pendingBkash = updatedList,
                        stats = state.stats.copy(pendingBkashCount = updatedList.size),
                        snackMessage = msg
                    )
                }
            }.onFailure { ex ->
                _uiState.update { it.copy(snackMessage = "Rejection failed: ${ex.message}") }
            }
        }
    }

    fun createCode() {
        viewModelScope.launch {
            val type = _uiState.value.selectedCodeType
            val discountPct = if (type == "free") 100 else _uiState.value.discountPct
            val maxUses = _uiState.value.maxUses
            val exp = _uiState.value.expiresAt

            val result = adminRepository.createCode(type, discountPct, maxUses, exp)
            result.onSuccess { code ->
                _uiState.update {
                    val newCodeItem = AdminPromoCode(
                        code = code,
                        type = if (type == "free") "lifetime" else "discount",
                        discountPct = discountPct,
                        maxUses = maxUses,
                        uses = 0,
                        expiresAt = exp,
                        active = true
                    )
                    it.copy(
                        codes = listOf(newCodeItem) + it.codes,
                        stats = it.stats.copy(activeCodes = it.stats.activeCodes + 1),
                        snackMessage = "Created code: $code"
                    )
                }
            }.onFailure { ex ->
                _uiState.update { it.copy(snackMessage = "Code creation failed: ${ex.message}") }
            }
        }
    }

    fun toggleCode(code: String, currentActive: Boolean) {
        val newActive = !currentActive
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                val updatedCodes = state.codes.map {
                    if (it.code == code) it.copy(active = newActive) else it
                }
                val activeCount = updatedCodes.count { it.active && it.uses < it.maxUses }
                state.copy(
                    codes = updatedCodes,
                    stats = state.stats.copy(activeCodes = activeCount)
                )
            }

            val result = adminRepository.toggleCode(code, newActive)
            result.onFailure {
                // Revert on failure
                loadDashboard()
                _uiState.update { it.copy(snackMessage = "Failed to update code state") }
            }
        }
    }

    fun setCodeSearchQuery(q: String) {
        _uiState.update { it.copy(codeSearchQuery = q) }
    }

    fun setCodeType(type: String) {
        val max = if (type == "free") 10 else 100
        _uiState.update { it.copy(selectedCodeType = type, maxUses = max) }
    }

    fun setDiscountPct(pct: Int) {
        _uiState.update { it.copy(discountPct = pct.coerceIn(1, 99)) }
    }

    fun setMaxUses(uses: Int) {
        _uiState.update { it.copy(maxUses = uses.coerceAtLeast(1)) }
    }

    fun setExpiresAt(date: String) {
        _uiState.update { it.copy(expiresAt = date) }
    }

    fun dismissSnack() {
        _uiState.update { it.copy(snackMessage = null) }
    }
}
