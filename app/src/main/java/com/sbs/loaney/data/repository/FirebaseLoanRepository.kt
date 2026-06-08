package com.sbs.loaney.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.local.entity.LoanEntity
import com.sbs.loaney.data.local.entity.LoanItemEntity
import com.sbs.loaney.data.local.entity.PaymentEntity
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FirebaseLoanRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ILoanRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Firestore Collection Names
    private val LOANS_COLLECTION = "loans"
    private val PAYMENTS_COLLECTION = "payments"
    private val LOAN_ITEMS_COLLECTION = "loanItems"
    private val BANK_ACCOUNTS_COLLECTION = "bankAccounts"

    private inline fun <reified T> getSubcollectionFlow(subcollectionName: String): Flow<List<T>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(uid).collection(subcollectionName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList()) // Safely fallback if network/permissions fail
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(T::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    private suspend fun getLoanByIdOnce(loanId: Long): LoanEntity? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val doc = firestore.collection("users").document(uid)
                .collection(LOANS_COLLECTION).document(loanId.toString()).get().await()
            doc.toObject(LoanEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun sendChangeEmail(loan: LoanEntity, subject: String, body: String) {
        val email = loan.email
        if (email.isNullOrBlank()) return
        val currentUser = auth.currentUser ?: return
        val senderUid = currentUser.uid

        try {
            val senderDoc = firestore.collection("users").document(senderUid).get().await()
            val senderName = senderDoc.getString("name") ?: currentUser.displayName ?: "Someone"

            val formattedSubject = subject.replace("\$senderName", senderName)
            val formattedBody = body.replace("\$senderName", senderName)

            val emailDoc = mapOf(
                "to" to email,
                "message" to mapOf(
                    "subject" to formattedSubject,
                    "text" to formattedBody
                )
            )
            firestore.collection("mail").add(emailDoc).await()
            android.util.Log.d("FirebaseLoanRepository", "Change email queued for: $email")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseLoanRepository", "Failed to queue change email: ${e.message}")
        }
    }

    private fun getUserDocRef() = firestore.collection("users").document(
        auth.currentUser?.uid ?: throw Exception("Unauthorized: Please log in again to sync data.")
    )

    private val allLoansBaseFlow = getSubcollectionFlow<LoanEntity>(LOANS_COLLECTION)
    private val allPaymentsBaseFlow = getSubcollectionFlow<PaymentEntity>(PAYMENTS_COLLECTION)
    private val allLoanItemsBaseFlow = getSubcollectionFlow<LoanItemEntity>(LOAN_ITEMS_COLLECTION)

    override fun getAllLoans(): Flow<List<LoanWithPayments>> {
        return combine(allLoansBaseFlow, allPaymentsBaseFlow, allLoanItemsBaseFlow) { loans, payments, items ->
            loans.filter { !it.deleted && it.status != LoanStatus.FULLY_PAID && it.status != LoanStatus.FORGIVEN }
                 .sortedByDescending { it.createdAt }
                 .map { loan ->
                     LoanWithPayments(
                         loan = loan,
                         payments = payments.filter { it.loanId == loan.id },
                         loanItems = items.filter { it.loanId == loan.id }
                     )
                 }
        }
    }

    override fun getLoansByType(type: LoanType): Flow<List<LoanWithPayments>> {
        return getAllLoans().map { loans ->
            loans.filter { it.loan.type == type }
        }
    }

    override fun getLoanById(loanId: Long): Flow<LoanWithPayments?> {
        return combine(allLoansBaseFlow, allPaymentsBaseFlow, allLoanItemsBaseFlow) { loans, payments, items ->
            val loan = loans.find { it.id == loanId } ?: return@combine null
            LoanWithPayments(
                loan = loan,
                payments = payments.filter { it.loanId == loanId },
                loanItems = items.filter { it.loanId == loanId }
            )
        }
    }

    override suspend fun insertLoan(loan: LoanEntity): Long {
        val id = if (loan.id == 0L) System.currentTimeMillis() else loan.id
        val newLoan = loan.copy(id = id)
        getUserDocRef().collection(LOANS_COLLECTION).document(id.toString()).set(newLoan).await()
        return id
    }

    override suspend fun updateLoan(loan: LoanEntity) {
        val oldLoan = getLoanByIdOnce(loan.id)
        getUserDocRef().collection(LOANS_COLLECTION).document(loan.id.toString()).set(loan).await()
        
        if (oldLoan != null && !loan.email.isNullOrBlank()) {
            val currency = settingsRepository.currencySymbolFlow.first()
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val oldReturn = oldLoan.promisedReturnDate?.let { dateFormat.format(it) } ?: "None"
            val newReturn = loan.promisedReturnDate?.let { dateFormat.format(it) } ?: "None"
            
            var detailsChanged = ""
            if (oldLoan.amount != loan.amount) {
                detailsChanged += "- Amount changed from $currency${oldLoan.amount} to $currency${loan.amount}\n"
            }
            if (oldLoan.promisedReturnDate != loan.promisedReturnDate) {
                detailsChanged += "- Promised return date changed from $oldReturn to $newReturn\n"
            }
            if (oldLoan.status != loan.status) {
                detailsChanged += "- Status changed from ${oldLoan.status} to ${loan.status}\n"
            }
            if (oldLoan.notes != loan.notes || oldLoan.purpose != loan.purpose) {
                detailsChanged += "- Description/Notes updated.\n"
            }
            
            if (detailsChanged.isNotEmpty()) {
                val subject = "Loaney Update: Tracked Loan Details Modified by \$senderName"
                val body = "Hi there,\n\n\$senderName updated the details of your tracked loan:\n\n$detailsChanged\nLogin to the Loaney app to view details."
                sendChangeEmail(loan, subject, body)
            }
        }
    }

    override suspend fun softDeleteLoan(loanId: Long, timestamp: Long, notes: String?) {
        val updates = mutableMapOf<String, Any?>(
            "deleted" to true,
            "removedAt" to timestamp
        )
        if (notes != null) {
            updates["notes"] = notes
        }
        val loan = getLoanByIdOnce(loanId)
        getUserDocRef().collection(LOANS_COLLECTION).document(loanId.toString()).update(updates).await()
        
        if (loan != null && !loan.email.isNullOrBlank()) {
            val subject = "Loaney Update: Tracked Loan Settle/Deleted by \$senderName"
            val body = "Hi there,\n\n\$senderName has removed or settled the tracked loan from their list.\n\nLogin to the Loaney app to view details."
            sendChangeEmail(loan, subject, body)
        }
    }

    override suspend fun restoreLoan(loanId: Long) {
        val uid = auth.currentUser?.uid ?: return
        val userDoc = firestore.collection("users").document(uid)
        
        // Fetch loan
        val loanDoc = userDoc.collection(LOANS_COLLECTION).document(loanId.toString()).get().await()
        val loan = loanDoc.toObject(LoanEntity::class.java) ?: return
        
        // Fetch payments and items to recalculate status
        val paymentsSnapshot = userDoc.collection(PAYMENTS_COLLECTION).whereEqualTo("loanId", loanId).get().await()
        val payments = paymentsSnapshot.toObjects(PaymentEntity::class.java)
        
        val itemsSnapshot = userDoc.collection(LOAN_ITEMS_COLLECTION).whereEqualTo("loanId", loanId).get().await()
        val items = itemsSnapshot.toObjects(LoanItemEntity::class.java)
        
        val totalLoan = loan.amount + items.sumOf { it.amount }
        val paid = payments.sumOf { it.amount }
        
        var newStatus = when {
            paid >= totalLoan -> LoanStatus.FULLY_PAID
            paid > 0 -> LoanStatus.PARTIALLY_PAID
            java.util.Date().after(loan.promisedReturnDate) -> LoanStatus.OVERDUE
            else -> LoanStatus.ACTIVE
        }
        
        // If we are restoring it from History, it should NOT be in a completed state
        if (newStatus == LoanStatus.FULLY_PAID || newStatus == LoanStatus.FORGIVEN) {
            newStatus = if (paid > 0) LoanStatus.PARTIALLY_PAID else LoanStatus.ACTIVE
        }
        
        val updates = mapOf(
            "deleted" to false,
            "removedAt" to null,
            "status" to newStatus.name
        )
        userDoc.collection(LOANS_COLLECTION).document(loanId.toString()).update(updates).await()
    }

    override fun getDeletedLoans(): Flow<List<LoanWithPayments>> {
        return combine(allLoansBaseFlow, allPaymentsBaseFlow, allLoanItemsBaseFlow) { loans, payments, items ->
            loans.filter { it.deleted || it.status == LoanStatus.FULLY_PAID || it.status == LoanStatus.FORGIVEN }
                 .sortedByDescending { it.removedAt ?: 0L }
                 .map { loan ->
                     LoanWithPayments(
                         loan = loan,
                         payments = payments.filter { it.loanId == loan.id },
                         loanItems = items.filter { it.loanId == loan.id }
                     )
                 }
        }
    }

    override suspend fun deleteLoan(loan: LoanEntity) {
        getUserDocRef().collection(LOANS_COLLECTION).document(loan.id.toString()).delete().await()
    }

    override suspend fun deleteExpiredLoans(threshold: Long) {
        val snapshot = getUserDocRef().collection(LOANS_COLLECTION).get().await()
        for (doc in snapshot.documents) {
            val loan = doc.toObject(LoanEntity::class.java) ?: continue
            if ((loan.deleted || loan.status == LoanStatus.FULLY_PAID || loan.status == LoanStatus.FORGIVEN) &&
                loan.removedAt != null && loan.removedAt < threshold) {
                getUserDocRef().collection(LOANS_COLLECTION).document(doc.id).delete().await()
            }
        }
    }

    override suspend fun insertPayment(payment: PaymentEntity) {
        val id = if (payment.id == 0L) System.currentTimeMillis() else payment.id
        val newPayment = payment.copy(id = id)
        getUserDocRef().collection(PAYMENTS_COLLECTION).document(id.toString()).set(newPayment).await()
        
        val loan = getLoanByIdOnce(payment.loanId)
        if (loan != null && !loan.email.isNullOrBlank()) {
            val currency = settingsRepository.currencySymbolFlow.first()
            val subject = "Loaney Update: Payment Added by \$senderName"
            val body = "Hi there,\n\n\$senderName added a payment of $currency${payment.amount} to your tracked loan.\n\nPayment Details:\n- Amount: $currency${payment.amount}\n- Method: ${payment.method}\n- Note: ${payment.note ?: "None"}\n\nLogin to the Loaney app to view details."
            sendChangeEmail(loan, subject, body)
        }
    }

    override suspend fun deletePayment(payment: PaymentEntity) {
        getUserDocRef().collection(PAYMENTS_COLLECTION).document(payment.id.toString()).delete().await()
        
        val loan = getLoanByIdOnce(payment.loanId)
        if (loan != null && !loan.email.isNullOrBlank()) {
            val currency = settingsRepository.currencySymbolFlow.first()
            val subject = "Loaney Update: Payment Removed by \$senderName"
            val body = "Hi there,\n\n\$senderName removed a payment of $currency${payment.amount} from your tracked loan.\n\nLogin to the Loaney app to view details."
            sendChangeEmail(loan, subject, body)
        }
    }

    override suspend fun insertLoanItem(loanItem: LoanItemEntity) {
        val id = if (loanItem.id == 0L) System.currentTimeMillis() else loanItem.id
        val newItem = loanItem.copy(id = id)
        getUserDocRef().collection(LOAN_ITEMS_COLLECTION).document(id.toString()).set(newItem).await()
        
        val loan = getLoanByIdOnce(loanItem.loanId)
        if (loan != null && !loan.email.isNullOrBlank()) {
            val currency = settingsRepository.currencySymbolFlow.first()
            val subject = "Loaney Update: Item Added by \$senderName"
            val body = "Hi there,\n\n\$senderName added a physical item/asset valued at $currency${loanItem.amount} to your tracked loan.\n\nItem Details:\n- Estimated Value: $currency${loanItem.amount}\n- Note/Description: ${loanItem.note ?: "None"}\n\nLogin to the Loaney app to view details."
            sendChangeEmail(loan, subject, body)
        }
    }

    override suspend fun deleteLoanItem(loanItem: LoanItemEntity) {
        getUserDocRef().collection(LOAN_ITEMS_COLLECTION).document(loanItem.id.toString()).delete().await()
        
        val loan = getLoanByIdOnce(loanItem.loanId)
        if (loan != null && !loan.email.isNullOrBlank()) {
            val currency = settingsRepository.currencySymbolFlow.first()
            val subject = "Loaney Update: Item Removed by \$senderName"
            val body = "Hi there,\n\n\$senderName removed an item valued at $currency${loanItem.amount} from your tracked loan.\n\nLogin to the Loaney app to view details."
            sendChangeEmail(loan, subject, body)
        }
    }

    override fun getAllBankAccounts(): Flow<List<BankAccountEntity>> {
        return getSubcollectionFlow<BankAccountEntity>(BANK_ACCOUNTS_COLLECTION)
    }

    override suspend fun insertBankAccount(account: BankAccountEntity): Long {
        val id = if (account.id == 0L) System.currentTimeMillis() else account.id
        val newAccount = account.copy(id = id)
        getUserDocRef().collection(BANK_ACCOUNTS_COLLECTION).document(id.toString()).set(newAccount).await()
        return id
    }

    override suspend fun updateBankAccount(account: BankAccountEntity) {
        getUserDocRef().collection(BANK_ACCOUNTS_COLLECTION).document(account.id.toString()).set(account).await()
    }

    override suspend fun deleteBankAccount(account: BankAccountEntity) {
        getUserDocRef().collection(BANK_ACCOUNTS_COLLECTION).document(account.id.toString()).delete().await()
    }
}
