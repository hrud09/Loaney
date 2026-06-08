package com.sbs.loaney.data.repository

import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.LoanItemEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanType
import kotlinx.coroutines.flow.Flow

interface ILoanRepository {
    fun getAllLoans(): Flow<List<LoanWithPayments>>
    fun getLoansByType(type: LoanType): Flow<List<LoanWithPayments>>
    fun getLoanById(loanId: Long): Flow<LoanWithPayments?>
    suspend fun insertLoan(loan: LoanEntity): Long
    suspend fun updateLoan(loan: LoanEntity)
    suspend fun softDeleteLoan(loanId: Long, timestamp: Long = System.currentTimeMillis(), notes: String? = null)
    suspend fun restoreLoan(loanId: Long)
    fun getDeletedLoans(): Flow<List<LoanWithPayments>>
    suspend fun deleteLoan(loan: LoanEntity)
    suspend fun deleteExpiredLoans(threshold: Long)
    
    suspend fun insertPayment(payment: PaymentEntity)
    suspend fun deletePayment(payment: PaymentEntity)
    
    suspend fun insertLoanItem(loanItem: LoanItemEntity)
    suspend fun deleteLoanItem(loanItem: LoanItemEntity)
    
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>
    suspend fun insertBankAccount(account: BankAccountEntity): Long
    suspend fun updateBankAccount(account: BankAccountEntity)
    suspend fun deleteBankAccount(account: BankAccountEntity)
}
