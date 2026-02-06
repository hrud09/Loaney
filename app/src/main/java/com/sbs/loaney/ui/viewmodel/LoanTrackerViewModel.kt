package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class LoanTrackerUiState(
    val selectedLoan: LoanWithPayments? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class LoanTrackerViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _selectedLoanId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<LoanTrackerUiState> = _selectedLoanId
        .filterNotNull()
        .flatMapLatest { id -> repository.getLoanById(id) }
        .map { loan ->
            LoanTrackerUiState(selectedLoan = loan)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoanTrackerUiState()
        )

    fun selectLoan(loanId: Long) {
        _selectedLoanId.value = loanId
    }

    fun addPayment(amount: Double, method: String, note: String?) {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            val payment = PaymentEntity(
                loanId = loanId,
                amount = amount,
                date = Date(),
                method = method,
                note = note,
                proofUri = null
            )
            repository.insertPayment(payment)
            updateLoanStatus(loanId)
        }
    }

    fun markAsSettled() {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            val loanWithPayments = repository.getLoanById(loanId).firstOrNull() ?: return@launch
            val balance = loanWithPayments.loan.amount - loanWithPayments.payments.sumOf { it.amount }
            if (balance > 0) {
                // Add a final payment to settle
                addPayment(balance, "Settled", "Final settlement")
            } else {
                repository.updateLoan(loanWithPayments.loan.copy(status = LoanStatus.FULLY_PAID))
            }
        }
    }

    private suspend fun updateLoanStatus(loanId: Long) {
        val loanWithPayments = repository.getLoanById(loanId).firstOrNull() ?: return
        val totalPaid = loanWithPayments.payments.sumOf { it.amount }
        val loan = loanWithPayments.loan

        val newStatus = when {
            totalPaid >= loan.amount -> LoanStatus.FULLY_PAID
            totalPaid > 0 -> LoanStatus.PARTIALLY_PAID
            Date().after(loan.promisedReturnDate) -> LoanStatus.OVERDUE
            else -> LoanStatus.ACTIVE
        }

        if (newStatus != loan.status) {
            repository.updateLoan(loan.copy(status = newStatus))
        }
    }
}
