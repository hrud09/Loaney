package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.model.LinkedLoanNotification
import com.sbs.loaney.data.repository.UserLinkRepository
import com.sbs.loaney.data.repository.BankAccountShareRepository
import com.sbs.loaney.data.repository.ILoanRepository
import com.sbs.loaney.data.local.entity.BankAccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.local.entity.LoanEntity
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val userLinkRepository: UserLinkRepository,
    private val repository: ILoanRepository,
    private val shareRepository: BankAccountShareRepository
) : ViewModel() {

    private val processedSyncNotificationIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    val notifications: StateFlow<List<LinkedLoanNotification>> = userLinkRepository
        .observeIncomingNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            notifications.collect { list ->
                for (notif in list) {
                    processIncomingSyncNotification(notif)
                }
            }
        }
    }

    private suspend fun processIncomingSyncNotification(notification: LinkedLoanNotification) {
        val syncType = notification.notificationType
        if (syncType != "LINK_ACCEPTED" && syncType != "LINK_REJECTED" &&
            syncType != "PAYMENT_ADDED" && syncType != "LOAN_ITEM_ADDED" &&
            syncType != "LOAN_STATUS_UPDATED" && syncType != "LOAN_UPDATED") {
            return
        }

        if (!processedSyncNotificationIds.add(notification.id)) return

        val localLoanIdStr = notification.recipientLoanId ?: return
        val localLoanId = localLoanIdStr.toLongOrNull() ?: return

        when (syncType) {
            "LINK_ACCEPTED" -> {
                val remoteLoanId = notification.senderLoanId ?: return
                repository.acceptLoanLink(localLoanId, notification.senderUid, remoteLoanId)
            }
            "LINK_REJECTED" -> {
                val localLoanWithPayments = repository.getLoanById(localLoanId).firstOrNull() ?: return
                repository.updateLoan(localLoanWithPayments.loan.copy(
                    linkedOwnerUid = null,
                    linkedLoanId = null
                ))
            }
            "PAYMENT_ADDED" -> {
                val localLoanWithPayments = repository.getLoanById(localLoanId).firstOrNull() ?: return
                val amount = notification.paymentAmount ?: notification.amount
                if (amount <= 0.0) return

                val paymentTime = notification.paymentDateMillis ?: notification.createdAt
                val syncKey = notification.paymentSyncId
                val alreadyExists = localLoanWithPayments.payments.any { existing ->
                    (existing.amount == amount && Math.abs(existing.date.time - paymentTime) < 5000) ||
                            (syncKey != null && existing.note?.contains(syncKey) == true)
                }

                if (!alreadyExists) {
                    val payment = com.sbs.loaney.data.local.entity.PaymentEntity(
                        loanId = localLoanId,
                        amount = amount,
                        date = Date(paymentTime),
                        method = notification.paymentMethod ?: "Online",
                        note = notification.paymentNote ?: "Synced payment from ${notification.senderName}"
                    )
                    repository.insertPayment(payment)
                    updateLoanStatus(localLoanId)
                }
            }
            "LOAN_ITEM_ADDED" -> {
                val localLoanWithPayments = repository.getLoanById(localLoanId).firstOrNull() ?: return
                val amount = notification.itemAmount ?: notification.amount
                if (amount <= 0.0) return

                val itemTime = notification.itemDateMillis ?: notification.createdAt
                val syncKey = notification.itemSyncId
                val alreadyExists = localLoanWithPayments.loanItems.any { existing ->
                    (existing.amount == amount && Math.abs(existing.date.time - itemTime) < 5000) ||
                            (syncKey != null && existing.note?.contains(syncKey) == true)
                }

                if (!alreadyExists) {
                    val item = com.sbs.loaney.data.local.entity.LoanItemEntity(
                        loanId = localLoanId,
                        amount = amount,
                        date = Date(itemTime),
                        note = notification.itemNote ?: "Synced item from ${notification.senderName}"
                    )
                    repository.insertLoanItem(item)
                    updateLoanStatus(localLoanId)
                }
            }
            "LOAN_STATUS_UPDATED" -> {
                val localLoanWithPayments = repository.getLoanById(localLoanId).firstOrNull() ?: return
                val statusStr = notification.loanStatus ?: return
                try {
                    val newStatus = LoanStatus.valueOf(statusStr)
                    if (localLoanWithPayments.loan.status != newStatus) {
                        repository.updateLoan(localLoanWithPayments.loan.copy(
                            status = newStatus,
                            removedAt = if (newStatus == LoanStatus.FULLY_PAID) System.currentTimeMillis() else null
                        ))
                    }
                } catch (_: Exception) {}
            }
            "LOAN_UPDATED" -> {
                val localLoanWithPayments = repository.getLoanById(localLoanId).firstOrNull() ?: return
                val currentLoan = localLoanWithPayments.loan
                val updatedLoan = currentLoan.copy(
                    amount = if (notification.amount > 0.0) notification.amount else currentLoan.amount,
                    promisedReturnDate = if (notification.promisedReturnDateMillis > 0L) Date(notification.promisedReturnDateMillis) else currentLoan.promisedReturnDate,
                    purpose = notification.purpose ?: currentLoan.purpose,
                    notes = notification.notes ?: currentLoan.notes
                )
                repository.updateLoan(updatedLoan)
                updateLoanStatus(localLoanId)
            }
        }
    }

    private suspend fun updateLoanStatus(loanId: Long) {
        val loanWithPayments = repository.getLoanById(loanId).firstOrNull() ?: return
        val totalLoan = loanWithPayments.loan.amount + loanWithPayments.loanItems.sumOf { it.amount }
        val totalPaid = loanWithPayments.payments.sumOf { it.amount }
        val loan = loanWithPayments.loan

        val newStatus = when {
            totalPaid >= totalLoan -> LoanStatus.FULLY_PAID
            totalPaid > 0 -> LoanStatus.PARTIALLY_PAID
            Date().after(loan.promisedReturnDate) -> LoanStatus.OVERDUE
            else -> LoanStatus.ACTIVE
        }

        if (newStatus != loan.status) {
            repository.updateLoan(loan.copy(
                status = newStatus,
                removedAt = if (newStatus == LoanStatus.FULLY_PAID) System.currentTimeMillis() else null
            ))
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            userLinkRepository.markNotificationRead(notificationId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            userLinkRepository.deleteNotification(notificationId)
        }
    }

    fun importSharedBankAccount(notification: LinkedLoanNotification) {
        viewModelScope.launch {
            val account = BankAccountEntity(
                accountName = notification.accountName,
                accountNumber = notification.accountNumber,
                bankName = notification.bankName,
                branchName = notification.branchName,
                swiftCode = notification.swiftCode,
                coverImageUri = null,
                isCard = notification.isCard,
                isMfs = notification.isMfs,
                mfsProvider = notification.mfsProvider,
                qrCodeUri = notification.qrCodeUri
            )
            repository.insertBankAccount(account)
            userLinkRepository.deleteNotification(notification.id)
        }
    }

    fun acceptSharedBankAccount(notification: LinkedLoanNotification) {
        viewModelScope.launch {
            val shareId = notification.shareId ?: return@launch
            val entity = shareRepository.acceptShare(shareId)
            val existing = repository.getBankAccountByShareId(shareId)
            if (existing == null) {
                repository.insertBankAccount(entity)
            } else {
                repository.updateBankAccount(entity.copy(id = existing.id))
            }
            userLinkRepository.markNotificationRead(notification.id)
        }
    }

    fun approveLoanRequest(notification: LinkedLoanNotification) {
        viewModelScope.launch {
            val senderRefLoanId = notification.senderLoanId ?: notification.id
            
            // Prevent duplicate imports
            val existingLoans = repository.getAllLoansOnce()
            val alreadyImported = existingLoans.any { 
                it.loan.linkedOwnerUid == notification.senderUid && it.loan.linkedLoanId == senderRefLoanId 
            }
            if (alreadyImported) {
                userLinkRepository.deleteNotification(notification.id)
                return@launch
            }

            val myType = if (notification.loanType == "LEND") LoanType.BORROW else LoanType.LEND
            val loanDate = if (notification.loanDateMillis != null && notification.loanDateMillis > 0L) {
                Date(notification.loanDateMillis)
            } else {
                Date(notification.createdAt)
            }
            val returnDate = if (notification.promisedReturnDateMillis > 0L) {
                Date(notification.promisedReturnDateMillis)
            } else {
                Date(notification.createdAt)
            }

            val loan = LoanEntity(
                type = myType,
                personName = notification.senderName,
                phoneNumber = "", // Recipient can update contact details if needed
                amount = notification.amount,
                loanDate = loanDate,
                promisedReturnDate = returnDate,
                purpose = notification.purpose,
                notes = notification.notes,
                interest = notification.interest,
                status = LoanStatus.ACTIVE,
                linkedOwnerUid = notification.senderUid,
                linkedLoanId = senderRefLoanId
            )
            val myLoanId = repository.insertLoan(loan)
            
            // Notify sender that we accepted and linked
            userLinkRepository.sendLoanSyncNotification(
                recipientUid = notification.senderUid,
                notificationId = "link_acc_${System.currentTimeMillis()}",
                loanType = myType.name,
                notificationType = "LINK_ACCEPTED",
                senderLoanId = myLoanId.toString(),
                recipientLoanId = senderRefLoanId,
                amount = notification.amount,
                currency = notification.currency
            )
            
            userLinkRepository.deleteNotification(notification.id)
        }
    }

    fun rejectLoanRequest(notification: LinkedLoanNotification) {
        viewModelScope.launch {
            val senderRefLoanId = notification.senderLoanId ?: notification.id
            userLinkRepository.sendLinkRejectedNotification(
                recipientUid = notification.senderUid,
                senderLoanId = null,
                recipientLoanId = senderRefLoanId
            )
            userLinkRepository.deleteNotification(notification.id)
        }
    }

    fun importLinkedLoan(notification: LinkedLoanNotification) {
        approveLoanRequest(notification)
    }

    fun confirmLoanUpdate(notification: LinkedLoanNotification) {
        viewModelScope.launch {
            val proposedJson = notification.proposedChangesJson ?: return@launch
            val proposedLoan = Gson().fromJson(proposedJson, LoanEntity::class.java)
            
            // The notification has linkedLoanId which is my local loanId.
            // UPDATE_PROPOSAL uses recipientLoanId to tell us which of our local loans this applies to
            val localLoanIdStr = notification.recipientLoanId ?: return@launch
            val localLoanId = localLoanIdStr.toLongOrNull() ?: return@launch
            
            val localLoanWithPayments = repository.getLoanById(localLoanId).firstOrNull() ?: return@launch
            val currentLocalLoan = localLoanWithPayments.loan
            
            // Apply proposed changes (keep our own ID and linkage fields)
            val updatedLocalLoan = proposedLoan.copy(
                id = currentLocalLoan.id,
                linkedOwnerUid = currentLocalLoan.linkedOwnerUid,
                linkedLoanId = currentLocalLoan.linkedLoanId,
                pendingUpdateJson = null,
                // Invert type for the local copy since the proposed loan is from the sender's perspective
                type = if (proposedLoan.type == LoanType.LEND) LoanType.BORROW else LoanType.LEND,
                personName = currentLocalLoan.personName // Don't override their local name for this person
            )
            
            repository.updateLoan(updatedLocalLoan)
            
            userLinkRepository.sendLoanSyncNotification(
                recipientUid = notification.senderUid,
                notificationId = "upd_acc_${System.currentTimeMillis()}",
                loanType = "",
                notificationType = "UPDATE_ACCEPTED",
                senderLoanId = updatedLocalLoan.id.toString(),
                recipientLoanId = notification.senderLoanId
            )
            
            userLinkRepository.deleteNotification(notification.id)
        }
    }

    fun rejectLoanUpdate(notification: LinkedLoanNotification) {
        viewModelScope.launch {
            userLinkRepository.sendLoanSyncNotification(
                recipientUid = notification.senderUid,
                notificationId = "upd_rej_${System.currentTimeMillis()}",
                loanType = "",
                notificationType = "UPDATE_REJECTED",
                senderLoanId = notification.recipientLoanId,
                recipientLoanId = notification.senderLoanId
            )
            userLinkRepository.deleteNotification(notification.id)
        }
    }

    fun handleSyncResponse(notification: LinkedLoanNotification) {
        viewModelScope.launch {
            val localLoanIdStr = notification.recipientLoanId ?: return@launch
            val localLoanId = localLoanIdStr.toLongOrNull() ?: return@launch
            
            val localLoanWithPayments = repository.getLoanById(localLoanId).firstOrNull() ?: return@launch
            val currentLocalLoan = localLoanWithPayments.loan

            when (notification.notificationType) {
                "LINK_ACCEPTED" -> {
                    // Recipient accepted our initial loan request. 
                    // notification.senderLoanId is THEIR new loan ID.
                    val remoteLoanId = notification.senderLoanId ?: return@launch
                    repository.acceptLoanLink(localLoanId, notification.senderUid, remoteLoanId)
                }
                "UPDATE_ACCEPTED" -> {
                    // Recipient accepted our proposed changes. Apply our pendingUpdateJson.
                    val pendingJson = currentLocalLoan.pendingUpdateJson
                    if (pendingJson != null) {
                        val proposedLoan = Gson().fromJson(pendingJson, LoanEntity::class.java)
                        val finalLoan = proposedLoan.copy(pendingUpdateJson = null)
                        repository.updateLoan(finalLoan)
                    }
                }
                "UPDATE_REJECTED" -> {
                    // Recipient rejected. Just clear the pending update.
                    repository.updateLoan(currentLocalLoan.copy(pendingUpdateJson = null))
                }
            }
            
            userLinkRepository.deleteNotification(notification.id)
        }
    }
}
