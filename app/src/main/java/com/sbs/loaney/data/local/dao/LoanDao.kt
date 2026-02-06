package com.sbs.loaney.data.local.dao

import androidx.room.*
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanType
import kotlinx.coroutines.flow.Flow

data class LoanWithPayments(
    @Embedded val loan: LoanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "loanId"
    )
    val payments: List<PaymentEntity>
)

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY createdAt DESC")
    fun getAllLoans(): Flow<List<LoanWithPayments>>

    @Query("SELECT * FROM loans WHERE type = :type ORDER BY createdAt DESC")
    fun getLoansByType(type: LoanType): Flow<List<LoanWithPayments>>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    fun getLoanById(loanId: Long): Flow<LoanWithPayments?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)
}
