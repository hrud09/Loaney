package com.sbs.loaney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountName: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val branchName: String? = null,
    val swiftCode: String? = null,
    val coverImageUri: String? = null, // URI string for the selected cover image
    val isCard: Boolean = false, // Flag indicating if it's a card or a generic bank account
    val isMfs: Boolean = false, // Flag indicating if it's a mobile financial service (bKash, Nagad, etc.)
    val mfsProvider: String? = null, // The provider name (e.g., "bKash", "Nagad", "Rocket")
    val qrCodeUri: String? = null // URI string for the QR code image
)
