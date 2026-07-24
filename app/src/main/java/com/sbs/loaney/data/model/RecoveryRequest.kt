package com.sbs.loaney.data.model

/**
 * "Assisted recovery" — an opt-in service where a lender asks Loaney to help recover an overdue
 * loan from the borrower, in exchange for a small success fee (a percentage of the amount actually
 * recovered).
 *
 * ⚠️ LEGAL GATE — READ BEFORE ENABLING [RecoveryConfig.IS_LIVE].
 * In Bangladesh, contacting a borrower to collect a debt on a lender's behalf is legally sensitive
 * (harassment exposure under the Cyber Security Act; and third-party recovery is best done through a
 * licensed partner — a money-recovery law firm). Until (a) a Bangladeshi lawyer's written opinion and
 * (b) a signed partner firm are in place, this feature must NOT contact the borrower or dispatch any
 * outreach. While [RecoveryConfig.IS_LIVE] is `false`, [com.sbs.loaney.data.repository.RecoveryRepository]
 * only *records* the lender's request (interest registration) and never messages the other party.
 */
object RecoveryConfig {

    /**
     * Master switch for the live recovery pipeline.
     *
     * `false`  → scaffold mode: requests are recorded and shown back to the lender as
     *            "under review", but NO borrower outreach / collector dispatch happens.
     * `true`   → only flip this once the legal opinion + partner firm are secured, and the
     *            server-side dispatch (partner hand-off) is implemented. Do not enable client-only.
     */
    const val IS_LIVE = false

    /** Success fee charged on the amount actually recovered (percent). Kept intentionally small. */
    const val FEE_PERCENT = 5.0

    /** Recovery is only meaningful above this outstanding amount (in the user's currency units). */
    const val MIN_ELIGIBLE_AMOUNT = 1000.0

    /** A loan must be at least this many days overdue before recovery help is offered. */
    const val MIN_DAYS_OVERDUE = 7

    /** Computes the estimated success fee for a given recoverable amount. */
    fun estimatedFee(outstandingAmount: Double): Double = outstandingAmount * FEE_PERCENT / 100.0
}

/**
 * Lifecycle of a single recovery request. Only [SUBMITTED] and [UNDER_REVIEW] are reachable while
 * [RecoveryConfig.IS_LIVE] is `false`; the later states are driven by the (future) partner pipeline.
 */
enum class RecoveryStatus {
    /** Lender has just submitted the request; nothing has been reviewed yet. */
    SUBMITTED,

    /** Loaney (or the partner) is reviewing eligibility and the evidence package. */
    UNDER_REVIEW,

    /** Partner has begun contacting the borrower. (Live pipeline only.) */
    IN_PROGRESS,

    /** Borrower has settled — full or partial recovery achieved. (Live pipeline only.) */
    RESOLVED,

    /** Lender withdrew the request. */
    CANCELLED,

    /** Not eligible / not offered in this jurisdiction yet. */
    UNAVAILABLE
}

/**
 * A recovery request document. Firestore-serializable, so it needs a no-arg constructor and only
 * primitive/`String` fields (mirrors the convention used by [LinkedLoanNotification]).
 *
 * Stored at:  users/{lenderUid}/recoveryRequests/{id}
 * and mirrored to a top-level `recoveryRequests/{id}` collection for back-office/partner review.
 */
data class RecoveryRequest(
    /** Document id — we reuse the originating loan id so a loan maps to at most one active request. */
    val id: String = "",

    /** UID of the lender who asked for help. */
    val lenderUid: String = "",

    /** Local loan id this request is for. */
    val loanId: Long = 0L,

    /** Display name of the borrower, copied at submission time. */
    val borrowerName: String = "",

    /** Borrower contact channels (whichever the lender recorded). */
    val borrowerPhone: String = "",
    val borrowerEmail: String = "",

    /** Outstanding (unpaid) amount at submission time. */
    val outstandingAmount: Double = 0.0,

    /** Currency symbol used by the lender (e.g. "৳"). */
    val currency: String = "৳",

    /** Success fee percent quoted to the lender at submission time (snapshot of config). */
    val quotedFeePercent: Double = RecoveryConfig.FEE_PERCENT,

    /** Days overdue at submission time. */
    val daysOverdue: Int = 0,

    /**
     * Whether the borrower had confirmed/linked this loan in-app. A confirmed debt is far stronger
     * for recovery, so this is the single most important signal for the reviewer.
     */
    val borrowerConfirmed: Boolean = false,

    /** Whether the lender attached a proof document/photo to the loan. */
    val hasProof: Boolean = false,

    /** Whether the lender recorded a witness. */
    val hasWitness: Boolean = false,

    /** Number of recorded partial payments (part of the evidence picture). */
    val paymentCount: Int = 0,

    /** Optional note from the lender. */
    val lenderNote: String = "",

    /** Current status. Stored as the enum name for Firestore friendliness. */
    val status: String = RecoveryStatus.SUBMITTED.name,

    /** Epoch millis of submission. */
    val createdAt: Long = System.currentTimeMillis(),

    /** Snapshot of whether the live pipeline was active when this was created (audit trail). */
    val wasLive: Boolean = RecoveryConfig.IS_LIVE
)
