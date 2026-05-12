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
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Email → UID lookup
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Queries Firestore for a registered user whose `email` field matches [targetEmail].
     * Returns the UID string if found, or null if the email is not registered in Loaney.
     *
     * Strategy:
     * 1. Try lowercase (canonical form — how emails are stored going forward).
     * 2. If no result, try the exact casing provided (safety net for legacy accounts).
     *
     * NOTE: This requires the Firestore security rules to allow authenticated users
     * to READ the /users collection.  See FirebaseGuide.md for the updated rules.
     */
    suspend fun lookupUidByEmail(targetEmail: String): String? {
        val trimmed = targetEmail.trim()
        return try {
            // Pass 1 — lowercase (canonical)
            val lowercase = trimmed.lowercase()
            var snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", lowercase)
                .limit(1)
                .get()
                .await()

            // Pass 2 — original casing (legacy accounts stored before normalisation fix)
            if (snapshot.isEmpty && trimmed != lowercase) {
                snapshot = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("email", trimmed)
                    .limit(1)
                    .get()
                    .await()
            }

            if (snapshot.isEmpty) null else snapshot.documents.first().id
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Log.e(TAG, "Email lookup PERMISSION_DENIED — Firestore rules need updating. See FirebaseGuide.md")
            } else {
                Log.e(TAG, "Email lookup failed: ${e.message}")
            }
            null
        }
    }

    /**
     * Same as [lookupUidByEmail] but also returns the display name of the found user.
     * Returns Pair(uid, name) or null.
     */
    suspend fun lookupUserByEmail(targetEmail: String): Pair<String, String>? {
        val trimmed = targetEmail.trim()
        return try {
            // Pass 1 — lowercase
            val lowercase = trimmed.lowercase()
            var snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", lowercase)
                .limit(1)
                .get()
                .await()

            // Pass 2 — original casing (legacy accounts)
            if (snapshot.isEmpty && trimmed != lowercase) {
                snapshot = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("email", trimmed)
                    .limit(1)
                    .get()
                    .await()
            }

            if (snapshot.isEmpty) null else {
                val doc = snapshot.documents.first()
                val name = doc.getString("name") ?: "Loaney User"
                Pair(doc.id, name)
            }
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Log.e(TAG, "User lookup PERMISSION_DENIED — Firestore rules need updating. See FirebaseGuide.md")
            } else {
                Log.e(TAG, "User lookup failed: ${e.message}")
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
        promisedReturnDateMillis: Long
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
                id = loanId.toString(),
                senderName = senderName,
                senderUid = senderUid,
                loanType = loanType,
                amount = amount,
                currency = currency,
                promisedReturnDateMillis = promisedReturnDateMillis,
                createdAt = System.currentTimeMillis(),
                isRead = false
            )

            firestore.collection(USERS_COLLECTION)
                .document(recipientUid)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(loanId.toString())
                .set(notification)
                .await()

            Log.d(TAG, "Loan notification sent to UID: $recipientUid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send loan notification: ${e.message}")
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
        promisedReturnDateMillis: Long
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

            val emailDoc = mapOf(
                "to" to recipientEmail,
                "message" to mapOf(
                    "subject" to subject,
                    "text" to textContent
                )
            )

            firestore.collection("mail").add(emailDoc).await()
            Log.d(TAG, "Email notification queued for: $recipientEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email notification: ${e.message}")
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
                val items = snapshot?.toObjects(LinkedLoanNotification::class.java) ?: emptyList()
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
