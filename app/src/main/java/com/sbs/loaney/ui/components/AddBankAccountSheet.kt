package com.sbs.loaney.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.sbs.loaney.R
import com.sbs.loaney.data.local.entity.BankAccountEntity
import com.sbs.loaney.ui.theme.neubrutalistCard
import com.sbs.loaney.util.BankAccountOcrExtractor
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

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
    val coroutineScope = rememberCoroutineScope()
    var isOcrProcessing by remember { mutableStateOf(false) }
    var ocrFieldsApplied by remember { mutableStateOf(false) }

    // ── Camera capture URI (for TakePicture contract) ─────────────────
    val cameraImageUri = remember {
        val photoDir = File(context.cacheDir, "ocr_photos")
        if (!photoDir.exists()) photoDir.mkdirs()
        val photoFile = File(photoDir, "ocr_capture_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
    }

    // Shared OCR processing function
    fun runOcrOnImage(uri: Uri) {
        isOcrProcessing = true
        coroutineScope.launch {
            try {
                val result = BankAccountOcrExtractor.extractFromImage(context, uri)
                // Auto-fill fields that were extracted (only fill blank fields)
                result.accountNumber?.let { if (accountNumber.isBlank()) accountNumber = it }
                result.accountName?.let { if (accountName.isBlank()) accountName = it }
                result.bankName?.let { if (bankName.isBlank()) bankName = it }
                result.branchName?.let { if (branchName.isBlank()) branchName = it }
                result.swiftCode?.let { if (swiftCode.isBlank()) swiftCode = it }

                val filledCount = listOfNotNull(
                    result.accountNumber, result.accountName, result.bankName,
                    result.branchName, result.swiftCode
                ).size

                if (filledCount > 0) {
                    ocrFieldsApplied = true
                    Toast.makeText(context, "$filledCount field(s) auto-filled from image", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No bank details found in the image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to scan image", Toast.LENGTH_SHORT).show()
            } finally {
                isOcrProcessing = false
            }
        }
    }

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            runOcrOnImage(cameraImageUri)
        }
    }

    // Gallery OCR picker launcher
    val galleryOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { runOcrOnImage(it) }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(cameraImageUri)
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
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

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetValue ->
            if (sheetValue == SheetValue.Hidden) {
                if (showPromptOnClose) {
                    showCloseConfirmation = true
                    false
                } else {
                    true
                }
            } else {
                true
            }
        }
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (editingAccount != null) "Edit Bank Account" else stringResource(id = R.string.add_bank_account), 
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

            // Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0 to "Bank Account", 1 to "Card", 2 to "MFS").forEach { (index, title) ->
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

            // MFS Provider Chips
            if (selectedTab == 2) {
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
                val imageUri = if (selectedTab == 2) qrCodeUri else proofUri
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Upload",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = if (selectedTab == 2) Icons.Default.QrCode else Icons.Default.Image
                        val textStr = if (selectedTab == 2) "Upload My QR Code" else stringResource(id = R.string.tap_custom_cover)
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(textStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── OCR Scan Document Button ─────────────────────────────────
            if (selectedTab != 2 && editingAccount == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Scan Document to Auto-fill",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            "Take a photo or pick a screenshot of your bank statement, cheque, or passbook",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPermission) {
                                        cameraLauncher.launch(cameraImageUri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(true),
                                enabled = !isOcrProcessing
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", fontWeight = FontWeight.Medium)
                            }
                            OutlinedButton(
                                onClick = {
                                    galleryOcrLauncher.launch("image/*")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(true),
                                enabled = !isOcrProcessing
                            ) {
                                Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontWeight = FontWeight.Medium)
                            }
                        }

                        // Processing indicator
                        AnimatedVisibility(
                            visible = isOcrProcessing,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Scanning document...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Success indicator
                        AnimatedVisibility(
                            visible = ocrFieldsApplied && !isOcrProcessing,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Fields auto-filled! Review below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab != 2) {
                if (selectedTab == 0) {
                    val countries = com.sbs.loaney.data.model.BanksData.countriesWithBanks.keys.toList()
                    val banksForCountry = com.sbs.loaney.data.model.BanksData.countriesWithBanks[selectedCountry] ?: emptyList()
                    
                    SearchableDropdown(
                        value = selectedCountry,
                        onValueChange = { selectedCountry = it },
                        label = "Country (Optional)",
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
                        label = "Card Issuer (e.g. Visa, Mastercard)",
                        leadingIcon = Icons.Default.CreditCard,
                        options = cardIssuers
                    )
                }
            }

            CustomLightTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = when (selectedTab) {
                    1 -> "Cardholder Name"
                    2 -> "Account Holder Name"
                    else -> stringResource(id = R.string.account_holder_name_hint)
                },
                leadingIcon = Icons.Default.Person,
                placeholder = if (selectedTab == 1) userName else null
            )

            if (selectedTab == 2) {
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
                        label = "Mobile Number",
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
                        if (selectedTab == 1) {
                            accountNumber = it.take(16)
                        } else {
                            accountNumber = it
                        }
                    },
                    label = if (selectedTab == 1) "Card Number" else stringResource(id = R.string.account_number_hint),
                    leadingIcon = Icons.Default.DateRange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = if (selectedTab == 1) CardNumberVisualTransformation() else VisualTransformation.None
                )
            }

            if (selectedTab == 0) {
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
                    "Update Account"
                } else {
                    when (selectedTab) {
                        1 -> "Save Card"
                        2 -> "Save MFS Account"
                        else -> stringResource(id = R.string.save_bank_account)
                    }
                }
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }

            if (showCloseConfirmation) {
                if (isFullyFilled) {
                    AlertDialog(
                        onDismissRequest = { showCloseConfirmation = false },
                        title = { Text("Save Card/Account?") },
                        text = { Text("You have filled in all required fields. Choose how you would like to proceed:") },
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
                                    Text("Save Account")
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
                                    Text("Save as Draft")
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
                                        Text("Discard", textAlign = TextAlign.Center)
                                    }
                                    
                                    TextButton(
                                        onClick = { showCloseConfirmation = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Keep Editing", textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    )
                } else {
                    AlertDialog(
                        onDismissRequest = { showCloseConfirmation = false },
                        title = { Text("Save Draft?") },
                        text = { Text("The form is not fully filled. Would you like to save it as a draft so you can finish it later?") },
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
                                    Text("Save as Draft")
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
                                        Text("Discard", textAlign = TextAlign.Center)
                                    }
                                    
                                    TextButton(
                                        onClick = { showCloseConfirmation = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Keep Editing", textAlign = TextAlign.Center)
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
