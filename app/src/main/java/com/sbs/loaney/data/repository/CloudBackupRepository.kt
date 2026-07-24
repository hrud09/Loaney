package com.sbs.loaney.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors the on-device (Room) loan data into the signed-in user's Firestore account.
 *
 * This backs a guest up to the cloud: everything they tracked while offline is uploaded under
 * `users/{uid}/…` so it is tied to their account and survives a reinstall or device change. It
 * writes into the same collection layout [FirebaseLoanRepository] reads from, keeping a future
 * cloud-restore/sync straightforward.
 *
 * Uses the document id = entity id convention, so re-running is an idempotent upsert rather than a
 * duplicate.
 */
@Singleton
class CloudBackupRepository @Inject constructor(
    private val localRepository: ILoanRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Uploads all local loans (active + history), their payments and items, and bank accounts to
     * the current user's Firestore document. Returns the number of loans backed up, or a failure
     * (callers treat this as best-effort — a failed backup must never block sign-up).
     */
    suspend fun uploadLocalDataToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid
                ?: return@withContext Result.failure(Exception("Not signed in"))
            val userDoc = firestore.collection("users").document(uid)

            // getAllLoansOnce() returns only active loans; pull history separately so settled and
            // deleted records are preserved in the backup too. Both carry their payments and items.
            val allLoans = localRepository.getAllLoansOnce() + localRepository.getDeletedLoans().first()
            val bankAccounts = localRepository.getAllBankAccounts().first()

            if (allLoans.isEmpty() && bankAccounts.isEmpty()) {
                return@withContext Result.success(0)
            }

            val ops = mutableListOf<Pair<DocumentReference, Any>>()
            for (lwp in allLoans) {
                ops += userDoc.collection("loans").document(lwp.loan.id.toString()) to lwp.loan
                for (payment in lwp.payments) {
                    ops += userDoc.collection("payments").document(payment.id.toString()) to payment
                }
                for (item in lwp.loanItems) {
                    ops += userDoc.collection("loanItems").document(item.id.toString()) to item
                }
            }
            for (account in bankAccounts) {
                ops += userDoc.collection("bankAccounts").document(account.id.toString()) to account
            }

            // A Firestore batch tops out at 500 writes; chunk well under that.
            ops.chunked(450).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { (ref, data) -> batch.set(ref, data) }
                batch.commit().await()
            }

            Result.success(allLoans.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
