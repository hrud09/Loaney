package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.ILoanRepository
import com.sbs.loaney.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sbs.loaney.data.local.entity.BankAccountEntity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import com.sbs.loaney.data.model.CalendarEvent
import com.sbs.loaney.data.model.CalendarEventType
import java.text.SimpleDateFormat
import java.util.Locale

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val overdueAmount: Double = 0.0,
    val overdueCount: Int = 0,
    val dueSoonCount: Int = 0,
    val lentLoans: List<LoanWithPayments> = emptyList(),
    val borrowedLoans: List<LoanWithPayments> = emptyList(),
    val upcomingDeadlines: List<LoanWithPayments> = emptyList(),
    val allEvents: Map<String, List<CalendarEvent>> = emptyMap(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val userName: String = "Sajibur",
    val currencySymbol: String = "৳",
    val userProfilePhoto: String? = null,
    val hasSeenTutorial: Boolean = true // Default true to avoid showing it while loading
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ILoanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllLoans(),
        repository.getAllBankAccounts(),
        settingsRepository.userNameFlow,
        settingsRepository.currencySymbolFlow,
        settingsRepository.userProfilePhotoFlow,
        settingsRepository.hasSeenTutorialFlow
    ) { args ->
        val loansWithPayments = args[0] as List<LoanWithPayments>
        val accounts = args[1] as List<BankAccountEntity>
        val name = args[2] as String
        val currency = args[3] as String
        val photo = args[4] as String?
        val hasSeen = args[5] as Boolean
        
        val summary = calculateSummary(loansWithPayments)
        summary.copy(
            bankAccounts = accounts,
            userName = name,
            currencySymbol = currency,
            userProfilePhoto = photo,
            hasSeenTutorial = hasSeen
        )
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    private fun calculateSummary(loans: List<LoanWithPayments>): HomeUiState {
        var totalLentBalance = 0.0
        var totalBorrowedBalance = 0.0
        var pendingNet = 0.0
        var overdueAmount = 0.0
        var overdueCount = 0
        var dueSoonCount = 0

        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 30) // Show up to 30 days in calendar visibility for home, but we want all events for the popup
        val thirtyDaysFromNow = calendar.time

        val eventMap = mutableMapOf<String, MutableList<CalendarEvent>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        loans.forEach { item ->
            val loan = item.loan
            
            // Initiation Event
            val initDateStr = dateFormat.format(loan.loanDate)
            eventMap.getOrPut(initDateStr) { mutableListOf() }.add(
                CalendarEvent(
                    type = CalendarEventType.LOAN_INITIATION,
                    date = loan.loanDate,
                    amount = loan.amount,
                    personName = loan.personName,
                    loanId = loan.id,
                    loanType = loan.type
                )
            )

            // Deadline Event
            val deadlineDateStr = dateFormat.format(loan.promisedReturnDate)
            eventMap.getOrPut(deadlineDateStr) { mutableListOf() }.add(
                CalendarEvent(
                    type = CalendarEventType.DEADLINE,
                    date = loan.promisedReturnDate,
                    amount = loan.amount,
                    personName = loan.personName,
                    loanId = loan.id,
                    loanType = loan.type
                )
            )

            // Payment Events
            item.payments.forEach { payment ->
                val paymentDateStr = dateFormat.format(payment.date)
                eventMap.getOrPut(paymentDateStr) { mutableListOf() }.add(
                    CalendarEvent(
                        type = CalendarEventType.PARTIAL_PAYMENT,
                        date = payment.date,
                        amount = payment.amount,
                        personName = loan.personName,
                        loanId = loan.id,
                        loanType = loan.type,
                        paymentId = payment.id
                    )
                )
            }

            // Skip forgiven and fully paid loans for balance calculations
            if (loan.status == LoanStatus.FORGIVEN || loan.status == LoanStatus.FULLY_PAID) return@forEach

            val totalLoan = loan.amount + item.loanItems.sumOf { it.amount }
            val paid = item.payments.sumOf { it.amount }
            val balance = (totalLoan - paid).coerceAtLeast(0.0)

            if (loan.type == LoanType.LEND) {
                totalLentBalance += balance
                pendingNet += balance
            } else {
                totalBorrowedBalance += balance
                pendingNet -= balance
            }

            if (balance > 0) {
                if (now.after(loan.promisedReturnDate)) {
                    overdueAmount += balance
                    overdueCount++
                } else if (loan.promisedReturnDate.before(thirtyDaysFromNow)) {
                    dueSoonCount++
                }
            }
        }

        return HomeUiState(
            isLoading = false,
            totalLent = totalLentBalance,
            totalBorrowed = totalBorrowedBalance,
            pendingAmount = pendingNet,
            overdueAmount = overdueAmount,
            overdueCount = overdueCount,
            dueSoonCount = dueSoonCount,
            lentLoans = loans.filter { it.loan.type == LoanType.LEND && it.loan.status != LoanStatus.FULLY_PAID && it.loan.status != LoanStatus.FORGIVEN },
            borrowedLoans = loans.filter { it.loan.type == LoanType.BORROW && it.loan.status != LoanStatus.FULLY_PAID && it.loan.status != LoanStatus.FORGIVEN },
            upcomingDeadlines = loans.filter { item ->
                val loan = item.loan
                if (loan.status == LoanStatus.FORGIVEN || loan.status == LoanStatus.FULLY_PAID) return@filter false
                
                val totalLoan = loan.amount + item.loanItems.sumOf { it.amount }
                val paid = item.payments.sumOf { it.amount }
                val balance = (totalLoan - paid).coerceAtLeast(0.0)
                
                balance > 0 && loan.promisedReturnDate.before(thirtyDaysFromNow)
            }.sortedBy { it.loan.promisedReturnDate },
            allEvents = eventMap.mapValues { it.value.sortedBy { event -> event.date } }
        )
    }

    fun addBankAccount(
        accountName: String,
        accountNumber: String,
        bankName: String,
        branchName: String?,
        swiftCode: String?,
        coverImageUri: String?,
        isCard: Boolean = false,
        isMfs: Boolean = false,
        mfsProvider: String? = null,
        qrCodeUri: String? = null
    ) {
        viewModelScope.launch {
            val account = BankAccountEntity(
                accountName = accountName,
                accountNumber = accountNumber,
                bankName = bankName,
                branchName = branchName,
                swiftCode = swiftCode,
                coverImageUri = coverImageUri,
                isCard = isCard,
                isMfs = isMfs,
                mfsProvider = mfsProvider,
                qrCodeUri = qrCodeUri
            )
            repository.insertBankAccount(account)
        }
    }

    fun deleteBankAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.deleteBankAccount(account)
        }
    }

    fun updateBankAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            repository.updateBankAccount(account)
        }
    }

    fun setHasSeenTutorial(completed: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHasSeenTutorial(completed)
        }
    }
}
