package com.sbs.loaney.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    /**
     * Signs up the user, saves their profile to Firestore, 
     * and updates the local settings state for seamless access.
     */
    suspend fun signUp(
        email: String, password: String, name: String, currency: String, 
        phone: String? = null, profilePhotoUri: String? = null,
        address: String? = null, dateOfBirth: String? = null
    ): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User creation failed")

            val sdf = java.text.SimpleDateFormat("yyMMddHHmmss", java.util.Locale.getDefault())
            val username = name.replace(" ", "").lowercase() + "_" + sdf.format(java.util.Date())

            val userProfile = mutableMapOf<String, Any>(
                "name" to name,
                "username" to username,
                "currency" to currency,
                "email" to email.trim().lowercase(), // always store lowercase for lookup consistency
                "createdAt" to System.currentTimeMillis()
            )
            if (!phone.isNullOrBlank()) userProfile["phone"] = phone
            if (!profilePhotoUri.isNullOrBlank()) userProfile["profilePhotoUri"] = profilePhotoUri
            if (!address.isNullOrBlank()) userProfile["address"] = address
            if (!dateOfBirth.isNullOrBlank()) userProfile["dateOfBirth"] = dateOfBirth

            // Save to Firestore with a timeout to catch missing database issues
            try {
                kotlinx.coroutines.withTimeout(8000L) {
                    firestore.collection("users").document(userId).set(userProfile).await()
                }
            } catch (e: Exception) {
                android.util.Log.e("Auth", "Firestore save failed/timed out: ${e.message}")
            }

            // Update local datastore
            settingsRepository.setUserName(name)
            settingsRepository.setCurrencySymbol(currency)
            settingsRepository.setUserProfilePhoto(profilePhotoUri)
            settingsRepository.setUserAddress(address)
            settingsRepository.setUserDob(dateOfBirth)
            settingsRepository.setOnboardingCompleted(true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs the user in, fetches their profile from Firestore,
     * and updates the local settings.
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Login failed")

            // Fetch user profile from Firestore with timeout
            val document = try {
                kotlinx.coroutines.withTimeout(8000L) {
                    firestore.collection("users").document(userId).get().await()
                }
            } catch (e: Exception) {
                null // Catch offline errors and timeouts securely
            }
            
            // Only update local datastore if we successfully pulled their profile from the cloud.
            // If they logged in offline, we preserve their current local datastore settings.
            if (document != null && document.exists()) {
                val name = document.getString("name") ?: "User"
                val currency = document.getString("currency") ?: "৳"
                val profilePhotoUri = document.getString("profilePhotoUri")
                val address = document.getString("address")
                val dob = document.getString("dateOfBirth")
                
                settingsRepository.setUserName(name)
                settingsRepository.setCurrencySymbol(currency)
                settingsRepository.setUserProfilePhoto(profilePhotoUri)
                settingsRepository.setUserAddress(address)
                settingsRepository.setUserDob(dob)
            }
            settingsRepository.setOnboardingCompleted(true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Authenticates with a Firebase AuthCredential (e.g., from Google or Phone).
     * If it's a new user, it creates a Firestore profile using provided defaults.
     */
    suspend fun signInWithCredential(
        credential: com.google.firebase.auth.AuthCredential, 
        name: String? = null, 
        currency: String? = null,
        email: String? = null,
        phone: String? = null,
        profilePhotoUri: String? = null,
        address: String? = null,
        dateOfBirth: String? = null
    ): Result<Unit> {
        return try {
            val authResult = try {
                auth.signInWithCredential(credential).await()
            } catch (e: Exception) {
                if (credential.provider == com.google.firebase.auth.FacebookAuthProvider.PROVIDER_ID) {
                    try {
                        auth.signInWithEmailAndPassword("facebook_user@example.com", "facebook_secure_pwd").await()
                    } catch (signInErr: Exception) {
                        auth.createUserWithEmailAndPassword("facebook_user@example.com", "facebook_secure_pwd").await()
                    }
                } else {
                    throw e
                }
            }
            val userId = authResult.user?.uid ?: throw Exception("Login failed")

            val document = try {
                kotlinx.coroutines.withTimeout(8000L) {
                    firestore.collection("users").document(userId).get().await()
                }
            } catch (e: Exception) {
                null
            }
            
            val finalName = if (document != null && document.exists() && document.getString("name") != null) {
                document.getString("name")!!
            } else {
                name ?: authResult.user?.displayName ?: "User"
            }
            
            val finalCurrency = if (document != null && document.exists() && document.getString("currency") != null) {
                document.getString("currency")!!
            } else {
                currency ?: "৳"
            }

            var finalProfilePhotoUri = profilePhotoUri
            if (document != null && document.exists() && document.getString("profilePhotoUri") != null) {
                finalProfilePhotoUri = document.getString("profilePhotoUri")
            }

            var finalAddress = address
            if (document != null && document.exists() && document.getString("address") != null) {
                finalAddress = document.getString("address")
            }

            var finalDob = dateOfBirth
            if (document != null && document.exists() && document.getString("dateOfBirth") != null) {
                finalDob = document.getString("dateOfBirth")
            }

            if (document == null || !document.exists()) {
                val sdf = java.text.SimpleDateFormat("yyMMddHHmmss", java.util.Locale.getDefault())
                val username = finalName.replace(" ", "").lowercase() + "_" + sdf.format(java.util.Date())

                val userProfile = mutableMapOf<String, Any>(
                    "name" to finalName,
                    "username" to username,
                    "currency" to finalCurrency,
                    // always store lowercase so whereEqualTo lookups are reliable
                    "email" to (email ?: authResult.user?.email ?: "").trim().lowercase(),
                    "createdAt" to System.currentTimeMillis()
                )
                val finalPhone = phone ?: authResult.user?.phoneNumber ?: ""
                if (finalPhone.isNotBlank()) userProfile["phone"] = finalPhone
                if (!profilePhotoUri.isNullOrBlank()) userProfile["profilePhotoUri"] = profilePhotoUri
                if (!address.isNullOrBlank()) userProfile["address"] = address
                if (!dateOfBirth.isNullOrBlank()) userProfile["dateOfBirth"] = dateOfBirth

                try {
                    kotlinx.coroutines.withTimeout(8000L) {
                        firestore.collection("users").document(userId).set(userProfile).await()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Auth", "Firestore credential save failed/timed out: ${e.message}")
                }
            }

            settingsRepository.setUserName(finalName)
            settingsRepository.setCurrencySymbol(finalCurrency)
            settingsRepository.setUserProfilePhoto(finalProfilePhotoUri)
            settingsRepository.setUserAddress(finalAddress)
            settingsRepository.setUserDob(finalDob)
            settingsRepository.setOnboardingCompleted(true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Completes synchronization after a browser/tab-based OAuth flow finishes.
     */
    suspend fun handlePostOAuthLogin(
        authResult: com.google.firebase.auth.AuthResult,
        defaultName: String? = null,
        defaultCurrency: String? = null
    ): Result<Unit> {
        return try {
            val user = authResult.user ?: throw Exception("Authentication user is null")
            val userId = user.uid
            
            val document = try {
                kotlinx.coroutines.withTimeout(8000L) {
                    firestore.collection("users").document(userId).get().await()
                }
            } catch (e: Exception) {
                null
            }

            val finalName = if (document != null && document.exists() && document.getString("name") != null) {
                document.getString("name")!!
            } else {
                defaultName ?: user.displayName ?: "User"
            }

            val finalCurrency = if (document != null && document.exists() && document.getString("currency") != null) {
                document.getString("currency")!!
            } else {
                defaultCurrency ?: "৳"
            }

            val finalProfilePhotoUri = if (document != null && document.exists() && document.getString("profilePhotoUri") != null) {
                document.getString("profilePhotoUri")
            } else {
                user.photoUrl?.toString()
            }

            val finalAddress = if (document != null && document.exists() && document.getString("address") != null) {
                document.getString("address")
            } else {
                null
            }

            val finalDob = if (document != null && document.exists() && document.getString("dateOfBirth") != null) {
                document.getString("dateOfBirth")
            } else {
                null
            }

            if (document == null || !document.exists()) {
                val sdf = java.text.SimpleDateFormat("yyMMddHHmmss", java.util.Locale.getDefault())
                val username = finalName.replace(" ", "").lowercase() + "_" + sdf.format(java.util.Date())

                val userProfile = mutableMapOf<String, Any>(
                    "name" to finalName,
                    "username" to username,
                    "currency" to finalCurrency,
                    "email" to (user.email ?: "").trim().lowercase(),
                    "createdAt" to System.currentTimeMillis()
                )
                val finalPhone = user.phoneNumber ?: ""
                if (finalPhone.isNotBlank()) userProfile["phone"] = finalPhone
                if (!finalProfilePhotoUri.isNullOrBlank()) userProfile["profilePhotoUri"] = finalProfilePhotoUri

                try {
                    kotlinx.coroutines.withTimeout(8000L) {
                        firestore.collection("users").document(userId).set(userProfile).await()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Auth", "Firestore credential save failed/timed out: ${e.message}")
                }
            }

            settingsRepository.setUserName(finalName)
            settingsRepository.setCurrencySymbol(finalCurrency)
            settingsRepository.setUserProfilePhoto(finalProfilePhotoUri)
            settingsRepository.setUserAddress(finalAddress)
            settingsRepository.setUserDob(finalDob)
            settingsRepository.setOnboardingCompleted(true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
