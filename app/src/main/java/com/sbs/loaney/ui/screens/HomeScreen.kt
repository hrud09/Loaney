package com.sbs.loaney.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sbs.loaney.R
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.components.CustomLightTextField
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateToAddLoan: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToHistory: () -> Unit, // Replaces Manage navigation
    onNavigateToSettings: () -> Unit, // New
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddBankSheet by remember { mutableStateOf(false) }

    val balance = uiState.totalLent - uiState.totalBorrowed
    val bankAccounts = uiState.bankAccounts
    val allLoans = (uiState.lentLoans + uiState.borrowedLoans).sortedByDescending { it.loan.loanDate }
    val uniqueRecipients = allLoans.distinctBy { it.loan.personName }.take(5)
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- TOP HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.greeting_hi, uiState.userName),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = stringResource(id = R.string.greeting_morning),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSubtextDark
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(8.dp)
                                .background(NeonLime, CircleShape)
                        )
                    }
                }
            }

            // --- BALANCE CARD ---
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.your_balance),
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSubtextLight)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${String.format("%,.0f", balance)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonLime,
                            fontSize = 36.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ActionButton(icon = Icons.Default.ArrowDownward, label = stringResource(id = R.string.deposit)) { onNavigateToAddLoan() }
                        ActionButton(icon = Icons.Default.ArrowUpward, label = stringResource(id = R.string.withdraw)) { onNavigateToAddLoan() }
                        ActionButton(icon = Icons.Default.History, label = stringResource(id = R.string.history)) { onNavigateToHistory() }
                        ActionButton(icon = Icons.Default.Menu, label = stringResource(id = R.string.details)) { onNavigateToHistory() }
                    }
                }
            }

            // --- LATEST RECIPIENT ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.latest_recipient),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = "More", tint = TextSubtextDark)
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(uniqueRecipients, key = { it.loan.id }) { item ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.loan.personName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onNavigateToAddLoan() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Recipient", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }

            // --- TWO CARDS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Card (Dark) - Total Lent
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = stringResource(id = R.string.total_lent),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSubtextLight, lineHeight = 18.sp)
                            )
                            Icon(Icons.Default.Fastfood, contentDescription = null, tint = NeonLime, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "+${uiState.currencySymbol}${String.format("%,.0f", uiState.totalLent)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Right Card (Light) - Total Borrowed
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = stringResource(id = R.string.total_borrowed),
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSubtextDark, lineHeight = 18.sp)
                            )
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "-${uiState.currencySymbol}${String.format("%,.0f", uiState.totalBorrowed)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // --- BANK ACCOUNTS ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.bank_accounts),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = stringResource(id = R.string.add),
                        style = MaterialTheme.typography.labelLarge.copy(color = SkyBlue, fontWeight = FontWeight.Bold),
                        modifier = Modifier.clickable { showAddBankSheet = true }
                    )
                }

                if (bankAccounts.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_bank_accounts),
                        color = TextSubtextDark,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(bankAccounts, key = { it.id }) { account ->
                            BankAccountCard(
                                account = account,
                                context = context,
                                onDelete = { viewModel.deleteBankAccount(it) }
                            )
                        }
                    }
                }
            }

            // --- TRANSACTIONS ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.transactions),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = stringResource(id = R.string.see_all),
                        style = MaterialTheme.typography.labelLarge.copy(color = TextSubtextDark),
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }

                // ... Rest logic remains exactly the same logic but colors are tied to Theme.
                if (allLoans.isEmpty()) {
                    Text(stringResource(id = R.string.no_recent_transactions), color = TextSubtextDark, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    allLoans.take(5).forEach { item ->
                        val isLent = item.loan.type == LoanType.LEND
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail(item.loan.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.loan.personName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.loan.personName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isLent) stringResource(id = R.string.lent) else stringResource(id = R.string.borrowed),
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSubtextDark)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isLent) "+" else "-"}${uiState.currencySymbol} ${String.format("%,.0f", item.loan.amount)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = dateFormat.format(item.loan.loanDate),
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSubtextDark)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showAddBankSheet) {
        AddBankAccountBottomSheet(
            onDismiss = { showAddBankSheet = false },
            onAdd = { request ->
                viewModel.addBankAccount(
                    accountName = request.accountName,
                    accountNumber = request.accountNumber,
                    bankName = request.bankName,
                    branchName = request.branchName,
                    swiftCode = request.swiftCode,
                    coverImageUri = request.coverImageUri
                )
                showAddBankSheet = false
            }
        )
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF2C2C2E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = TextSubtextLight)
        )
    }
}

