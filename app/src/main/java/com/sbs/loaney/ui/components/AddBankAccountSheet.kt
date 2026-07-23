package com.sbs.loaney.ui.components

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.Crossfade
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sbs.loaney.R
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.ui.theme.neubrutalistCard
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextAlign
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

data class AddBankAccountRequest(
    val accountName: String,
    val accountNumber: String,
    val bankName: String,
    val branchName: String?,
    val swiftCode: String?,
    val coverImageUri: String?,
    val isCard: Boolean,
    val isMfs: Boolean,
    val mfsProvider: String?,
    val qrCodeUri: String?
)

@Serializable
data class DraftBankAccount(
    val accountName: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val branchName: String? = null,
    val swiftCode: String? = null,
    val coverImageUri: String? = null,
    val selectedTab: Int = 0,
    val mfsProvider: String? = null,
    val qrCodeUri: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankAccountBottomSheet(
    editingAccount: BankAccountEntity? = null,
    userName: String = "",
    draftJson: String? = null,
    onDismiss: () -> Unit,
    onAdd: (AddBankAccountRequest) -> Unit,
    onSaveDraft: (String) -> Unit = {},
    onClearDraft: () -> Unit = {}
) {
    // Parse draft if editingAccount is null
    val draft = remember(draftJson) {
        if (editingAccount == null && !draftJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<DraftBankAccount>(draftJson)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    var accountName by remember { 
        mutableStateOf(
            editingAccount?.accountName 
                ?: draft?.accountName 
                ?: ""
        ) 
    }
    var accountNumber by remember { 
        mutableStateOf(
            editingAccount?.accountNumber 
                ?: draft?.accountNumber 
                ?: ""
        ) 
    }
    var bankName by remember { 
        mutableStateOf(
            editingAccount?.bankName 
                ?: draft?.bankName 
                ?: ""
        ) 
    }
    var selectedCountry by remember { mutableStateOf("") }
    var branchName by remember { 
        mutableStateOf(
            editingAccount?.branchName 
                ?: draft?.branchName 
                ?: ""
        ) 
    }
    var swiftCode by remember { 
        mutableStateOf(
            editingAccount?.swiftCode 
                ?: draft?.swiftCode 
                ?: ""
        ) 
    }
    var proofUri by remember { 
        mutableStateOf<Uri?>(
            (editingAccount?.coverImageUri ?: draft?.coverImageUri)?.let { Uri.parse(it) }
        ) 
    }
    var selectedTab by remember { 
        mutableIntStateOf(
            when {
                editingAccount?.isMfs == true -> 2
                editingAccount?.isCard == true -> 1
                editingAccount == null && draft != null -> draft.selectedTab
                else -> 0
            }
        ) 
    } // 0: Bank, 1: Card, 2: MFS
    var mfsProvider by remember { 
        mutableStateOf(
            editingAccount?.mfsProvider 
                ?: draft?.mfsProvider 
                ?: "bKash"
        ) 
    }
    var qrCodeUri by remember { 
        mutableStateOf<Uri?>(
            (editingAccount?.qrCodeUri ?: draft?.qrCodeUri)?.let { Uri.parse(it) }
        ) 
    }

    val currentMfsProvider = if (selectedTab == 2) mfsProvider else null
    val currentBankName = if (selectedTab == 2) mfsProvider else bankName
    
    val currentDraft = DraftBankAccount(
        accountName = accountName,
        accountNumber = accountNumber,
        bankName = currentBankName,
        branchName = branchName.ifBlank { null },
        swiftCode = swiftCode.ifBlank { null },
        coverImageUri = if (selectedTab == 2) null else proofUri?.toString(),
        selectedTab = selectedTab,
        mfsProvider = currentMfsProvider,
        qrCodeUri = if (selectedTab == 2) qrCodeUri?.toString() else null
    )
    
    val defaultDraft = DraftBankAccount()
    val baseDraft = draft ?: defaultDraft
    
    val hasChanges = currentDraft != baseDraft && currentDraft != defaultDraft
    val isFullyFilled = accountName.isNotBlank() && accountNumber.isNotBlank() && (selectedTab == 2 || bankName.isNotBlank())
    
    var showCloseConfirmation by remember { mutableStateOf(false) }
    val showPromptOnClose = editingAccount == null && (hasChanges || draft != null)
    
    val mfsProviders = listOf("bKash", "Nagad", "Rocket", "Upay")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        if (selectedTab == 2) {
            qrCodeUri = uri
        } else {
            proofUri = uri 
        }
    }

    val context = LocalContext.current
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
                            accountNumber = cursor.getString(numberIndex)?.replace(Regex("[^0-9]"), "")?.take(15) ?: ""
                        }
                        if (nameIndex >= 0 && accountName.isBlank()) {
                            accountName = cursor.getString(nameIndex) ?: ""
                        }
                    }
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedTab) {
        focusManager.clearFocus()
        scrollState.scrollTo(0)
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    BackHandler(enabled = showPromptOnClose || showCloseConfirmation) {
        if (showCloseConfirmation) {
            showCloseConfirmation = false
        } else {
            showCloseConfirmation = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (showPromptOnClose) {
                showCloseConfirmation = true
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
                // Fixed panel height so switching tabs never resizes / re-anchors the sheet.
                .fillMaxHeight(0.88f)
        ) {
            // ---- Fixed header (never moves between tabs) ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (editingAccount != null) stringResource(id = R.string.addbank_edit_bank_account) else stringResource(id = R.string.add_bank_account),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = {
                        if (showPromptOnClose) {
                            showCloseConfirmation = true
                        } else {
                            onDismiss()
                        }
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Fixed Segmented Toggle (never moves between tabs) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    0 to stringResource(id = R.string.addbank_tab_bank_account),
                    1 to stringResource(id = R.string.addbank_tab_card),
                    2 to stringResource(id = R.string.addbank_tab_mfs)
                ).forEach { (index, title) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title, color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Scrollable tab-specific content (only this area changes) ----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Crossfade(
                    targetState = selectedTab,
                    label = "TabContentSwitch",
                    modifier = Modifier.fillMaxWidth()
                ) { currentTab ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // MFS Provider Chips
                        if (currentTab == 2) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(mfsProviders) { provider ->
                                    val isSelected = mfsProvider == provider
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { 
                                            mfsProvider = provider 
                                            bankName = provider 
                                        },
                                        label = { Text(provider) },
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

                        // Image Picker
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val imageUri = if (currentTab == 2) qrCodeUri else proofUri
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Upload",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val icon = if (currentTab == 2) Icons.Default.QrCode else Icons.Default.Image
                                    val textStr = if (currentTab == 2) stringResource(id = R.string.addbank_upload_qr_code) else stringResource(id = R.string.tap_custom_cover)
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(textStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        
                        if (currentTab != 2) {
                            if (currentTab == 0) {
                                val countries = com.sbs.loaney.data.model.BanksData.countriesWithBanks.keys.toList()
                                val banksForCountry = com.sbs.loaney.data.model.BanksData.countriesWithBanks[selectedCountry] ?: emptyList()
                                
                                SearchableDropdown(
                                    value = selectedCountry,
                                    onValueChange = { selectedCountry = it },
                                    label = stringResource(id = R.string.addbank_country_optional),
                                    leadingIcon = Icons.Default.Public,
                                    options = countries
                                )
                                
                                SearchableDropdown(
                                    value = bankName,
                                    onValueChange = { bankName = it },
                                    label = stringResource(id = R.string.bank_name_hint),
                                    leadingIcon = Icons.Default.AccountBalance,
                                    options = banksForCountry
                                )
                            } else {
                                val cardIssuers = listOf("Visa", "Mastercard", "American Express", "Discover", "JCB", "UnionPay")
                                SearchableDropdown(
                                    value = bankName,
                                    onValueChange = { bankName = it },
                                    label = stringResource(id = R.string.addbank_card_issuer),
                                    leadingIcon = Icons.Default.CreditCard,
                                    options = cardIssuers
                                )
                            }
                        }

                        CustomLightTextField(
                            value = accountName,
                            onValueChange = { accountName = it },
                            label = when (currentTab) {
                                1 -> stringResource(id = R.string.addbank_cardholder_name)
                                2 -> stringResource(id = R.string.account_holder_name_hint)
                                else -> stringResource(id = R.string.account_holder_name_hint)
                            },
                            leadingIcon = Icons.Default.Person,
                            placeholder = if (currentTab == 1) userName else null
                        )

                        if (currentTab == 2) {
                            // MFS Mobile Number with Contact Picker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CustomLightTextField(
                                    value = accountNumber,
                                    onValueChange = {
                                        accountNumber = it.filter { char -> char.isDigit() }.take(15)
                                    },
                                    label = stringResource(id = R.string.addbank_mobile_number),
                                    leadingIcon = Icons.Default.Phone,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                FilledIconButton(
                                    onClick = { 
                                        val intent = android.content.Intent(android.content.Intent.ACTION_PICK).apply {
                                            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                                        }
                                        contactLauncher.launch(intent) 
                                    },
                                    modifier = Modifier.size(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Contacts, contentDescription = "Pick from Contacts", modifier = Modifier.size(24.dp))
                                }
                            }
                        } else {
                            CustomLightTextField(
                                value = accountNumber,
                                onValueChange = {
                                    if (currentTab == 1) {
                                        accountNumber = it.take(16)
                                    } else {
                                        accountNumber = it
                                    }
                                },
                                label = if (currentTab == 1) stringResource(id = R.string.addbank_card_number) else stringResource(id = R.string.account_number_hint),
                                leadingIcon = Icons.Default.DateRange,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = if (currentTab == 1) CardNumberVisualTransformation() else VisualTransformation.None
                            )
                        }

                        if (currentTab == 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CustomLightTextField(
                                    value = branchName ?: "",
                                    onValueChange = { branchName = it },
                                    label = stringResource(id = R.string.branch_optional),
                                    leadingIcon = Icons.Default.LocationOn,
                                    modifier = Modifier.weight(1f)
                                )

                                CustomLightTextField(
                                    value = swiftCode ?: "",
                                    onValueChange = { swiftCode = it },
                                    label = stringResource(id = R.string.swift_optional),
                                    leadingIcon = Icons.Default.Info,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } // ---- end scrollable content ----

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Fixed Save button (never moves between tabs) ----
            Button(
                onClick = {
                    onAdd(AddBankAccountRequest(
                        accountName = accountName,
                        accountNumber = accountNumber,
                        bankName = if (selectedTab == 2) mfsProvider else bankName,
                        branchName = if (selectedTab == 0) branchName?.ifBlank { null } else null,
                        swiftCode = if (selectedTab == 0) swiftCode?.ifBlank { null } else null,
                        coverImageUri = if (selectedTab == 2) null else proofUri?.toString(),
                        isCard = selectedTab == 1,
                        isMfs = selectedTab == 2,
                        mfsProvider = if (selectedTab == 2) mfsProvider else null,
                        qrCodeUri = if (selectedTab == 2) qrCodeUri?.toString() else null
                    ))
                    onClearDraft()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = accountName.isNotBlank() && accountNumber.isNotBlank() && (selectedTab == 2 || bankName.isNotBlank())
            ) {
                val actionLabel = if (editingAccount != null) {
                    stringResource(id = R.string.addbank_update_account)
                } else {
                    when (selectedTab) {
                        1 -> stringResource(id = R.string.addbank_save_card)
                        2 -> stringResource(id = R.string.addbank_save_mfs_account)
                        else -> stringResource(id = R.string.save_bank_account)
                    }
                }
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }

            if (showCloseConfirmation) {
                if (isFullyFilled) {
                    AlertDialog(
                        onDismissRequest = { showCloseConfirmation = false },
                        title = { Text(stringResource(id = R.string.addbank_save_card_account_title)) },
                        text = { Text(stringResource(id = R.string.addbank_all_fields_filled_msg)) },
                        confirmButton = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showCloseConfirmation = false
                                        onAdd(AddBankAccountRequest(
                                            accountName = accountName,
                                            accountNumber = accountNumber,
                                            bankName = if (selectedTab == 2) mfsProvider else bankName,
                                            branchName = if (selectedTab == 0) branchName.ifBlank { null } else null,
                                            swiftCode = if (selectedTab == 0) swiftCode.ifBlank { null } else null,
                                            coverImageUri = if (selectedTab == 2) null else proofUri?.toString(),
                                            isCard = selectedTab == 1,
                                            isMfs = selectedTab == 2,
                                            mfsProvider = if (selectedTab == 2) mfsProvider else null,
                                            qrCodeUri = if (selectedTab == 2) qrCodeUri?.toString() else null
                                        ))
                                        onClearDraft()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(id = R.string.addbank_save_account))
                                }

                                OutlinedButton(
                                    onClick = {
                                        showCloseConfirmation = false
                                        val draftJson = Json.encodeToString(currentDraft)
                                        onSaveDraft(draftJson)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(id = R.string.addbank_save_as_draft))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            showCloseConfirmation = false
                                            onClearDraft()
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(stringResource(id = R.string.addbank_discard), textAlign = TextAlign.Center)
                                    }

                                    TextButton(
                                        onClick = { showCloseConfirmation = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(id = R.string.addbank_keep_editing), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    )
                } else {
                    AlertDialog(
                        onDismissRequest = { showCloseConfirmation = false },
                        title = { Text(stringResource(id = R.string.addbank_save_draft_title)) },
                        text = { Text(stringResource(id = R.string.addbank_form_not_filled_msg)) },
                        confirmButton = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showCloseConfirmation = false
                                        val draftJson = Json.encodeToString(currentDraft)
                                        onSaveDraft(draftJson)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(id = R.string.addbank_save_as_draft))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            showCloseConfirmation = false
                                            onClearDraft()
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(stringResource(id = R.string.addbank_discard), textAlign = TextAlign.Center)
                                    }

                                    TextButton(
                                        onClick = { showCloseConfirmation = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(id = R.string.addbank_keep_editing), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
