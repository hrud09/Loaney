package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.LoanRepository
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
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val userName: String = "Sajibur",
    val currencySymbol: String = "৳"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllLoans(),
        repository.getAllBankAccounts(),
        settingsRepository.userNameFlow,
        settingsRepository.currencySymbolFlow
    ) { loansWithPayments, accounts, name, currency ->
        val summary = calculateSummary(loansWithPayments)
        summary.copy(
            bankAccounts = accounts,
            userName = name,
            currencySymbol = currency
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
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val sevenDaysFromNow = calendar.time

        loans.forEach { item ->
            val loan = item.loan
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
                } else if (loan.promisedReturnDate.before(sevenDaysFromNow)) {
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
            lentLoans = loans.filter { it.loan.type == LoanType.LEND && it.loan.status != LoanStatus.FULLY_PAID },
            borrowedLoans = loans.filter { it.loan.type == LoanType.BORROW && it.loan.status != LoanStatus.FULLY_PAID }
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
}
