package com.surajpole.parkingfinder.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.surajpole.parkingfinder.data.model.UserProfile
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signUp(name: String, email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("User creation failed")
            
            // Save profile in Firestore
            val profile = UserProfile(
                id = user.uid,
                name = name.trim(),
                email = email.trim(),
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid).set(profile).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Login failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google sign in failed")
            
            // Check if user exists in Firestore, if not create record
            try {
                val doc = firestore.collection("users").document(user.uid).get().await()
                if (!doc.exists()) {
                    val profile = UserProfile(
                        id = user.uid,
                        name = user.displayName ?: "Google User",
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString() ?: "",
                        loginMethod = "google",
                        createdAt = System.currentTimeMillis()
                    )
                    firestore.collection("users").document(user.uid).set(profile).await()
                }
            } catch (_: Exception) {
                // If Firestore rules are locked, continue with authenticated user
            }
            Result.success(user)
        } catch (e: Exception) {
            val userFriendlyMsg = when {
                e.message?.contains("provider is disabled", ignoreCase = true) == true ->
                    "Google Provider is disabled in Firebase Console. Enable it in Authentication > Sign-in method."
                e.message?.contains("invalid", ignoreCase = true) == true ->
                    "Invalid Google ID token. Please verify SHA-1 in Firebase Console."
                else -> e.localizedMessage ?: "Google sign in error"
            }
            Result.failure(Exception(userFriendlyMsg, e))
        }
    }

    suspend fun signInAsGuest(): Result<FirebaseUser?> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                try {
                    val profile = UserProfile(
                        id = user.uid,
                        name = "Guest Driver",
                        email = "guest@apniparking.local",
                        loginMethod = "guest",
                        createdAt = System.currentTimeMillis()
                    )
                    firestore.collection("users").document(user.uid).set(profile).await()
                } catch (_: Exception) {}
            }
            Result.success(user)
        } catch (e: Exception) {
            // Even if Firebase anonymous auth is disabled on console, don't crash
            Result.success(auth.currentUser)
        }
    }

    suspend fun recordGoogleFallbackUser(uid: String, name: String, email: String, photoUrl: String): Result<Unit> {
        return try {
            val profile = UserProfile(
                id = uid,
                name = name,
                email = email,
                photoUrl = photoUrl,
                loginMethod = "google_fallback",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            Result.success(doc.toObject(UserProfile::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
