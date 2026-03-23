package com.sbs.loaney.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.ContactsContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.model.calculateLoaneyPiePoints
import com.sbs.loaney.ui.components.FullScreenImageViewer
import com.sbs.loaney.ui.components.LoaneyPieRewardOverlay
import com.sbs.loaney.ui.viewmodel.LoanTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.util.PdfReceiptGenerator
import com.sbs.loaney.util.sendReminder

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
    var showForgiveConfirmation by remember { mutableStateOf(false) }

    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var isImageExpanded by remember { mutableStateOf(false) }
    var showEditLoanSheet by remember { mutableStateOf(false) }

    // Loaney Pie reward overlay
    var showRewardOverlay by remember { mutableStateOf(false) }
    var rewardPoints by remember { mutableStateOf(0) }

    // When true the bottom action bar fades out immediately so it
    // doesn't overlap the home-screen nav bar during the exit transition
    var isNavigatingBack by remember { mutableStateOf(false) }

    // Intercept back-navigation: hide the bar first, then navigate
    val handleBack: () -> Unit = {
        isNavigatingBack = true
        onNavigateBack()
    }

    // Intercept system back gesture too
    BackHandler { handleBack() }

    LaunchedEffect(loanId) {
        viewModel.selectLoan(loanId)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showSettleConfirmation) {
        AlertDialog(
            onDismissRequest = { showSettleConfirmation = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(id = R.string.settle_loan_title)) },
            text = { Text(stringResource(id = R.string.settle_loan_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markAsSettled()
                        showSettleConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(id = R.string.settle), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Forgive Debt Confirmation Dialog
    if (showForgiveConfirmation) {
        val remainingAmount = uiState.selectedLoan?.let { lwp ->
            val total = lwp.loan.amount + lwp.loanItems.sumOf { it.amount }
            val paid = lwp.payments.sumOf { it.amount }
            (total - paid).coerceAtLeast(0.0)
        } ?: 0.0
        AlertDialog(
            onDismissRequest = { showForgiveConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarn) },
            title = { Text("Forgive Debt?") },
            text = { Text("Are you sure you want to write off the remaining ${uiState.currencySymbol}${String.format("%,.0f", remainingAmount)}? This will not log it as received cash.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.forgiveLoan()
                        showForgiveConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AmberWarn)
                ) {
                    Text("Forgive", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgiveConfirmation = false }) {
                    Text(stringResource(id = R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        containerColor = AlimCream,
        topBar = {
            Column(modifier = Modifier.background(AlimDark)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            stringResource(id = R.string.loan_details), 
                            fontWeight = FontWeight.SemiBold,
                            color = AlimWhite
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back", 
                                tint = AlimWhite
                            )
                        }
                    },
                    actions = {
                        if (uiState.selectedLoan != null) {
                            IconButton(onClick = {
                                uiState.selectedLoan?.let { lwp ->
                                    PdfReceiptGenerator.generateAndShare(
                                        context = context,
                                        loan = lwp.loan,
                                        payments = lwp.payments,
                                        loanItems = lwp.loanItems,
                                        currencySymbol = uiState.currencySymbol
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Receipt", tint = AlimWhite)
                            }
                            IconButton(onClick = { showEditLoanSheet = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AlimWhite)
                            }
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlimWhite)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AlimDark,
                        titleContentColor = AlimWhite
                    )
                )
            }
        },
        bottomBar = {
            if (!isNavigatingBack) {
            if (uiState.selectedLoan != null && uiState.selectedLoan?.loan?.status != LoanStatus.FULLY_PAID && uiState.selectedLoan?.loan?.status != LoanStatus.FORGIVEN) {
                Surface(
                    color = AlimWhite,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                             Button(
                                 onClick = { showAddPaymentSheet = true },
                                 modifier = Modifier
                                     .weight(1f)
                                     .height(52.dp)
                                     .shadow(8.dp, CircleShape, spotColor = AlimGreen.copy(alpha = 0.3f)),
                                 shape = CircleShape,
                                 colors = ButtonDefaults.buttonColors(containerColor = AlimGreen, contentColor = AlimWhite)
                             ) {
                                 Text(stringResource(id = R.string.pay), fontWeight = FontWeight.Bold)
                             }

                             OutlinedButton(
                                 onClick = { showAddLoanSheet = true },
                                 modifier = Modifier
                                     .weight(1f)
                                     .height(52.dp),
                                 shape = CircleShape,
                                 border = BorderStroke(1.dp, AlimGreen),
                                 colors = ButtonDefaults.outlinedButtonColors(contentColor = AlimGreen)
                             ) {
                                 Text(stringResource(id = R.string.add_more), fontWeight = FontWeight.Bold)
                             }

                             FilledIconButton(
                                 onClick = { showSettleConfirmation = true },
                                 modifier = Modifier.size(52.dp),
                                 shape = CircleShape,
                                 colors = IconButtonDefaults.filledIconButtonColors(containerColor = AlimGreen.copy(alpha = 0.1f))
                             ) {
                                 Icon(Icons.Default.CheckCircle, contentDescription = "Settle", tint = AlimGreen)
                             }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Forgive Debt
                            TextButton(
                                onClick = { showForgiveConfirmation = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("Forgive Debt", style = MaterialTheme.typography.labelMedium)
                            }
                            // Send Reminder
                            TextButton(
                                onClick = {
                                    uiState.selectedLoan?.let { lwp ->
                                        val loan = lwp.loan
                                        val total = loan.amount + lwp.loanItems.sumOf { it.amount }
                                        val paid = lwp.payments.sumOf { it.amount }
                                        val remaining = (total - paid).coerceAtLeast(0.0)
                                        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                                        sendReminder(
                                            context = context,
                                            contactName = loan.personName,
                                            amount = remaining,
                                            dueDate = dateFormat.format(loan.promisedReturnDate),
                                            phoneNumber = loan.phoneNumber.ifBlank { null },
                                            currencySymbol = uiState.currencySymbol
                                        )
                                    }
                                },
                                 colors = ButtonDefaults.textButtonColors(contentColor = AlimGreen)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send Reminder", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            } // end if (!isNavigatingBack)
        }
    ) { padding ->
        if (uiState.selectedLoan == null) {
             com.sbs.loaney.ui.components.AnimatedLoadingScreen(modifier = Modifier.padding(padding))
        } else {
            val loanWithPayments = uiState.selectedLoan!!
            val loan = loanWithPayments.loan
            val payments = loanWithPayments.payments
            val loanItems = loanWithPayments.loanItems

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
                    .background(AlimCream)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Card - Clean White Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = AlimWhite,
                    shadowElevation = 2.dp
                ) {
                     Row(
                        modifier = Modifier
                            .padding(15.dp) // 20.dp * 0.75
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Profile Photo 
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (loan.profilePhotoUri != null) {
                                        expandedImageUri = loan.profilePhotoUri
                                        isImageExpanded = true
                                    } else {
                                        showEditLoanSheet = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (loan.profilePhotoUri != null) {
                                AsyncImage(
                                    model = loan.profilePhotoUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                 val isLent = loan.type == LoanType.LEND
                                 val bgColor = AlimGreen.copy(alpha = 0.1f)
                                 val textColor = AlimGreen
                                 Box(
                                     modifier = Modifier.fillMaxSize().background(bgColor, CircleShape),
                                     contentAlignment = Alignment.Center
                                 ) {
                                     Text(
                                         text = loan.personName.firstOrNull()?.toString()?.uppercase() ?: "",
                                         style = MaterialTheme.typography.titleMedium,
                                         fontWeight = FontWeight.Bold,
                                         color = textColor
                                     )
                                 }
                             }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loan.personName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                 text = if (loan.type == LoanType.LEND) stringResource(id = R.string.due_date_format, dateFormat.format(loan.promisedReturnDate)) else stringResource(id = R.string.repay_by, dateFormat.format(loan.promisedReturnDate)),
                                 style = MaterialTheme.typography.labelMedium,
                                 color = AlimDark.copy(alpha = 0.6f)
                             )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.0f", remaining)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = AlimDark,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp)) // 8.dp * 0.75
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { // 8.dp * 0.75
                                if (!loan.email.isNullOrBlank()) {
                                    FilledIconButton(
                                        onClick = {
                                             val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${loan.email}"))
                                             context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(27.dp), // 36.dp * 0.75
                                     colors = IconButtonDefaults.filledIconButtonColors(containerColor = AlimGreen.copy(alpha = 0.1f))
                                 ) {
                                     Icon(Icons.Default.Email, contentDescription = "Email", tint = AlimGreen, modifier = Modifier.size(14.dp)) // 18.dp * 0.75
                                 }
                                }
                                FilledIconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${loan.phoneNumber}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(27.dp), // 36.dp * 0.75
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(14.dp)) // 18.dp * 0.75
                                }
                            }
                        }
                    }
                }

                // Info Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) { // 12.dp * 0.75
                    InfoTile(
                        title = stringResource(id = R.string.total_amount),
                        value = "${uiState.currencySymbol}${String.format("%.0f", totalLoan)}",
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        title = stringResource(id = R.string.loan_type),
                        value = if (loan.type == LoanType.LEND) stringResource(id = R.string.given) else stringResource(id = R.string.taken),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Detailed Information
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = AlimWhite,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(icon = Icons.Default.Info, label = stringResource(id = R.string.reason_for_loan), value = loan.purpose ?: stringResource(id = R.string.not_specified))
                        val localizedRelationship = when (loan.relationshipType) {
                            "Friend" -> stringResource(id = R.string.relationship_friend)
                            "Family" -> stringResource(id = R.string.relationship_family)
                            "Colleague" -> stringResource(id = R.string.relationship_colleague)
                            "Neighbor" -> stringResource(id = R.string.relationship_neighbor)
                            "Other" -> stringResource(id = R.string.relationship_other)
                            else -> loan.relationshipType ?: stringResource(id = R.string.not_specified)
                        }
                        DetailRow(icon = Icons.Default.Person, label = stringResource(id = R.string.relationship), value = localizedRelationship)
                        if (!loan.email.isNullOrBlank()) {
                            DetailRow(icon = Icons.Default.Email, label = stringResource(id = R.string.email_optional), value = loan.email)
                        }
                        DetailRow(icon = Icons.Default.LocationOn, label = stringResource(id = R.string.location_optional), value = loan.address ?: stringResource(id = R.string.not_specified))
                        DetailRow(icon = Icons.Default.Group, label = stringResource(id = R.string.witness_optional), value = loan.witness ?: stringResource(id = R.string.not_specified))
                        if (loan.interest != null) {
                            DetailRow(icon = Icons.Default.Percent, label = stringResource(id = R.string.interest_rate_optional), value = "${loan.interest}%")
                        }
                        DetailRow(icon = Icons.AutoMirrored.Filled.Notes, label = stringResource(id = R.string.note_optional), value = loan.notes ?: stringResource(id = R.string.no_notes))

                        val statusColor = when (loan.status) {
                            LoanStatus.OVERDUE -> CoralRose
                            LoanStatus.FULLY_PAID -> AlimGreen
                            else -> AlimDark
                        }
                        DetailRow(icon = Icons.Default.CheckCircle, label = stringResource(id = R.string.status), value = loan.status?.name ?: "", highlightColor = statusColor)
                    }
                }

                // Timeline / History
                Text(
                    text = stringResource(id = R.string.loan_history),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AlimDark
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { // 16.dp * 0.75
                    if (historyEvents.isEmpty()) {
                        Text(stringResource(id = R.string.no_history), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
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

                Spacer(modifier = Modifier.height(75.dp)) // 100.dp * 0.75
            }
        }
    }

    if (showAddPaymentSheet) {
        AddPaymentBottomSheet(
            onDismiss = { showAddPaymentSheet = false },
            onAddPayment = { amount, method, note, proofUri ->
                viewModel.addPayment(amount, method, note, proofUri)
                showAddPaymentSheet = false

                // Calculate Loaney Pie reward
                val dueDate = uiState.selectedLoan?.loan?.promisedReturnDate
                val daysBeforeDue = if (dueDate != null) {
                    val diffMs = dueDate.time - System.currentTimeMillis()
                    (diffMs / (1000 * 60 * 60 * 24)).toInt()
                } else 0
                // Reward points for on-time or early payments
                val userXpLevel = 1 // TODO: pull from UserProfile ViewModel
                val pts = calculateLoaneyPiePoints(
                    amountPaid   = amount,
                    daysBeforeDue = daysBeforeDue,
                    userXpLevel  = userXpLevel
                )
                if (pts > 0 && daysBeforeDue >= 0) {
                    rewardPoints = pts
                    showRewardOverlay = true
                }
            }
        )
    }

    // Loaney Pie reward overlay — rendered last so it sits above everything
    LoaneyPieRewardOverlay(
        pointsEarned       = rewardPoints,
        isVisible          = showRewardOverlay,
        onAnimationComplete = { showRewardOverlay = false }
    )

    if (showAddLoanSheet) {
        AddMoreLoanBottomSheet(
            onDismiss = { showAddLoanSheet = false },
            onAddLoan = { amount, note, proofUri ->
                viewModel.addLoanItem(amount, note, proofUri)
                showAddLoanSheet = false
            }
        )
    }

    if (showEditLoanSheet && uiState.selectedLoan != null) {
        val loan = uiState.selectedLoan!!.loan
        EditLoanBottomSheet(
            loan = loan,
            onDismiss = { showEditLoanSheet = false },
            onUpdateLoan = { name, phone, email, address, amount, lDate, rDate, purpose, notes, int, rel, wit, pUri, photoUri ->
                viewModel.updateLoan(name, phone, email, address, amount, lDate, rDate, purpose, notes, int, rel, wit, pUri, photoUri)
                showEditLoanSheet = false
            }
        )
    }

    FullScreenImageViewer(
        visible = isImageExpanded,
        imageUri = expandedImageUri,
        onDismiss = { isImageExpanded = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLoanBottomSheet(
    loan: com.sbs.loaney.data.local.entity.LoanEntity,
    onDismiss: () -> Unit,
    onUpdateLoan: (String, String, String?, String?, Double, Date, Date, String?, String?, Double?, String?, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(loan.personName) }
    var phone by remember { mutableStateOf(loan.phoneNumber) }
    var email by remember { mutableStateOf(loan.email ?: "") }
    var address by remember { mutableStateOf(loan.address ?: "") }
    var amount by remember { mutableStateOf(loan.amount.toLong().toString()) }
    var loanDate by remember { mutableLongStateOf(loan.loanDate.time) }
    var returnDate by remember { mutableLongStateOf(loan.promisedReturnDate.time) }
    var purpose by remember { mutableStateOf(loan.purpose ?: "") }
    var notes by remember { mutableStateOf(loan.notes ?: "") }
    var interestRate by remember { mutableStateOf(loan.interest?.toString() ?: "") }
    var selectedRelationship by remember { mutableStateOf(loan.relationshipType ?: "Other") }
    var witness by remember { mutableStateOf(loan.witness ?: "") }
    var proofUri by remember { mutableStateOf<Uri?>(loan.proofUri?.let { Uri.parse(it) }) }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(loan.profilePhotoUri?.let { Uri.parse(it) }) }

    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    var showLoanDatePicker by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }

    val loanReasons = listOf(
        R.string.reason_food,
        R.string.reason_emergency,
        R.string.reason_shopping,
        R.string.reason_travel,
        R.string.reason_bills,
        R.string.reason_other
    )
    val relationships = listOf(
        stringResource(R.string.relationship_friend) to "Friend",
        stringResource(R.string.relationship_family) to "Family",
        stringResource(R.string.relationship_colleague) to "Colleague",
        stringResource(R.string.relationship_neighbor) to "Neighbor",
        stringResource(R.string.relationship_other) to "Other"
    )

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        proofUri = uri
    }
    
    val profilePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        profilePhotoUri = uri
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

    if (showLoanDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = loanDate)
        DatePickerDialog(
            onDismissRequest = { showLoanDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { loanDate = it }
                    showLoanDatePicker = false
                }) { Text(stringResource(id = R.string.ok), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLoanDatePicker = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showReturnDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = returnDate)
        DatePickerDialog(
            onDismissRequest = { showReturnDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { returnDate = it }
                    showReturnDatePicker = false
                }) { Text(stringResource(id = R.string.ok), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDatePicker = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 36.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(stringResource(id = R.string.edit_loan_details), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // Name & Phone using CustomLightTextField for consistency and contact selection
            CustomLightTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                leadingIcon = Icons.Default.Person,
                trailingIcon = Icons.Default.ContactPhone,
                onTrailingIconClick = { 
                    val intent = Intent(Intent.ACTION_PICK).apply {
                        type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                    }
                    contactLauncher.launch(intent) 
                }
            )

            CustomLightTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone",
                leadingIcon = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            CustomLightTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = "Amount",
                leadingIcon = Icons.Default.AttachMoney,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Purpose / Reason for Loan
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(id = R.string.reason_for_loan), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(loanReasons) { rRes ->
                        val r = stringResource(id = rRes)
                        val isSelected = purpose == r
                        FilterChip(
                            selected = isSelected,
                            onClick = { purpose = if (isSelected) "" else r },
                            label = { Text(r) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            // Dates
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f).clickable { showLoanDatePicker = true }) {
                    CustomLightTextField(
                        value = dateFormat.format(Date(loanDate)),
                        onValueChange = {},
                        label = "Loan Date",
                        readOnly = true,
                        enabled = false,
                        leadingIcon = Icons.Default.CalendarToday
                    )
                }
                Box(modifier = Modifier.weight(1f).clickable { showReturnDatePicker = true }) {
                    CustomLightTextField(
                        value = dateFormat.format(Date(returnDate)),
                        onValueChange = {},
                        label = "Due Date",
                        readOnly = true,
                        enabled = false,
                        leadingIcon = Icons.Default.Event
                    )
                }
            }

            // Relationship
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Relationship", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(relationships) { pair ->
                        val label = pair.first
                        val value = pair.second
                        val isSelected = selectedRelationship == value
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRelationship = value },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            // Optional Details
            CustomLightTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email (Optional)",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            CustomLightTextField(
                value = address,
                onValueChange = { address = it },
                label = "Address (Optional)",
                leadingIcon = Icons.Default.LocationOn
            )

            CustomLightTextField(
                value = witness,
                onValueChange = { witness = it },
                label = "Witness (Optional)",
                leadingIcon = Icons.Default.Group
            )

            CustomLightTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes (Optional)",
                leadingIcon = Icons.AutoMirrored.Filled.Notes
            )
            
            // Attachments
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Profile Photo & Attachments", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Profile Photo
                    Box(
                        modifier = Modifier
                            .size(60.dp)
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
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Add Profile Photo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    // Proof Attachment
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imageLauncher.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (proofUri != null) {
                            AsyncImage(
                                model = proofUri,
                                contentDescription = "Proof",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.AttachFile, contentDescription = "Add Proof", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onUpdateLoan(
                        name, phone, email.ifBlank { null }, address.ifBlank { null },
                        amount.toDoubleOrNull() ?: 0.0, Date(loanDate), Date(returnDate),
                        purpose.ifBlank { null }, notes.ifBlank { null }, interestRate.toDoubleOrNull(),
                        selectedRelationship, witness.ifBlank { null }, proofUri?.toString(), profilePhotoUri?.toString()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && amount.isNotBlank()
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, highlightColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp)) // 20.dp * 0.75
        Spacer(modifier = Modifier.width(9.dp)) // 12.dp * 0.75
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(15.dp)), // 20.dp * 0.75
        shape = SmallCardShape, // item card
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) { // 16.dp * 0.75
            Text(
                title, 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp)) // 8.dp * 0.75
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TimelineItem(date: String, title: String, subtitle: String, isLoan: Boolean = false, amount: String? = null) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(45.dp)) { // 60.dp * 0.75
            Text(
                text = date.substringBefore(" "), // Day
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = date.split(" ").getOrNull(1) ?: "", // Month
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp)) // 16.dp * 0.75

        Card(
            modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)), // 16.dp * 0.75
            shape = SoftChipShape, // inner card
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp), // 16.dp * 0.75
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp) // 10.dp * 0.75
                        .background(if (isLoan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(9.dp)) // 12.dp * 0.75
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (amount != null) {
                    Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isLoan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground)
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp) // 24.dp * 0.75
                .padding(bottom = 36.dp) // 48.dp * 0.75
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(15.dp) // 20.dp * 0.75
        ) {
            Text(stringResource(id = R.string.add_payment), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text(stringResource(id = R.string.amount), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Column {
                Text(stringResource(id = R.string.payment_method), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp)) // 8.dp * 0.75
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { // 8.dp * 0.75
                    methods.take(3).forEach { m ->
                        val selected = method == m
                        FilterChip(
                            selected = selected,
                            onClick = { method = m },
                            label = { Text(m) },
                             colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha=0.3f),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true,
                                selected = selected
                            ),
                            shape = CircleShape
                        )
                    }
                }
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { // 8.dp * 0.75
                    methods.drop(3).forEach { m ->
                        val selected = method == m
                        FilterChip(
                            selected = selected,
                            onClick = { method = m },
                            label = { Text(m) },
                             colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha=0.3f),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
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
                label = { Text(stringResource(id = R.string.note_optional), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let {
                        onAddPayment(it, method, note.ifBlank { null }, proofUri?.toString())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(42.dp), // 56.dp * 0.75
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = amount.isNotBlank()
            ) {
                Text(stringResource(id = R.string.save_payment), fontWeight = FontWeight.Bold)
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp) // 24.dp * 0.75
                .padding(bottom = 36.dp) // 48.dp * 0.75
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(15.dp) // 20.dp * 0.75
        ) {
            Text(stringResource(id = R.string.add_more_loan), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text(stringResource(id = R.string.amount), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(id = R.string.note_optional), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let {
                        onAddLoan(it, note.ifBlank { null }, proofUri?.toString())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(42.dp), // 56.dp * 0.75
                shape = RoundedCornerShape(12.dp), // 16.dp * 0.75
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                enabled = amount.isNotBlank()
            ) {
                Text(stringResource(id = R.string.add_loan), fontWeight = FontWeight.Bold)
            }
        }
    }
}
