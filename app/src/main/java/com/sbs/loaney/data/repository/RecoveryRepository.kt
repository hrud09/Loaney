package com.sbs.loaney.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sbs.loaney.data.model.RecoveryConfig
import com.sbs.loaney.data.model.RecoveryRequest
import com.sbs.loaney.data.model.RecoveryStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records "assisted recovery" requests.
 *
 * ⚠️ While [RecoveryConfig.IS_LIVE] is `false` this repository NEVER contacts the borrower or hands
 * anything to a collector. It only persists the lender's request so it can be shown back to them
 * ("under review") and picked up later by a back-office/partner once the legal + partner
 * prerequisites are in place. See the header of [RecoveryRequest] for the full rationale.
 *
 * Constructor-injected the same way as [UserLinkRepository]; no Hilt module entry is needed.
 */
@Singleton
class RecoveryRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "RecoveryRepository"
        private const val USERS_COLLECTION = "users"
        private const val REQUESTS_SUBCOLLECTION = "recoveryRequests"
        /** Top-level mirror the back office / partner reads from. */
        private const val REQUESTS_ROOT_COLLECTION = "recoveryRequests"
    }

    /** Outcome of a submit attempt, so the UI can react (toast / status) without guessing. */
    sealed interface SubmitOutcome {
        object Success : SubmitOutcome
        /** No signed-in user — recovery needs an account to route the case. */
        object NeedsSignIn : SubmitOutcome
        data class Error(val message: String) : SubmitOutcome
    }

    /**
     * Records a recovery request for [request].
     *
     * The request is written both under the lender's own document (so they can see it) and to the
     * top-level review collection. No borrower outreach happens here while the feature is gated —
     * the gate is enforced structurally: there is simply no messaging code on this path.
     */
    suspend fun submitRequest(request: RecoveryRequest): SubmitOutcome {
        val uid = auth.currentUser?.uid ?: return SubmitOutcome.NeedsSignIn

        val doc = request.copy(
            id = request.loanId.toString(),
            lenderUid = uid,
            status = RecoveryStatus.UNDER_REVIEW.name,
            wasLive = RecoveryConfig.IS_LIVE
        )

        return try {
            // Under the lender's own tree — this is what observeRequestForLoan reads back.
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(REQUESTS_SUBCOLLECTION)
                .document(doc.id)
                .set(doc)
                .await()

            // Mirror for back-office / partner review. Namespaced by uid to keep ids unique.
            firestore.collection(REQUESTS_ROOT_COLLECTION)
                .document("${uid}_${doc.id}")
                .set(doc)
                .await()

            Log.d(TAG, "Recovery request recorded for loan ${doc.loanId} (live=${RecoveryConfig.IS_LIVE})")
            SubmitOutcome.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record recovery request: ${e.message}", e)
            SubmitOutcome.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Real-time [Flow] of the recovery request for [loanId], or `null` if none exists / not signed in.
     */
    fun observeRequestForLoan(loanId: Long): Flow<RecoveryRequest?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .collection(REQUESTS_SUBCOLLECTION)
            .document(loanId.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val request = try {
                    snapshot?.toObject(RecoveryRequest::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing recovery request $loanId: ${e.message}", e)
                    null
                }
                trySend(request)
            }
        awaitClose { listener.remove() }
    }

    /** Marks a request cancelled (lender withdrew). Best-effort. */
    suspend fun cancelRequest(loanId: Long) {
        val uid = auth.currentUser?.uid ?: return
        val update = mapOf("status" to RecoveryStatus.CANCELLED.name)
        try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(REQUESTS_SUBCOLLECTION)
                .document(loanId.toString())
                .update(update)
                .await()
            firestore.collection(REQUESTS_ROOT_COLLECTION)
                .document("${uid}_$loanId")
                .update(update)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recovery request $loanId: ${e.message}", e)
        }
    }
}
