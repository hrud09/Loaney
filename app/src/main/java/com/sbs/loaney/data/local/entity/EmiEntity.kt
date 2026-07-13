package com.sbs.loaney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emis")
data class EmiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String = "",
    val totalAmount: Double = 0.0,
    val downPayment: Double = 0.0,
    val tenureMonths: Int = 12,
    val interestRate: Double = 0.0,
    val monthlyInstallment: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val dueDateDay: Int = 5,
    val associatedBankAccountId: Long? = null,
    val paymentsPaid: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
