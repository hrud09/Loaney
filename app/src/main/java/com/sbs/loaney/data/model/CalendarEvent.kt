package com.sbs.loaney.data.model

import java.util.Date

enum class CalendarEventType {
    LOAN_INITIATION,
    DEADLINE,
    PARTIAL_PAYMENT
}

data class CalendarEvent(
    val type: CalendarEventType,
    val date: Date,
    val amount: Double,
    val personName: String,
    val loanId: Long,
    val loanType: LoanType,
    val paymentId: Long? = null
)
