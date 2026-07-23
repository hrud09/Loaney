package com.sbs.loaney.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlin.math.pow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.ui.viewmodel.EmiUiState
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.R
import com.sbs.loaney.data.local.entity.EmiEntity
import com.sbs.loaney.ui.components.CustomLightTextField
import com.sbs.loaney.ui.components.SearchableDropdown
import com.sbs.loaney.ui.components.ToolInfoDialog
import com.sbs.loaney.ui.components.ToolInfoType
import com.sbs.loaney.ui.components.bounceClick
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.BrandOffer
import com.sbs.loaney.ui.viewmodel.EmiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiScreen(
    onNavigateBack: () -> Unit,
    viewModel: EmiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddEmiSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Parameters for Add EMI Sheet (used during autofill from calculator too)
    var sheetItemName by remember { mutableStateOf("") }
    var sheetTotalAmount by remember { mutableStateOf("") }
    var sheetDownPayment by remember { mutableStateOf("") }
    var sheetTenure by remember { mutableStateOf("12") }
    var sheetRate by remember { mutableStateOf("0") }
    var sheetDueDay by remember { mutableStateOf("5") }
    var sheetSelectedBankName by remember { mutableStateOf("") }
    var sheetSelectedBankId by remember { mutableStateOf<Long?>(null) }

    fun openAddSheet(
        name: String = "",
        amount: Double = 0.0,
        downPayment: Double = 0.0,
        tenure: Int = 12,
        rate: Double = 0.0
    ) {
        sheetItemName = name
        sheetTotalAmount = if (amount > 0) amount.toInt().toString() else ""
        sheetDownPayment = if (downPayment > 0) downPayment.toInt().toString() else ""
        sheetTenure = tenure.toString()
        sheetRate = if (rate > 0) rate.toString() else "0"
        sheetDueDay = "5"
        sheetSelectedBankName = ""
        sheetSelectedBankId = null
        showAddEmiSheet = true
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(AlimDark)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "EMI Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AlimWhite
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AlimWhite
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "EMI Info",
                                tint = AlimWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AlimDark,
                        titleContentColor = AlimWhite
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { openAddSheet() },
                    containerColor = AlimGreen,
                    contentColor = AlimWhite,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add EMI", modifier = Modifier.size(28.dp))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AlimDark,
                contentColor = AlimWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AlimGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tracker", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Calculator", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Offers", fontWeight = FontWeight.SemiBold) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> TrackerTab(
                        uiState = uiState,
                        onPayInstallment = { viewModel.payInstallment(it) },
                        onDeleteEmi = { viewModel.deleteEmi(it) },
                        onAddManualClick = { openAddSheet() }
                    )
                    1 -> CalculatorTab(
                        uiState = uiState,
                        onAmountChanged = { viewModel.updateCalculatorAmount(it) },
                        onDownPaymentChanged = { viewModel.updateCalculatorDownPayment(it) },
                        onRateChanged = { viewModel.updateCalculatorRate(it) },
                        onTenureChanged = { viewModel.updateCalculatorTenure(it) },
                        onSaveAsEmi = {
                            openAddSheet(
                                name = "Calculated Purchase",
                                amount = uiState.calculatorAmount,
                                downPayment = uiState.calculatorDownPayment,
                                tenure = uiState.calculatorTenure,
                                rate = uiState.calculatorRate
                            )
                        }
                    )
                    2 -> OffersTab(
                        uiState = uiState,
                        onCategorySelected = { viewModel.updateBrandCategory(it) }
                    )
                }
            }
        }

        // Add EMI Bottom Sheet Form
        if (showAddEmiSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddEmiSheet = false },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Add EMI Tracker",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { showAddEmiSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    CustomLightTextField(
                        value = sheetItemName,
                        onValueChange = { sheetItemName = it },
                        label = "Item Name",
                        leadingIcon = Icons.Default.ShoppingBag
                    )

                    CustomLightTextField(
                        value = sheetTotalAmount,
                        onValueChange = { sheetTotalAmount = it },
                        label = "Total Purchase Price",
                        leadingIcon = Icons.Default.AttachMoney,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    CustomLightTextField(
                        value = sheetDownPayment,
                        onValueChange = { sheetDownPayment = it },
                        label = "Down Payment (Optional)",
                        leadingIcon = Icons.Default.MoneyOff,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    CustomLightTextField(
                        value = sheetTenure,
                        onValueChange = { sheetTenure = it },
                        label = "Tenure (Months)",
                        leadingIcon = Icons.Default.CalendarToday,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    CustomLightTextField(
                        value = sheetRate,
                        onValueChange = { sheetRate = it },
                        label = "Annual Interest Rate (%)",
                        leadingIcon = Icons.Default.Percent,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    CustomLightTextField(
                        value = sheetDueDay,
                        onValueChange = { sheetDueDay = it },
                        label = "Monthly Due Day (1-31)",
                        leadingIcon = Icons.Default.Today,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Link bank account dropdown
                    val bankOptions = uiState.bankAccounts.map { 
                        if (it.isCard) "${it.bankName} - Card (*${it.accountNumber.takeLast(4)})"
                        else if (it.isMfs) "${it.mfsProvider} - MFS (${it.accountNumber})"
                        else "${it.bankName} - Account (${it.accountNumber})"
                    }
                    
                    SearchableDropdown(
                        value = sheetSelectedBankName,
                        onValueChange = { name ->
                            sheetSelectedBankName = name
                            val index = bankOptions.indexOf(name)
                            if (index >= 0) {
                                sheetSelectedBankId = uiState.bankAccounts[index].id
                            }
                        },
                        label = "Linked Account/Card (Optional)",
                        leadingIcon = Icons.Default.AccountBalance,
                        options = bankOptions
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val price = sheetTotalAmount.toDoubleOrNull() ?: 0.0
                            val down = sheetDownPayment.toDoubleOrNull() ?: 0.0
                            val months = sheetTenure.toIntOrNull() ?: 12
                            val rateVal = sheetRate.toDoubleOrNull() ?: 0.0
                            val day = sheetDueDay.toIntOrNull() ?: 5

                            if (sheetItemName.isBlank() || price <= 0 || months <= 0) {
                                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Math calculations for EMI installment
                            val principal = (price - down).coerceAtLeast(0.0)
                            val monthlyInstallment = if (rateVal <= 0.0) {
                                principal / months
                            } else {
                                val r = (rateVal / 12.0) / 100.0
                                (principal * r * (1 + r).pow(months)) / ((1 + r).pow(months) - 1)
                            }

                            viewModel.addEmi(
                                itemName = sheetItemName,
                                totalAmount = price,
                                downPayment = down,
                                tenureMonths = months,
                                interestRate = rateVal,
                                monthlyInstallment = monthlyInstallment,
                                startDate = System.currentTimeMillis(),
                                dueDateDay = day,
                                bankAccountId = sheetSelectedBankId
                            )
                            showAddEmiSheet = false
                            Toast.makeText(context, "EMI Tracker added successfully", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlimGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Add EMI Tracker", color = AlimWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        if (showInfoDialog) {
            ToolInfoDialog(
                toolType = ToolInfoType.EMI_HUB,
                onDismiss = { showInfoDialog = false }
            )
        }
    }
}

// ── 1. Tracker Tab ───────────────────────────────────────────────────────────
@Composable
fun TrackerTab(
    uiState: EmiUiState,
    onPayInstallment: (EmiEntity) -> Unit,
    onDeleteEmi: (EmiEntity) -> Unit,
    onAddManualClick: () -> Unit
) {
    if (uiState.emis.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(AlimGreen.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = AlimGreen, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("No Active EMIs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "You don't have any Equated Monthly Installments tracked yet. Swipe to the Calculator tab to estimate interest and save your EMI deals directly!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onAddManualClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AlimGreen)
                    ) {
                        Text("Add Manually", color = AlimWhite)
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AlimDark)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("EMI LIABILITIES", color = AlimWhite.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = CoralRose)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "৳${String.format("%,.0f", uiState.monthlyLiability)}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = AlimWhite,
                                fontWeight = FontWeight.Black
                            )
                            Text("/mo", color = AlimWhite.copy(alpha = 0.6f), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = AlimWhite.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Outstanding", color = AlimWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                                Text("৳${String.format("%,.0f", uiState.totalOutstanding)}", color = AlimWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Active EMIs", color = AlimWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                                Text("${uiState.activeEmis.size} Active", color = AlimGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text("Your Installments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            // EMI Items list
            items(uiState.emis) { emi ->
                EmiItemCard(
                    emi = emi,
                    bankAccounts = uiState.bankAccounts,
                    onPayInstallment = { onPayInstallment(emi) },
                    onDeleteEmi = { onDeleteEmi(emi) }
                )
            }
        }
    }
}

@Composable
fun EmiItemCard(
    emi: EmiEntity,
    bankAccounts: List<BankAccountEntity>,
    onPayInstallment: () -> Unit,
    onDeleteEmi: () -> Unit
) {
    val linkedAccount = bankAccounts.firstOrNull { it.id == emi.associatedBankAccountId }
    val progress = if (emi.tenureMonths > 0) emi.paymentsPaid.toFloat() / emi.tenureMonths else 0f
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete EMI Tracker?") },
            text = { Text("Are you sure you want to stop tracking this installment of \"${emi.itemName}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEmi()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emi.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = emi.itemName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳${String.format("%,.0f", emi.monthlyInstallment)}/mo • ${emi.tenureMonths} Months",
                        color = if (emi.isCompleted) Color.Gray else AlimGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (emi.isCompleted) "Completed 🎉" else "Installment ${emi.paymentsPaid} of ${emi.tenureMonths}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(progress * 100).toInt()}% Paid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (emi.isCompleted) Color.Gray else AlimGreen,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(12.dp))

            // Footer / Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Linked account details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (linkedAccount?.isCard == true) Icons.Default.CreditCard 
                        else if (linkedAccount?.isMfs == true) Icons.Default.PhoneAndroid
                        else Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = linkedAccount?.let { 
                            if (it.isCard) "${it.bankName} Card" else if (it.isMfs) it.mfsProvider else it.bankName
                        } ?: "No Linked Account",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Pay installment button
                if (!emi.isCompleted) {
                    Button(
                        onClick = onPayInstallment,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlimGreen.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AlimGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paid Installment", color = AlimGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── 2. Calculator Tab ────────────────────────────────────────────────────────
@Composable
fun CalculatorTab(
    uiState: EmiUiState,
    onAmountChanged: (Double) -> Unit,
    onDownPaymentChanged: (Double) -> Unit,
    onRateChanged: (Double) -> Unit,
    onTenureChanged: (Int) -> Unit,
    onSaveAsEmi: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact EMI Result + Donut Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AlimGreen)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: EMI Result
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ESTIMATED MONTHLY EMI",
                        color = AlimWhite.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "৳${String.format("%,.0f", uiState.calcMonthlyInstallment)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = AlimWhite,
                            fontWeight = FontWeight.Black
                        )
                        Text("/mo", color = AlimWhite.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = AlimWhite.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Interest", color = AlimWhite.copy(alpha = 0.7f), fontSize = 9.sp)
                            Text("৳${String.format("%,.0f", uiState.calcTotalInterest)}", color = AlimWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total", color = AlimWhite.copy(alpha = 0.7f), fontSize = 9.sp)
                            Text("৳${String.format("%,.0f", uiState.calcTotalPayable)}", color = AlimWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Right: Donut Chart
                if (uiState.calcPrincipal > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val principalShare = if (uiState.calcTotalPayable > 0) uiState.calcPrincipal / uiState.calcTotalPayable else 1.0
                        val interestShare = if (uiState.calcTotalPayable > 0) uiState.calcTotalInterest / uiState.calcTotalPayable else 0.0
                        EmiDonutChart(
                            principalPercent = principalShare.toFloat(),
                            interestPercent = interestShare.toFloat(),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(AlimGreen.copy(alpha = 0.6f), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${(principalShare * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AlimWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(8.dp).background(CoralRose, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${(interestShare * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AlimWhite)
                        }
                    }
                }
            }
        }

        // Sliders Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Slider: Purchase Amount
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Purchase Price", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("৳${String.format("%,.0f", uiState.calculatorAmount)}", color = AlimGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = uiState.calculatorAmount.toFloat(),
                        onValueChange = { onAmountChanged(it.toDouble()) },
                        valueRange = 5000f..500000f,
                        steps = 99,
                        colors = SliderDefaults.colors(activeTrackColor = AlimGreen, thumbColor = AlimGreen)
                    )
                }

                // Slider: Down Payment
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Down Payment", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("৳${String.format("%,.0f", uiState.calculatorDownPayment)}", color = AlimGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = uiState.calculatorDownPayment.toFloat(),
                        onValueChange = { onDownPaymentChanged(it.toDouble().coerceAtMost(uiState.calculatorAmount)) },
                        valueRange = 0f..uiState.calculatorAmount.toFloat(),
                        colors = SliderDefaults.colors(activeTrackColor = AlimGreen, thumbColor = AlimGreen)
                    )
                }

                // Slider: Interest Rate
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Interest Rate", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("${String.format("%.1f", uiState.calculatorRate)}%", color = AlimGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = uiState.calculatorRate.toFloat(),
                        onValueChange = { onRateChanged(it.toDouble()) },
                        valueRange = 0f..25f,
                        steps = 50,
                        colors = SliderDefaults.colors(activeTrackColor = AlimGreen, thumbColor = AlimGreen)
                    )
                }

                // Slider: Tenure Months
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tenure", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("${uiState.calculatorTenure} Months", color = AlimGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = uiState.calculatorTenure.toFloat(),
                        onValueChange = { onTenureChanged(it.toInt()) },
                        valueRange = 3f..36f,
                        steps = 11,
                        colors = SliderDefaults.colors(activeTrackColor = AlimGreen, thumbColor = AlimGreen)
                    )
                }
            }
        }

        // Save Button
        Button(
            onClick = onSaveAsEmi,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AlimGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save as Active EMI", color = AlimWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun EmiDonutChart(
    principalPercent: Float,
    interestPercent: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sweepPrincipal = principalPercent * 360f
        val sweepInterest = interestPercent * 360f

        // Draw background track
        drawArc(
            color = Color.LightGray.copy(alpha = 0.3f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 24f)
        )

        // Draw Principal section
        drawArc(
            color = AlimGreen,
            startAngle = -90f,
            sweepAngle = sweepPrincipal,
            useCenter = false,
            style = Stroke(width = 24f)
        )

        // Draw Interest section
        drawArc(
            color = CoralRose,
            startAngle = -90f + sweepPrincipal,
            sweepAngle = sweepInterest,
            useCenter = false,
            style = Stroke(width = 24f)
        )
    }
}

// ── 3. Offers Tab ────────────────────────────────────────────────────────────
@Composable
fun OffersTab(
    uiState: EmiUiState,
    onCategorySelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(AlimGreen.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = AlimGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "EMI Offers Coming Soon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "We are partnering with your favorite brands (Samsung, Apple, Star Tech, Ryans, and more) to bring you the best 0% and discounted EMI deals. Stay tuned!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun Double.pow(n: Int): Double = this.pow(n.toDouble())
