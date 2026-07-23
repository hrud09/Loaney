package com.sbs.loaney.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import coil.compose.AsyncImage
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.annotation.DrawableRes
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.sbs.loaney.R
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.AuthState
import com.sbs.loaney.ui.viewmodel.AuthViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var isGuestMode by remember { mutableStateOf(true) }
    var isSignUp by remember { mutableStateOf(false) }
    var isPhoneMode by remember { mutableStateOf(false) }
    var isOtpMode by remember { mutableStateOf(false) }
    
    // Form States
    var profilePhotoUri by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("Guest_" + (System.currentTimeMillis() / 1000).toString()) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("৳") }
    var passwordVisible by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    var verificationId by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var isVerifyingPhone by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val activity = context as? Activity

    val currencies = listOf(
        "৳" to "BDT", "$" to "USD", "€" to "EUR", "£" to "GBP", "₹" to "INR"
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            authViewModel.resetState()
            if (isSignUp) {
                android.widget.Toast.makeText(context, context.getString(R.string.auth_sign_up_completed), android.widget.Toast.LENGTH_SHORT).show()
            }
            onAuthSuccess()
        }
    }

    // A Facebook sign-in can outlive this Activity — the Custom Tab may hand control back to a
    // freshly recreated process. Pick the result back up instead of stranding the user.
    LaunchedEffect(Unit) {
        authViewModel.checkPendingFacebookAuth()
    }


    // --- Image Picker Setup ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profilePhotoUri = uri?.toString()
    }

    // --- Phone Auth Callbacks ---
    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                isVerifyingPhone = false
                authViewModel.signInWithCredential(credential, name.takeIf { isSignUp }, selectedCurrency)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                isVerifyingPhone = false
                localError = e.message ?: context.getString(R.string.auth_phone_verification_failed)
            }
            override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                isVerifyingPhone = false
                verificationId = verId
                isOtpMode = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = when {
                    isGuestMode -> stringResource(R.string.auth_hop_into_loaney)
                    isSignUp -> stringResource(R.string.auth_create_account)
                    else -> stringResource(R.string.auth_welcome_back)
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isGuestMode -> stringResource(R.string.auth_guest_subtitle)
                    isSignUp -> stringResource(R.string.auth_signup_subtitle)
                    else -> stringResource(R.string.auth_signin_subtitle)
                },
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Image Picker (Moved to top of form)
            AnimatedVisibility(visible = !isGuestMode && isSignUp && !isOtpMode) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.auth_profile_picture),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = profilePhotoUri ?: R.drawable.default_profile_pic,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (profilePhotoUri == null) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Add Photo",
                                tint = AlimGreen,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(24.dp).background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape).padding(4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            AnimatedVisibility(visible = isGuestMode) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; localError = null },
                        label = { Text(stringResource(R.string.auth_your_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AlimGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AlimGreen,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.auth_local_currency),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        currencies.forEach { (symbol, code) ->
                            val isSelected = selectedCurrency == symbol
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) AlimGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCurrency = symbol }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) AlimGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(symbol, style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold, color = if (isSelected) AlimGreen else MaterialTheme.colorScheme.onSurface
                                    ))
                                    Text(code, style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) AlimGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 9.sp
                                    ))
                                }
                            }
                        }
                    }
                }
            }

            // Toggle Bar (Sign In / Sign Up)
            if (!isOtpMode) {
                AnimatedVisibility(visible = !isGuestMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isSignUp) AlimGreen else Color.Transparent)
                            .clickable { isSignUp = false; isPhoneMode = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.auth_log_in), color = if (!isSignUp) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSignUp) AlimGreen else Color.Transparent)
                            .clickable { isSignUp = true; isPhoneMode = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.auth_sign_up), color = if (isSignUp) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Error Message
            val errorMessage = localError ?: (authState as? AuthState.Error)?.message
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Dynamic User Form
            if (!isGuestMode && isOtpMode) {
                Text(stringResource(R.string.auth_otp_sent, phoneNumber), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it; localError = null },
                    label = { Text(stringResource(R.string.auth_sms_code)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else if (!isGuestMode && isPhoneMode) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it; localError = null },
                    label = { Text(stringResource(R.string.auth_phone_with_country_code)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AlimGreen) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else if (!isGuestMode) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; localError = null },
                    label = { Text(stringResource(R.string.auth_email)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AlimGreen) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; localError = null },
                    label = { Text(stringResource(R.string.auth_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AlimGreen) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = null, tint = AlimGreen)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Profile Setup snippet for Sign Up
            AnimatedVisibility(visible = !isGuestMode && isSignUp && !isOtpMode) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; localError = null },
                        label = { Text(stringResource(R.string.auth_full_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AlimGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    if (!isPhoneMode) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it; localError = null },
                            label = { Text(stringResource(R.string.auth_phone_number_optional)) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AlimGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; localError = null },
                            label = { Text(stringResource(R.string.email_optional)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AlimGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it; localError = null },
                        label = { Text(stringResource(R.string.auth_address_optional)) },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = AlimGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.auth_birth_year_optional)) },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = AlimGreen) },
                            singleLine = true,
                            readOnly = true,
                            enabled = false,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLeadingIconColor = AlimGreen,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState()
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                                        val birthYear = calendar.get(java.util.Calendar.YEAR)
                                        dateOfBirth = birthYear.toString()
                                        localError = null
                                    }
                                    showDatePicker = false
                                }) {
                                    Text(stringResource(R.string.ok), color = AlimGreen, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        ) {
                            DatePicker(
                                state = datePickerState,
                                colors = DatePickerDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                                    headlineContentColor = MaterialTheme.colorScheme.onBackground,
                                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    navigationContentColor = MaterialTheme.colorScheme.onBackground,
                                    yearContentColor = MaterialTheme.colorScheme.onBackground,
                                    currentYearContentColor = AlimGreen,
                                    selectedYearContentColor = Color.White,
                                    selectedYearContainerColor = AlimGreen,
                                    dayContentColor = MaterialTheme.colorScheme.onBackground,
                                    selectedDayContentColor = Color.White,
                                    selectedDayContainerColor = AlimGreen,
                                    todayContentColor = AlimGreen,
                                    todayDateBorderColor = AlimGreen
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.auth_local_currency),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        currencies.forEach { (symbol, code) ->
                            val isSelected = selectedCurrency == symbol
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) AlimGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCurrency = symbol }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) AlimGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(symbol, style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold, color = if (isSelected) AlimGreen else MaterialTheme.colorScheme.onSurface
                                    ))
                                    Text(code, style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) AlimGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 9.sp
                                    ))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Action Button
            val isMainLoading = authState == AuthState.Loading || isVerifyingPhone
            Button(
                onClick = {
                    keyboardController?.hide()
                    localError = null
                    if (isGuestMode) {
                        if (name.isBlank()) {
                            localError = context.getString(R.string.auth_enter_name_to_continue)
                        } else {
                            authViewModel.continueAsGuest(name, selectedCurrency)
                        }
                    } else if (isOtpMode) {
                        try {
                            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                            authViewModel.signInWithCredential(
                                credential = credential,
                                name = name.takeIf { isSignUp },
                                currency = selectedCurrency,
                                email = email.takeIf { isSignUp },
                                phone = phoneNumber.takeIf { isSignUp },
                                profilePhotoUri = profilePhotoUri.takeIf { isSignUp },
                                address = address.takeIf { isSignUp },
                                dateOfBirth = dateOfBirth.takeIf { isSignUp }
                            )
                        } catch (e: Exception) {
                            localError = context.getString(R.string.auth_invalid_sms_code)
                        }
                    } else if (isPhoneMode) {
                        if (activity != null) {
                            isVerifyingPhone = true
                            val builder = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                                .setPhoneNumber(phoneNumber)
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(activity)
                                .setCallbacks(callbacks)
                            PhoneAuthProvider.verifyPhoneNumber(builder.build())
                        } else {
                            localError = context.getString(R.string.auth_activity_required_phone)
                        }
                    } else {
                        if (isSignUp) {
                            authViewModel.signUp(email, password, name, selectedCurrency, phoneNumber, profilePhotoUri, address, dateOfBirth)
                        } else {
                            authViewModel.signIn(email, password)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isMainLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlimGreen, contentColor = AlimWhite, disabledContainerColor = AlimGreen.copy(alpha = 0.5f)
                )
            ) {
                if (isMainLoading) {
                    CircularProgressIndicator(color = AlimWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    val btnText = when {
                        isGuestMode -> stringResource(R.string.auth_continue_as_guest)
                        isOtpMode -> stringResource(R.string.auth_verify_continue)
                        isPhoneMode -> stringResource(R.string.auth_send_sms_code)
                        isSignUp -> stringResource(R.string.auth_create_account)
                        else -> stringResource(R.string.auth_log_in)
                    }
                    Text(btnText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Alternative Providers
            if (!isOtpMode) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Text(stringResource(R.string.auth_or), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (isGuestMode) {
                    OutlinedButton(
                        onClick = {
                            isGuestMode = false
                            localError = null
                            if (name.startsWith("Guest_")) {
                                name = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Text(stringResource(R.string.auth_sign_in_cloud_backup), fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Google and Facebook each cover sign-up and sign-in in one tap, so they stay
                    // put whichever side of the Log In / Sign Up toggle we are on. The typed name
                    // and currency only seed a profile that does not exist yet.
                    SocialSignInButton(
                        text = stringResource(R.string.auth_continue_with_google),
                        iconRes = R.drawable.ic_google_logo,
                        enabled = !isMainLoading,
                        onClick = {
                            keyboardController?.hide()
                            localError = null
                            if (activity == null) {
                                localError = context.getString(R.string.auth_google_signin_failed)
                            } else {
                                authViewModel.signInWithGoogle(
                                    activity = activity,
                                    name = name.takeIf { it.isNotBlank() },
                                    currency = selectedCurrency
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SocialSignInButton(
                        text = stringResource(R.string.auth_continue_with_facebook),
                        iconRes = R.drawable.ic_facebook_logo,
                        enabled = !isMainLoading,
                        onClick = {
                            keyboardController?.hide()
                            localError = null
                            if (activity == null) {
                                localError = context.getString(R.string.auth_facebook_signin_failed)
                            } else {
                                authViewModel.signInWithFacebook(
                                    activity = activity,
                                    name = name.takeIf { it.isNotBlank() },
                                    currency = selectedCurrency
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            isPhoneMode = !isPhoneMode
                            localError = null
                        },
                        enabled = !isMainLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Text(if (isPhoneMode) stringResource(R.string.auth_continue_with_email) else stringResource(R.string.auth_continue_with_phone), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            isGuestMode = true
                            isPhoneMode = false
                            isSignUp = false
                            localError = null
                            if (name.isBlank() || name.startsWith("Guest_")) {
                                name = "Guest_" + (System.currentTimeMillis() / 1000).toString()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.auth_use_without_signing_in), color = AlimGreen, fontWeight = FontWeight.Bold)
                    }
                }
                
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { isOtpMode = false }) {
                    Text(stringResource(R.string.auth_back_to_sign_in), color = AlimGreen)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SocialSignInButton(
    text: String,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            // Both logos carry their own brand colours; tinting would flatten them to one shade.
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}
