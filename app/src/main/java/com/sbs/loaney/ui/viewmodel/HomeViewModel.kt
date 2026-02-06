package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

data class HomeUiState(
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val overdueAmount: Double = 0.0,
    val overdueCount: Int = 0,
    val dueSoonCount: Int = 0,
    val lentLoans: List<LoanWithPayments> = emptyList(),
    val borrowedLoans: List<LoanWithPayments> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.getAllLoans()
        .map { loansWithPayments ->
            calculateSummary(loansWithPayments)
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
            val paid = item.payments.sumOf { it.amount }
            val balance = (loan.amount - paid).coerceAtLeast(0.0)

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
}
