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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.sbs.loaney.R
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.AuthState
import com.sbs.loaney.ui.viewmodel.AuthViewModel
import java.util.concurrent.TimeUnit

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
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("৳") }
    var passwordVisible by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }

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
                android.widget.Toast.makeText(context, "Sign up completed!", android.widget.Toast.LENGTH_SHORT).show()
            }
            onAuthSuccess()
        }
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
                localError = e.message ?: "Phone verification failed"
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
                    isGuestMode -> "Hop into Loaney"
                    isSignUp -> "Create Account"
                    else -> "Welcome Back"
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isGuestMode -> "Use the app with just your name. Your data stays secure on this device."
                    isSignUp -> "Create an account when you want cloud backup."
                    else -> "Sign in to back up or restore your cloud data."
                },
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Image Picker (Moved to top of form)
            AnimatedVisibility(visible = !isGuestMode && isSignUp && !isOtpMode) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Profile Picture",
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
                        label = { Text("Your Name") },
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
                        text = "Local Currency",
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
                        Text("Log In", color = if (!isSignUp) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                        Text("Sign Up", color = if (isSignUp) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                Text("Enter the 6-digit code sent to $phoneNumber", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it; localError = null },
                    label = { Text("SMS Code") },
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
                    label = { Text("Phone Number (with country code)") },
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
                    label = { Text("Email") },
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
                    label = { Text("Password") },
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
                        label = { Text("Full Name") },
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
                            label = { Text("Phone Number (Optional)") },
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
                            label = { Text("Email (Optional)") },
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
                        label = { Text("Address (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = AlimGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it; localError = null },
                        label = { Text("Date of Birth (Optional)") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = AlimGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AlimGreen, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Local Currency",
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
                            localError = "Enter your name to continue"
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
                            localError = "Invalid SMS code"
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
                            localError = "Activity context required for Phone Auth"
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
                        isGuestMode -> "Continue as Guest"
                        isOtpMode -> "Verify & Continue"
                        isPhoneMode -> "Send SMS Code"
                        isSignUp -> "Create Account"
                        else -> "Log In"
                    }
                    Text(btnText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Alternative Providers
            if (!isOtpMode) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Text(" or ", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (isGuestMode) {
                    OutlinedButton(
                        onClick = {
                            isGuestMode = false
                            localError = null
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Text("Sign in for cloud backup", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { 
                            isPhoneMode = !isPhoneMode
                            localError = null
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Text(if (isPhoneMode) "Continue with Email" else "Continue with Phone", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            isGuestMode = true
                            isPhoneMode = false
                            isSignUp = false
                            localError = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use without signing in", color = AlimGreen, fontWeight = FontWeight.Bold)
                    }
                }
                
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = { isOtpMode = false }) {
                    Text("Back to Sign In", color = AlimGreen)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
