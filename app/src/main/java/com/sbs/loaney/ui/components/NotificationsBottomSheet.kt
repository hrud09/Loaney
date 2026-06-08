package com.sbs.loaney.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.model.LinkedLoanNotification
import com.sbs.loaney.ui.theme.AlimDark
import com.sbs.loaney.ui.theme.AlimGreen
import com.sbs.loaney.ui.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.sbs.loaney.util.PdfReceiptGenerator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    onDismiss: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "No notifications",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You have no new notifications.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onDeleteClick = { viewModel.deleteNotification(notification.id) },
                            onNotificationClick = {
                                if (!notification.isRead) {
                                    viewModel.markAsRead(notification.id)
                                }
                            },
                            onImportClick = { viewModel.importSharedBankAccount(notification) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: LinkedLoanNotification,
    onDeleteClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val context = LocalContext.current
    val isShareType = notification.loanType.startsWith("SHARE_")
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else AlimGreen.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNotificationClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isShareType) AlimGreen.copy(alpha = 0.2f)
                        else if (notification.loanType == "LEND") MaterialTheme.colorScheme.errorContainer 
                        else AlimGreen.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.senderName.take(1).uppercase(),
                    color = if (notification.loanType == "LEND" && !isShareType) MaterialTheme.colorScheme.error else AlimGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isShareType) {
                    val shareLabel = when (notification.loanType) {
                        "SHARE_CARD" -> "shared a card"
                        "SHARE_MFS" -> "shared an MFS account"
                        else -> "shared a bank account"
                    }
                    Text(
                        text = "${notification.senderName} $shareLabel: ${notification.bankName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = AlimDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Holder: ${notification.accountName}\nNo: ${notification.accountNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val actionText = if (notification.loanType == "LEND") "wants to borrow" else "lent you"
                    Text(
                        text = "${notification.senderName} $actionText ${notification.currency}${notification.amount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = AlimDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Return: ${dateFormat.format(Date(notification.promisedReturnDateMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(notification.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                // Shared Bank Account import action
                if (isShareType) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onImportClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlimGreen)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Shared PDF viewer action
                if (!notification.pdfBase64.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            PdfReceiptGenerator.openPdfFromBase64(context, notification.pdfBase64, notification.senderName)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        border = BorderStroke(1.dp, AlimGreen)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = AlimGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlimGreen)
                    }
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
