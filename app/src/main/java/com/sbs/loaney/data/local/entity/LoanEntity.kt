package com.sbs.loaney.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import java.util.Date

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LoanType = LoanType.LEND,
    val personName: String = "",
    val phoneNumber: String = "",
    val email: String? = null,
    val address: String? = null,
    val amount: Double = 0.0,
    val loanDate: Date = Date(),
    val promisedReturnDate: Date = Date(),
    val purpose: String? = null,
    val notes: String? = null,
    val interest: Double? = null,
    val proofUri: String? = null,
    val profilePhotoUri: String? = null,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val relationshipType: String? = null,
    val witness: String? = null,
    @ColumnInfo(name = "isDeleted") val deleted: Boolean = false,
    val removedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
