package com.sbs.loaney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import java.util.Date

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LoanType,
    val personName: String,
    val phoneNumber: String,
    val email: String? = null,
    val address: String? = null,
    val amount: Double,
    val loanDate: Date,
    val promisedReturnDate: Date,
    val purpose: String?,
    val notes: String?,
    val interest: Double?,
    val proofUri: String? = null,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val relationshipType: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
