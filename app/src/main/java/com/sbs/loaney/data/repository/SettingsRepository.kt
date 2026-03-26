package com.sbs.loaney.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Use Context property delegate to create a single instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    // Define Keys
    private object PreferencesKeys {
        val THEME_MODE = intPreferencesKey("theme_mode") // 0: System, 1: Light, 2: Dark, 3: Colorful
        val ACCENT_COLOR = intPreferencesKey("accent_color") // 0-5, index into colorful accent presets
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PROFILE_PHOTO = stringPreferencesKey("user_profile_photo")
        val USER_ADDRESS = stringPreferencesKey("user_address")
        val USER_DOB = stringPreferencesKey("user_dob")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // --- Flows ---
    val themeModeFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: 1
        }

    val accentColorFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] ?: 0
        }

    val currencySymbolFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "৳" // Default Taka
        }

    val appLanguageFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] ?: "en"
        }

    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        }

    val userNameFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_NAME] ?: "Sajibur"
        }

    val userProfilePhotoFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_PROFILE_PHOTO]
        }

    val userAddressFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_ADDRESS]
        }

    val userDobFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_DOB]
        }

    val onboardingCompletedFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false // Default to false
        }

    // --- Updaters ---
    suspend fun setThemeMode(mode: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setAccentColor(index: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = index
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun setUserProfilePhoto(uri: String?) {
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(PreferencesKeys.USER_PROFILE_PHOTO)
            } else {
                preferences[PreferencesKeys.USER_PROFILE_PHOTO] = uri
            }
        }
    }

    suspend fun setUserAddress(address: String?) {
        dataStore.edit { preferences ->
            if (address.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.USER_ADDRESS)
            } else {
                preferences[PreferencesKeys.USER_ADDRESS] = address
            }
        }
    }

    suspend fun setUserDob(dob: String?) {
        dataStore.edit { preferences ->
            if (dob.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.USER_DOB)
            } else {
                preferences[PreferencesKeys.USER_DOB] = dob
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
}
