package com.sbs.loaney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deposits")
data class DepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                   // "DPS" or "FDR"
    val bankName: String,
    val monthlyDeposit: Double,         // For DPS only; 0 for FDR
    val principalAmount: Double,        // For FDR = lump sum; For DPS = monthly × tenure
    val annualRate: Double,
    val tenureMonths: Int,
    val maturityAmount: Double,
    val startDate: Long,
    val maturityDate: Long,
    val associatedBankAccountId: Long?,
    val isMatured: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val compoundingsPerYear: Int = 4,   // Quarterly is the norm for Bangladeshi banks
    val taxRatePercent: Double = 10.0   // AIT on interest: 10% with TIN, 15% without
)
