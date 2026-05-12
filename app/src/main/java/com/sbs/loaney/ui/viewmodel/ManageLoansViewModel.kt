package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.ILoanRepository
import com.sbs.loaney.data.repository.SettingsRepository
import com.sbs.loaney.data.repository.UserLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/** Status of the email-based counterpart lookup performed when saving a loan. */
enum class EmailLinkStatus {
    /** No email entered or lookup not yet started. */
    IDLE,
    /** Actively querying Firestore. */
    CHECKING,
    /** A registered Loaney account was found for the entered email. */
    FOUND,
    /** No registered account matches the entered email. */
    NOT_FOUND
}

data class ManageLoansUiState(
    val lentLoans: List<LoanWithPayments> = emptyList(),
    val borrowedLoans: List<LoanWithPayments> = emptyList(),
    val isLoading: Boolean = false,
    val currencySymbol: String = "৳",
    /** Populated with the display name of the matched user when status == FOUND. */
    val linkedUserName: String? = null,
    val emailLinkStatus: EmailLinkStatus = EmailLinkStatus.IDLE
)

@HiltViewModel
class ManageLoansViewModel @Inject constructor(
    private val repository: ILoanRepository,
    private val settingsRepository: SettingsRepository,
    private val userLinkRepository: UserLinkRepository
) : ViewModel() {

    // Internal mutable state for email-lookup status
    private val _emailLinkStatus = MutableStateFlow(EmailLinkStatus.IDLE)
    private val _linkedUserName = MutableStateFlow<String?>(null)

    /** Tracks the in-flight email-lookup coroutine so we can cancel on each new keystroke. */
    private var emailLookupJob: Job? = null

    val uiState: StateFlow<ManageLoansUiState> = combine(
        repository.getAllLoans(),
        settingsRepository.currencySymbolFlow,
        _emailLinkStatus,
        _linkedUserName
    ) { allLoans, currency, emailStatus, linkedName ->
        ManageLoansUiState(
            lentLoans = allLoans.filter { it.loan.type == LoanType.LEND },
            borrowedLoans = allLoans.filter { it.loan.type == LoanType.BORROW },
            currencySymbol = currency,
            emailLinkStatus = emailStatus,
            linkedUserName = linkedName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageLoansUiState()
    )

    /**
     * Debounced email lookup — called from the UI as the user types.
     * Cancels any in-flight Firestore query first, then waits 600 ms before querying
     * to avoid firing on every keystroke.
     */
    fun checkEmailLink(email: String) {
        emailLookupJob?.cancel()
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailLinkStatus.value = EmailLinkStatus.IDLE
            _linkedUserName.value = null
            return
        }
        emailLookupJob = viewModelScope.launch {
            _emailLinkStatus.value = EmailLinkStatus.CHECKING
            delay(600L) // debounce: wait for the user to finish typing
            val result = userLinkRepository.lookupUserByEmail(email)
            if (result != null) {
                _linkedUserName.value = result.second
                _emailLinkStatus.value = EmailLinkStatus.FOUND
            } else {
                _linkedUserName.value = null
                _emailLinkStatus.value = EmailLinkStatus.NOT_FOUND
            }
        }
    }

    /** Resets the email lookup state (e.g. when the form is cleared). */
    fun resetEmailLinkStatus() {
        _emailLinkStatus.value = EmailLinkStatus.IDLE
        _linkedUserName.value = null
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
        relationshipType: String? = null,
        witness: String? = null,
        proofUri: String? = null,
        profilePhotoUri: String? = null,
        onSuccess: (Long) -> Unit = {}
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
            val loanId = repository.insertLoan(loan)

            // ── Cross-user notification ──────────────────────────────────────
            // If the email belongs to a registered Loaney user, send them a
            // notification so they see this loan from their side as well.
            if (!email.isNullOrBlank()) {
                val recipientUid = userLinkRepository.lookupUidByEmail(email)
                val currencySymbol = settingsRepository.currencySymbolFlow.first()
                if (recipientUid != null) {
                    userLinkRepository.sendLoanNotification(
                        recipientUid = recipientUid,
                        loanId = loanId,
                        loanType = type.name,
                        amount = amount,
                        currency = currencySymbol,
                        promisedReturnDateMillis = returnDate.time
                    )
                }
            }
            // ────────────────────────────────────────────────────────────────

            // Reset lookup state after submission
            resetEmailLinkStatus()
            onSuccess(loanId)
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
