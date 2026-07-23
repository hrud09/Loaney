package com.sbs.loaney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.local.entity.DepositEntity
import com.sbs.loaney.data.model.BanksData
import com.sbs.loaney.ui.components.AnimatedCurrencyText
import com.sbs.loaney.ui.components.CustomLightTextField
import com.sbs.loaney.ui.components.SearchableDropdown
import com.sbs.loaney.ui.components.ToolInfoDialog
import com.sbs.loaney.ui.components.ToolInfoType
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.DepositUiState
import com.sbs.loaney.ui.viewmodel.DepositViewModel
import com.sbs.loaney.util.DepositCalculator
import com.sbs.loaney.util.DepositType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun money(symbol: String, value: Double): String =
    "$symbol${String.format(Locale.getDefault(), "%,.0f", value)}"

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

private fun tenureLabel(months: Int): String = when {
    months <= 0 -> "-"
    months < 12 -> "$months mo"
    months % 12 == 0 -> "${months / 12} yr"
    else -> "${months / 12} yr ${months % 12} mo"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
    onNavigateBack: () -> Unit,
    viewModel: DepositViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Savings",
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
                            contentDescription = "Deposit Info",
                            tint = AlimWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AlimDark,
                    titleContentColor = AlimWhite
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
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
                    text = { Text("Calculator", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My Deposits", fontWeight = FontWeight.SemiBold) }
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> CalculatorTab(
                        uiState = uiState,
                        onTypeChange = viewModel::setType,
                        onBankChange = viewModel::setBankName,
                        onAmountChange = viewModel::setAmount,
                        onRateChange = viewModel::setRate,
                        onTenureChange = viewModel::setTenure,
                        onCompoundingChange = viewModel::setCompoundingsPerYear,
                        onTinChange = viewModel::setHasTin,
                        onSave = {
                            viewModel.saveDeposit()
                            selectedTab = 1
                        }
                    )
                    1 -> TrackerTab(
                        uiState = uiState,
                        onDelete = viewModel::deleteDeposit,
                        onAddClick = { selectedTab = 0 }
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        ToolInfoDialog(
            toolType = ToolInfoType.DPS_FDR,
            onDismiss = { showInfoDialog = false }
        )
    }
}

// ── Calculator ───────────────────────────────────────────────────────────────

@Composable
private fun CalculatorTab(
    uiState: DepositUiState,
    onTypeChange: (DepositType) -> Unit,
    onBankChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onTenureChange: (String) -> Unit,
    onCompoundingChange: (Int) -> Unit,
    onTinChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    val isDps = uiState.type == DepositType.DPS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DepositTypeToggle(selected = uiState.type, onSelect = onTypeChange)

        Text(
            text = if (isDps) {
                "A Deposit Pension Scheme: you pay a fixed amount every month for the full term."
            } else {
                "A Fixed Deposit Receipt: you lock a lump sum away for the full term."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SearchableDropdown(
            value = uiState.bankName,
            onValueChange = onBankChange,
            label = "Bank",
            leadingIcon = Icons.Default.AccountBalance,
            options = BanksData.countriesWithBanks["Bangladesh"].orEmpty()
        )

        CustomLightTextField(
            value = uiState.amountInput,
            onValueChange = onAmountChange,
            label = if (isDps) "Monthly Deposit" else "Deposit Amount",
            leadingIcon = Icons.Default.Payments,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = if (isDps) "5000" else "100000"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                CustomLightTextField(
                    value = uiState.rateInput,
                    onValueChange = onRateChange,
                    label = "Interest Rate (%)",
                    leadingIcon = Icons.Default.Percent,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = "8.5"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                CustomLightTextField(
                    value = uiState.tenureInput,
                    onValueChange = onTenureChange,
                    label = "Tenure (months)",
                    leadingIcon = Icons.Default.CalendarMonth,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = "60"
                )
            }
        }

        CompoundingSelector(
            selected = uiState.compoundingsPerYear,
            onSelect = onCompoundingChange
        )

        // AIT on deposit interest is 10% with a TIN, 15% without.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "I have a TIN",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Source tax on interest: ${uiState.taxRatePercent.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.hasTin,
                onCheckedChange = onTinChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AlimWhite,
                    checkedTrackColor = AlimGreen
                )
            )
        }

        if (uiState.hasProjection) {
            ProjectionCard(uiState = uiState)

            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlimDark,
                    contentColor = AlimWhite
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save as Deposit", fontWeight = FontWeight.Bold)
            }

            if (!uiState.canSave) {
                Text(
                    "Pick a bank to save this deposit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            "Indicative only. Actual returns depend on your bank's exact compounding and tax rules.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DepositTypeToggle(
    selected: DepositType,
    onSelect: (DepositType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DepositType.entries.forEach { type ->
            val isSelected = type == selected
            Surface(
                onClick = { onSelect(type) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AlimGreen else Color.Transparent
            ) {
                Text(
                    text = type.name,
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AlimWhite else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompoundingSelector(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(
        12 to "Monthly",
        4 to "Quarterly",
        2 to "Half-yearly",
        1 to "Yearly"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Compounding",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(value) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlimGreen,
                        selectedLabelColor = AlimWhite
                    )
                )
            }
        }
    }
}

@Composable
private fun ProjectionCard(uiState: DepositUiState) {
    val p = uiState.projection
    val symbol = uiState.currencySymbol

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AlimGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "You'll receive at maturity",
                style = MaterialTheme.typography.bodyMedium,
                color = AlimWhite.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            AnimatedCurrencyText(
                targetValue = p.netMaturityAmount,
                currencySymbol = symbol,
                color = AlimWhite,
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "after ${tenureLabel(uiState.tenureMonths)}, net of tax",
                style = MaterialTheme.typography.bodySmall,
                color = AlimWhite.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = AlimWhite.copy(alpha = 0.25f))
            Spacer(Modifier.height(16.dp))

            ProjectionRow("Total deposited", money(symbol, p.totalDeposited))
            ProjectionRow("Interest earned", "+ ${money(symbol, p.grossInterest)}")
            ProjectionRow(
                "Source tax (${uiState.taxRatePercent.toInt()}%)",
                "- ${money(symbol, p.taxOnInterest)}"
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = AlimWhite.copy(alpha = 0.25f))
            Spacer(Modifier.height(8.dp))

            ProjectionRow(
                "Net maturity value",
                money(symbol, p.netMaturityAmount),
                emphasise = true
            )
            ProjectionRow(
                "Effective yield",
                String.format(Locale.getDefault(), "%.2f%% / yr", p.effectiveAnnualYield)
            )

            if (uiState.tenureMonths >= 2) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlimWhite.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "If you break it early",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AlimWhite
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Cashing out at ${tenureLabel(uiState.tenureMonths / 2)} at the " +
                                "${DepositCalculator.DEFAULT_PREMATURE_RATE.toInt()}% penalty rate returns about " +
                                money(symbol, uiState.prematureProjection.netMaturityAmount) + ".",
                            style = MaterialTheme.typography.bodySmall,
                            color = AlimWhite.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectionRow(label: String, value: String, emphasise: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = AlimWhite.copy(alpha = if (emphasise) 1f else 0.85f),
            fontWeight = if (emphasise) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = AlimWhite,
            fontWeight = if (emphasise) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

// ── Tracker ──────────────────────────────────────────────────────────────────

@Composable
private fun TrackerTab(
    uiState: DepositUiState,
    onDelete: (DepositEntity) -> Unit,
    onAddClick: () -> Unit
) {
    if (uiState.deposits.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Savings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No deposits yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Work out a DPS or FDR in the calculator, then save it here to track it to maturity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlimGreen, contentColor = AlimWhite)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Calculator", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AlimDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryStat(
                        label = "Invested",
                        value = money(uiState.currencySymbol, uiState.totalInvested)
                    )
                    SummaryStat(
                        label = "Matures to",
                        value = money(uiState.currencySymbol, uiState.totalProjectedValue),
                        valueColor = AlimGreen
                    )
                    SummaryStat(
                        label = "Active",
                        value = uiState.activeDeposits.size.toString()
                    )
                }
            }
        }

        if (uiState.activeDeposits.isNotEmpty()) {
            item { SectionLabel("Active") }
            items(uiState.activeDeposits, key = { it.id }) { deposit ->
                DepositCard(deposit, uiState.currencySymbol, onDelete)
            }
        }

        if (uiState.maturedDeposits.isNotEmpty()) {
            item { SectionLabel("Matured") }
            items(uiState.maturedDeposits, key = { it.id }) { deposit ->
                DepositCard(deposit, uiState.currencySymbol, onDelete)
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, valueColor: Color = AlimWhite) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AlimWhite.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun DepositCard(
    deposit: DepositEntity,
    currencySymbol: String,
    onDelete: (DepositEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val span = (deposit.maturityDate - deposit.startDate).coerceAtLeast(1L)
    val progress = ((now - deposit.startDate).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    val isMatured = progress >= 1f || deposit.isMatured

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (deposit.type == DepositType.DPS.name) CyberIndigo else AlimGreen
                        ) {
                            Text(
                                deposit.type,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AlimWhite
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            deposit.bankName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (deposit.type == DepositType.DPS.name) {
                            "${money(currencySymbol, deposit.monthlyDeposit)}/mo at ${deposit.annualRate}% for ${tenureLabel(deposit.tenureMonths)}"
                        } else {
                            "${money(currencySymbol, deposit.principalAmount)} at ${deposit.annualRate}% for ${tenureLabel(deposit.tenureMonths)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete deposit",
                        tint = CoralRose
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isMatured) AlimGreen else CyberIndigo,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (isMatured) "Matured on" else "Matures on",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatDate(deposit.maturityDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Maturity value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        money(currencySymbol, deposit.maturityAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AlimGreen
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete deposit?") },
            text = { Text("This removes the ${deposit.type} at ${deposit.bankName} from your tracker. It does not touch the actual deposit at your bank.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(deposit)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = CoralRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
