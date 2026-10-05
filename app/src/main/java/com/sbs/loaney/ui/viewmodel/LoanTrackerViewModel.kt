package com.sbs.loaney.ui.viewmodel

import com.sbs.loaney.util.AnalyticsHelper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.LoanItemEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.RecoveryRequest
import com.sbs.loaney.data.repository.ILoanRepository
import com.sbs.loaney.data.repository.RecoveryRepository
import com.sbs.loaney.data.repository.SettingsRepository
import com.sbs.loaney.data.repository.UserLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class LoanTrackerUiState(
    val selectedLoan: LoanWithPayments? = null,
    val isLoading: Boolean = false,
    val currencySymbol: String = "৳",
    val userName: String = "",
    /** Existing assisted-recovery request for the selected loan, if any. */
    val recoveryRequest: RecoveryRequest? = null
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
    private val settingsRepository: SettingsRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val userLinkRepository: UserLinkRepository,
    private val recoveryRepository: RecoveryRepository
) : ViewModel() {

    private val _selectedLoanId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<LoanTrackerUiState> = combine(
        _selectedLoanId.filterNotNull().flatMapLatest { id -> repository.getLoanById(id) },
        settingsRepository.currencySymbolFlow,
        settingsRepository.userNameFlow,
        _selectedLoanId.filterNotNull().flatMapLatest { id -> recoveryRepository.observeRequestForLoan(id) }
    ) { loan, currency, userName, recovery ->
        LoanTrackerUiState(
            selectedLoan = loan,
            currencySymbol = currency,
            userName = userName,
            recoveryRequest = recovery
        )
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoanTrackerUiState()
        )

    fun selectLoan(loanId: Long) {
        _selectedLoanId.value = loanId
    }

    /** Marks the forced first-loan onboarding complete once the feature tour finishes. */
    fun markTutorialSeen() {
        viewModelScope.launch {
            settingsRepository.setHasSeenTutorial(true)
        }
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
            analyticsHelper.logPaymentCompleted(amount)
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

    /** Opt this loan in or out of automatic due-date reminder emails to the borrower. */
    fun setAutoRemind(enabled: Boolean) {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            val current = repository.getLoanById(loanId).firstOrNull() ?: return@launch
            repository.updateLoan(current.loan.copy(autoRemindEnabled = enabled))
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.softDeleteLoan(loan.id)
        }
    }

    // ── Assisted recovery ────────────────────────────────────────────────────────────────────
    // Records the lender's request only. While RecoveryConfig.IS_LIVE is false, nothing is sent to
    // the borrower — the gate is enforced inside RecoveryRepository. See RecoveryRequest header.

    /** Outstanding balance on the currently selected loan (used for the fee estimate + eligibility). */
    fun outstandingBalance(loan: LoanWithPayments): Double {
        val total = loan.loan.amount + loan.loanItems.sumOf { it.amount }
        val paid = loan.payments.sumOf { it.amount }
        return (total - paid).coerceAtLeast(0.0)
    }

    /** Whole days a loan is past its promised return date (0 if not yet due). */
    fun daysOverdue(loan: LoanEntity): Int {
        val diff = System.currentTimeMillis() - loan.promisedReturnDate.time
        return (diff / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }

    fun requestRecovery(
        note: String,
        onResult: (RecoveryRepository.SubmitOutcome) -> Unit
    ) {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            val lwp = repository.getLoanById(loanId).firstOrNull()
            if (lwp == null) {
                onResult(RecoveryRepository.SubmitOutcome.Error("Loan not found"))
                return@launch
            }
            val loan = lwp.loan
            val currency = settingsRepository.currencySymbolFlow.first()
            val request = RecoveryRequest(
                loanId = loan.id,
                borrowerName = loan.personName,
                borrowerPhone = loan.phoneNumber,
                borrowerEmail = loan.email ?: "",
                outstandingAmount = outstandingBalance(lwp),
                currency = currency,
                daysOverdue = daysOverdue(loan),
                borrowerConfirmed = loan.linkedOwnerUid != null,
                hasProof = !loan.proofUri.isNullOrBlank(),
                hasWitness = !loan.witness.isNullOrBlank(),
                paymentCount = lwp.payments.size,
                lenderNote = note.trim()
            )
            onResult(recoveryRepository.submitRequest(request))
        }
    }

    fun cancelRecovery() {
        val loanId = _selectedLoanId.value ?: return
        viewModelScope.launch {
            recoveryRepository.cancelRequest(loanId)
        }
    }

    fun deleteLoanWithReason(
        loan: LoanEntity,
        reason: DeletionReason,
        otherReasonText: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
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
                        repository.softDeleteLoan(loan.id, notes = updatedNotes)
                    }
                }
            } finally {
                onComplete()
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
            
            if (currentLoan.linkedOwnerUid != null) {
                // If it's a linked loan, propose changes instead of immediate save
                val pendingJson = Gson().toJson(updatedLoan)
                val newLoan = currentLoan.copy(pendingUpdateJson = pendingJson)
                repository.updateLoan(newLoan)
                
                // Send notification to the linked user
                userLinkRepository.sendLoanSyncNotification(
                    recipientUid = currentLoan.linkedOwnerUid,
                    notificationId = "update_${System.currentTimeMillis()}",
                    loanType = "UPDATE_PROPOSAL", // Sender proposes update
                    notificationType = "UPDATE_PROPOSAL",
                    proposedChangesJson = pendingJson,
                    senderLoanId = currentLoan.id.toString(),
                    recipientLoanId = currentLoan.linkedLoanId,
                    amount = updatedLoan.amount
                )
            } else {
                repository.updateLoan(updatedLoan)
                updateLoanStatus(loanId)
            }
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
