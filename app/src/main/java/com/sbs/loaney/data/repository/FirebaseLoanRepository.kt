package com.sbs.loaney.data.repository

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
import javax.inject.Inject

class FirebaseLoanRepository @Inject constructor() : ILoanRepository {

    private val firestore = FirebaseFirestore.getInstance()
    
    // Firestore Collection Names
    private val LOANS_COLLECTION = "loans"
    private val PAYMENTS_COLLECTION = "payments"
    private val LOAN_ITEMS_COLLECTION = "loanItems"
    private val BANK_ACCOUNTS_COLLECTION = "bankAccounts"

    // Helper to observe Firestore collections as Flow
    private inline fun <reified T> Query.snapshotsFlow(): Flow<List<T>> = callbackFlow {
        val listener = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.toObjects(T::class.java) ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    private val allLoansBaseFlow = firestore.collection(LOANS_COLLECTION).snapshotsFlow<LoanEntity>()
    private val allPaymentsBaseFlow = firestore.collectionGroup(PAYMENTS_COLLECTION).snapshotsFlow<PaymentEntity>()
    private val allLoanItemsBaseFlow = firestore.collectionGroup(LOAN_ITEMS_COLLECTION).snapshotsFlow<LoanItemEntity>()

    override fun getAllLoans(): Flow<List<LoanWithPayments>> {
        return combine(allLoansBaseFlow, allPaymentsBaseFlow, allLoanItemsBaseFlow) { loans, payments, items ->
            loans.filter { !it.isDeleted && it.status != LoanStatus.FULLY_PAID && it.status != LoanStatus.FORGIVEN }
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
            val loan = loans.find { it.id == loanId && !it.isDeleted } ?: return@combine null
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
        firestore.collection(LOANS_COLLECTION).document(id.toString()).set(newLoan).await()
        return id
    }

    override suspend fun updateLoan(loan: LoanEntity) {
        firestore.collection(LOANS_COLLECTION).document(loan.id.toString()).set(loan).await()
    }

    override suspend fun softDeleteLoan(loanId: Long, timestamp: Long) {
        val updates = mapOf(
            "isDeleted" to true,
            "removedAt" to timestamp
        )
        firestore.collection(LOANS_COLLECTION).document(loanId.toString()).update(updates).await()
    }

    override suspend fun restoreLoan(loanId: Long) {
        val updates = mapOf(
            "isDeleted" to false,
            "removedAt" to null
        )
        firestore.collection(LOANS_COLLECTION).document(loanId.toString()).update(updates).await()
    }

    override fun getDeletedLoans(): Flow<List<LoanWithPayments>> {
        return combine(allLoansBaseFlow, allPaymentsBaseFlow, allLoanItemsBaseFlow) { loans, payments, items ->
            loans.filter { it.isDeleted || it.status == LoanStatus.FULLY_PAID || it.status == LoanStatus.FORGIVEN }
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
        firestore.collection(LOANS_COLLECTION).document(loan.id.toString()).delete().await()
    }

    override suspend fun deleteExpiredLoans(threshold: Long) {
        val snapshot = firestore.collection(LOANS_COLLECTION).get().await()
        for (doc in snapshot.documents) {
            val loan = doc.toObject(LoanEntity::class.java) ?: continue
            if ((loan.isDeleted || loan.status == LoanStatus.FULLY_PAID || loan.status == LoanStatus.FORGIVEN) &&
                loan.removedAt != null && loan.removedAt < threshold) {
                firestore.collection(LOANS_COLLECTION).document(doc.id).delete().await()
            }
        }
    }

    override suspend fun insertPayment(payment: PaymentEntity) {
        val id = if (payment.id == 0L) System.currentTimeMillis() else payment.id
        val newPayment = payment.copy(id = id)
        firestore.collection(LOANS_COLLECTION).document(newPayment.loanId.toString())
                 .collection(PAYMENTS_COLLECTION).document(id.toString()).set(newPayment).await()
    }

    override suspend fun deletePayment(payment: PaymentEntity) {
        firestore.collection(LOANS_COLLECTION).document(payment.loanId.toString())
                 .collection(PAYMENTS_COLLECTION).document(payment.id.toString()).delete().await()
    }

    override suspend fun insertLoanItem(loanItem: LoanItemEntity) {
        val id = if (loanItem.id == 0L) System.currentTimeMillis() else loanItem.id
        val newItem = loanItem.copy(id = id)
        firestore.collection(LOANS_COLLECTION).document(newItem.loanId.toString())
                 .collection(LOAN_ITEMS_COLLECTION).document(id.toString()).set(newItem).await()
    }

    override suspend fun deleteLoanItem(loanItem: LoanItemEntity) {
        firestore.collection(LOANS_COLLECTION).document(loanItem.loanId.toString())
                 .collection(LOAN_ITEMS_COLLECTION).document(loanItem.id.toString()).delete().await()
    }

    override fun getAllBankAccounts(): Flow<List<BankAccountEntity>> {
        return firestore.collection(BANK_ACCOUNTS_COLLECTION).snapshotsFlow()
    }

    override suspend fun insertBankAccount(account: BankAccountEntity): Long {
        val id = if (account.id == 0L) System.currentTimeMillis() else account.id
        val newAccount = account.copy(id = id)
        firestore.collection(BANK_ACCOUNTS_COLLECTION).document(id.toString()).set(newAccount).await()
        return id
    }

    override suspend fun updateBankAccount(account: BankAccountEntity) {
        firestore.collection(BANK_ACCOUNTS_COLLECTION).document(account.id.toString()).set(account).await()
    }

    override suspend fun deleteBankAccount(account: BankAccountEntity) {
        firestore.collection(BANK_ACCOUNTS_COLLECTION).document(account.id.toString()).delete().await()
    }
}
