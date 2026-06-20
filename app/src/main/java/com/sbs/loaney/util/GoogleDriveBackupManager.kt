package com.sbs.loaney.util

import android.content.Context
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

    suspend fun backup(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService() ?: return@withContext Result.failure(Exception("Google Account not signed in"))

            // Close the database to checkpoint WAL and SHM files
            AppDatabase.getDatabase(context).close()

            val dbFile = context.getDatabasePath(databaseName)
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found"))
            }

            // Find if a backup file already exists in AppData folder
            val filesList = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$databaseName'")
                .execute()

            val existingFile = filesList.files.firstOrNull()

            val metadata = File().apply {
                name = databaseName
                parents = Collections.singletonList("appDataFolder")
            }

            val mediaContent = FileContent("application/octet-stream", dbFile)

            if (existingFile != null) {
                // Update existing backup
                service.files().update(existingFile.id, null, mediaContent).execute()
            } else {
                // Create new backup
                service.files().create(metadata, mediaContent).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restore(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService() ?: return@withContext Result.failure(Exception("Google Account not signed in"))

            // Close the database to prepare for file replacement
            AppDatabase.getDatabase(context).close()

            val filesList = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$databaseName'")
                .execute()

            val backupFile = filesList.files.firstOrNull()
                ?: return@withContext Result.failure(Exception("No backup found on Google Drive"))

            val dbFile = context.getDatabasePath(databaseName)
            val dbShm = context.getDatabasePath("$databaseName-shm")
            val dbWal = context.getDatabasePath("$databaseName-wal")

            // Delete existing files to avoid conflicts with write-ahead logs
            if (dbFile.exists()) dbFile.delete()
            if (dbShm.exists()) dbShm.delete()
            if (dbWal.exists()) dbWal.delete()

            // Download new file
            FileOutputStream(dbFile).use { outputStream ->
                service.files().get(backupFile.id).executeMediaAndDownloadTo(outputStream)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
