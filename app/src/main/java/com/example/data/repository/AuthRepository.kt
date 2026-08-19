package com.example.data.repository

import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import com.google.firebase.firestore.ListenerRegistration

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUser(): Flow<User?> = callbackFlow {
        var userListener: ListenerRegistration? = null
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                userListener?.remove()
                userListener = null
                trySend(null)
            } else {
                userListener?.remove()
                userListener = db.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.w("AuthRepository", "User listener note: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val user = snapshot.toObject(User::class.java)
                            trySend(user)
                        } else {
                            val user = User(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                name = firebaseUser.displayName ?: "",
                                photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                                balance = 0,
                                streak = 0,
                                referralCode = firebaseUser.uid.take(6).uppercase()
                            )
                            trySend(user)
                        }
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
            userListener?.remove()
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign in failed")

            val userRef = db.collection("users").document(firebaseUser.uid)
            val snapshot = userRef.get().await()

            val user = if (!snapshot.exists()) {
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    balance = 0,
                    streak = 0,
                    joinedAt = System.currentTimeMillis(),
                    referralCode = firebaseUser.uid.take(6).uppercase()
                )
                userRef.set(newUser).await()
                newUser
            } else {
                snapshot.toObject(User::class.java) ?: throw Exception("Failed to parse user")
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<User> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val firebaseUser = authResult.user ?: throw Exception("Anonymous Sign in failed")

            val userRef = db.collection("users").document(firebaseUser.uid)
            val snapshot = userRef.get().await()

            val user = if (!snapshot.exists()) {
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = "guest@example.com",
                    name = "Guest User",
                    photoUrl = "",
                    balance = 500,
                    streak = 1,
                    joinedAt = System.currentTimeMillis(),
                    referralCode = firebaseUser.uid.take(6).uppercase()
                )
                userRef.set(newUser).await()
                newUser
            } else {
                snapshot.toObject(User::class.java) ?: throw Exception("Failed to parse user")
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
