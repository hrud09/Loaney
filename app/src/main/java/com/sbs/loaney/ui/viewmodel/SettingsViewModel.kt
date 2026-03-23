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
    val userProfilePhoto: String? = null
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
        settingsRepository.userProfilePhotoFlow
    ) { values ->
        SettingsUiState(
            themeMode = values[0] as Int,
            accentColor = values[1] as Int,
            currencySymbol = values[2] as String,
            appLanguage = values[3] as String,
            notificationsEnabled = values[4] as Boolean,
            userName = values[5] as String,
            userProfilePhoto = values[6] as String?
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
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(completed)
        }
    }
}
