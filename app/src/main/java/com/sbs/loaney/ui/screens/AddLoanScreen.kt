package com.sbs.loaney.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.ManageLoansViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    initialType: LoanType = LoanType.LEND,
    onNavigateBack: () -> Unit,
    viewModel: ManageLoansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialType) {
        if (initialType == LoanType.LEND) {
            pagerState.scrollToPage(0)
        } else {
            pagerState.scrollToPage(1)
        }
    }
    
    val selectedLoanType = if (pagerState.currentPage == 0) LoanType.LEND else LoanType.BORROW

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("0") }
    var loanDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var returnDate by remember { mutableLongStateOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L) }
    var purpose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    var proofFileName by remember { mutableStateOf<String?>(null) }
    var selectedRelationship by remember { mutableStateOf("Other") }
    var email by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var witness by remember { mutableStateOf("") }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    var showSuggestions by remember { mutableStateOf(false) }
    var showExtraDetails by remember { mutableStateOf(false) }
    
    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    
    val nameSuggestions = remember(name, uiState.lentLoans, uiState.borrowedLoans) {
        if (name.isBlank()) emptyList()
        else (uiState.lentLoans + uiState.borrowedLoans).map { it.loan.personName }.distinct()
            .filter { it.contains(name, ignoreCase = true) && it != name }
    }

    var showLoanDatePicker by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }

    val loanReasons = listOf("🍔 Food", "🚑 Emergency", "🛍️ Shopping", "🚌 Travel", "Bills", "Other")
    val relationships = listOf(
        stringResource(R.string.relationship_friend) to "Friend",
        stringResource(R.string.relationship_family) to "Family",
        stringResource(R.string.relationship_colleague) to "Colleague",
        stringResource(R.string.relationship_neighbor) to "Neighbor",
        stringResource(R.string.relationship_other) to "Other"
    )
    val quickAmounts = listOf(500, 1000, 5000)

    val datePickerColors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        headlineContentColor = MaterialTheme.colorScheme.onBackground,
        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        navigationContentColor = MaterialTheme.colorScheme.onBackground,
        yearContentColor = MaterialTheme.colorScheme.onBackground,
        currentYearContentColor = MaterialTheme.colorScheme.primary,
        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
        dayContentColor = MaterialTheme.colorScheme.onBackground,
        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
        todayContentColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary
    )

    if (showLoanDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = loanDate)
        DatePickerDialog(
            onDismissRequest = { showLoanDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { loanDate = it }
                    showLoanDatePicker = false
                }) { Text(stringResource(id = R.string.ok), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLoanDatePicker = false }) { Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        ) {
            DatePicker(state = datePickerState, colors = datePickerColors)
        }
    }

    if (showReturnDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = returnDate)
        DatePickerDialog(
            onDismissRequest = { showReturnDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { returnDate = it }
                    showReturnDatePicker = false
                }) { Text(stringResource(id = R.string.ok), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDatePicker = false }) { Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> 
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
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

    val profilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> 
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        profilePhotoUri = uri
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        containerColor = AlimCream,
        topBar = {
            Column(modifier = Modifier.background(AlimDark)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            if (selectedLoanType == LoanType.LEND) stringResource(id = R.string.send_loan_title) else stringResource(id = R.string.request_loan_title), 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AlimWhite
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = stringResource(id = R.string.back), 
                                tint = AlimWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AlimDark,
                        titleContentColor = AlimWhite
                    )
                )
                
                // Tab Row inside the dark header
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = AlimDark,
                    contentColor = AlimGreen,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = AlimGreen,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {}
                ) {
                    listOf(0 to stringResource(id = R.string.lend), 1 to stringResource(id = R.string.borrow)).forEach { (pageIndex, text) ->
                        val selected = pagerState.currentPage == pageIndex
                        Tab(
                            selected = selected,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pageIndex) } },
                            text = {
                                Text(
                                    text = text,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) AlimGreen else AlimWhite.copy(alpha = 0.6f)
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AlimCream,
                shadowElevation = 0.dp
            ) {
                Button(
                    onClick = {
                        val isNameValid = name.isNotBlank()
                        val isAmountValid = (amount.toDoubleOrNull() ?: 0.0) > 0
                        
                        nameError = !isNameValid
                        amountError = !isAmountValid
                        
                        if (isNameValid && isAmountValid) {
                            viewModel.addLoan(
                                type = selectedLoanType, name = name, phone = phone, 
                                email = email.ifBlank { null }, address = address.ifBlank { null },
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                loanDate = Date(loanDate), returnDate = Date(returnDate),
                                purpose = purpose.ifBlank { null }, notes = notes.ifBlank { null },
                                interest = interestRate.toDoubleOrNull(), relationshipType = selectedRelationship,
                                witness = witness.ifBlank { null },
                                proofUri = proofUri?.toString(),
                                profilePhotoUri = profilePhotoUri?.toString()
                            )
                            onNavigateBack()
                        } else {
                            val emptyFields = mutableListOf<String>()
                            if (!isNameValid) emptyFields.add("Name")
                            if (!isAmountValid) emptyFields.add("Amount")
                            
                            Toast.makeText(
                                context, 
                                "Please fill mandatory fields: ${emptyFields.joinToString(", ")}", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                        .shadow(12.dp, CircleShape, spotColor = AlimGreen.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlimGreen,
                        contentColor = AlimWhite
                    ),
                    shape = CircleShape
                ) {
                    val actionText = if (selectedLoanType == LoanType.LEND) stringResource(id = R.string.send_loan_title) else stringResource(id = R.string.request_loan_title)
                    Text(actionText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AlimCream)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Big Amount Input
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(id = R.string.amount), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(0.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${uiState.currencySymbol} ",
                                style = MaterialTheme.typography.displayMedium,
                                color = if (amountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            BasicTextField(
                                value = amount,
                                onValueChange = { 
                                    if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                                        amount = if (it.startsWith("0") && it.length > 1 && !it.startsWith("0.")) it.substring(1) else it
                                        if (amount.isEmpty()) amount = "0"
                                    }
                                },
                                textStyle = MaterialTheme.typography.displayMedium.copy(
                                    color = if (amountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.width(IntrinsicSize.Min)
                            )
                        }

                        // Quick amount pills
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            quickAmounts.forEach { quickVal ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { 
                                        val curr = amount.toDoubleOrNull() ?: 0.0
                                        amount = (curr + quickVal).toLong().toString() 
                                    }
                                ) {
                                    Text(
                                        text = "+$quickVal",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Core Recipient Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Profile Photo Picker
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { profilePhotoLauncher.launch(arrayOf("image/*")) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profilePhotoUri != null) {
                                        AsyncImage(
                                            model = profilePhotoUri,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.AddAPhoto, contentDescription = "Add Profile Photo", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                            
                            CustomLightTextField(
                                value = name,
                                onValueChange = { 
                                    name = it
                                    showSuggestions = true 
                                    if (it.isNotBlank()) nameError = false
                                },
                                label = stringResource(id = R.string.recipient_name),
                                leadingIcon = Icons.Default.Person,
                                trailingIcon = Icons.Default.ContactPhone,
                                isError = nameError,
                                onTrailingIconClick = { 
                                    val intent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                                        type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                                    }
                                    contactLauncher.launch(intent) 
                                }
                            )
                            
                            if (showSuggestions && nameSuggestions.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = true,
                                    onDismissRequest = { showSuggestions = false },
                                    properties = PopupProperties(focusable = false),
                                    modifier = Modifier.fillMaxWidth(0.8f).background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    nameSuggestions.forEach { suggestion ->
                                        DropdownMenuItem(
                                            text = { Text(suggestion, color = MaterialTheme.colorScheme.onBackground) },
                                            onClick = {
                                                name = suggestion
                                                showSuggestions = false
                                            }
                                        )
                                    }
                                }
                            }

                            CustomLightTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = stringResource(id = R.string.phone_optional),
                                leadingIcon = Icons.Default.Phone,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.weight(1f).clickable { showLoanDatePicker = true }) {
                                    CustomLightTextField(
                                        value = dateFormat.format(Date(loanDate)),
                                        onValueChange = {},
                                        label = stringResource(id = R.string.loan_date),
                                        readOnly = true,
                                        enabled = false,
                                        leadingIcon = Icons.Default.CalendarToday,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Box(modifier = Modifier.weight(1f).clickable { showReturnDatePicker = true }) {
                                    CustomLightTextField(
                                        value = dateFormat.format(Date(returnDate)),
                                        onValueChange = {},
                                        label = stringResource(id = R.string.due_date),
                                        readOnly = true,
                                        enabled = false,
                                        leadingIcon = Icons.Default.Event,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Reason for Loan
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(id = R.string.reason_for_loan), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(loanReasons) { r ->
                                val isSelected = purpose == r
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { purpose = if (isSelected) "" else r },
                                    label = { Text(r, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }

                    // Relationship Picker
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(id = R.string.relationship), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(relationships) { (label, value) ->
                                val isSelected = selectedRelationship == value
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedRelationship = value },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }

                    // Extra Details Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showExtraDetails = !showExtraDetails }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showExtraDetails) stringResource(id = R.string.hide_extra_details) else "Add More Details",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            imageVector = if (showExtraDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    AnimatedVisibility(
                        visible = showExtraDetails,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CustomLightTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = stringResource(id = R.string.email_optional),
                                leadingIcon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            CustomLightTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = stringResource(id = R.string.location_optional),
                                leadingIcon = Icons.Default.LocationOn
                            )

                            CustomLightTextField(
                                value = interestRate,
                                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) interestRate = it },
                                label = stringResource(id = R.string.interest_rate_optional),
                                leadingIcon = Icons.Default.Percent,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            CustomLightTextField(
                                value = witness,
                                onValueChange = { witness = it },
                                label = stringResource(id = R.string.witness_optional),
                                leadingIcon = Icons.Default.Group
                            )

                            // Attachments
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(id = R.string.attachment),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { imageLauncher.launch(arrayOf("image/*")) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = stringResource(id = R.string.add_attachment),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (proofUri != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.LightGray)
                                        ) {
                                            AsyncImage(
                                                model = proofUri,
                                                contentDescription = stringResource(id = R.string.preview),
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            IconButton(
                                                onClick = {
                                                    proofUri = null
                                                    proofFileName = null
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(20.dp)
                                                    .padding(2.dp)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = PureWhite,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun borderStroke(): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder)
}

@Composable
fun CustomLightTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        isError = isError,
        label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AlimGreen,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = AlimWhite,
            unfocusedContainerColor = AlimWhite,
            focusedTextColor = AlimDark,
            unfocusedTextColor = AlimDark,
            cursorColor = AlimGreen
        ),
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = if (value.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailingIcon = if (trailingIcon != null) {
            {
                IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                    Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else null,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
    )
}
