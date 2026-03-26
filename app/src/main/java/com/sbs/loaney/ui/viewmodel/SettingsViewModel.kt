package com.sbs.loaney.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbs.loaney.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: Int = 1, // 0: System, 1: Light, 2: Dark, 3: Colorful
    val accentColor: Int = 0, // Index into colorful accent presets
    val currencySymbol: String = "৳",
    val appLanguage: String = "en",
    val notificationsEnabled: Boolean = true,
    val userName: String = "Sajibur",
    val userProfilePhoto: String? = null,
    val userAddress: String? = null,
    val userDob: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeModeFlow,
        settingsRepository.accentColorFlow,
        settingsRepository.currencySymbolFlow,
        settingsRepository.appLanguageFlow,
        settingsRepository.notificationsEnabledFlow,
        settingsRepository.userNameFlow,
        settingsRepository.userProfilePhotoFlow,
        settingsRepository.userAddressFlow,
        settingsRepository.userDobFlow
    ) { values ->
        SettingsUiState(
            themeMode = values[0] as Int,
            accentColor = values[1] as Int,
            currencySymbol = values[2] as String,
            appLanguage = values[3] as String,
            notificationsEnabled = values[4] as Boolean,
            userName = values[5] as String,
            userProfilePhoto = values[6] as String?,
            userAddress = values[7] as String?,
            userDob = values[8] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setAccentColor(index: Int) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(index)
        }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            settingsRepository.setCurrencySymbol(symbol)
        }
    }

    fun setAppLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(languageCode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            settingsRepository.setUserName(name)
        }
    }

    fun setUserProfilePhoto(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setUserProfilePhoto(uri)
            
            // Push the update to Firebase Cloud Firestore to maintain sync across devices
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null && uri != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .update("profilePhotoUri", uri)
            }
        }
    }

    fun setUserAddress(address: String?) {
        viewModelScope.launch {
            settingsRepository.setUserAddress(address)
            
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null && address != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .update("address", address)
            }
        }
    }

    fun setUserDob(dob: String?) {
        viewModelScope.launch {
            settingsRepository.setUserDob(dob)
            
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null && dob != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .update("dateOfBirth", dob)
            }
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(completed)
        }
    }
}
