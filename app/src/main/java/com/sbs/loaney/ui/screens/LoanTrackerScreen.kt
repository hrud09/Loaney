package com.sbs.loaney.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
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
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    
    var showAddPaymentSheet by remember { mutableStateOf(false) }

    LaunchedEffect(loanId) {
        viewModel.selectLoan(loanId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedLoan?.loan?.personName ?: "Loan Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selectedLoan != null && uiState.selectedLoan?.loan?.status != LoanStatus.FULLY_PAID) {
                QuickActionBar(
                    onAddPayment = { showAddPaymentSheet = true },
                    onMarkAsSettled = { viewModel.markAsSettled() }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.selectedLoan == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val item = uiState.selectedLoan!!
                val primaryColor = MaterialTheme.colorScheme.primary
                
                val timelineItems = remember(item) {
                    val list = mutableListOf<TimelineItem>()
                    list.add(TimelineItem(
                        date = item.loan.loanDate,
                        title = "Loan Created",
                        description = "Amount: ৳${item.loan.amount}${if (item.loan.email != null) "\nEmail: ${item.loan.email}" else ""}",
                        icon = Icons.Default.Info,
                        color = primaryColor
                    ))

                    item.payments.forEach { payment ->
                        list.add(TimelineItem(
                            date = payment.date,
                            title = "Repayment Added",
                            description = "Amount: ৳${payment.amount} via ${payment.method}${if (payment.note != null) "\nNote: ${payment.note}" else ""}",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50)
                        ))
                    }
                    list.sortByDescending { it.date }
                    list
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LoanSnapshotPanel(item)
                    }

                    item {
                        Text(
                            text = "Activity Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(timelineItems) { timelineItem ->
                        TimelineRow(timelineItem, dateFormat)
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
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
    }
}

@Composable
fun LoanSnapshotPanel(item: LoanWithPayments) {
    val loan = item.loan
    val paid = item.payments.sumOf { it.amount }
    val balance = (loan.amount - paid).coerceAtLeast(0.0)
    val typeColor = if (loan.type == LoanType.LEND) Color(0xFF4CAF50) else Color(0xFFFF9800)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SnapshotItem("Total Amount", "৳${loan.amount}", typeColor)
                SnapshotItem("Remaining", "৳$balance", if (balance > 0) Color.Red else Color(0xFF4CAF50))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SnapshotItem("Paid Amount", "৳$paid", Color(0xFF4CAF50))
                SnapshotItem("Deadline", SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(loan.promisedReturnDate), MaterialTheme.colorScheme.onSurface)
            }
            
            if (loan.email != null) {
                Text(text = "Contact: ${loan.email}", style = MaterialTheme.typography.bodySmall)
            }
            
            val statusColor = when (loan.status) {
                LoanStatus.OVERDUE -> Color.Red
                LoanStatus.FULLY_PAID -> Color(0xFF4CAF50)
                LoanStatus.PARTIALLY_PAID -> Color(0xFF2196F3)
                else -> MaterialTheme.colorScheme.primary
            }
            
            Box(
                modifier = Modifier
                    .background(statusColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(loan.status.name, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SnapshotItem(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

data class TimelineItem(
    val date: Date,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun TimelineRow(item: TimelineItem, dateFormat: SimpleDateFormat) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(item.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(18.dp))
            }
            Box(modifier = Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(dateFormat.format(item.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun QuickActionBar(onAddPayment: () -> Unit, onMarkAsSettled: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onAddPayment, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Repayment")
            }
            OutlinedButton(onClick = onMarkAsSettled, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Settle")
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
    ) { uri: Uri? ->
        proofUri = uri
    }
    
    val methods = listOf("Cash", "Bank", "bKash", "Nagad", "Rocket")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Add Repayment", style = MaterialTheme.typography.headlineSmall)
            
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("৳") }
            )

            Text("Payment Method", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.take(3).forEach { m ->
                    FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.drop(3).forEach { m ->
                    FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Proof of Payment Upload
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Proof of Payment", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (proofUri != null) "Image Selected" else "Optional",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(onClick = { launcher.launch("image/*") }) {
                        Text(if (proofUri != null) "Change" else "Upload")
                    }
                }
            }

            Button(
                onClick = { 
                    amount.toDoubleOrNull()?.let { onAddPayment(it, method, note.ifBlank { null }, proofUri?.toString()) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotBlank()
            ) {
                Text("Confirm Payment")
            }
        }
    }
}
