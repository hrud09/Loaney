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
) : ILoanRepository {
    override fun getAllLoans(): Flow<List<LoanWithPayments>> = loanDao.getAllLoans()

    override fun getLoansByType(type: LoanType): Flow<List<LoanWithPayments>> = loanDao.getLoansByType(type)

    override fun getLoanById(loanId: Long): Flow<LoanWithPayments?> = loanDao.getLoanById(loanId)

    override suspend fun insertLoan(loan: LoanEntity): Long = loanDao.insertLoan(loan)

    override suspend fun updateLoan(loan: LoanEntity) = loanDao.updateLoan(loan)

    override suspend fun softDeleteLoan(loanId: Long, timestamp: Long) = loanDao.softDeleteLoan(loanId, timestamp)

    override suspend fun restoreLoan(loanId: Long) = loanDao.restoreLoan(loanId)

    override fun getDeletedLoans(): Flow<List<LoanWithPayments>> = loanDao.getDeletedLoans()

    override suspend fun deleteLoan(loan: LoanEntity) = loanDao.deleteLoan(loan)

    override suspend fun deleteExpiredLoans(threshold: Long) = loanDao.deleteExpiredLoans(threshold)

    override suspend fun insertPayment(payment: PaymentEntity) = loanDao.insertPayment(payment)

    override suspend fun deletePayment(payment: PaymentEntity) = loanDao.deletePayment(payment)

    override suspend fun insertLoanItem(loanItem: LoanItemEntity) = loanDao.insertLoanItem(loanItem)

    override suspend fun deleteLoanItem(loanItem: LoanItemEntity) = loanDao.deleteLoanItem(loanItem)

    override fun getAllBankAccounts(): Flow<List<BankAccountEntity>> = bankAccountDao.getAllBankAccounts()

    override suspend fun insertBankAccount(account: BankAccountEntity): Long = bankAccountDao.insertBankAccount(account)

    override suspend fun updateBankAccount(account: BankAccountEntity) = bankAccountDao.updateBankAccount(account)

    override suspend fun deleteBankAccount(account: BankAccountEntity) = bankAccountDao.deleteBankAccount(account)
}
