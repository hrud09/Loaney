package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val authRepository: AuthRepository
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
                _authState.value = AuthState.Success
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Log in failed")
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
                _authState.value = AuthState.Success
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Sign in failed")
            }
        }
    }

    /**
     * Initiates Facebook login via Firebase's browser redirection flow.
     */
    fun signInWithFacebook(
        activity: android.app.Activity,
        defaultName: String? = null,
        defaultCurrency: String? = null
    ) {
        _authState.value = AuthState.Loading
        val provider = com.google.firebase.auth.OAuthProvider.newBuilder("facebook.com")
        provider.setScopes(listOf("email", "public_profile"))

        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.startActivityForSignInWithProvider(activity, provider.build())
            .addOnSuccessListener { authResult ->
                viewModelScope.launch {
                    val result = authRepository.handlePostOAuthLogin(authResult, defaultName, defaultCurrency)
                    result.onSuccess {
                        _authState.value = AuthState.Success
                    }.onFailure { error ->
                        _authState.value = AuthState.Error(error.message ?: "Facebook login failed")
                    }
                }
            }
            .addOnFailureListener { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Facebook login failed")
            }
    }

    /**
     * Checks for a pending Facebook auth result (e.g. if the activity was recreated during redirect).
     */
    fun checkPendingFacebookAuth(
        defaultName: String? = null,
        defaultCurrency: String? = null
    ) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            _authState.value = AuthState.Loading
            pendingResultTask.addOnSuccessListener { authResult ->
                viewModelScope.launch {
                    val result = authRepository.handlePostOAuthLogin(authResult, defaultName, defaultCurrency)
                    result.onSuccess {
                        _authState.value = AuthState.Success
                    }.onFailure { error ->
                        _authState.value = AuthState.Error(error.message ?: "Facebook login failed")
                    }
                }
            }.addOnFailureListener { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Facebook login failed")
            }
        }
    }
}
