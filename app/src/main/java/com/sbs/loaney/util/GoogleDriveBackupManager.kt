package com.sbs.loaney.util

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.sbs.loaney.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Collections

class GoogleDriveBackupManager(private val context: Context) {

    private val databaseName = "loaney_database"

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton("https://www.googleapis.com/auth/drive.appdata")
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("Loaney")
        .build()
    }

    private fun findBackupFile(service: Drive): File? =
        service.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$databaseName'")
            .execute()
            .files
            ?.firstOrNull()

    suspend fun backup(): Result<Unit> = withContext(Dispatchers.IO) {
        val snapshot = java.io.File(context.cacheDir, "$databaseName.backup")
        try {
            val service = getDriveService() ?: return@withContext Result.failure(Exception("Google Account not signed in"))

            val db = AppDatabase.getDatabase(context)
            val dbFile = context.getDatabasePath(databaseName)
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found"))
            }

            // Flush the WAL into the main file without closing the shared instance (closing it
            // would break every DAO and Flow the app is using), then upload a point-in-time copy
            // so writes during the upload can't produce a torn file.
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            dbFile.copyTo(snapshot, overwrite = true)

            val mediaContent = FileContent("application/octet-stream", snapshot)
            val existingFile = findBackupFile(service)

            if (existingFile != null) {
                service.files().update(existingFile.id, null, mediaContent).execute()
            } else {
                val metadata = File().apply {
                    name = databaseName
                    parents = Collections.singletonList("appDataFolder")
                }
                service.files().create(metadata, mediaContent).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            snapshot.delete()
        }
    }

    /**
     * Downloads the backup to a temp file and validates it before touching the live database.
     * On success the local database has been replaced and the caller MUST restart the app
     * (see [restartApp]); the in-memory Room instance and Hilt-held DAOs are no longer valid.
     */
    suspend fun restore(): Result<Unit> = withContext(Dispatchers.IO) {
        val downloaded = java.io.File(context.cacheDir, "$databaseName.restore")
        try {
            val service = getDriveService() ?: return@withContext Result.failure(Exception("Google Account not signed in"))

            val backupFile = findBackupFile(service)
                ?: return@withContext Result.failure(Exception("No backup found on Google Drive"))

            FileOutputStream(downloaded).use { outputStream ->
                service.files().get(backupFile.id).executeMediaAndDownloadTo(outputStream)
            }

            validateBackup(downloaded)?.let { problem ->
                return@withContext Result.failure(Exception(problem))
            }

            // Only now, with a known-good file in hand, replace the live database.
            AppDatabase.closeForReplacement()
            val dbFile = context.getDatabasePath(databaseName)
            context.getDatabasePath("$databaseName-wal").delete()
            context.getDatabasePath("$databaseName-shm").delete()
            downloaded.copyTo(dbFile, overwrite = true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            downloaded.delete()
        }
    }

    /** Returns an error message if [file] isn't a database this app version can open. */
    private fun validateBackup(file: java.io.File): String? {
        if (file.length() < 100) return "Backup file is empty or incomplete"
        val header = ByteArray(16)
        file.inputStream().use { it.read(header) }
        if (String(header, Charsets.US_ASCII) != "SQLite format 3\u0000") {
            return "Backup file is corrupted"
        }
        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                when {
                    db.version > AppDatabase.SCHEMA_VERSION ->
                        "This backup was made by a newer version of Loaney. Please update the app first."
                    !db.isDatabaseIntegrityOk -> "Backup file is corrupted"
                    else -> null
                }
            }
        } catch (e: Exception) {
            "Backup file is corrupted"
        }
    }

    companion object {
        /** Relaunches the app in a fresh process so everything reopens the restored database. */
        fun restartApp(context: Context) {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            val restart = Intent.makeRestartActivityTask(launch.component)
            context.startActivity(restart)
            Runtime.getRuntime().exit(0)
        }
    }
}
