package com.example.ui.screens.earn.referearn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.MsCoinIcon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.data.repository.ConfigRepository
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

class ReferEarnViewModel : ViewModel() {
    private val configRepository = ConfigRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    var referrerReward by mutableStateOf(5000)
        private set
        
    var refereeReward by mutableStateOf(100)
        private set
        
    var userReferralCode by mutableStateOf("")
        private set
        
    var isCodeApplied by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val config = configRepository.getConfig()
            referrerReward = config.referral_referrer
            refereeReward = config.referral_referee
            loadUser()
        }
    }
    
    private suspend fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        userRepository.getUser(uid).onSuccess { user ->
            if (user.referralCode.isEmpty()) {
                val newCode = uid.take(6).uppercase()
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("referralCode", newCode).await()
                userReferralCode = newCode
            } else {
                userReferralCode = user.referralCode
            }
            isCodeApplied = !user.referredBy.isNullOrEmpty()
        }
    }
    
    fun applyCode(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.applyReferralCode(uid, code, referrerReward, refereeReward).fold(
                onSuccess = {
                    isCodeApplied = true
                    onSuccess()
                },
                onFailure = {
                    onError(it.message ?: "Failed to apply code")
                }
            )
        }
    }
}

@Composable
fun ReferEarnScreen(onBack: () -> Unit, viewModel: ReferEarnViewModel = viewModel()) {
    val context = LocalContext.current
    var inputCode by androidx.compose.runtime.remember { mutableStateOf("") }
    
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
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refer & Earn", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Invite your friends and\nearn unlimited coins",
            color = Color.White,
            fontSize = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // Illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.People, contentDescription = "Friends", tint = SecondaryTeal, modifier = Modifier.size(90.dp))
            MsCoinIcon(
                size = 36.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 40.dp, y = (-5).dp)
            )
            MsCoinIcon(
                size = 28.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = (-45).dp, y = 5.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Your Referral Code", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(viewModel.userReferralCode.ifEmpty { "LOADING..." }, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = TextSecondary)
            }
        }
        
        if (!viewModel.isCodeApplied) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Have a Referral Code?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it },
                    placeholder = { Text("Enter code here") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { 
                        if (inputCode.isNotBlank()) {
                            viewModel.applyCode(
                                code = inputCode.trim(),
                                onSuccess = { Toast.makeText(context, "Code applied!", Toast.LENGTH_SHORT).show() },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentYellow, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("How it Works", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        val steps = listOf(
            "Share your referral code",
            "Your friend joins and completes tasks",
            "You earn ${viewModel.referrerReward} coins, and they earn ${viewModel.refereeReward} coins"
        )
        
        steps.forEachIndexed { index, step ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}.", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(16.dp))
                Text(step, color = Color.White, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { /* TODO */ },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Filled.Share, contentDescription = "Share")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Invite Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
