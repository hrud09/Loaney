package com.sbs.loaney

import android.app.Application
import com.sbs.loaney.notification.LoanReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LoaneyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create notification channel
        LoanReminderWorker.createNotificationChannel(this)
        // Schedule daily loan reminder check
        LoanReminderWorker.schedule(this)
    }
}
