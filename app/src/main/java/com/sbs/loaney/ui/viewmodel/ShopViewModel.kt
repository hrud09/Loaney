package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.model.Coupon
import com.sbs.loaney.data.model.CouponCategory
import com.sbs.loaney.data.model.mockCoupons
import com.sbs.loaney.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopUiState(
    val totalPies: Int = 0,
    val coupons: List<Coupon> = mockCoupons,
    val selectedCategory: CouponCategory? = null,
    val isPurchasing: Boolean = false,
    val purchaseSuccess: Boolean = false
) {
    val filteredCoupons: List<Coupon>
        get() = if (selectedCategory == null) coupons else coupons.filter { it.category == selectedCategory }
}

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        // Collect user's total pies from repository (assuming it's stored in Settings/DataStore)
        // For now, we'll mock it or pull from settings if added there.
        // Since we added it to UserProfile but didn't persist it in SettingsRepository yet,
        // let's assume a default for now or add a simple pieFlow to settings.
    }

    // Since I don't have totalPies in SettingsRepository yet, I'll add a temporary mock flow
    // or just use a hardcoded value that matches the profile placeholder.
    private val _mockPieFlow = MutableStateFlow(1240) 

    init {
        _mockPieFlow.onEach { pies ->
            _uiState.update { it.copy(totalPies = pies) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: CouponCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun purchaseCoupon(coupon: Coupon) {
        if (_uiState.value.totalPies >= coupon.costInPies) {
            viewModelScope.launch {
                _uiState.update { it.copy(isPurchasing = true) }
                // Simulate network delay
                kotlinx.coroutines.delay(1000)
                _mockPieFlow.update { it - coupon.costInPies }
                _uiState.update { it.copy(isPurchasing = false, purchaseSuccess = true) }
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(purchaseSuccess = false) }
            }
        }
    }
}
