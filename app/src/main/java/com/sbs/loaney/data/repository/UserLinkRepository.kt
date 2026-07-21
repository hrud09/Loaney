package com.sbs.loaney.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sbs.loaney.data.model.LinkedLoanNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles cross-user interconnectivity through email-based lookup.
 *
 * Flow:
 * 1. [lookupUidByEmail] → searches `users` collection for a document where `email == targetEmail`.
 * 2. [sendLoanNotification] → writes a [LinkedLoanNotification] into the recipient's
 *    `loanNotifications` subcollection so they see it the next time they open the app.
 * 3. [observeIncomingNotifications] → real-time Flow of unread notifications for the
 *    current logged-in user.
 * 4. [markNotificationRead] → marks a single notification as read.
 */
@Singleton
class UserLinkRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "UserLinkRepository"
        private const val USERS_COLLECTION = "users"
        private const val NOTIFICATIONS_SUBCOLLECTION = "loanNotifications"

        /** LinkedLoanNotification.loanType discriminator for due-date reminders. */
        const val REMINDER_TYPE = "REMINDER"
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Email → UID lookup
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Queries Firestore for a registered user matching either [email] or [phone].
     * Returns the UID string if found, or null if neither is registered.
     */
    suspend fun lookupUid(email: String? = null, phone: String? = null): String? {
        val trimmedEmail = email?.trim()?.takeIf { it.isNotBlank() }
        val lowercaseEmail = trimmedEmail?.lowercase()
        val trimmedPhone = phone?.trim()?.takeIf { it.isNotBlank() }

        if (trimmedEmail == null && trimmedPhone == null) return null

        return try {
            if (trimmedEmail != null) {
                val searchEmails = listOf(lowercaseEmail, trimmedEmail).distinct()
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereIn("email", searchEmails)
                    .limit(1)
                    .get()
                    .await()
                if (!snapshot.isEmpty) return snapshot.documents.first().id
            }

            if (trimmedPhone != null) {
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("phone", trimmedPhone)
                    .limit(1)
                    .get()
                    .await()
                if (!snapshot.isEmpty) return snapshot.documents.first().id
            }
            null
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Log.e(TAG, "Lookup PERMISSION_DENIED — Firestore rules need updating. See FirebaseGuide.md")
            } else {
                Log.e(TAG, "Lookup failed: ${e.message}")
            }
            null
        }
    }

    /**
     * Same as [lookupUid] but also returns the display name of the found user.
     * Returns Pair(uid, name) or null.
     */
    suspend fun lookupUser(email: String? = null, phone: String? = null): Pair<String, String>? {
        val trimmedEmail = email?.trim()?.takeIf { it.isNotBlank() }
        val lowercaseEmail = trimmedEmail?.lowercase()
        val trimmedPhone = phone?.trim()?.takeIf { it.isNotBlank() }

        if (trimmedEmail == null && trimmedPhone == null) return null

        return try {
            if (trimmedEmail != null) {
                val searchEmails = listOf(lowercaseEmail, trimmedEmail).distinct()
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereIn("email", searchEmails)
                    .limit(1)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    return Pair(doc.id, doc.getString("name") ?: "Loaney User")
                }
            }

            if (trimmedPhone != null) {
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("phone", trimmedPhone)
                    .limit(1)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    return Pair(doc.id, doc.getString("name") ?: "Loaney User")
                }
            }
            null
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Log.e(TAG, "Lookup PERMISSION_DENIED — Firestore rules need updating. See FirebaseGuide.md")
            } else {
                Log.e(TAG, "Lookup failed: ${e.message}")
            }
            null
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Write notification to recipient
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Writes a [LinkedLoanNotification] into the *recipient's* Firestore subcollection.
     *
     * Called right after a new loan is inserted if the email field is filled and
     * belongs to a registered Loaney user.
     *
     * @param recipientUid       UID of the user who should receive the notification.
     * @param loanId             ID of the newly created loan (used as the notification document ID).
     * @param loanType           The loan type from *the sender's* perspective ("LEND" or "BORROW").
     * @param amount             Loan amount.
     * @param currency           Currency symbol (e.g. "৳").
     * @param promisedReturnDateMillis  Due-date as epoch millis.
     */
    suspend fun sendLoanNotification(
        recipientUid: String,
        loanId: Long,
        loanType: String,
        amount: Double,
        currency: String,
        promisedReturnDateMillis: Long,
        pdfBase64: String? = null
    ) {
        val currentUser = auth.currentUser ?: return
        val senderUid = currentUser.uid

        // Prevent sending a notification to yourself.
        if (senderUid == recipientUid) return

        try {
            val senderDoc = firestore.collection(USERS_COLLECTION)
                .document(senderUid)
                .get()
                .await()
            val senderName = senderDoc.getString("name") ?: currentUser.displayName ?: "Someone"

            val notificationId = "${senderUid}_${loanId}_${System.currentTimeMillis()}"
            val notification = LinkedLoanNotification(
                id = notificationId,
                senderName = senderName,
                senderUid = senderUid,
                senderLoanId = loanId.toString(),
                loanType = loanType,
                amount = amount,
                currency = currency,
                promisedReturnDateMillis = promisedReturnDateMillis,
                createdAt = System.currentTimeMillis(),
                isRead = false,
                pdfBase64 = pdfBase64
            )

            firestore.collection(USERS_COLLECTION)
                .document(recipientUid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .set(notification)
                .await()

            Log.d(TAG, "Loan notification sent to UID: $recipientUid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send loan notification: ${e.message}")
        }
    }

    /**
     * Sends a specialized notification for Two-Way Sync events (Importing, Updating, Accepting).
     */
    suspend fun sendLoanSyncNotification(
        recipientUid: String,
        notificationId: String,
        loanType: String,
        notificationType: String,
        proposedChangesJson: String? = null,
        senderLoanId: String? = null,
        recipientLoanId: String? = null,
        amount: Double = 0.0,
        currency: String = "৳",
        promisedReturnDateMillis: Long = 0L
    ) {
        val currentUser = auth.currentUser ?: return
        val senderUid = currentUser.uid

        // Prevent sending a notification to yourself.
        if (senderUid == recipientUid) return

        try {
            val senderDoc = firestore.collection(USERS_COLLECTION)
                .document(senderUid)
                .get()
                .await()
            val senderName = senderDoc.getString("name") ?: currentUser.displayName ?: "Someone"

            val notification = LinkedLoanNotification(
                id = notificationId,
                senderName = senderName,
                senderUid = senderUid,
                loanType = loanType,
                amount = amount,
                currency = currency,
                promisedReturnDateMillis = promisedReturnDateMillis,
                createdAt = System.currentTimeMillis(),
                isRead = false,
                notificationType = notificationType,
                proposedChangesJson = proposedChangesJson,
                senderLoanId = senderLoanId,
                recipientLoanId = recipientLoanId
            )

            firestore.collection(USERS_COLLECTION)
                .document(recipientUid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .set(notification)
                .await()

            Log.d(TAG, "Loan sync notification ($notificationType) sent to UID: $recipientUid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send loan sync notification: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Write email notification to `mail` collection
    // ──────────────────────────────────────────────────────────────────────────
    
    /**
     * Backs up a local system reminder to Firestore if the user is logged in.
     */
    suspend fun backupSystemNotification(
        notificationId: String,
        title: String,
        message: String,
        loanId: Long?
    ) {
        val currentUser = auth.currentUser ?: return
        val senderUid = currentUser.uid

        try {
            val notification = LinkedLoanNotification(
                id = notificationId,
                senderName = "Loaney",
                senderUid = "system",
                loanType = "SYSTEM",
                notificationType = "SYSTEM_REMINDER",
                createdAt = System.currentTimeMillis(),
                isRead = false,
                title = title,
                message = message,
                senderLoanId = loanId?.toString(),
                recipientLoanId = loanId?.toString()
            )

            firestore.collection(USERS_COLLECTION)
                .document(senderUid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .set(notification)
                .await()

            Log.d(TAG, "System notification backed up to UID: $senderUid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup system notification: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Write email notification to `mail` collection
    // ──────────────────────────────────────────────────────────────────────────
    
    /**
     * Writes to the 'mail' collection so the Firebase Trigger Email extension
     * automatically sends an email to the recipient.
     */
    suspend fun sendEmailNotification(
        recipientEmail: String,
        loanType: String,
        amount: Double,
        currency: String,
        promisedReturnDateMillis: Long,
        pdfBase64: String? = null
    ) {
        val currentUser = auth.currentUser ?: return
        val senderUid = currentUser.uid

        try {
            val senderDoc = firestore.collection(USERS_COLLECTION)
                .document(senderUid)
                .get()
                .await()
            val senderName = senderDoc.getString("name") ?: currentUser.displayName ?: "Someone"

            val actionText = if (loanType == "LEND") "lent you" else "wants to borrow"
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val returnDateString = dateFormat.format(java.util.Date(promisedReturnDateMillis))

            val subject = "New Loaney update from $senderName"
            val textContent = "Hi there!\n\n$senderName $actionText $currency$amount.\n\nPromised return date: $returnDateString\n\nLogin to the Loaney app to view details."

            val emailDoc = mutableMapOf<String, Any>(
                "to" to recipientEmail,
                "message" to mapOf(
                    "subject" to subject,
                    "text" to textContent
                )
            )

            if (pdfBase64 != null) {
                emailDoc["attachments"] = listOf(
                    mapOf(
                        "filename" to "Loaney_Receipt.pdf",
                        "content" to pdfBase64,
                        "encoding" to "base64"
                    )
                )
            }

            firestore.collection("mail").add(emailDoc).await()
            Log.d(TAG, "Email notification queued for: $recipientEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email notification: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Automated due-date reminders to the borrower
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Emails the borrower that a loan is due or overdue, via the same `mail` collection
     * the Trigger Email extension already watches. This is the only channel that reaches
     * someone who does not have Loaney installed — auto-SMS and auto-WhatsApp are not
     * possible for a consumer app (Play's SMS policy / WhatsApp Business API).
     *
     * Only ever called for loans the owner explicitly opted in, and rate-limited by the
     * caller via LoanEntity.lastReminderSentAt.
     */
    suspend fun sendReminderEmail(
        recipientEmail: String,
        amount: Double,
        currency: String,
        dueDateMillis: Long,
        daysOverdue: Int
    ) {
        val currentUser = auth.currentUser ?: return

        try {
            val senderDoc = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()
            val senderName = senderDoc.getString("name") ?: currentUser.displayName ?: "Someone"

            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val dueDateString = dateFormat.format(java.util.Date(dueDateMillis))
            val formattedAmount = String.format(java.util.Locale.getDefault(), "%,.0f", amount)

            val subject: String
            val body: String
            if (daysOverdue > 0) {
                val dayWord = if (daysOverdue == 1) "day" else "days"
                subject = "Reminder: $currency$formattedAmount to $senderName is overdue"
                body = "Hi there,\n\nThis is a friendly reminder that $currency$formattedAmount you owe " +
                        "$senderName was due on $dueDateString — $daysOverdue $dayWord ago.\n\n" +
                        "If you have already settled it, please ignore this message.\n\n" +
                        "Sent automatically by Loaney on behalf of $senderName."
            } else {
                subject = "Reminder: $currency$formattedAmount to $senderName is due tomorrow"
                body = "Hi there,\n\nThis is a friendly reminder that $currency$formattedAmount you owe " +
                        "$senderName is due on $dueDateString.\n\n" +
                        "If you have already settled it, please ignore this message.\n\n" +
                        "Sent automatically by Loaney on behalf of $senderName."
            }

            val emailDoc = mapOf(
                "to" to recipientEmail,
                "message" to mapOf("subject" to subject, "text" to body)
            )

            firestore.collection("mail").add(emailDoc).await()
            Log.d(TAG, "Reminder email queued for: $recipientEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue reminder email: ${e.message}")
        }
    }

    /** In-app reminder for a borrower who is a registered Loaney user. */
    suspend fun sendReminderNotification(
        recipientUid: String,
        loanId: Long,
        amount: Double,
        currency: String,
        dueDateMillis: Long
    ) {
        val currentUser = auth.currentUser ?: return
        if (currentUser.uid == recipientUid) return

        try {
            val senderDoc = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()
            val senderName = senderDoc.getString("name") ?: currentUser.displayName ?: "Someone"

            val notification = LinkedLoanNotification(
                id = "reminder_$loanId",
                senderName = senderName,
                senderUid = currentUser.uid,
                loanType = REMINDER_TYPE,
                amount = amount,
                currency = currency,
                promisedReturnDateMillis = dueDateMillis,
                createdAt = System.currentTimeMillis(),
                isRead = false
            )

            firestore.collection(USERS_COLLECTION)
                .document(recipientUid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document("reminder_$loanId")
                .set(notification)
                .await()

            Log.d(TAG, "Reminder notification sent to UID: $recipientUid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reminder notification: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Write bank account share notification to recipient
    // ──────────────────────────────────────────────────────────────────────────

    suspend fun sendBankAccountNotification(
        recipientUid: String,
        account: com.sbs.loaney.data.local.entity.BankAccountEntity,
        shareId: String? = null,
        permission: com.sbs.loaney.data.model.SharePermission? = null
    ) {
        val currentUser = auth.currentUser ?: return
        val senderUid = currentUser.uid

        // Prevent sending a notification to yourself.
        if (senderUid == recipientUid) return

        try {
            val senderDoc = firestore.collection(USERS_COLLECTION)
                .document(senderUid)
                .get()
                .await()
            val senderName = senderDoc.getString("name") ?: currentUser.displayName ?: "Someone"

            val notificationId = System.currentTimeMillis().toString()
            val notification = LinkedLoanNotification(
                id = notificationId,
                senderName = senderName,
                senderUid = senderUid,
                loanType = if (account.isMfs) "SHARE_MFS" else if (account.isCard) "SHARE_CARD" else "SHARE_BANK",
                amount = 0.0,
                createdAt = System.currentTimeMillis(),
                isRead = false,
                accountName = account.accountName,
                accountNumber = account.accountNumber,
                bankName = account.bankName,
                branchName = account.branchName,
                swiftCode = account.swiftCode,
                isCard = account.isCard,
                isMfs = account.isMfs,
                mfsProvider = account.mfsProvider,
                qrCodeUri = account.qrCodeUri,
                shareId = shareId,
                sharePermission = permission?.name
            )

            firestore.collection(USERS_COLLECTION)
                .document(recipientUid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .set(notification)
                .await()

            Log.d(TAG, "Bank account notification sent to UID: $recipientUid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send bank notification: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Write bank account share email notification to `mail` collection
    // ──────────────────────────────────────────────────────────────────────────

    suspend fun sendBankAccountEmail(
        recipientEmail: String,
        account: com.sbs.loaney.data.local.entity.BankAccountEntity
    ) {
        val currentUser = auth.currentUser ?: return
        val senderUid = currentUser.uid

        try {
            val senderDoc = firestore.collection(USERS_COLLECTION)
                .document(senderUid)
                .get()
                .await()
            val senderName = senderDoc.getString("name") ?: currentUser.displayName ?: "Someone"

            val accountType = if (account.isMfs) "MFS Account" else if (account.isCard) "Card" else "Bank Account"
            val subject = "$senderName shared a $accountType with you"
            val details = buildString {
                append("Here are the details:\n")
                append("- Type: $accountType\n")
                append("- Institution/Provider: ${account.bankName}\n")
                append("- Account Holder/Name: ${account.accountName}\n")
                append("- Account/Mobile/Card Number: ${account.accountNumber}\n")
                if (!account.branchName.isNullOrBlank()) append("- Branch: ${account.branchName}\n")
                if (!account.swiftCode.isNullOrBlank()) append("- SWIFT: ${account.swiftCode}\n")
            }
            val body = "Hi there,\n\n$senderName has shared their $accountType details with you on Loaney.\n\n$details\nLogin to the Loaney app to view and import details directly into your wallet."

            val emailDoc = mapOf(
                "to" to recipientEmail,
                "message" to mapOf(
                    "subject" to subject,
                    "text" to body
                )
            )

            firestore.collection("mail").add(emailDoc).await()
            Log.d(TAG, "Bank share email queued for: $recipientEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send bank share email: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Observe incoming notifications (for the current user)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns a real-time [Flow] of all [LinkedLoanNotification] documents stored in
     * the current user's `loanNotifications` subcollection, ordered newest-first.
     */
    fun observeIncomingNotifications(): Flow<List<LinkedLoanNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .collection(NOTIFICATIONS_SUBCOLLECTION)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = mutableListOf<LinkedLoanNotification>()
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        try {
                            val item = doc.toObject(LinkedLoanNotification::class.java)
                            if (item != null) {
                                items.add(item)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deserializing notification document ${doc.id}: ${e.message}", e)
                        }
                    }
                }
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mark as read
    // ──────────────────────────────────────────────────────────────────────────

    suspend fun markNotificationRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notification read: ${e.message}")
        }
    }

    /**
     * Returns unread notification count as a simple suspend call (for badge purposes).
     */
    suspend fun getUnreadCount(): Int {
        val uid = auth.currentUser?.uid ?: return 0
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Delete Notification
    // ──────────────────────────────────────────────────────────────────────────
    
    suspend fun deleteNotification(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification: ${e.message}")
        }
    }
}
