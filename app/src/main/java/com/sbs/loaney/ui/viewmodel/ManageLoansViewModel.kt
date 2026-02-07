package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ManageLoansUiState(
    val loans: List<LoanWithPayments> = emptyList(),
    val selectedType: LoanType = LoanType.LEND,
    val isLoading: Boolean = false
)

@HiltViewModel
class ManageLoansViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _selectedType = MutableStateFlow(LoanType.LEND)
    val selectedType: StateFlow<LoanType> = _selectedType.asStateFlow()

    val uiState: StateFlow<ManageLoansUiState> = combine(
        repository.getAllLoans(),
        _selectedType
    ) { allLoans, type ->
        ManageLoansUiState(
            loans = allLoans.filter { it.loan.type == type },
            selectedType = type
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageLoansUiState()
    )

    fun setLoanType(type: LoanType) {
        _selectedType.value = type
    }

    fun addLoan(
        type: LoanType,
        name: String,
        phone: String,
        email: String?,
        address: String?,
        amount: Double,
        loanDate: Date,
        returnDate: Date,
        purpose: String?,
        notes: String?,
        interest: Double?,
        proofUri: String? = null
    ) {
        viewModelScope.launch {
            val loan = LoanEntity(
                type = type,
                personName = name,
                phoneNumber = phone,
                email = email,
                address = address,
                amount = amount,
                loanDate = loanDate,
                promisedReturnDate = returnDate,
                purpose = purpose,
                notes = notes,
                interest = interest,
                proofUri = proofUri,
                status = LoanStatus.ACTIVE
            )
            repository.insertLoan(loan)
        }
    }

    fun addPayment(loanId: Long, amount: Double, method: String, note: String?, proofUri: String? = null) {
        viewModelScope.launch {
            val payment = PaymentEntity(
                loanId = loanId,
                amount = amount,
                date = Date(),
                method = method,
                note = note,
                proofUri = proofUri
            )
            repository.insertPayment(payment)
            updateLoanStatus(loanId)
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
