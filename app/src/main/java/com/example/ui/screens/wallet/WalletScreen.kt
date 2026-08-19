package com.example.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.ui.components.MsCoinIcon
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.*

import com.example.data.repository.ConfigRepository
import com.example.data.repository.AppConfig
import com.example.data.repository.UserRepository
import com.example.data.model.Transaction
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

data class PaymentMethod(
    val id: String = "",
    val name: String = "",
    val minimumAmount: Int = 100,
    val isActive: Boolean = true
)

class WalletViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val configRepository = ConfigRepository()
    private val userRepository = UserRepository()
    
    var balance by mutableStateOf(0)
        private set
        
    var isProcessing by mutableStateOf(false)
        private set

    var paymentMethods by mutableStateOf<List<PaymentMethod>>(emptyList())
        private set
        
    var config by mutableStateOf(AppConfig())
        private set
        
    init {
        loadData()
        loadConfig()
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            config = configRepository.getConfig()
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load balance
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val userDoc = db.collection("users").document(uid).get().await()
                    balance = userDoc.getLong("balance")?.toInt() ?: 0
                }
                
                // Load payment methods from Admin Panel (Firestore)
                val pmSnapshot = db.collection("payment_methods")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                
                val methods = pmSnapshot.documents.mapNotNull { it.toObject(PaymentMethod::class.java) }
                
                if (methods.isEmpty()) {
                    paymentMethods = listOf(
                        PaymentMethod("upi", "UPI", configRepository.getConfig().min_withdraw, true),
                        PaymentMethod("paytm", "Paytm", configRepository.getConfig().min_withdraw, true)
                    )
                } else {
                    paymentMethods = methods
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun requestWithdrawal(method: PaymentMethod, coins: Int, details: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Not logged in")
            return
        }
        
        val rupees = coins / config.rate.toDouble()
        if (rupees < method.minimumAmount) {
            onError("Minimum withdrawal for ${method.name} is ₹${method.minimumAmount}")
            return
        }
        
        if (coins > balance) {
            onError("Insufficient coin balance")
            return
        }
        
        if (details.isBlank()) {
            onError("Please enter payout details")
            return
        }
        
        viewModelScope.launch {
            try {
                // Deduct balance
                userRepository.updateUserBalance(uid, -coins)
                
                // Add transaction
                
                
                
                // Add withdraw request to Firestore (for admin panel)
                val req = hashMapOf(
                    "userId" to uid,
                    "method" to method.name,
                    "coins" to coins,
                    "rupees" to rupees,
                    "details" to details,
                    "status" to "pending",
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("withdraw_requests").add(req).await()
                
                balance -= coins
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to withdraw")
            }
        }
    }
}

@Composable
fun WalletScreen(onBack: () -> Unit = {}, onHistoryClick: () -> Unit = {}, viewModel: WalletViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var inputCoins by remember { mutableStateOf("") }
    var payoutDetails by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Wallet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Filled.History, contentDescription = "History", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(brush = Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryViolet)))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Total Balance", color = Color.White.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MsCoinIcon(size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${viewModel.balance}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                Text("≈ ₹${viewModel.balance / viewModel.config.rate.toDouble()}", color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { if (viewModel.isProcessing) return@Button 
                    if (selectedMethod == null && viewModel.paymentMethods.isNotEmpty()) {
                        selectedMethod = viewModel.paymentMethods.first()
                    }
                    if (selectedMethod != null) {
                        showDialog = true 
                    } else {
                        Toast.makeText(context, "No payment method available", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentYellow, contentColor = Color.Black),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (viewModel.isProcessing) "Processing..." else "Withdraw", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Transactions")
            }
        }
    }
    
    if (showDialog && selectedMethod != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Withdraw Coins") },
            text = {
                Column {
                    Text("Current Coins: ${viewModel.balance}", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Method: ${selectedMethod?.name ?: ""}")
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            viewModel.paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name) },
                                    onClick = {
                                        selectedMethod = method
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payoutDetails,
                        onValueChange = { payoutDetails = it },
                        label = { Text("Enter ${selectedMethod?.name ?: ""} Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputCoins,
                        onValueChange = { inputCoins = it },
                        label = { Text("Coins to Withdraw") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val coinsVal = inputCoins.toIntOrNull() ?: 0
                    val rupeesVal = coinsVal / viewModel.config.rate.toDouble()
                    Text("You will receive: ₹$rupeesVal", color = Color.Gray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val coinsVal = inputCoins.toIntOrNull() ?: 0
                    if (selectedMethod != null) {
                        viewModel.requestWithdrawal(
                            method = selectedMethod!!,
                            coins = coinsVal,
                            details = payoutDetails,
                            onSuccess = {
                                Toast.makeText(context, "Withdrawal Requested Successfully!", Toast.LENGTH_SHORT).show()
                                showDialog = false
                                inputCoins = ""
                                payoutDetails = ""
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }) {
                    Text("Withdraw Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
