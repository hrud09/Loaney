package com.sbs.loaney.data.repository

import com.sbs.loaney.data.local.dao.LoanDao
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.LoanItemEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.local.dao.BankAccountDao
import com.sbs.loaney.data.model.LoanType
import kotlinx.coroutines.flow.Flow

class LoanRepository(
    private val loanDao: LoanDao,
    private val bankAccountDao: BankAccountDao
) {
    fun getAllLoans(): Flow<List<LoanWithPayments>> = loanDao.getAllLoans()

    fun getLoansByType(type: LoanType): Flow<List<LoanWithPayments>> = loanDao.getLoansByType(type)

    fun getLoanById(loanId: Long): Flow<LoanWithPayments?> = loanDao.getLoanById(loanId)

    suspend fun insertLoan(loan: LoanEntity): Long = loanDao.insertLoan(loan)

    suspend fun updateLoan(loan: LoanEntity) = loanDao.updateLoan(loan)

    suspend fun softDeleteLoan(loanId: Long) = loanDao.softDeleteLoan(loanId)

    suspend fun restoreLoan(loanId: Long) = loanDao.restoreLoan(loanId)

    fun getDeletedLoans(): Flow<List<LoanWithPayments>> = loanDao.getDeletedLoans()

    suspend fun deleteLoan(loan: LoanEntity) = loanDao.deleteLoan(loan)

    suspend fun insertPayment(payment: PaymentEntity) = loanDao.insertPayment(payment)

    suspend fun deletePayment(payment: PaymentEntity) = loanDao.deletePayment(payment)

    suspend fun insertLoanItem(loanItem: LoanItemEntity) = loanDao.insertLoanItem(loanItem)

    suspend fun deleteLoanItem(loanItem: LoanItemEntity) = loanDao.deleteLoanItem(loanItem)

    fun getAllBankAccounts(): Flow<List<BankAccountEntity>> = bankAccountDao.getAllBankAccounts()

    suspend fun insertBankAccount(account: BankAccountEntity): Long = bankAccountDao.insertBankAccount(account)

    suspend fun updateBankAccount(account: BankAccountEntity) = bankAccountDao.updateBankAccount(account)

    suspend fun deleteBankAccount(account: BankAccountEntity) = bankAccountDao.deleteBankAccount(account)
}
