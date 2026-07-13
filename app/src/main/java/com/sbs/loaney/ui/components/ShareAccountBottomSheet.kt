package com.sbs.loaney.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.model.BankAccountShare
import com.sbs.loaney.data.model.SharePermission
import com.sbs.loaney.data.model.ShareStatus
import com.sbs.loaney.ui.theme.AlimGreen
import com.sbs.loaney.ui.viewmodel.EmailLinkStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAccountBottomSheet(
    account: BankAccountEntity,
    shareEmail: String,
    onShareEmailChange: (String) -> Unit,
    shareStatus: EmailLinkStatus,
    shareLinkedName: String?,
    outgoingShares: List<BankAccountShare>,
    onShare: (SharePermission) -> Unit,
    onRevokeShare: (BankAccountShare) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPermission by remember { mutableStateOf(SharePermission.VIEW) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val accountLabel = when {
        account.isCard -> "Card"
        account.isMfs -> "MFS Account"
        else -> "Bank Account"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Share $accountLabel",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${account.bankName} • ${account.accountName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = shareEmail,
                onValueChange = onShareEmailChange,
                label = { Text("Recipient Email") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AlimGreen,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            when (shareStatus) {
                EmailLinkStatus.CHECKING -> {
                    Text("Checking database...", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                EmailLinkStatus.FOUND -> {
                    Text(
                        "Registered user: $shareLinkedName",
                        color = AlimGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                EmailLinkStatus.NOT_FOUND -> {
                    Text(
                        "No Loaney account found. An email with account details will be sent.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {}
            }

            Text("Permission", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SharePermission.entries.forEach { permission ->
                    FilterChip(
                        selected = selectedPermission == permission,
                        onClick = { selectedPermission = permission },
                        label = {
                            Text(
                                when (permission) {
                                    SharePermission.VIEW -> "View only"
                                    SharePermission.USE -> "Use in payments"
                                },
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AlimGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AlimGreen
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val text = buildString {
                            append("${if (account.isMfs) "Provider" else "Bank"}: ${account.bankName}\n")
                            append("Holder: ${account.accountName}\n")
                            append("Number: ${account.accountNumber}\n")
                            if (!account.branchName.isNullOrBlank()) append("Branch: ${account.branchName}\n")
                            if (!account.swiftCode.isNullOrBlank()) append("SWIFT: ${account.swiftCode}\n")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            putExtra(Intent.EXTRA_SUBJECT, "Shared $accountLabel from Loaney")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Other apps")
                }

                Button(
                    onClick = { onShare(selectedPermission) },
                    enabled = shareEmail.isNotBlank() &&
                        android.util.Patterns.EMAIL_ADDRESS.matcher(shareEmail).matches() &&
                        shareStatus != EmailLinkStatus.CHECKING,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AlimGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (outgoingShares.isNotEmpty()) {
                Text(
                    "Shared with",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(outgoingShares, key = { it.shareId }) { share ->
                        OutgoingShareRow(share = share, onRevoke = { onRevokeShare(share) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OutgoingShareRow(
    share: BankAccountShare,
    onRevoke: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = share.sharedWithName.ifBlank { share.sharedWithEmail },
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = buildString {
                        append(
                            when (share.permissionEnum) {
                                SharePermission.VIEW -> "View only"
                                SharePermission.USE -> "Can use in payments"
                            }
                        )
                        append(" • ")
                        append(
                            when (share.statusEnum) {
                                ShareStatus.PENDING -> "Pending"
                                ShareStatus.ACTIVE -> "Active"
                                ShareStatus.REVOKED -> "Revoked"
                            }
                        )
                        append(" • ")
                        append(dateFormat.format(Date(share.createdAt)))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRevoke) {
                Icon(Icons.Default.Close, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
