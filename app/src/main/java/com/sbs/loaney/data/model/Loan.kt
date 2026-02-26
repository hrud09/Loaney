package com.sbs.loaney.data.model

import java.util.Date

enum class LoanType {
    LEND,
    BORROW
}

enum class LoanStatus {
    ACTIVE,
    PARTIALLY_PAID,
    FULLY_PAID,
    OVERDUE
}

data class Loan(
    val id: Long = 0,
    val type: LoanType,
    val personName: String,
    val phoneNumber: String,
    val amount: Double,
    val loanDate: Date,
    val promisedReturnDate: Date,
    val purpose: String?,
    val notes: String?,
    val interest: Double?,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val email: String? = null,
    val address: String? = null
)
