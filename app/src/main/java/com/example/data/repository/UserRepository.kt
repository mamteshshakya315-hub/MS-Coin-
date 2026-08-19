package com.example.data.repository

import com.example.data.model.Transaction
import com.example.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getTopUsers(limit: Int = 50): Result<List<User>> {
        return try {
            val snapshot = db.collection("users")
                .orderBy("balance", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) Result.success(user) else Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserBalance(uid: String, amount: Int, title: String = "", type: String = "credit", icon: String = "star"): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            val txRef = db.collection("transactions").document()
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentBalance = snapshot.getLong("balance") ?: 0L
                transaction.update(userRef, "balance", currentBalance + amount)
                
                if (title.isNotEmpty()) {
                    val tx = Transaction(
                        id = txRef.id,
                        userId = uid,
                        title = title,
                        amount = amount,
                        type = type,
                        icon = icon,
                        timestamp = System.currentTimeMillis()
                    )
                    transaction.set(txRef, tx)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDailyStreak(uid: String, newStreak: Int): Result<Unit> {
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "streak" to newStreak,
                    "lastClaimedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateVideoWatched(uid: String, increment: Int): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("videosWatchedToday") ?: 0L
                transaction.update(
                    userRef,
                    "videosWatchedToday", currentCount + increment,
                    "lastVideoWatchedAt", System.currentTimeMillis()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetVideoLimit(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("videosWatchedToday", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateSpins(uid: String, increment: Int): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("spinsToday") ?: 0L
                transaction.update(
                    userRef,
                    "spinsToday", currentCount + increment,
                    "lastSpinAt", System.currentTimeMillis()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateScratches(uid: String, increment: Int): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentCount = snapshot.getLong("scratchesToday") ?: 0L
                transaction.update(
                    userRef,
                    "scratchesToday", currentCount + increment,
                    "lastScratchAt", System.currentTimeMillis()
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetSpinsLimit(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("spinsToday", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetScratchesLimit(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("scratchesToday", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        return try {
            val ref = db.collection("transactions").document()
            val newTx = transaction.copy(id = ref.id)
            ref.set(newTx).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTransactions(uid: String): Result<List<Transaction>> {
        return try {
            val snapshot = db.collection("transactions")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val txs = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
                .sortedByDescending { it.timestamp }
            Result.success(txs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getTransactionsFlow(uid: String): Flow<List<Transaction>> = callbackFlow {
        val listener = db.collection("transactions")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val txs = snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
                        .sortedByDescending { it.timestamp }
                    trySend(txs)
                }
            }
        awaitClose { listener.remove() }
    }
    
    
    fun getWithdrawRequestsFlow(uid: String): kotlinx.coroutines.flow.Flow<List<com.example.data.model.WithdrawRequest>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = db.collection("withdraw_requests")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reqs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.data.model.WithdrawRequest::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(reqs)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun applyReferralCode(uid: String, code: String, referrerReward: Int, refereeReward: Int): Result<Unit> {
        return try {
            // Find user with this code
            val snapshot = db.collection("users")
                .whereEqualTo("referralCode", code)
                .get()
                .await()
                
            if (snapshot.isEmpty) {
                return Result.failure(Exception("Invalid referral code"))
            }
            
            val referrer = snapshot.documents.first()
            val referrerUid = referrer.id
            
            if (referrerUid == uid) {
                return Result.failure(Exception("You cannot refer yourself"))
            }
            
            // Check if current user already used a code
            val currentUserDoc = db.collection("users").document(uid).get().await()
            val referredBy = currentUserDoc.getString("referredBy")
            if (!referredBy.isNullOrEmpty()) {
                return Result.failure(Exception("You have already used a referral code"))
            }
            
            db.runTransaction { transaction ->
                val referrerRef = db.collection("users").document(referrerUid)
                val currentUserRef = db.collection("users").document(uid)
                val referrerTxRef = db.collection("transactions").document()
                val currentUserTxRef = db.collection("transactions").document()
                
                // ALL READS MUST HAPPEN FIRST
                val referrerSnap = transaction.get(referrerRef)
                val currentUserSnap = transaction.get(currentUserRef)
                
                val referrerBal = referrerSnap.getLong("balance") ?: 0L
                val currentBal = currentUserSnap.getLong("balance") ?: 0L
                
                // THEN ALL WRITES
                transaction.update(referrerRef, "balance", referrerBal + referrerReward)
                transaction.update(currentUserRef, "balance", currentBal + refereeReward)
                transaction.update(currentUserRef, "referredBy", referrerUid)
                
                // Write transactions
                val refTx = Transaction(
                    id = referrerTxRef.id,
                    userId = referrerUid,
                    title = "Referral Reward",
                    amount = referrerReward,
                    type = "credit",
                    icon = "people",
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(referrerTxRef, refTx)
                
                val curTx = Transaction(
                    id = currentUserTxRef.id,
                    userId = uid,
                    title = "Referral Bonus",
                    amount = refereeReward,
                    type = "credit",
                    icon = "people",
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(currentUserTxRef, curTx)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
