package com.sbs.loaney.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sbs.loaney.R
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.ui.theme.*

@Composable
fun BankAccountCard(
    account: BankAccountEntity,
    context: Context,
    onDelete: (BankAccountEntity) -> Unit,
    onEdit: (BankAccountEntity) -> Unit,
    onShare: (BankAccountEntity) -> Unit
) {
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenWidth * 0.85f

    // MFS header color (only for the top section)
    val mfsHeaderColor = if (account.isMfs) {
        when (account.mfsProvider?.lowercase()) {
            "bkash" -> Color(0xFFE2136E) // Pink
            "nagad" -> Color(0xFFED4D36) // Orange
            "rocket" -> Color(0xFF8C158C) // Purple
            "upay" -> Color(0xFF005BAC) // Blue
            else -> MaterialTheme.colorScheme.primary
        }
    } else {
        Color.Unspecified
    }

    // Content area always uses standard theme colors
    val contentColor = MaterialTheme.colorScheme.onBackground
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .width(cardWidth)
            .padding(4.dp)
            .neubrutalistCard(
                shape = BigCardShape,
                backgroundColor = MaterialTheme.colorScheme.surface
            )
    ) {
        Column {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                if (!account.coverImageUri.isNullOrBlank() && !account.isMfs) {
                    AsyncImage(
                        model = account.coverImageUri,
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (account.isMfs) mfsHeaderColor else NbYellow
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val headerIcon = if (account.isMfs) Icons.Default.PhoneIphone else Icons.Default.AccountBalance
                        val headerTint = if (account.isMfs) NbPureWhite.copy(alpha = 0.4f) else NbPureBlack.copy(alpha = 0.1f)
                        Icon(headerIcon, contentDescription = null, tint = headerTint, modifier = Modifier.size(48.dp))
                    }
                }
                
                // Delete overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 10.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable { onDelete(account) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Edit overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 44.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable { onEdit(account) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Share overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 78.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable { onShare(account) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Copy All overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            val text = buildString {
                                append("${if (account.isMfs) "Provider" else "Bank"}: ${account.bankName}\n")
                                append("${if (account.isMfs) "Holder" else "Account Name"}: ${account.accountName}\n")
                                append("${if (account.isMfs) "Mobile No" else "Account No"}.: ${account.accountNumber}\n")
                                if (!account.branchName.isNullOrBlank()) append("Branch: ${account.branchName}\n")
                                if (!account.swiftCode.isNullOrBlank()) append("SWIFT: ${account.swiftCode}\n")
                            }
                            clipboardManager.setPrimaryClip(ClipData.newPlainText(if (account.isMfs) "MFS Account" else "Bank Account", text))
                            Toast.makeText(context, "All Details Copied!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy All", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground, thickness = 2.dp)

            // Details Section
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Bank Name", account.bankName))
                            Toast.makeText(context, "Name Copied!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    
                    if (account.isMfs && !account.qrCodeUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(2.dp)
                        ) {
                            AsyncImage(
                                model = account.qrCodeUri,
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText(if (account.isCard) "Card Number" else "Account Number", account.accountNumber))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    val numberLabel = when {
                        account.isCard -> "CARD NUMBER"
                        account.isMfs -> "MOBILE NUMBER"
                        else -> stringResource(id = R.string.account_number)
                    }
                    Text(numberLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp), color = labelColor)
                    
                    val displayNum = when {
                        account.isCard -> account.accountNumber.chunked(4).joinToString(" ")
                        else -> account.accountNumber
                    }
                    Text(
                        text = displayNum,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        ),
                        maxLines = 1
                    )
                }

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                val holderLabel = when {
                    account.isCard -> "Cardholder Name"
                    account.isMfs -> "Account Holder"
                    else -> stringResource(id = R.string.account_holder)
                }
                BankField(holderLabel, account.accountName, clipboardManager, context, labelColor, contentColor)

                if (!account.isMfs && (!account.branchName.isNullOrBlank() || !account.swiftCode.isNullOrBlank())) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!account.branchName.isNullOrBlank()) {
                            Box(modifier = Modifier.weight(1f)) {
                                BankField(stringResource(id = R.string.branch), account.branchName, clipboardManager, context, labelColor, contentColor)
                            }
                        }
                        if (!account.swiftCode.isNullOrBlank()) {
                            Box(modifier = Modifier.weight(1f)) {
                                BankField(stringResource(id = R.string.swift), account.swiftCode, clipboardManager, context, labelColor, contentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BankField(
    label: String, 
    value: String, 
    clipboardManager: ClipboardManager, 
    context: Context, 
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, 
    contentColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
                    Toast.makeText(context, "$label Copied!", Toast.LENGTH_SHORT).show()
                }
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp), color = labelColor)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = contentColor),
                maxLines = 1
            )
        }
    }
}
