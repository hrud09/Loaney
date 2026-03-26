package com.sbs.loaney.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "loan_items",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("loanId")]
)
data class LoanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long = 0,
    val amount: Double = 0.0,
    val date: Date = Date(),
    val note: String? = null,
    val proofUri: String? = null
)
