package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.repository.ILoanRepository
import com.sbs.loaney.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val deletedLoans: List<LoanWithPayments> = emptyList(),
    val isLoading: Boolean = false,
    val currencySymbol: String = "৳"
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ILoanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    init {
        cleanupOldHistory()
    }

    private fun cleanupOldHistory() {
        viewModelScope.launch {
            val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
            repository.deleteExpiredLoans(ninetyDaysAgo)
        }
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getDeletedLoans(),
        settingsRepository.currencySymbolFlow
    ) { deletedLoans, currency ->
        HistoryUiState(
            deletedLoans = deletedLoans,
            currencySymbol = currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun restoreLoan(loanId: Long) {
        viewModelScope.launch {
            repository.restoreLoan(loanId)
        }
    }

    fun deletePermanently(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }
}
