package com.sbs.loaney.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sbs.loaney.data.local.entity.BankAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY id DESC")
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccountEntity): Long

    @Update
    suspend fun updateBankAccount(account: BankAccountEntity)

    @Delete
    suspend fun deleteBankAccount(account: BankAccountEntity)

    @Query("SELECT * FROM bank_accounts WHERE shareId = :shareId LIMIT 1")
    suspend fun getByShareId(shareId: String): BankAccountEntity?

    @Query("DELETE FROM bank_accounts WHERE shareId = :shareId")
    suspend fun deleteByShareId(shareId: String)
}
