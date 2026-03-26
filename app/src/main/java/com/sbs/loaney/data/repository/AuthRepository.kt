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
    suspend fun signUp(email: String, password: String, name: String, currency: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User creation failed")

            val userProfile = mapOf(
                "name" to name,
                "currency" to currency,
                "email" to email,
                "createdAt" to System.currentTimeMillis()
            )

            // Save to Firestore with a timeout to catch missing database issues
            try {
                kotlinx.coroutines.withTimeout(8000L) {
                    firestore.collection("users").document(userId).set(userProfile).await()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Ignore the timeout so the user isn't stuck un-loggable. The user was created successfully in Auth.
                // We will proceed to log them in, but they should check their Firestore configuration.
                android.util.Log.e("Auth", "Firestore save timed out. Please ensure Cloud Firestore is enabled in Firebase Console.")
            }

            // Update local datastore
            settingsRepository.setUserName(name)
            settingsRepository.setCurrencySymbol(currency)
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
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                null
            }
            
            val name = document?.getString("name") ?: "User"
            val currency = document?.getString("currency") ?: "৳"

            // Update local datastore
            settingsRepository.setUserName(name)
            settingsRepository.setCurrencySymbol(currency)
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
        currency: String? = null
    ): Result<Unit> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val userId = authResult.user?.uid ?: throw Exception("Login failed")

            val document = try {
                kotlinx.coroutines.withTimeout(8000L) {
                    firestore.collection("users").document(userId).get().await()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
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

            if (document == null || !document.exists()) {
                val userProfile = mapOf(
                    "name" to finalName,
                    "currency" to finalCurrency,
                    "email" to (authResult.user?.email ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )
                try {
                    kotlinx.coroutines.withTimeout(8000L) {
                        firestore.collection("users").document(userId).set(userProfile).await()
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.e("Auth", "Firestore credential save timed out.")
                }
            }

            settingsRepository.setUserName(finalName)
            settingsRepository.setCurrencySymbol(finalCurrency)
            settingsRepository.setOnboardingCompleted(true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
