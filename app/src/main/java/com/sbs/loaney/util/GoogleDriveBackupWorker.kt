package com.sbs.loaney.util

import android.content.Context
import androidx.work.*
import com.sbs.loaney.data.repository.SettingsRepository
import com.sbs.loaney.data.repository.dataStore
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GoogleDriveBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "google_drive_backup"
        const val ONE_OFF_WORK_NAME = "google_drive_backup_oneoff"

        fun schedule(context: Context, interval: String, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
            
            if (!enabled) return

            val request = when (interval) {
                "night" -> {
                    PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(1, TimeUnit.DAYS)
                        .setInitialDelay(calculateInitialDelayForNight(), TimeUnit.MILLISECONDS)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                }
                "week" -> {
                    PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(7, TimeUnit.DAYS)
                        .setInitialDelay(calculateInitialDelayForWeek(), TimeUnit.MILLISECONDS)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                }
                else -> null // "loan" doesn't use periodic scheduling; it runs immediately on database writes
            }

            if (request != null) {
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
        }

        fun triggerImmediateBackup(context: Context) {
            val request = OneTimeWorkRequestBuilder<GoogleDriveBackupWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_OFF_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun calculateInitialDelayForNight(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }

        private fun calculateInitialDelayForWeek(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.WEEK_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext.dataStore)
        val autoBackupEnabled = settingsRepository.autoBackupEnabledFlow.first()
        
        // If this is a periodic run, check if autoBackup is enabled first
        if (runAttemptCount == 0 && !autoBackupEnabled && !isManualRun()) {
            return Result.success()
        }

        val result = GoogleDriveBackupManager(applicationContext).backup()
        return if (result.isSuccess) {
            Result.success()
        } else {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun isManualRun(): Boolean {
        // If it's a one-off task, we can assume it was triggered by a loan track or manual action
        return tags.contains(ONE_OFF_WORK_NAME)
    }
}
