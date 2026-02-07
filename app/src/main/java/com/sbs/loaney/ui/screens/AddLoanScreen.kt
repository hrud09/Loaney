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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.model.LoanType
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

    Scaffold(
        containerColor = TertiaryRed,
        topBar = {
            TopAppBar(
                title = { Text("New Loan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.1f), CircleShape)
                    .padding(4.dp)
            ) {
                listOf(LoanType.LEND to "LEND", LoanType.BORROW to "BORROW").forEach { (loanType, text) ->
                    val selected = selectedLoanType == loanType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) Color.Black else Color.Transparent)
                            .clickable { selectedLoanType = loanType }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = if (selected) Color.White else Color.Black.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Input Fields
            Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)

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
                        modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                    ) {
                        nameSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion, color = Color.Black) },
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
            
            // Proof / Attachments
            Text("Attachment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                 Box(
                     modifier = Modifier
                         .size(80.dp)
                         .clip(RoundedCornerShape(16.dp))
                         .background(Color.Black.copy(alpha = 0.05f))
                         .clickable { imageLauncher.launch("image/*") }
                         .padding(4.dp),
                     contentAlignment = Alignment.Center
                 ) {
                     if (proofUri != null) {
                         Icon(Icons.Default.ArrowBack, contentDescription = "File", tint = Color.Black)
                         Text("File", color = Color.Black)
                     } else {
                         Icon(Icons.Default.Add, contentDescription = "Upload", tint = Color.Black)
                     }
                 }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = name.isNotBlank() && amount.isNotBlank()
            ) {
                Text("Save Loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
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
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black.copy(alpha = 0.1f),
                unfocusedBorderColor = Color.Black.copy(alpha = 0.1f),
                focusedContainerColor = Color.White.copy(alpha = 0.3f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.3f),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            trailingIcon = if (trailingIcon != null) {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }, enabled = true) {
                        Icon(trailingIcon, contentDescription = null, tint = Color.Black)
                    }
                }
            } else null,
            singleLine = true
        )
    }
}
