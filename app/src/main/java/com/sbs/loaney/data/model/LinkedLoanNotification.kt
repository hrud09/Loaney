package com.sbs.loaney.data.model

/**
 * A notification document stored under a *recipient* user's Firestore subcollection:
 *   users/{recipientUid}/loanNotifications/{notificationId}
 *
 * Written by the *sender* when they create a loan that includes the recipient's registered email.
 */
data class LinkedLoanNotification(
    /** Unique ID (= loanId timestamp of the originating loan). */
    val id: String = "",

    /** Display name of the person who created the loan. */
    val senderName: String = "",

    /** UID of the user who created the loan (for traceability). */
    val senderUid: String = "",

    /**
     * Whether the current user is the LENDER or BORROWER **from the sender's perspective**.
     * "LEND"  → sender lent money TO the recipient  (recipient owes money)
     * "BORROW" → sender borrowed money FROM the recipient (recipient is owed money)
     */
    val loanType: String = "",

    /** Monetary amount involved. */
    val amount: Double = 0.0,

    /** Currency symbol used by the sender (e.g. "৳"). */
    val currency: String = "৳",

    /** Epoch millis of the promised return date. */
    val promisedReturnDateMillis: Long = 0L,

    /** When this notification was created (epoch millis). */
    val createdAt: Long = System.currentTimeMillis(),

    /** Whether the recipient has acknowledged / dismissed the notification. */
    val isRead: Boolean = false,

    // Additional fields for Bank/Card/MFS sharing
    val accountName: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val branchName: String? = null,
    val swiftCode: String? = null,
    val isCard: Boolean = false,
    val isMfs: Boolean = false,
    val mfsProvider: String? = null,
    val qrCodeUri: String? = null,

    /** Persistent share ID when this notification represents a live share grant. */
    val shareId: String? = null,
    val sharePermission: String? = null,

    val pdfBase64: String? = null,
    
    // Linking and Syncing fields
    /**
     * Type of notification: 
     * "LOAN_REQUEST" (default/legacy), "UPDATE_PROPOSAL", "LINK_ACCEPTED", "UPDATE_ACCEPTED", "UPDATE_REJECTED"
     */
    val notificationType: String = "LOAN_REQUEST",
    
    /** JSON string representing the proposed changes to the loan entity (e.g. amount, date, status) */
    val proposedChangesJson: String? = null,
    
    /** The ID of the loan in the SENDER'S collection */
    val senderLoanId: String? = null,
    
    /** The ID of the loan in the RECIPIENT'S collection */
    val recipientLoanId: String? = null,
    
    // System notification fields
    val title: String? = null,
    val message: String? = null
)