// --- BANK ACCOUNTS COMPONENTS ---

@Composable
fun BankAccountCard(
    account: BankAccountEntity,
    context: Context,
    onDelete: (BankAccountEntity) -> Unit
) {
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.width(320.dp)
    ) {
        Column {
            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                if (!account.coverImageUri.isNullOrBlank()) {
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
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E1E22), Color(0xFF2E2E36))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    }
                }
                
                // Copy All overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            val text = buildString {
                                append("Bank: ${account.bankName}\n")
                                append("Account Name: ${account.accountName}\n")
                                append("Account No.: ${account.accountNumber}\n")
                                if (!account.branchName.isNullOrBlank()) append("Branch: ${account.branchName}\n")
                                if (!account.swiftCode.isNullOrBlank()) append("SWIFT: ${account.swiftCode}\n")
                            }
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Bank Account", text))
                            Toast.makeText(context, "All Details Copied!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy All", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bank Name & Delete Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onDelete(account) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }

                // Main Account Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(id = R.string.account_number), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp), color = TextSubtextDark)
                        Text(
                            text = account.accountNumber,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.5.sp
                            ),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Account Number", account.accountNumber))
                            Toast.makeText(context, "Account No. Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SkyBlue, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                // Holder Name
                BankField(stringResource(id = R.string.account_holder), account.accountName, clipboardManager, context)

                // Branch & SWIFT Row
                if (!account.branchName.isNullOrBlank() || !account.swiftCode.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!account.branchName.isNullOrBlank()) {
                            Box(modifier = Modifier.weight(1f)) {
                                BankField(stringResource(id = R.string.branch), account.branchName, clipboardManager, context)
                            }
                        }
                        if (!account.swiftCode.isNullOrBlank()) {
                            Box(modifier = Modifier.weight(1f)) {
                                BankField(stringResource(id = R.string.swift), account.swiftCode, clipboardManager, context)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BankField(label: String, value: String, clipboardManager: ClipboardManager, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp), color = TextSubtextDark)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                maxLines = 1
            )
        }
        IconButton(
            onClick = {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "$label Copied!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
    }
}

data class AddBankAccountRequest(
    val accountName: String,
    val accountNumber: String,
    val bankName: String,
    val branchName: String?,
    val swiftCode: String?,
    val coverImageUri: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankAccountBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (AddBankAccountRequest) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var swiftCode by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> proofUri = uri }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(id = R.string.add_bank_account), 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onBackground
            )

            // Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (proofUri != null) {
                    AsyncImage(
                        model = proofUri,
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(id = R.string.tap_custom_cover), color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            CustomLightTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = stringResource(id = R.string.bank_name_hint),
                leadingIcon = Icons.Default.AccountBalance
            )

            CustomLightTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = stringResource(id = R.string.account_holder_name_hint),
                leadingIcon = Icons.Default.Person
            )

            CustomLightTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = stringResource(id = R.string.account_number_hint),
                leadingIcon = Icons.Default.DateRange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomLightTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = stringResource(id = R.string.branch_optional),
                    leadingIcon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f)
                )

                CustomLightTextField(
                    value = swiftCode,
                    onValueChange = { swiftCode = it },
                    label = stringResource(id = R.string.swift_optional),
                    leadingIcon = Icons.Default.Info,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Button(
                onClick = {
                    onAdd(AddBankAccountRequest(
                        accountName = accountName,
                        accountNumber = accountNumber,
                        bankName = bankName,
                        branchName = branchName.ifBlank { null },
                        swiftCode = swiftCode.ifBlank { null },
                        coverImageUri = proofUri?.toString()
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = Color.Black),
                enabled = accountName.isNotBlank() && accountNumber.isNotBlank() && bankName.isNotBlank()
            ) {
                Text(stringResource(id = R.string.save_bank_account), fontWeight = FontWeight.Bold)
            }
        }
    }
}
