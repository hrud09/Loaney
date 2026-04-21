package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.LoanItemEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.repository.ILoanRepository
import com.sbs.loaney.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class LoanTrackerUiState(
    val selectedLoan: LoanWithPayments? = null,
    val isLoading: Boolean = false,
    val currencySymbol: String = "৳"
)

enum class DeletionReason {
    PAID_FULLY,
    FORGIVEN,
    MISTAKE,
    OTHER
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LoanTrackerViewModel @Inject constructor(
    private val repository: ILoanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedLoanId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<LoanTrackerUiState> = combine(
        _selectedLoanId.filterNotNull().flatMapLatest { id -> repository.getLoanById(id) },
        settingsRepository.currencySymbolFlow
    ) { loan, currency ->
        LoanTrackerUiState(
            selectedLoan = loan,
            currencySymbol = currency
        )
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoanTrackerUiState()
        )

    fun selectLoan(loanId: Long) {
        _selectedLoanId.value = loanId
    }

    fun addPayment(amount: Double, method: String, note: String?, proofUri: String? = null) {
        val loanId = _selectedLoanId.value ?: return
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

    fun addLoanItem(amount: Double, note: String?, proofUri: String? = null) {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            val loanItem = LoanItemEntity(
                loanId = loanId,
                amount = amount,
                date = Date(),
                note = note,
                proofUri = proofUri
            )
            repository.insertLoanItem(loanItem)
            updateLoanStatus(loanId)
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.softDeleteLoan(loan.id)
        }
    }

    fun deleteLoanWithReason(loan: LoanEntity, reason: DeletionReason, otherReasonText: String? = null) {
        viewModelScope.launch {
            when (reason) {
                DeletionReason.PAID_FULLY -> {
                    repository.updateLoan(loan.copy(
                        status = LoanStatus.FULLY_PAID,
                        removedAt = System.currentTimeMillis()
                    ))
                }
                DeletionReason.FORGIVEN -> {
                    repository.updateLoan(loan.copy(
                        status = LoanStatus.FORGIVEN,
                        removedAt = System.currentTimeMillis()
                    ))
                }
                DeletionReason.MISTAKE -> {
                    repository.softDeleteLoan(loan.id)
                }
                DeletionReason.OTHER -> {
                    val updatedNotes = if (!otherReasonText.isNullOrBlank()) {
                        val currentNotes = loan.notes ?: ""
                        if (currentNotes.isBlank()) "Deletion Reason: $otherReasonText" 
                        else "$currentNotes\nDeletion Reason: $otherReasonText"
                    } else {
                        loan.notes
                    }
                    repository.updateLoan(loan.copy(
                        notes = updatedNotes,
                        removedAt = System.currentTimeMillis()
                    ))
                    repository.softDeleteLoan(loan.id)
                }
            }
        }
    }

    fun markAsSettled() {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            val loanWithPayments = repository.getLoanById(loanId).firstOrNull() ?: return@launch
            val totalLoan = loanWithPayments.loan.amount + loanWithPayments.loanItems.sumOf { it.amount }
            val balance = totalLoan - loanWithPayments.payments.sumOf { it.amount }
            if (balance > 0) {
                // Add a final payment to settle
                addPayment(balance, "Settled", "Final settlement")
            } else {
                repository.updateLoan(loanWithPayments.loan.copy(
                    status = LoanStatus.FULLY_PAID,
                    removedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    fun forgiveLoan() {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            val loanWithPayments = repository.getLoanById(loanId).firstOrNull() ?: return@launch
            repository.updateLoan(loanWithPayments.loan.copy(
                status = LoanStatus.FORGIVEN,
                removedAt = System.currentTimeMillis()
            ))
        }
    }

    fun updateLoan(
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
        relationshipType: String?,
        witness: String?,
        proofUri: String?,
        profilePhotoUri: String?
    ) {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            val currentLoan = repository.getLoanById(loanId).firstOrNull()?.loan ?: return@launch
            val updatedLoan = currentLoan.copy(
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
                relationshipType = relationshipType,
                witness = witness,
                proofUri = proofUri,
                profilePhotoUri = profilePhotoUri
            )
            repository.updateLoan(updatedLoan)
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
            repository.updateLoan(loan.copy(
                status = newStatus,
                removedAt = if (newStatus == LoanStatus.FULLY_PAID) System.currentTimeMillis() else null
            ))
        }
    }
}
