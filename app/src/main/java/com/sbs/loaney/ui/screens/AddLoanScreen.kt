package com.sbs.loaney.ui.screens

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.theme.PrimaryLime
import com.sbs.loaney.ui.theme.SecondaryOrange
import com.sbs.loaney.ui.theme.TertiaryRed
import com.sbs.loaney.ui.viewmodel.ManageLoansViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageLoansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var selectedLoanType by remember { mutableStateOf(LoanType.LEND) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var loanDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var returnDate by remember { mutableLongStateOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L) }
    var purpose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    
    var showSuggestions by remember { mutableStateOf(false) }
    val nameSuggestions = remember(name, uiState.loans) {
        if (name.isBlank()) emptyList()
        else uiState.loans.map { it.loan.personName }.distinct()
            .filter { it.contains(name, ignoreCase = true) && it != name }
    }

    var showLoanDatePicker by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }

    if (showLoanDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = loanDate)
        DatePickerDialog(
            onDismissRequest = { showLoanDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { loanDate = it }
                    showLoanDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showLoanDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showReturnDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = returnDate)
        DatePickerDialog(
            onDismissRequest = { showReturnDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { returnDate = it }
                    showReturnDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        
                        if (numberIndex >= 0) {
                            phone = cursor.getString(numberIndex)
                        }
                        if (nameIndex >= 0 && name.isBlank()) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                }
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> proofUri = uri }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val themeColor = if (selectedLoanType == LoanType.LEND) PrimaryLime else SecondaryOrange

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New Loan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Segmented Control
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf(LoanType.LEND to "LEND", LoanType.BORROW to "BORROW").forEach { (loanType, text) ->
                        val selected = selectedLoanType == loanType
                        val color = if (loanType == LoanType.LEND) PrimaryLime else SecondaryOrange
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (selected) color else Color.Transparent)
                                .clickable { selectedLoanType = loanType }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text,
                                color = if (selected) Color.Black else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Input Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Loan Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        CustomTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                showSuggestions = true 
                            },
                            label = "Person Name",
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (showSuggestions && nameSuggestions.isNotEmpty()) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { showSuggestions = false },
                                properties = PopupProperties(focusable = false),
                                modifier = Modifier.fillMaxWidth(0.8f).background(MaterialTheme.colorScheme.surface)
                            ) {
                                nameSuggestions.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = { Text(suggestion, color = Color.White) },
                                        onClick = {
                                            name = suggestion
                                            showSuggestions = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    CustomTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                        label = "Amount (৳)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            CustomTextField(
                                value = dateFormat.format(Date(loanDate)),
                                onValueChange = {},
                                label = "Date",
                                readOnly = true,
                                trailingIcon = Icons.Default.DateRange,
                                onTrailingIconClick = { showLoanDatePicker = true },
                                modifier = Modifier.clickable { showLoanDatePicker = true }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            CustomTextField(
                                value = dateFormat.format(Date(returnDate)),
                                onValueChange = {},
                                label = "Due Date",
                                readOnly = true,
                                trailingIcon = Icons.Default.DateRange,
                                onTrailingIconClick = { showReturnDatePicker = true },
                                modifier = Modifier.clickable { showReturnDatePicker = true }
                            )
                        }
                    }
                }
            }

            // Contact Info
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Contact & Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                    CustomTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        trailingIcon = Icons.Default.Phone,
                        onTrailingIconClick = { 
                            val intent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                                type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                            }
                            contactLauncher.launch(intent) 
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    CustomTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Location / Address",
                        trailingIcon = Icons.Default.LocationOn,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Attachments
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Attachment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (proofUri != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryLime)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Document Attached", color = Color.White)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Upload Proof (Image)", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Action Button
            Button(
                onClick = {
                    viewModel.addLoan(
                        type = selectedLoanType, name = name, phone = phone, 
                        email = email.ifBlank { null }, address = address.ifBlank { null },
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        loanDate = Date(loanDate), returnDate = Date(returnDate),
                        purpose = purpose.ifBlank { null }, notes = notes.ifBlank { null },
                        interest = null, proofUri = proofUri?.toString()
                    )
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank() && amount.isNotBlank()
            ) {
                Text("Save Loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White.copy(alpha = 0.2f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            trailingIcon = if (trailingIcon != null) {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(trailingIcon, contentDescription = null, tint = Color.Gray)
                    }
                }
            } else null,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}
