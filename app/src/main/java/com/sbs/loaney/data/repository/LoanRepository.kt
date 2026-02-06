package com.sbs.loaney.data.repository

import com.sbs.loaney.data.local.dao.LoanDao
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanType
import kotlinx.coroutines.flow.Flow

class LoanRepository(private val loanDao: LoanDao) {
    fun getAllLoans(): Flow<List<LoanWithPayments>> = loanDao.getAllLoans()

    fun getLoansByType(type: LoanType): Flow<List<LoanWithPayments>> = loanDao.getLoansByType(type)

    fun getLoanById(loanId: Long): Flow<LoanWithPayments?> = loanDao.getLoanById(loanId)

    suspend fun insertLoan(loan: LoanEntity): Long = loanDao.insertLoan(loan)

    suspend fun updateLoan(loan: LoanEntity) = loanDao.updateLoan(loan)

    suspend fun deleteLoan(loan: LoanEntity) = loanDao.deleteLoan(loan)

    suspend fun insertPayment(payment: PaymentEntity) = loanDao.insertPayment(payment)

    suspend fun deletePayment(payment: PaymentEntity) = loanDao.deletePayment(payment)
}
