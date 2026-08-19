package com.example.ui.screens.earn.scratchcard

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserRepository
import com.example.ui.theme.*
import com.example.ui.components.MsCoinIcon
import com.example.util.AdManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.data.model.Transaction

class ScratchCardViewModel : ViewModel() {
    private val configRepository = com.example.data.repository.ConfigRepository()
    var scratchLimit by mutableStateOf(10)
        private set
    var scratchReward by mutableStateOf(15)
        private set
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    var scratchesToday by mutableStateOf(0)
        private set

    init {
        loadUser()
        loadConfig()
    }
    private fun loadConfig() {
        viewModelScope.launch {
            val config = configRepository.getConfig()
            scratchLimit = config.scratch_limit
            scratchReward = config.scratch_reward
        }
    }


    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUser(uid).onSuccess { user ->
                val calCurrent = java.util.Calendar.getInstance()
                calCurrent.timeInMillis = System.currentTimeMillis()
                
                val calLast = java.util.Calendar.getInstance()
                calLast.timeInMillis = user.lastScratchAt
                
                val isSameDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)
                
                if (!isSameDay) {
                    userRepository.resetScratchesLimit(uid)
                    scratchesToday = 0
                } else {
                    scratchesToday = user.scratchesToday
                }
            }
        }
    }
    
    fun addCoinsAndRecordScratch(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.updateUserBalance(uid, amount)
            userRepository.updateScratches(uid, 1)
            userRepository.addTransaction(Transaction(userId = uid, title = "Scratch Card", amount = amount, type = "credit", icon = "star"))
            scratchesToday++
        }
    }
}

@Composable
fun ScratchCardScreen(onBack: () -> Unit, viewModel: ScratchCardViewModel = viewModel()) {
    val context = LocalContext.current
    var isScratched by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }
    var scratchProgress by remember { mutableFloatStateOf(0f) }
    var reward by remember(viewModel.scratchReward) { mutableStateOf(viewModel.scratchReward) }
    val cardsLeft = maxOf(0, viewModel.scratchLimit - viewModel.scratchesToday)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B16))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scratch Card", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Scratch the card and\nwin exciting coins",
            color = Color.White,
            fontSize = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isScratched) Color(0xFF2C1558) else Color(0xFF651FFF))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        if (!isScratched && cardsLeft > 0 && !isAdLoading) {
                            scratchProgress += 0.05f
                            if (scratchProgress >= 1f) {
                                isAdLoading = true
                                AdManager.showRewardedAd(
                                    activity = context as Activity,
                                    onRewarded = {
                                        isAdLoading = false
                                        isScratched = true
                                        viewModel.addCoinsAndRecordScratch(reward)
                                    },
                                    onFailed = {
                                        isAdLoading = false
                                        // On fail, still give reward to not frustrate user, or handle otherwise.
                                        // We will give reward so they can continue testing.
                                        isScratched = true
                                        viewModel.addCoinsAndRecordScratch(reward)
                                    }
                                )
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isAdLoading) {
                CircularProgressIndicator(color = Color.White)
            } else if (isScratched) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MsCoinIcon(size = 48.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You Won", color = Color(0xFFAAAAB4), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$reward Coins!", color = Color(0xFFFFC107), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "Scratch\nHere\n${(scratchProgress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        if (isScratched && cardsLeft > 0) {
            Button(
                onClick = { 
                    isScratched = false
                    scratchProgress = 0f
                    reward = viewModel.scratchReward
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Scratch Another", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else if (cardsLeft == 0) { 
            Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Come back tomorrow", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "You have $cardsLeft/10 cards left today",
            color = Color(0xFFAAAAB4),
            fontSize = 14.sp
        )
    }
}
