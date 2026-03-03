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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (selectedLoanType == LoanType.LEND) stringResource(id = R.string.send_loan_title) else stringResource(id = R.string.request_loan_title), 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                        onDragEnd = {
                            if (totalDrag > 100f) {
                                onNavigateBack()
                            }
                        }
                    )
                }
        ) {
            // Segmented Control (Transfer / Request style)
            Surface(
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                val lendColor = if (selectedLoanType == LoanType.LEND) MaterialTheme.colorScheme.primary else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(lendColor)
                            .clickable { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(id = R.string.lend), color = if (selectedLoanType == LoanType.LEND) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }

                    val borrowColor = if (selectedLoanType == LoanType.BORROW) MaterialTheme.colorScheme.error else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(borrowColor)
                            .clickable { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(id = R.string.borrow), color = if (selectedLoanType == LoanType.BORROW) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Big Amount Input
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(id = R.string.amount), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${uiState.currencySymbol} ",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
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
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }

                // Quick amount pills
                Spacer(modifier = Modifier.height(16.dp))
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
                shape = CardShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, GlassBorder, CardShape)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Profile Photo Picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
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

                    Box(modifier = Modifier.fillMaxWidth()) {
                        CustomLightTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                showSuggestions = true 
                            },
                            label = stringResource(id = R.string.recipient_name),
                            leadingIcon = Icons.Default.Person,
                            trailingIcon = Icons.Default.ContactPhone,
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
                    }

                    // Extra Details Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showExtraDetails = !showExtraDetails }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showExtraDetails) stringResource(id = R.string.hide_extra_details) else stringResource(id = R.string.add_extra_details),
                            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = if (showExtraDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // PROGRESSIVE DISCLOSURE: Extra Details Form
                    AnimatedVisibility(
                        visible = showExtraDetails,
                        enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
                        exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            
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

                            // Reason for Loan
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(id = R.string.reason_for_loan), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(loanReasons) { r ->
                                        val isSelected = purpose == r
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { purpose = if (isSelected) "" else r },
                                            label = { Text(r) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = Color.Transparent,
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                enabled = true,
                                                selected = isSelected
                                            ),
                                            shape = CircleShape
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

                            CustomLightTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = stringResource(id = R.string.email_optional),
                                leadingIcon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            // Relationship Picker
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(id = R.string.relationship), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(relationships) { (label, value) ->
                                        val isSelected = selectedRelationship == value
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedRelationship = value },
                                            label = { Text(label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = Color.Transparent,
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                enabled = true,
                                                selected = isSelected
                                            ),
                                            shape = CircleShape
                                        )
                                    }
                                }
                            }

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
                                Text(stringResource(id = R.string.attachment), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Upload Button
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { imageLauncher.launch(arrayOf("image/*")) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_attachment), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Preview Section
                                    if (proofUri != null) {
                                        Column(
                                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                                            horizontalAlignment = Alignment.Start
                                        ) {
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
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } // end animated visibility
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
                        interest = interestRate.toDoubleOrNull(), relationshipType = selectedRelationship,
                        witness = witness.ifBlank { null },
                        proofUri = proofUri?.toString(),
                        profilePhotoUri = profilePhotoUri?.toString()
                    )
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape,
                enabled = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                val actionText = if (selectedLoanType == LoanType.LEND) stringResource(id = R.string.send_loan_title) else stringResource(id = R.string.request_loan_title)
                Text(actionText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
                }
            }
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
    onTrailingIconClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = modifier.fillMaxWidth(),
        shape = ButtonShape,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            cursorColor = MaterialTheme.colorScheme.secondary
        ),
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = if (value.isNotEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant) }
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
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
