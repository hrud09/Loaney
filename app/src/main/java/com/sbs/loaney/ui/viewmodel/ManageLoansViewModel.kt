package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.LoanRepository
import com.sbs.loaney.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ManageLoansUiState(
    val lentLoans: List<LoanWithPayments> = emptyList(),
    val borrowedLoans: List<LoanWithPayments> = emptyList(),
    val isLoading: Boolean = false,
    val currencySymbol: String = "৳"
)

@HiltViewModel
class ManageLoansViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<ManageLoansUiState> = combine(
        repository.getAllLoans(),
        settingsRepository.currencySymbolFlow
    ) { allLoans, currency ->
        ManageLoansUiState(
            lentLoans = allLoans.filter { it.loan.type == LoanType.LEND },
            borrowedLoans = allLoans.filter { it.loan.type == LoanType.BORROW },
            currencySymbol = currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageLoansUiState()
    )

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
        relationshipType: String? = null,
        witness: String? = null,
        proofUri: String? = null,
        profilePhotoUri: String? = null
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
                profilePhotoUri = profilePhotoUri,
                relationshipType = relationshipType,
                witness = witness,
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
        val totalLoan = loanWithPayments.loan.amount + loanWithPayments.loanItems.sumOf { it.amount }
        val totalPaid = loanWithPayments.payments.sumOf { it.amount }
        val loan = loanWithPayments.loan
        
        val newStatus = when {
            totalPaid >= totalLoan -> LoanStatus.FULLY_PAID
            totalPaid > 0 -> LoanStatus.PARTIALLY_PAID
            Date().after(loan.promisedReturnDate) -> LoanStatus.OVERDUE
            else -> LoanStatus.ACTIVE
        }
        
        if (newStatus != loan.status) {
            repository.updateLoan(loan.copy(status = newStatus))
        }
    }
}
