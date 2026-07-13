package com.sbs.loaney.data.local.dao

import androidx.room.*
import com.sbs.loaney.data.local.entity.DepositEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DepositDao {

    @Query("SELECT * FROM deposits ORDER BY createdAt DESC")
    fun getAllDeposits(): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits WHERE isMatured = 0 ORDER BY maturityDate ASC")
    fun getActiveDeposits(): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits WHERE isMatured = 1 ORDER BY maturityDate DESC")
    fun getMaturedDeposits(): Flow<List<DepositEntity>>

    @Query("SELECT * FROM deposits WHERE type = :type ORDER BY createdAt DESC")
    fun getDepositsByType(type: String): Flow<List<DepositEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: DepositEntity): Long

    @Update
    suspend fun updateDeposit(deposit: DepositEntity)

    @Delete
    suspend fun deleteDeposit(deposit: DepositEntity)
}
