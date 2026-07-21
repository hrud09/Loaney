package com.sbs.loaney.ui.viewmodel

import com.sbs.loaney.util.AnalyticsHelper

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.GoogleAuthProvider
import com.sbs.loaney.data.auth.GoogleAuthClient
import com.sbs.loaney.data.auth.GoogleSignInCancelled
import com.sbs.loaney.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Checks if user is already logged in natively
    fun isUserLoggedIn(): Boolean {
        return authRepository.currentUser != null
    }

    fun signUp(email: String, password: String, name: String, currency: String, phone: String? = null, profilePhotoUri: String? = null, address: String? = null, dateOfBirth: String? = null) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(email, password, name, currency, phone, profilePhotoUri, address, dateOfBirth)
            result.onSuccess {
                analyticsHelper.logEvent("sign_up_complete")
                _authState.value = AuthState.Success
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            result.onSuccess {
                analyticsHelper.logEvent("login_complete")
                _authState.value = AuthState.Success
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Log in failed")
            }
        }
    }

    fun continueAsGuest(name: String, currency: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.continueAsGuest(name, currency)
            result.onSuccess {
                analyticsHelper.logEvent("guest_mode_start")
                _authState.value = AuthState.Success
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Guest mode failed")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun signInWithCredential(
        credential: com.google.firebase.auth.AuthCredential, 
        name: String? = null, 
        currency: String? = null,
        email: String? = null,
        phone: String? = null,
        profilePhotoUri: String? = null,
        address: String? = null,
        dateOfBirth: String? = null
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithCredential(credential, name, currency, email, phone, profilePhotoUri, address, dateOfBirth)
            result.onSuccess {
                analyticsHelper.logEvent("login_complete")
                _authState.value = AuthState.Success
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Sign in failed")
            }
        }
    }

    /**
     * Signs in with Google. Covers sign-up and sign-in in one call — Firebase creates the account
     * if this Google identity is new, and signs into the existing one otherwise.
     *
     * [name] and [currency] only seed the profile of a brand new user; an existing user's stored
     * profile always wins.
     */
    fun signInWithGoogle(
        activity: Activity,
        name: String? = null,
        currency: String? = null
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            googleAuthClient.getIdToken(activity)
                .onSuccess { idToken ->
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    authRepository.signInWithCredential(credential, name, currency)
                        .onSuccess {
                            analyticsHelper.logEvent("login_complete")
                            _authState.value = AuthState.Success
                        }
                        .onFailure { error ->
                            _authState.value = AuthState.Error(error.message ?: "Google sign-in failed")
                        }
                }
                .onFailure { error ->
                    // Dismissing the account picker is a deliberate choice, not an error to report.
                    _authState.value = if (error is GoogleSignInCancelled) {
                        AuthState.Idle
                    } else {
                        AuthState.Error(error.message ?: "Google sign-in failed")
                    }
                }
        }
    }

    /**
     * Signs in with Facebook through Firebase's OAuth flow, which hands off to a Custom Tab.
     * As with Google, one call covers both sign-up and sign-in.
     */
    fun signInWithFacebook(
        activity: Activity,
        name: String? = null,
        currency: String? = null
    ) {
        _authState.value = AuthState.Loading

        val provider = com.google.firebase.auth.OAuthProvider.newBuilder("facebook.com")
            .setScopes(listOf("email", "public_profile"))
            .build()

        com.google.firebase.auth.FirebaseAuth.getInstance()
            .startActivityForSignInWithProvider(activity, provider)
            .addOnSuccessListener { authResult -> completeOAuth(authResult, name, currency) }
            .addOnFailureListener { e -> reportOAuthFailure(e) }
    }

    /**
     * Resumes a Facebook sign-in that was still in flight when the activity was destroyed —
     * the Custom Tab hands control back to a fresh process. Safe to call on every screen entry.
     */
    fun checkPendingFacebookAuth(
        name: String? = null,
        currency: String? = null
    ) {
        val pending = com.google.firebase.auth.FirebaseAuth.getInstance().pendingAuthResult ?: return

        _authState.value = AuthState.Loading
        pending
            .addOnSuccessListener { authResult -> completeOAuth(authResult, name, currency) }
            .addOnFailureListener { e -> reportOAuthFailure(e) }
    }

    private fun completeOAuth(
        authResult: com.google.firebase.auth.AuthResult,
        name: String?,
        currency: String?
    ) {
        viewModelScope.launch {
            authRepository.handlePostOAuthLogin(authResult, name, currency)
                .onSuccess {
                    analyticsHelper.logEvent("login_complete")
                    _authState.value = AuthState.Success
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Facebook login failed")
                }
        }
    }

    private fun reportOAuthFailure(e: Exception) {
        // Closing the Custom Tab surfaces as a generic web exception rather than a cancel type,
        // so match on the message. A user backing out should not be shown a red error.
        val cancelled = e.message?.contains("cancel", ignoreCase = true) == true
        _authState.value = if (cancelled) {
            AuthState.Idle
        } else {
            AuthState.Error(e.message ?: "Facebook login failed")
        }
    }
}
