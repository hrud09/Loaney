package com.sbs.loaney.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cloudBackupRepository: CloudBackupRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    /**
     * Pushes any data the user tracked as a guest up to their brand-new cloud account. Best-effort
     * and time-boxed: a slow or failed upload must never turn a successful sign-up into an error.
     */
    private suspend fun backupGuestDataBestEffort() {
        try {
            val backedUpLoans = kotlinx.coroutines.withTimeout(15000L) {
                cloudBackupRepository.uploadLocalDataToCloud()
            }.getOrDefault(0)

            // A guest who already had loans has been through first-run onboarding; converting to an
            // account must not throw them back into the forced "track your first loan" guided flow.
            if (backedUpLoans > 0) {
                settingsRepository.setHasSeenTutorial(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("Auth", "Guest data cloud backup failed/timed out: ${e.message}")
        }
    }

    suspend fun continueAsGuest(name: String, currency: String): Result<Unit> {
        return try {
            settingsRepository.setUserName(name.trim())
            settingsRepository.setCurrencySymbol(currency)
            settingsRepository.setUserProfilePhoto(null)
            settingsRepository.setUserAddress(null)
            settingsRepository.setUserDob(null)
            settingsRepository.setOnboardingCompleted(true)
            settingsRepository.setHasSeenTutorial(false) // Show tutorial for guests
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
            settingsRepository.setHasSeenTutorial(false) // First time sign up gets tutorial

            // Guest → account: back up everything they tracked offline to their new cloud account.
            backupGuestDataBestEffort()

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
                
                // Existing user: already has account, so skip tutorial
                settingsRepository.setHasSeenTutorial(true)
            } else {
                settingsRepository.setHasSeenTutorial(false)
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
            val authResult = auth.signInWithCredential(credential).await()
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
 
            var finalProfilePhotoUri = profilePhotoUri ?: authResult.user?.photoUrl?.toString()
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
                // New user: gets tutorial
                settingsRepository.setHasSeenTutorial(false)

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
                if (!finalProfilePhotoUri.isNullOrBlank()) userProfile["profilePhotoUri"] = finalProfilePhotoUri
                if (!address.isNullOrBlank()) userProfile["address"] = address
                if (!dateOfBirth.isNullOrBlank()) userProfile["dateOfBirth"] = dateOfBirth

                try {
                    kotlinx.coroutines.withTimeout(8000L) {
                        firestore.collection("users").document(userId).set(userProfile).await()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Auth", "Firestore credential save failed/timed out: ${e.message}")
                }

                // Guest → account: mirror any offline-tracked data to the new cloud account.
                backupGuestDataBestEffort()
            } else {
                // Existing user: skips tutorial
                settingsRepository.setHasSeenTutorial(true)
            }
 
            settingsRepository.setUserName(finalName)
            settingsRepository.setCurrencySymbol(finalCurrency)
            settingsRepository.setUserProfilePhoto(finalProfilePhotoUri)
            settingsRepository.setUserAddress(finalAddress)
            settingsRepository.setUserDob(finalDob)
            settingsRepository.setOnboardingCompleted(true)
 
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(friendly(e))
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
                // New user: gets tutorial
                settingsRepository.setHasSeenTutorial(false)

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

                // Guest → account: mirror any offline-tracked data to the new cloud account.
                backupGuestDataBestEffort()
            } else {
                // Existing user: skips tutorial
                settingsRepository.setHasSeenTutorial(true)
            }

            settingsRepository.setUserName(finalName)
            settingsRepository.setCurrencySymbol(finalCurrency)
            settingsRepository.setUserProfilePhoto(finalProfilePhotoUri)
            settingsRepository.setUserAddress(finalAddress)
            settingsRepository.setUserDob(finalDob)
            settingsRepository.setOnboardingCompleted(true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }
    }

    /**
     * Firebase's raw provider exceptions read like stack traces. Rewrite the ones a user can
     * actually act on; pass everything else through untouched.
     */
    private fun friendly(e: Throwable): Throwable = when (e) {
        // Firebase defaults to one account per email address. Signing in with Google or Facebook
        // using an address that already registered via email/password lands here.
        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> Exception(
            "An account already exists with this email address. Sign in using the method you " +
                "originally registered with."
        )
        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> Exception(
            "That sign-in attempt was rejected. Please try again."
        )
        is com.google.firebase.FirebaseNetworkException -> Exception(
            "No internet connection. Check your network and try again."
        )
        else -> e
    }
}
