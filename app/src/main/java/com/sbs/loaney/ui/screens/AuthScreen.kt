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
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
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
    startInSignUp: Boolean = false,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Entered from "Sign in to back up to cloud": open the create-account form directly so the
    // guest can sign up and have their tracked data mirrored to the new account.
    var isGuestMode by remember { mutableStateOf(!startInSignUp) }
    var isSignUp by remember { mutableStateOf(startInSignUp) }
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
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    // The number actually sent to Firebase, in E.164 (+8801XXXXXXXXX) form.
    var normalizedPhone by remember { mutableStateOf<String?>(null) }
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

    // --- Facebook Login (SDK) ---
    // Firebase only accepts Facebook on Android via the Facebook SDK's access token.
    val facebookConfigured = remember { context.getString(R.string.facebook_app_id).isNotBlank() }
    val facebookCallbackManager = remember { CallbackManager.Factory.create() }
    DisposableEffect(facebookCallbackManager) {
        onDispose {
            try {
                if (FacebookSdk.isInitialized()) {
                    LoginManager.getInstance().unregisterCallback(facebookCallbackManager)
                }
            } catch (_: Exception) { }
        }
    }


    // --- Image Picker Setup ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profilePhotoUri = uri?.toString()
    }

    // --- Phone Auth ---
    // Signs in with a verified phone credential, carrying the sign-up form along so a new account
    // gets the same profile whether the code was typed or auto-read from the SMS.
    fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        authViewModel.signInWithCredential(
            credential = credential,
            name = name.takeIf { isSignUp && it.isNotBlank() },
            currency = selectedCurrency,
            email = email.trim().takeIf { isSignUp && it.isNotBlank() },
            phone = normalizedPhone,
            profilePhotoUri = profilePhotoUri.takeIf { isSignUp },
            address = address.takeIf { isSignUp && it.isNotBlank() },
            dateOfBirth = dateOfBirth.takeIf { isSignUp && it.isNotBlank() }
        )
    }

    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Instant verification or SMS auto-retrieval: no code to type.
                isVerifyingPhone = false
                credential.smsCode?.let { otpCode = it }
                signInWithPhoneCredential(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                isVerifyingPhone = false
                android.util.Log.e("AuthScreen", "Phone verification failed", e)
                localError = context.getString(phoneVerificationErrorMessage(e))
            }
            override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                isVerifyingPhone = false
                verificationId = verId
                resendToken = token
                otpCode = ""
                isOtpMode = true
            }
        }
    }

    fun sendVerificationCode(resend: Boolean) {
        val host = activity
        if (host == null) {
            localError = context.getString(R.string.auth_activity_required_phone)
            return
        }
        val e164 = normalizePhoneNumber(phoneNumber, selectedCurrency)
        if (e164 == null) {
            localError = context.getString(R.string.auth_phone_invalid_number)
            return
        }
        normalizedPhone = e164
        isVerifyingPhone = true
        val builder = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(e164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(host)
            .setCallbacks(callbacks)
        val token = resendToken
        if (resend && token != null) builder.setForceResendingToken(token)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
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
                Text(stringResource(R.string.auth_otp_sent, normalizedPhone ?: phoneNumber), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { input -> otpCode = input.filter { it.isDigit() }.take(6); localError = null },
                    label = { Text(stringResource(R.string.auth_sms_code)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
                    placeholder = { Text("+8801XXXXXXXXX") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AlimGreen) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                        if (otpCode.length != 6 || verificationId.isBlank()) {
                            localError = context.getString(R.string.auth_invalid_sms_code)
                        } else {
                            try {
                                signInWithPhoneCredential(PhoneAuthProvider.getCredential(verificationId, otpCode))
                            } catch (e: Exception) {
                                localError = context.getString(R.string.auth_invalid_sms_code)
                            }
                        }
                    } else if (isPhoneMode) {
                        if (isSignUp && name.isBlank()) {
                            localError = context.getString(R.string.auth_enter_name_to_continue)
                        } else {
                            sendVerificationCode(resend = false)
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
                                    name = name.takeIf { isSignUp && it.isNotBlank() },
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
                            val registryOwner = activity as? ActivityResultRegistryOwner
                            if (!facebookConfigured || !FacebookSdk.isInitialized()) {
                                localError = context.getString(R.string.auth_facebook_not_configured)
                            } else if (registryOwner == null) {
                                localError = context.getString(R.string.auth_facebook_signin_failed)
                            } else {
                                val loginManager = LoginManager.getInstance()
                                // Drop any previous Facebook session so the user can pick an account.
                                loginManager.logOut()
                                loginManager.registerCallback(
                                    facebookCallbackManager,
                                    object : FacebookCallback<LoginResult> {
                                        override fun onSuccess(result: LoginResult) {
                                            authViewModel.signInWithFacebook(
                                                accessToken = result.accessToken.token,
                                                name = name.takeIf { isSignUp && it.isNotBlank() },
                                                currency = selectedCurrency
                                            )
                                        }

                                        override fun onCancel() {
                                            authViewModel.resetState()
                                        }

                                        override fun onError(error: FacebookException) {
                                            android.util.Log.e("AuthScreen", "Facebook login failed", error)
                                            authViewModel.reportError(
                                                context.getString(R.string.auth_facebook_signin_failed)
                                            )
                                        }
                                    }
                                )
                                loginManager.logIn(
                                    registryOwner,
                                    facebookCallbackManager,
                                    listOf("email", "public_profile")
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
                TextButton(
                    onClick = { localError = null; sendVerificationCode(resend = true) },
                    enabled = !isMainLoading
                ) {
                    Text(stringResource(R.string.auth_resend_code), color = AlimGreen)
                }
                TextButton(onClick = { isOtpMode = false; otpCode = ""; localError = null }) {
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

/**
 * Turns what the user typed into the E.164 form Firebase requires (e.g. +8801712345678).
 * Local numbers without a country code ("01712345678") get one inferred from the chosen
 * currency, defaulting to Bangladesh. Returns null if it can't be a valid phone number.
 */
internal fun normalizePhoneNumber(raw: String, currencySymbol: String): String? {
    val trimmed = raw.trim()
    val digits = trimmed.filter { it.isDigit() }
    if (digits.isEmpty()) return null

    val withCountry = when {
        trimmed.startsWith("+") -> digits
        digits.startsWith("00") -> digits.drop(2)
        else -> {
            val countryCode = when (currencySymbol) {
                "₹" -> "91"
                "$" -> "1"
                "£" -> "44"
                else -> "880"
            }
            when {
                // Already starts with the country code, just without the "+".
                digits.startsWith(countryCode) && digits.length > 10 -> digits
                digits.startsWith("0") -> countryCode + digits.drop(1)
                else -> countryCode + digits
            }
        }
    }
    // E.164 allows at most 15 digits; anything under 8 can't be a real mobile number.
    return if (withCountry.length in 8..15) "+$withCountry" else null
}

/** Maps a phone verification failure to a message the user can act on. */
private fun phoneVerificationErrorMessage(e: FirebaseException): Int {
    val raw = e.message.orEmpty()
    return when {
        e is FirebaseTooManyRequestsException -> R.string.auth_phone_too_many_requests
        e is FirebaseAuthInvalidCredentialsException -> R.string.auth_phone_invalid_number
        // Phone Auth is switched off, or the Firebase project has no billing account for SMS.
        raw.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
            raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            raw.contains("not enabled", ignoreCase = true) -> R.string.auth_phone_not_enabled
        // App verification (Play Integrity / reCAPTCHA) failed: SHA-1/SHA-256 not registered.
        raw.contains("not authorized", ignoreCase = true) ||
            raw.contains("app verification", ignoreCase = true) ||
            raw.contains("Play Integrity", ignoreCase = true) ||
            raw.contains("recaptcha", ignoreCase = true) -> R.string.auth_phone_app_not_verified
        raw.contains("network", ignoreCase = true) -> R.string.auth_error_network
        else -> R.string.auth_phone_verification_failed
    }
}
