package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.model.LinkedLoanNotification
import com.sbs.loaney.data.repository.UserLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val userLinkRepository: UserLinkRepository
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
}
