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
import com.sbs.loaney.data.local.AppDatabase
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.SettingsRepository
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

        val db = AppDatabase.getDatabase(applicationContext)
        val loans = db.loanDao().getAllLoansOnce()

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

        var notificationId = 1000

        for (loanWithPayments in loans) {
            val loan = loanWithPayments.loan
            // Skip fully paid loans
            if (loan.status == LoanStatus.FULLY_PAID) continue

            val totalLoan = loan.amount + loanWithPayments.loanItems.sumOf { it.amount }
            val paid = loanWithPayments.payments.sumOf { it.amount }
            val remaining = totalLoan - paid
            if (remaining <= 0) continue

            val deadline = loan.promisedReturnDate
            val loanTypeLabel = if (loan.type == LoanType.LEND) "lent to" else "borrowed from"

            // Deadline is tomorrow → "due tomorrow" notification
            val deadlineCal = Calendar.getInstance().apply { time = deadline }
            val tomorrowCal = Calendar.getInstance().apply { time = tomorrow }
            
            if (deadlineCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                deadlineCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
            ) {
                sendNotification(
                    id = notificationId++,
                    title = "⏰ Loan Due Tomorrow",
                    message = "${currencySymbol}${String.format("%,.0f", remaining)} $loanTypeLabel ${loan.personName} is due tomorrow!"
                )
            }

            // Deadline has passed → "overdue" notification
            if (deadline.before(today)) {
                val daysOverdue = ((today.time - deadline.time) / (1000 * 60 * 60 * 24)).toInt()
                sendNotification(
                    id = notificationId++,
                    title = "🚨 Overdue Loan",
                    message = "${currencySymbol}${String.format("%,.0f", remaining)} $loanTypeLabel ${loan.personName} is $daysOverdue day${if (daysOverdue > 1) "s" else ""} overdue!"
                )
            }
        }

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission not granted — silently skip
        }
    }
}
