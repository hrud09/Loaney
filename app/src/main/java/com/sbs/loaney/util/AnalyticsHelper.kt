package com.sbs.loaney.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    fun logEvent(eventName: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(eventName, params)
    }

    fun logLoanCreated(amount: Double, type: String) {
        val bundle = Bundle().apply {
            putDouble("loan_amount", amount)
            putString("loan_type", type)
        }
        logEvent("loan_created", bundle)
    }
    
    fun logPaymentCompleted(amount: Double) {
        val bundle = Bundle().apply {
            putDouble("payment_amount", amount)
        }
        logEvent("payment_completed", bundle)
    }

    fun logBankAccountAdded(isCard: Boolean, isMfs: Boolean) {
        val bundle = Bundle().apply {
            val accountType = when {
                isCard -> "card"
                isMfs -> "mfs"
                else -> "bank"
            }
            putString("account_type", accountType)
        }
        logEvent("bank_account_added", bundle)
    }
}
