package com.sbs.loaney.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.LoanTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanTrackerScreen(
    loanId: Long,
    onNavigateBack: () -> Unit,
    viewModel: LoanTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddPaymentSheet by remember { mutableStateOf(false) }
    var showAddLoanSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSettleConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(loanId) {
        viewModel.selectLoan(loanId)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRed) },
            title = { Text(stringResource(id = R.string.delete_loan_title)) },
            text = { Text(stringResource(id = R.string.delete_loan_msg, uiState.selectedLoan?.loan?.personName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        uiState.selectedLoan?.loan?.let {
                            viewModel.deleteLoan(it)
                            onNavigateBack()
                        }
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel), color = Color.Gray)
                }
            },
            containerColor = Color.White,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = Color.Gray
        )
    }

    if (showSettleConfirmation) {
        AlertDialog(
            onDismissRequest = { showSettleConfirmation = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonLime) },
            title = { Text(stringResource(id = R.string.settle_loan_title)) },
            text = { Text(stringResource(id = R.string.settle_loan_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markAsSettled()
                        showSettleConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonLime)
                ) {
                    Text(stringResource(id = R.string.settle), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel), color = Color.Gray)
                }
            },
            containerColor = Color.White,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = Color.Gray
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.loan_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (uiState.selectedLoan != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (uiState.selectedLoan != null && uiState.selectedLoan?.loan?.status != LoanStatus.FULLY_PAID) {
                Surface(
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                         Button(
                             onClick = { showAddPaymentSheet = true },
                             modifier = Modifier
                                 .weight(1f)
                                 .height(56.dp),
                             shape = RoundedCornerShape(16.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = Color.Black)
                         ) {
                             Text(stringResource(id = R.string.pay), fontWeight = FontWeight.Bold)
                         }

                         Button(
                             onClick = { showAddLoanSheet = true },
                             modifier = Modifier
                                 .weight(1f)
                                 .height(56.dp),
                             shape = RoundedCornerShape(16.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = Color.White)
                         ) {
                             Text(stringResource(id = R.string.add_more), fontWeight = FontWeight.Bold)
                         }
                         
                         FilledIconButton(
                             onClick = { showSettleConfirmation = true },
                             modifier = Modifier.size(56.dp),
                             shape = RoundedCornerShape(16.dp),
                             colors = IconButtonDefaults.filledIconButtonColors(containerColor = DashboardBg)
                         ) {
                             Icon(Icons.Default.CheckCircle, contentDescription = "Settle", tint = NeonLime)
                         }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.selectedLoan == null) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = NeonLime)
             }
        } else {
            val loanItem = uiState.selectedLoan!!
            val loan = loanItem.loan
            val payments = loanItem.payments
            val loanItems = loanItem.loanItems
            
            val totalLoan = loan.amount + loanItems.sumOf { it.amount }
            val paid = payments.sumOf { it.amount }
            val remaining = (totalLoan - paid).coerceAtLeast(0.0)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            // Combine all events for history
            val historyEvents = (
                listOf(HistoryEvent(loan.loanDate, stringResource(id = R.string.initial_loan), "${uiState.currencySymbol}${String.format("%.0f", loan.amount)}", true)) +
                loanItems.map { HistoryEvent(it.date, stringResource(id = R.string.additional_loan), "+${uiState.currencySymbol}${String.format("%.0f", it.amount)}", true) } +
                payments.map { HistoryEvent(it.date, stringResource(id = R.string.payment_received), "-${uiState.currencySymbol}${String.format("%.0f", it.amount)}", false, it.method) }
            ).sortedByDescending { it.date }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Card - Clean White Card
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, SubtleBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loan.personName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.due_date_format, dateFormat.format(loan.promisedReturnDate)),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.0f", remaining)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!loan.email.isNullOrBlank()) {
                                    FilledIconButton(
                                        onClick = {
                                             val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${loan.email}"))
                                             context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(36.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = DashboardBg)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                                    }
                                }
                                FilledIconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${loan.phoneNumber}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = DashboardBg)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Info Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile(
                        title = stringResource(id = R.string.total_amount), 
                        value = "${uiState.currencySymbol}${String.format("%.0f", totalLoan)}", 
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        title = stringResource(id = R.string.loan_type), 
                        value = if (loan.type.name == "LEND") stringResource(id = R.string.lent) else stringResource(id = R.string.borrowed), 
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Detailed Information
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, SubtleBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow(icon = Icons.Default.Info, label = stringResource(id = R.string.purpose), value = loan.purpose ?: stringResource(id = R.string.not_specified))
                        DetailRow(icon = Icons.Default.LocationOn, label = stringResource(id = R.string.address), value = loan.address ?: stringResource(id = R.string.not_specified))
                        DetailRow(icon = Icons.AutoMirrored.Filled.Notes, label = stringResource(id = R.string.notes), value = loan.notes ?: stringResource(id = R.string.no_notes))
                        
                        val statusColor = when (loan.status) {
                            LoanStatus.OVERDUE -> CoralRed
                            LoanStatus.FULLY_PAID -> NeonLime // Text on white would be hard to read if just neon lime, we use dark text with accent
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                        DetailRow(icon = Icons.Default.CheckCircle, label = "Status", value = loan.status.name, highlightColor = statusColor)
                    }
                }

                // Timeline / History
                Text(
                    text = stringResource(id = R.string.history_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (historyEvents.isEmpty()) {
                        Text(stringResource(id = R.string.no_history), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                    historyEvents.forEach { event ->
                        TimelineItem(
                            date = dateFormat.format(event.date),
                            title = event.title,
                            subtitle = if (event.method != null) stringResource(id = R.string.via_method, event.method) else "",
                            amount = event.amount,
                            isLoan = event.isLoan
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    
    if (showAddPaymentSheet) {
        AddPaymentBottomSheet(
            onDismiss = { showAddPaymentSheet = false },
            onAddPayment = { amount, method, note, proofUri ->
                viewModel.addPayment(amount, method, note, proofUri)
                showAddPaymentSheet = false
            }
        )
    }

    if (showAddLoanSheet) {
        AddMoreLoanBottomSheet(
            onDismiss = { showAddLoanSheet = false },
            onAddLoan = { amount, note, proofUri ->
                viewModel.addLoanItem(amount, note, proofUri)
                showAddLoanSheet = false
            }
        )
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, highlightColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                value, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Bold,
                color = highlightColor ?: MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

data class HistoryEvent(
    val date: Date,
    val title: String,
    val amount: String,
    val isLoan: Boolean,
    val method: String? = null
)

@Composable
fun InfoTile(title: String, value: String, highlight: Boolean = false, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, SubtleBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = if (highlight) NeonLime else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun TimelineItem(date: String, title: String, subtitle: String, isLoan: Boolean = false, amount: String? = null) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
            Text(
                text = date.substringBefore(" "), // Day
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = date.split(" ").getOrNull(1) ?: "", // Month
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Card(
            modifier = Modifier.weight(1f).border(1.dp, SubtleBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (isLoan) CoralRed else NeonLime, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                if (amount != null) {
                    Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isLoan) CoralRed else MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentBottomSheet(
    onDismiss: () -> Unit,
    onAddPayment: (Double, String, String?, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") } 
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> proofUri = uri }
    
    val methods = listOf("Cash", "Bank", "bKash", "Nagad", "Rocket")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp) 
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(stringResource(id = R.string.add_payment), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text(stringResource(id = R.string.amount), color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = NeonLime,
                    unfocusedBorderColor = SubtleBorder,
                    cursorColor = NeonLime
                )
            )
            
            Column {
                Text(stringResource(id = R.string.payment_method), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.take(3).forEach { m ->
                        val selected = method == m
                        FilterChip(
                            selected = selected,
                            onClick = { method = m },
                            label = { Text(m) },
                             colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonLime.copy(alpha=0.3f),
                                selectedLabelColor = Color.Black,
                                containerColor = DashboardBg,
                                labelColor = Color.Gray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = NeonLime,
                                enabled = true,
                                selected = selected
                            ),
                            shape = CircleShape
                        )
                    }
                }
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.drop(3).forEach { m ->
                        val selected = method == m
                        FilterChip(
                            selected = selected,
                            onClick = { method = m },
                            label = { Text(m) },
                             colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonLime.copy(alpha=0.3f),
                                selectedLabelColor = Color.Black,
                                containerColor = DashboardBg,
                                labelColor = Color.Gray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = NeonLime,
                                enabled = true,
                                selected = selected
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(id = R.string.note_optional), color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = NeonLime,
                    unfocusedBorderColor = SubtleBorder
                )
            )
            
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let {
                        onAddPayment(it, method, note.ifBlank { null }, proofUri?.toString())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = Color.Black),
                enabled = amount.isNotBlank()
            ) {
                Text(stringResource(id = R.string.confirm_payment), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoreLoanBottomSheet(
    onDismiss: () -> Unit,
    onAddLoan: (Double, String?, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> proofUri = uri }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(stringResource(id = R.string.add_more_loan), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text(stringResource(id = R.string.amount), color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = SkyBlue,
                    unfocusedBorderColor = SubtleBorder,
                    cursorColor = SkyBlue
                )
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(id = R.string.note_optional), color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = SkyBlue,
                    unfocusedBorderColor = SubtleBorder
                )
            )
            
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let {
                        onAddLoan(it, note.ifBlank { null }, proofUri?.toString())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = Color.White),
                enabled = amount.isNotBlank()
            ) {
                Text(stringResource(id = R.string.add_to_balance), fontWeight = FontWeight.Bold)
            }
        }
    }
}
