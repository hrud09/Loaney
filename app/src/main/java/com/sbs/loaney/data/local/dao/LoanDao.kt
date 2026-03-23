package com.sbs.loaney.data.local.dao

import androidx.room.*
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.LoanItemEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanType
import kotlinx.coroutines.flow.Flow

data class LoanWithPayments(
    @Embedded val loan: LoanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "loanId"
    )
    val payments: List<PaymentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "loanId"
    )
    val loanItems: List<LoanItemEntity> = emptyList()
)

@Dao
interface LoanDao {
    @Transaction
    @Query("SELECT * FROM loans WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllLoans(): Flow<List<LoanWithPayments>>

    @Transaction
    @Query("SELECT * FROM loans WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllLoansOnce(): List<LoanWithPayments>

    @Transaction
    @Query("SELECT * FROM loans WHERE type = :type AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getLoansByType(type: LoanType): Flow<List<LoanWithPayments>>

    @Transaction
    @Query("SELECT * FROM loans WHERE id = :loanId AND isDeleted = 0")
    fun getLoanById(loanId: Long): Flow<LoanWithPayments?>

    @Transaction
    @Query("SELECT * FROM loans WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedLoans(): Flow<List<LoanWithPayments>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("UPDATE loans SET isDeleted = 1 WHERE id = :loanId")
    suspend fun softDeleteLoan(loanId: Long)

    @Query("UPDATE loans SET isDeleted = 0 WHERE id = :loanId")
    suspend fun restoreLoan(loanId: Long)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanItem(loanItem: LoanItemEntity)

    @Delete
    suspend fun deleteLoanItem(loanItem: LoanItemEntity)
}
