package com.sbs.loaney.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
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
import com.sbs.loaney.ui.theme.PrimaryLime
import com.sbs.loaney.ui.theme.SecondaryOrange
import com.sbs.loaney.ui.theme.TertiaryRed
import com.sbs.loaney.ui.viewmodel.LoanTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    LaunchedEffect(loanId) {
        viewModel.selectLoan(loanId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Loan Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.selectedLoan != null) {
                        IconButton(onClick = { 
                            viewModel.deleteLoan(uiState.selectedLoan!!.loan)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TertiaryRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (uiState.selectedLoan != null && uiState.selectedLoan?.loan?.status != LoanStatus.FULLY_PAID) {
                // Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 16.dp), // Check safe area
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                         Button(
                             onClick = { showAddPaymentSheet = true },
                             modifier = Modifier
                                 .weight(1f)
                                 .height(56.dp),
                             shape = RoundedCornerShape(16.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = PrimaryLime, contentColor = Color.Black)
                         ) {
                             Text("Pay", fontWeight = FontWeight.Bold)
                         }

                         Button(
                             onClick = { showAddLoanSheet = true },
                             modifier = Modifier
                                 .weight(1f)
                                 .height(56.dp),
                             shape = RoundedCornerShape(16.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = SecondaryOrange, contentColor = Color.Black)
                         ) {
                             Text("Add More", fontWeight = FontWeight.Bold)
                         }
                         
                         FilledIconButton(
                             onClick = { viewModel.markAsSettled() },
                             modifier = Modifier.size(56.dp),
                             shape = RoundedCornerShape(16.dp),
                             colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                         ) {
                             Icon(Icons.Default.CheckCircle, contentDescription = "Settle", tint = PrimaryLime)
                         }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.selectedLoan == null) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = PrimaryLime)
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
            
             // Colors determine by status
            val statusColor = when (loan.status) {
                LoanStatus.OVERDUE -> TertiaryRed
                LoanStatus.FULLY_PAID -> PrimaryLime
                else -> SecondaryOrange
            }

            // Combine all events for history
            val historyEvents = (
                listOf(HistoryEvent(loan.loanDate, "Initial Loan", "৳${String.format("%.0f", loan.amount)}", true)) +
                loanItems.map { HistoryEvent(it.date, "Additional Loan", "+৳${String.format("%.0f", it.amount)}", true) } +
                payments.map { HistoryEvent(it.date, "Payment Received", "-৳${String.format("%.0f", it.amount)}", false, it.method) }
            ).sortedByDescending { it.date }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Card - Rearranged to be thinner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = statusColor)
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
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Due ${dateFormat.format(loan.promisedReturnDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "৳${String.format("%.0f", remaining)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
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
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.Black, modifier = Modifier.size(18.dp))
                                    }
                                }
                                FilledIconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${loan.phoneNumber}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.1f))
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.Black, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Info Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile(
                        title = "Total Amount", 
                        value = "৳${String.format("%.0f", totalLoan)}", 
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        title = "Loan Type", 
                        value = loan.type.name, 
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Detailed Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow(icon = Icons.Default.Info, label = "Purpose", value = loan.purpose ?: "Not specified")
                        DetailRow(icon = Icons.Default.LocationOn, label = "Address", value = loan.address ?: "Not specified")
                        DetailRow(icon = Icons.Default.Notes, label = "Notes", value = loan.notes ?: "No notes")
                        DetailRow(icon = Icons.Default.CheckCircle, label = "Status", value = loan.status.name, highlightColor = statusColor)
                    }
                }

                // Timeline / History
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    historyEvents.forEach { event ->
                        TimelineItem(
                            date = dateFormat.format(event.date),
                            title = event.title,
                            subtitle = if (event.method != null) "via ${event.method}" else "",
                            amount = event.amount,
                            isLoan = event.isLoan
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(60.dp))
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
                fontWeight = FontWeight.Medium,
                color = highlightColor ?: Color.White
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
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = if (highlight) PrimaryLime else Color.White
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
                color = Color.White
            )
            Text(
                text = date.split(" ").getOrNull(1) ?: "", // Month
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (isLoan) SecondaryOrange else PrimaryLime, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                if (amount != null) {
                    Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isLoan) SecondaryOrange else PrimaryLime)
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
    // Variable state for inputs
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") } // Matching previous implementation
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> proofUri = uri }
    
    val methods = listOf("Cash", "Bank", "bKash", "Nagad", "Rocket")

    // Use ModalBottomSheet for M3
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp) // Extra padding for system nav
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Add Payment", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryLime,
                    cursorColor = PrimaryLime
                )
            )
            
            Column {
                Text("Payment Method", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.take(3).forEach { m ->
                        val selected = method == m
                        FilterChip(
                            selected = selected,
                            onClick = { method = m },
                            label = { Text(m) },
                             colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryLime,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = Color.White
                            )
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
                                selectedContainerColor = PrimaryLime,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLime, contentColor = Color.Black),
                enabled = amount.isNotBlank()
            ) {
                Text("Confirm Payment", fontWeight = FontWeight.Bold)
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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Add More Loan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SecondaryOrange,
                    cursorColor = SecondaryOrange
                )
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
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
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryOrange, contentColor = Color.Black),
                enabled = amount.isNotBlank()
            ) {
                Text("Add to Balance", fontWeight = FontWeight.Bold)
            }
        }
    }
}
