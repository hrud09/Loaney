package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.model.LinkedLoanNotification
import com.sbs.loaney.data.repository.UserLinkRepository
import com.sbs.loaney.data.repository.ILoanRepository
import com.sbs.loaney.data.local.entity.BankAccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val userLinkRepository: UserLinkRepository,
    private val repository: ILoanRepository
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
}
