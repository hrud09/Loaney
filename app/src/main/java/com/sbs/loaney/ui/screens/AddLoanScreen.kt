package com.sbs.loaney.ui.screens

import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.theme.NeonLime
import com.sbs.loaney.ui.theme.SkyBlue
import com.sbs.loaney.ui.theme.SurfaceDark
import com.sbs.loaney.ui.theme.SurfaceElevated
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
    var proofFileName by remember { mutableStateOf<String?>(null) }
    var selectedRelationship by remember { mutableStateOf("Friend") }
    var witness by remember { mutableStateOf("") }
    
    var showSuggestions by remember { mutableStateOf(false) }
    val nameSuggestions = remember(name, uiState.loans) {
        if (name.isBlank()) emptyList()
        else uiState.loans.map { it.loan.personName }.distinct()
            .filter { it.contains(name, ignoreCase = true) && it != name }
    }

    var showLoanDatePicker by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }

    val loanReasons = listOf("🍔 Food", "🚑 Emergency", "🛍️ Shopping", "🚌 Travel", "Bills", "Other")
    val relationships = listOf("Friend", "Family", "Colleague", "Neighbor", "Other")

    val datePickerColors = DatePickerDefaults.colors(
        containerColor = SurfaceDark,
        titleContentColor = Color.White,
        headlineContentColor = Color.White,
        weekdayContentColor = Color.Gray,
        subheadContentColor = Color.Gray,
        navigationContentColor = Color.White,
        yearContentColor = Color.White,
        currentYearContentColor = NeonLime,
        selectedYearContentColor = Color.Black,
        selectedYearContainerColor = NeonLime,
        dayContentColor = Color.White,
        selectedDayContentColor = Color.Black,
        selectedDayContainerColor = NeonLime,
        todayContentColor = NeonLime,
        todayDateBorderColor = NeonLime
    )

    if (showLoanDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = loanDate)
        DatePickerDialog(
            onDismissRequest = { showLoanDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = SurfaceDark),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { loanDate = it }
                    showLoanDatePicker = false
                }) { Text("OK", color = NeonLime) }
            },
            dismissButton = {
                TextButton(onClick = { showLoanDatePicker = false }) { Text("Cancel", color = Color.Gray) }
            }
        ) {
            DatePicker(state = datePickerState, colors = datePickerColors)
        }
    }

    if (showReturnDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = returnDate)
        DatePickerDialog(
            onDismissRequest = { showReturnDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = SurfaceDark),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { returnDate = it }
                    showReturnDatePicker = false
                }) { Text("OK", color = NeonLime) }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDatePicker = false }) { Text("Cancel", color = Color.Gray) }
            }
        ) {
            DatePicker(state = datePickerState, colors = datePickerColors)
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
    ) { uri: Uri? -> 
        proofUri = uri
        uri?.let {
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    proofFileName = cursor.getString(nameIndex)
                }
            }
        }
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val themeColor = if (selectedLoanType == LoanType.LEND) NeonLime else SkyBlue

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "New Loan", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Segmented Control
            Surface(
                color = SurfaceElevated,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf(LoanType.LEND to "LEND", LoanType.BORROW to "BORROW").forEach { (loanType, text) ->
                        val selected = selectedLoanType == loanType
                        val color = if (loanType == LoanType.LEND) NeonLime else SkyBlue
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Loan Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CustomTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                showSuggestions = true 
                            },
                            label = "Person Name",
                            leadingIcon = Icons.Default.Person,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (showSuggestions && nameSuggestions.isNotEmpty()) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { showSuggestions = false },
                                properties = PopupProperties(focusable = false),
                                modifier = Modifier.fillMaxWidth(0.8f).background(SurfaceElevated)
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

                    // Relationship Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    val relationshipIcon = when (selectedRelationship) {
                        "Friend", "Family" -> Icons.Default.Handshake
                        "Colleague" -> Icons.Default.BusinessCenter
                        else -> Icons.Default.Group
                    }

                    Box(modifier = Modifier.width(120.dp)) {
                        OutlinedCard(
                            onClick = { expanded = true },
                            shape = CircleShape,
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = SurfaceElevated,
                                contentColor = Color.White
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(relationshipIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = SkyBlue)
                                Text(selectedRelationship, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(SurfaceElevated)
                        ) {
                            relationships.forEach { rel ->
                                DropdownMenuItem(
                                    text = { Text(rel, color = Color.White) },
                                    onClick = {
                                        selectedRelationship = rel
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                CustomTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                    label = "Amount",
                    leadingIcon = Icons.Default.Payments,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Reason for Loan Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Reason for Loan", 
                        style = MaterialTheme.typography.labelLarge, 
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(loanReasons) { reason ->
                            val isSelected = purpose == reason
                            FilterChip(
                                selected = isSelected,
                                onClick = { purpose = if (isSelected) "" else reason },
                                label = { Text(reason) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceElevated,
                                    labelColor = Color.LightGray,
                                    selectedContainerColor = SkyBlue.copy(alpha = 0.1f),
                                    selectedLabelColor = SkyBlue
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = SkyBlue,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = CircleShape
                            )
                        }
                    }
                }

                CustomTextField(
                    value = dateFormat.format(Date(loanDate)),
                    onValueChange = {},
                    label = "Date",
                    readOnly = true,
                    leadingIcon = Icons.Default.CalendarToday,
                    trailingIcon = Icons.Default.EditCalendar,
                    onTrailingIconClick = { showLoanDatePicker = true },
                    modifier = Modifier.clickable { showLoanDatePicker = true }
                )

                CustomTextField(
                    value = dateFormat.format(Date(returnDate)),
                    onValueChange = {},
                    label = "Due Date",
                    readOnly = true,
                    leadingIcon = Icons.Default.Event,
                    trailingIcon = Icons.Default.EditCalendar,
                    onTrailingIconClick = { showReturnDatePicker = true },
                    modifier = Modifier.clickable { showReturnDatePicker = true }
                )
            }

            // Contact Info
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Contact & Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                CustomTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    trailingIcon = Icons.Default.ContactPhone,
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
                    leadingIcon = Icons.Default.LocationOn,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Witness / Reference
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Witness / Reference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { 
                            Toast.makeText(context, "Note down who was present during this transaction for future reference.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                OutlinedTextField(
                    value = witness,
                    onValueChange = { witness = it },
                    placeholder = { Text("E.g., In front of Rahim", color = Color.Gray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = SurfaceElevated.copy(alpha = 0.5f),
                        unfocusedContainerColor = SurfaceElevated,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = SkyBlue
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            // Attachments
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Attachment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Upload Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceElevated)
                            .clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Attachment", tint = Color.LightGray)
                    }

                    // Preview Section
                    if (proofUri != null) {
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                            ) {
                                AsyncImage(
                                    model = proofUri,
                                    contentDescription = "Attachment Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Remove button
                                IconButton(
                                    onClick = { 
                                        proofUri = null
                                        proofFileName = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = proofFileName ?: "attachment.jpg",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            "No attachment selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f).padding(start = 16.dp)
                        )
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
                        interest = null, relationshipType = selectedRelationship,
                        witness = witness.ifBlank { null },
                        proofUri = proofUri?.toString()
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
                shape = CircleShape,
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
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SkyBlue,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = SurfaceElevated.copy(alpha = 0.5f),
            unfocusedContainerColor = SurfaceElevated,
            focusedLabelColor = SkyBlue,
            unfocusedLabelColor = Color.LightGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = SkyBlue
        ),
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = if (value.isNotEmpty()) SkyBlue else Color.Gray) }
        },
        trailingIcon = if (trailingIcon != null) {
            {
                IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                    Icon(trailingIcon, contentDescription = null, tint = Color.Gray)
                }
            }
        } else null,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
