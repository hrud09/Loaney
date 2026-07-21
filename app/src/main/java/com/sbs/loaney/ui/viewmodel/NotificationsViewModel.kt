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

    val notifications: StateFlow<List<LinkedLoanNotification>> = userLinkRepository
        .observeIncomingNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    fun importLinkedLoan(notification: LinkedLoanNotification) {
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
            val loan = LoanEntity(
                type = myType,
                personName = notification.senderName,
                phoneNumber = "", // Will require manual update if needed
                amount = notification.amount,
                loanDate = Date(notification.createdAt),
                promisedReturnDate = Date(notification.promisedReturnDateMillis),
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
                recipientLoanId = senderRefLoanId
            )
            
            userLinkRepository.deleteNotification(notification.id)
        }
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
