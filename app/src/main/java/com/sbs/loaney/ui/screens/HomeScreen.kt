package com.sbs.loaney.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.sbs.loaney.ui.components.OnboardingIllustration
import com.sbs.loaney.ui.components.OnboardingIllustrationType
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sbs.loaney.R
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.data.local.dao.LoanWithPayments
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.ui.components.*
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.HomeViewModel
import com.sbs.loaney.ui.viewmodel.HomeUiState
import com.sbs.loaney.ui.viewmodel.EmailLinkStatus
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToAddLoan: (String) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToHistory: (String?) -> Unit,
    onNavigateToHistoryScreen: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToTools: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    notificationsViewModel: com.sbs.loaney.ui.viewmodel.NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifications by notificationsViewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }
    val context = LocalContext.current
    var showAddBankSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var isImageExpanded by remember { mutableStateOf(false) }
    var showLentSummary by remember { mutableStateOf(false) }
    var showBorrowedSummary by remember { mutableStateOf(false) }
    var showFeaturedCalendar by remember { mutableStateOf(false) }
    
    var accountToDelete by remember { mutableStateOf<BankAccountEntity?>(null) }
    var accountToEdit by remember { mutableStateOf<BankAccountEntity?>(null) }
    var accountToShare by remember { mutableStateOf<BankAccountEntity?>(null) }
    var shareEmail by remember { mutableStateOf("") }
    val shareStatus by viewModel.shareStatus.collectAsState()
    val shareLinkedName by viewModel.shareLinkedName.collectAsState()
    val outgoingShares by viewModel.outgoingShares.collectAsState()

    LaunchedEffect(accountToShare?.id) {
        accountToShare?.let { viewModel.observeSharesForAccount(it) }
            ?: viewModel.clearOutgoingSharesObservation()
    }

    val bankAccounts = uiState.bankAccounts
    val allLoans by remember(uiState.lentLoans, uiState.borrowedLoans) {
        derivedStateOf { (uiState.lentLoans + uiState.borrowedLoans).sortedByDescending { it.loan.loanDate } }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(AlimDark)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Loaney", 
                            fontWeight = FontWeight.SemiBold,
                            color = AlimWhite
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AlimDark,
                        titleContentColor = AlimWhite
                    )
                )
            }
        }
    ) { padding ->
        var profileCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var notificationCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var balanceCardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var quickActionsCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var toolsCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var bankSectionCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

        val tutorialSteps = remember(
            balanceCardCoords, quickActionsCoords, toolsCoords,
            bankSectionCoords, notificationCoords, profileCoords
        ) {
            listOf(
                TutorialStep(
                    title = context.getString(R.string.home_tutorial_balance_title),
                    description = context.getString(R.string.home_tutorial_balance_desc),
                    targetCoordinates = balanceCardCoords
                ),
                TutorialStep(
                    title = context.getString(R.string.home_tutorial_record_title),
                    description = context.getString(R.string.home_tutorial_record_desc),
                    targetCoordinates = quickActionsCoords
                ),
                TutorialStep(
                    title = context.getString(R.string.home_tutorial_tools_title),
                    description = context.getString(R.string.home_tutorial_tools_desc),
                    targetCoordinates = toolsCoords
                ),
                TutorialStep(
                    title = context.getString(R.string.home_tutorial_accounts_title),
                    description = context.getString(R.string.home_tutorial_accounts_desc),
                    targetCoordinates = bankSectionCoords
                ),
                TutorialStep(
                    title = context.getString(R.string.home_tutorial_duedate_title),
                    description = context.getString(R.string.home_tutorial_duedate_desc),
                    targetCoordinates = notificationCoords
                ),
                TutorialStep(
                    title = context.getString(R.string.home_tutorial_customize_title),
                    description = context.getString(R.string.home_tutorial_customize_desc),
                    targetCoordinates = profileCoords
                )
            )
        }

        // Hold the tutorial until every target has been measured. Starting earlier means the
        // step list grows underneath the user as onGloballyPositioned fires one target at a
        // time, which shifts the step they're reading and makes the counter jump.
        val allTargetsMeasured = balanceCardCoords != null && quickActionsCoords != null &&
                toolsCoords != null && bankSectionCoords != null &&
                notificationCoords != null && profileCoords != null

        var showTutorial by remember { mutableStateOf(false) }

        LaunchedEffect(uiState.isLoading, uiState.hasSeenTutorial, allTargetsMeasured) {
            if (!uiState.isLoading && !uiState.hasSeenTutorial && allTargetsMeasured) {
                showTutorial = true
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            if (uiState.isLoading) {
                com.sbs.loaney.ui.components.AnimatedLoadingScreen()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // NEW ALIMBANK STYLE HEADER
                    AlimHeader(
                        userName = uiState.userName,
                        userProfilePhoto = uiState.userProfilePhoto,
                        unreadNotificationsCount = unreadCount,
                        onProfileClick = onProfileClick,
                        onNotificationsClick = { showNotificationsSheet = true },
                        onPositionedProfile = { profileCoords = it },
                        onPositionedNotification = { notificationCoords = it }
                    )
                    
                    Box(modifier = Modifier.onGloballyPositioned { balanceCardCoords = it }) {
                        AlimBalanceCard(
                            totalLent = uiState.totalLent,
                            totalBorrowed = uiState.totalBorrowed,
                            currencySymbol = uiState.currencySymbol,
                            onNavigateToAddLoan = onNavigateToAddLoan,
                            onNavigateToHistory = onNavigateToHistory,
                            onNavigateToHistoryScreen = onNavigateToHistoryScreen,
                            onReportClick = { onNavigateToHistory(null) },
                            onCalendarClick = { showFeaturedCalendar = true },
                            onToolsClick = onNavigateToTools,
                            onPositionedQuickActions = { quickActionsCoords = it },
                            onPositionedTools = { toolsCoords = it }
                        )
                    }

                    // SCROLLABLE CONTENT
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))


                Spacer(modifier = Modifier.height(20.dp))



                // ── BANK ACCOUNTS ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .onGloballyPositioned { bankSectionCoords = it },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.bank_accounts),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(id = R.string.add),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { showAddBankSheet = true }
                        )
                    }

                    if (bankAccounts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(id = R.string.no_bank_accounts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showAddBankSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(id = R.string.home_link_account), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        val lazyListState = rememberLazyListState()
                        val snapBehavior = rememberSnapFlingBehavior(lazyListState)
                        LazyRow(
                            state = lazyListState,
                            flingBehavior = snapBehavior,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(bankAccounts, key = { "${it.id}_${it.shareId}" }) { account ->
                                BankAccountCard(
                                    account = account,
                                    context = context,
                                    onDelete = { accountToDelete = it },
                                    onEdit = {
                                        if (account.isOwnedByMe) {
                                            accountToEdit = it
                                            showAddBankSheet = true
                                        }
                                    },
                                    onShare = { if (account.isOwnedByMe) accountToShare = it }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(130.dp))
                } // Column at 222
            } // Column at 183
        } // Column at 167
    } // else/isLoading
} // Box/Scaffold content

    if (showAddBankSheet) {
        AddBankAccountBottomSheet(
            editingAccount = accountToEdit,
            userName = uiState.userName,
            draftJson = uiState.draftBankAccountJson,
            onDismiss = { 
                showAddBankSheet = false
                accountToEdit = null
            },
            onAdd = { request ->
                if (accountToEdit != null) {
                    viewModel.updateBankAccount(
                        BankAccountEntity(
                            id = accountToEdit!!.id,
                            accountName = request.accountName,
                            accountNumber = request.accountNumber,
                            bankName = request.bankName,
                            branchName = request.branchName,
                            swiftCode = request.swiftCode,
                            coverImageUri = request.coverImageUri,
                            isCard = request.isCard,
                            isMfs = request.isMfs,
                            mfsProvider = request.mfsProvider,
                            qrCodeUri = request.qrCodeUri
                        )
                    )
                } else {
                    viewModel.addBankAccount(
                        accountName = request.accountName,
                        accountNumber = request.accountNumber,
                        bankName = request.bankName,
                        branchName = request.branchName,
                        swiftCode = request.swiftCode,
                        coverImageUri = request.coverImageUri,
                        isCard = request.isCard,
                        isMfs = request.isMfs,
                        mfsProvider = request.mfsProvider,
                        qrCodeUri = request.qrCodeUri
                    )
                }
                showAddBankSheet = false
                accountToEdit = null
            },
            onSaveDraft = { draftJson ->
                viewModel.saveDraftBankAccount(draftJson)
            },
            onClearDraft = {
                viewModel.saveDraftBankAccount(null)
            }
        )
    }

    if (accountToDelete != null) {
        val isSharedIncoming = accountToDelete!!.isSharedIncoming
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Text(if (isSharedIncoming) stringResource(id = R.string.home_remove_shared_account) else stringResource(id = R.string.home_delete_bank_account))
            },
            text = {
                Text(
                    if (isSharedIncoming) {
                        stringResource(id = R.string.home_remove_shared_account_msg)
                    } else {
                        stringResource(id = R.string.home_delete_bank_account_msg)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToDelete?.let {
                            if (it.isSharedIncoming) {
                                viewModel.removeSharedAccountLocally(it)
                            } else {
                                viewModel.deleteBankAccount(it)
                            }
                        }
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isSharedIncoming) stringResource(id = R.string.home_remove) else stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
 
     if (accountToShare != null) {
         ShareAccountBottomSheet(
             account = accountToShare!!,
             shareEmail = shareEmail,
             onShareEmailChange = {
                 shareEmail = it
                 viewModel.checkShareIdentifier(it)
             },
             shareStatus = shareStatus,
             shareLinkedName = shareLinkedName,
             outgoingShares = outgoingShares,
             onShare = { permission ->
                 accountToShare?.let { account ->
                     viewModel.shareBankAccount(account, shareEmail, permission) {
                         accountToShare = null
                         shareEmail = ""
                         Toast.makeText(context, context.getString(R.string.home_account_shared_success), Toast.LENGTH_SHORT).show()
                     }
                 }
             },
             onRevokeShare = { viewModel.revokeShare(it) },
             onDismiss = {
                 accountToShare = null
                 shareEmail = ""
                 viewModel.resetShareEmailStatus()
                 viewModel.clearOutgoingSharesObservation()
             }
         )
     }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            onDismiss = { showNotificationsSheet = false }
        )
    }

    FullScreenImageViewer(
        visible = isImageExpanded,
        imageUri = expandedImageUri,
        onDismiss = { isImageExpanded = false }
    )

    if (showLentSummary) {
        LoanSummaryDialog(
            title = stringResource(id = R.string.total_given),
            loans = uiState.lentLoans,
            currencySymbol = uiState.currencySymbol,
            accentColor = MaterialTheme.colorScheme.primary,
            onDismiss = { showLentSummary = false }
        )
    }

    if (showBorrowedSummary) {
        LoanSummaryDialog(
            title = stringResource(id = R.string.total_taken),
            loans = uiState.borrowedLoans,
            currencySymbol = uiState.currencySymbol,
            accentColor = MaterialTheme.colorScheme.error,
            onDismiss = { showBorrowedSummary = false }
        )
    }

        FeaturedCalendarPopup(
            visible = showFeaturedCalendar,
            onDismiss = { showFeaturedCalendar = false },
            allEvents = uiState.allEvents,
            currencySymbol = uiState.currencySymbol,
            onNavigateToDetail = onNavigateToDetail
        )

        TutorialOverlay(
            steps = tutorialSteps,
            isVisible = showTutorial,
            onComplete = {
                showTutorial = false
                viewModel.setHasSeenTutorial(true)
            }
        )
    }
}

