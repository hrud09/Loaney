package com.sbs.loaney

import android.app.Application
import com.sbs.loaney.data.local.AppDatabase
import com.sbs.loaney.notification.LoanReminderWorker
import com.sbs.loaney.widget.refreshLoaneyWidgets
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@HiltAndroidApp
class LoaneyApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Create notification channel
        LoanReminderWorker.createNotificationChannel(this)
        // Schedule daily loan reminder check
        LoanReminderWorker.schedule(this)

        keepWidgetsInSync()
    }

    /**
     * Redraw the home screen widget whenever loan data changes. Watching the Room Flow in one
     * place beats calling refresh from each of the ten repository mutations — those are easy to
     * forget, and a widget showing a stale balance is worse than no widget.
     *
     * Nothing but this app writes loan balances, so there is no case where data changes while
     * the process is dead and the widget silently goes stale.
     */
    private fun keepWidgetsInSync() {
        appScope.launch {
            AppDatabase.getDatabase(this@LoaneyApplication)
                .loanDao()
                .getAllLoans()
                .drop(1) // the first emission is just the current state; nothing changed yet
                .distinctUntilChanged()
                .collect {
                    refreshLoaneyWidgets(this@LoaneyApplication)
                }
        }
    }
}
