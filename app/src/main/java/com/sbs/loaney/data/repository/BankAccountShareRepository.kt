package com.sbs.loaney.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.model.BankAccountShare
import com.sbs.loaney.data.model.SharePermission
import com.sbs.loaney.data.model.ShareStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankAccountShareRepository @Inject constructor(
    private val userLinkRepository: UserLinkRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "BankAccountShareRepo"
        private const val USERS = "users"
        private const val OUTGOING = "bankAccountShares"
        private const val INCOMING = "sharedBankAccounts"
    }

    suspend fun shareAccount(
        account: BankAccountEntity,
        recipientEmail: String,
        permission: SharePermission
    ): Result<BankAccountShare> {
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val trimmedEmail = recipientEmail.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Invalid email"))
        }

        val recipient = userLinkRepository.lookupUserByEmail(trimmedEmail)
        val shareId = "${account.id}_${System.currentTimeMillis()}"

        val ownerDoc = firestore.collection(USERS).document(currentUser.uid).get().await()
        val ownerName = ownerDoc.getString("name") ?: currentUser.displayName ?: "Someone"

        val share = BankAccountShare.fromAccount(
            account = account,
            shareId = shareId,
            ownerUid = currentUser.uid,
            ownerName = ownerName,
            recipientUid = recipient?.first ?: "",
            recipientEmail = trimmedEmail,
            recipientName = recipient?.second ?: trimmedEmail,
            permission = permission
        )

        return try {
            firestore.collection(USERS)
                .document(currentUser.uid)
                .collection(OUTGOING)
                .document(shareId)
                .set(share)
                .await()

            if (recipient != null) {
                firestore.collection(USERS)
                    .document(recipient.first)
                    .collection(INCOMING)
                    .document(shareId)
                    .set(share)
                    .await()

                userLinkRepository.sendBankAccountNotification(recipient.first, account, shareId, permission)
            }

            userLinkRepository.sendBankAccountEmail(trimmedEmail, account)
            Log.d(TAG, "Share created: $shareId")
            Result.success(share)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share account: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun revokeShare(share: BankAccountShare) {
        val uid = auth.currentUser?.uid ?: return
        if (share.ownerUid != uid) return

        val updates = mapOf("status" to ShareStatus.REVOKED.name)
        try {
            firestore.collection(USERS)
                .document(uid)
                .collection(OUTGOING)
                .document(share.shareId)
                .update(updates)
                .await()

            if (share.sharedWithUid.isNotBlank()) {
                firestore.collection(USERS)
                    .document(share.sharedWithUid)
                    .collection(INCOMING)
                    .document(share.shareId)
                    .delete()
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to revoke share: ${e.message}")
        }
    }

    suspend fun acceptShare(shareId: String): BankAccountEntity {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        val doc = firestore.collection(USERS)
            .document(uid)
            .collection(INCOMING)
            .document(shareId)
            .get()
            .await()

        val share = doc.toObject(BankAccountShare::class.java)?.copy(shareId = doc.id)
            ?: throw IllegalStateException("Share not found")

        return acceptShare(share)
    }

    suspend fun acceptShare(share: BankAccountShare): BankAccountEntity {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        val acceptedAt = System.currentTimeMillis()
        val updates = mapOf(
            "status" to ShareStatus.ACTIVE.name,
            "acceptedAt" to acceptedAt
        )

        firestore.collection(USERS)
            .document(uid)
            .collection(INCOMING)
            .document(share.shareId)
            .update(updates)
            .await()

        firestore.collection(USERS)
            .document(share.ownerUid)
            .collection(OUTGOING)
            .document(share.shareId)
            .update(updates)
            .await()

        return share.copy(status = ShareStatus.ACTIVE.name, acceptedAt = acceptedAt).toEntity()
    }

    suspend fun syncAccountToShares(account: BankAccountEntity) {
        val uid = auth.currentUser?.uid ?: return
        if (account.isSharedIncoming) return

        try {
            val snapshot = firestore.collection(USERS)
                .document(uid)
                .collection(OUTGOING)
                .whereEqualTo("accountLocalId", account.id)
                .get()
                .await()

            val activeDocs = snapshot.documents.filter { doc ->
                doc.getString("status") == ShareStatus.ACTIVE.name
            }

            if (activeDocs.isEmpty()) return

            val patch = mapOf(
                "accountName" to account.accountName,
                "accountNumber" to account.accountNumber,
                "bankName" to account.bankName,
                "branchName" to account.branchName,
                "swiftCode" to account.swiftCode,
                "coverImageUri" to account.coverImageUri,
                "isCard" to account.isCard,
                "isMfs" to account.isMfs,
                "mfsProvider" to account.mfsProvider,
                "qrCodeUri" to account.qrCodeUri
            )

            for (doc in activeDocs) {
                val share = doc.toObject(BankAccountShare::class.java)?.copy(shareId = doc.id) ?: continue
                doc.reference.update(patch).await()
                if (share.sharedWithUid.isNotBlank()) {
                    firestore.collection(USERS)
                        .document(share.sharedWithUid)
                        .collection(INCOMING)
                        .document(share.shareId)
                        .update(patch)
                        .await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync shares: ${e.message}")
        }
    }

    suspend fun removeSharesForAccount(account: BankAccountEntity) {
        val uid = auth.currentUser?.uid ?: return
        if (account.isSharedIncoming) return

        try {
            val snapshot = firestore.collection(USERS)
                .document(uid)
                .collection(OUTGOING)
                .whereEqualTo("accountLocalId", account.id)
                .get()
                .await()

            for (doc in snapshot.documents) {
                val share = doc.toObject(BankAccountShare::class.java)?.copy(shareId = doc.id) ?: continue
                revokeShare(share)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove shares for account: ${e.message}")
        }
    }

    fun observeOutgoingShares(accountLocalId: Long): Flow<List<BankAccountShare>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS)
            .document(uid)
            .collection(OUTGOING)
            .whereEqualTo("accountLocalId", accountLocalId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BankAccountShare::class.java)?.copy(shareId = doc.id)
                }?.filter { it.statusEnum != ShareStatus.REVOKED } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun observeIncomingShares(): Flow<List<BankAccountShare>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS)
            .document(uid)
            .collection(INCOMING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BankAccountShare::class.java)?.copy(shareId = doc.id)
                }?.filter { it.statusEnum != ShareStatus.REVOKED } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
}
