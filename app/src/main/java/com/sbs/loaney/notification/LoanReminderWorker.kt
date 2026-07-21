package com.sbs.loaney.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.sbs.loaney.MainActivity
import com.sbs.loaney.R
import kotlinx.coroutines.tasks.await
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.SettingsRepository
import com.sbs.loaney.data.repository.UserLinkRepository
import com.sbs.loaney.data.repository.dataStore
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class LoanReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "loan_reminders"
        const val CHANNEL_NAME = "Loan Reminders"
        const val WORK_NAME = "loan_reminder_check"

        /**
         * Don't email the same borrower every single morning. Once every 3 days is enough to
         * be useful without turning an automated nudge into harassment.
         */
        private const val REMINDER_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for upcoming and overdue loan deadlines"
                    enableVibration(true)
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LoanReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        // Schedule to run at 9 AM each day
        private fun calculateInitialDelay(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }

    override suspend fun doWork(): Result {
        // Check if notifications are enabled
        val settingsRepository = SettingsRepository(applicationContext.dataStore)
        val notificationsEnabled = settingsRepository.notificationsEnabledFlow.first()
        if (!notificationsEnabled) return Result.success()

        val currencySymbol = settingsRepository.currencySymbolFlow.first()

        val db = com.sbs.loaney.data.local.AppDatabase.getDatabase(applicationContext)
        val loansWithPayments = try {
            db.loanDao().getAllLoansOnce()
        } catch (e: Exception) {
            return Result.failure()
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val tomorrow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }.time

        // UserLinkRepository has a no-arg constructor, so the plain (non-Hilt) WorkManager
        // factory used for this worker can build it directly.
        val userLinkRepository = UserLinkRepository()
        val now = System.currentTimeMillis()

        var notificationId = 1000

        for (item in loansWithPayments) {
            val loan = item.loan
            // Skip fully paid or deleted loans
            if (loan.status == LoanStatus.FULLY_PAID || loan.deleted) continue

            val currentLoanItems = item.loanItems
            val currentPayments = item.payments

            val totalLoan = loan.amount + currentLoanItems.sumOf { it.amount }
            val paid = currentPayments.sumOf { it.amount }
            val remaining = totalLoan - paid
            if (remaining <= 0) continue

            val deadline = loan.promisedReturnDate
            val loanTypeLabel = if (loan.type == LoanType.LEND) "lent to" else "borrowed from"

            val deadlineCal = Calendar.getInstance().apply { time = deadline }
            val tomorrowCal = Calendar.getInstance().apply { time = tomorrow }

            val isDueTomorrow = deadlineCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                    deadlineCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
            val isOverdue = deadline.before(today)
            val daysOverdue = if (isOverdue) {
                ((today.time - deadline.time) / (1000 * 60 * 60 * 24)).toInt()
            } else 0

            if (!isDueTomorrow && !isOverdue) continue

            // We can only chase the other party on money we LENT. On a BORROW, the person
            // who owes is the user themselves, so there is nobody to remind but them.
            val canChase = loan.type == LoanType.LEND &&
                    (loan.phoneNumber.isNotBlank() || !loan.email.isNullOrBlank())

            val amountText = "$currencySymbol${String.format("%,.0f", remaining)}"

            if (isDueTomorrow) {
                val title = "⏰ Loan Due Tomorrow"
                val message = "$amountText $loanTypeLabel ${loan.personName} is due tomorrow!"
                sendNotification(
                    id = notificationId++,
                    title = title,
                    message = message,
                    remindLoanId = if (canChase) loan.id else null
                )
                userLinkRepository.backupSystemNotification(
                    notificationId = "due_tomorrow_${loan.id}_${today.time}",
                    title = title,
                    message = message,
                    loanId = loan.id
                )
            }

            if (isOverdue) {
                val title = "🚨 Overdue Loan"
                val message = "$amountText $loanTypeLabel ${loan.personName} is $daysOverdue day${if (daysOverdue > 1) "s" else ""} overdue!"
                sendNotification(
                    id = notificationId++,
                    title = title,
                    message = message,
                    remindLoanId = if (canChase) loan.id else null
                )
                userLinkRepository.backupSystemNotification(
                    notificationId = "overdue_${loan.id}_${today.time}",
                    title = title,
                    message = message,
                    loanId = loan.id
                )
            }

            // Opt-in automatic email to the borrower. Never fires unless the owner turned it
            // on for this specific loan, and never more than once per cooldown window.
            val email = loan.email
            val cooledDown = (loan.lastReminderSentAt ?: 0L) + REMINDER_COOLDOWN_MS <= now
            val hasIdentifier = !email.isNullOrBlank() || loan.phoneNumber.isNotBlank()
            
            if (loan.type == LoanType.LEND && loan.autoRemindEnabled && hasIdentifier && cooledDown) {
                if (!email.isNullOrBlank()) {
                    userLinkRepository.sendReminderEmail(
                        recipientEmail = email,
                        amount = remaining,
                        currency = currencySymbol,
                        dueDateMillis = deadline.time,
                        daysOverdue = daysOverdue
                    )
                }

                // If they're a Loaney user too, put it in their app as well.
                userLinkRepository.lookupUid(email = email, phone = loan.phoneNumber)?.let { uid ->
                    userLinkRepository.sendReminderNotification(
                        recipientUid = uid,
                        loanId = loan.id,
                        amount = remaining,
                        currency = currencySymbol,
                        dueDateMillis = deadline.time
                    )
                }

                db.loanDao().updateLoan(loan.copy(lastReminderSentAt = now))
            }
        }

        return Result.success()
    }

    /**
     * @param remindLoanId when non-null, adds a "Send reminder" action that deep-links into
     *        that loan and opens the channel picker, so chasing someone is one tap from the
     *        notification rather than five taps into the app.
     */
    private fun sendNotification(id: Int, title: String, message: String, remindLoanId: Long?) {
        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext, id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        if (remindLoanId != null) {
            val remindIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(MainActivity.EXTRA_REMIND_LOAN_ID, remindLoanId)
            }
            // Distinct request code, otherwise FLAG_UPDATE_CURRENT would overwrite the
            // content intent above and both taps would land in the same place.
            val remindPendingIntent = PendingIntent.getActivity(
                applicationContext, 500_000 + remindLoanId.toInt(), remindIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Send reminder", remindPendingIntent)
        }

        try {
            NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
            com.google.firebase.analytics.FirebaseAnalytics.getInstance(applicationContext)
                .logEvent("reminder_notification_sent", null)
        } catch (e: SecurityException) {
            // Permission not granted — silently skip
        }
    }
}
