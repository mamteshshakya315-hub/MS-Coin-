package com.example.ui.screens.earn.spinwheel

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
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

class SpinWheelViewModel : ViewModel() {
    private val configRepository = com.example.data.repository.ConfigRepository()
    var spinLimit by mutableStateOf(10)
        private set

    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    var spinsToday by mutableStateOf(0)
        private set

    init {
        loadUser()
        loadConfig()
    }
    private fun loadConfig() {
        viewModelScope.launch {
            val config = configRepository.getConfig()
            spinLimit = config.spin_limit
        }
    }


    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUser(uid).onSuccess { user ->
                val calCurrent = java.util.Calendar.getInstance()
                calCurrent.timeInMillis = System.currentTimeMillis()
                
                val calLast = java.util.Calendar.getInstance()
                calLast.timeInMillis = user.lastSpinAt
                
                val isSameDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)
                
                if (!isSameDay) {
                    userRepository.resetSpinsLimit(uid)
                    spinsToday = 0
                } else {
                    spinsToday = user.spinsToday
                }
            }
        }
    }
    
    fun addCoinsAndRecordSpin(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.updateUserBalance(uid, amount)
            userRepository.updateSpins(uid, 1)
            if (amount > 0) {
                userRepository.addTransaction(Transaction(userId = uid, title = "Spin Wheel", amount = amount, type = "credit", icon = "star"))
            }
            spinsToday++
        }
    }
}

@Composable
fun SpinWheelScreen(onBack: () -> Unit, viewModel: SpinWheelViewModel = viewModel()) {
    val context = LocalContext.current
    var isSpinning by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }
    var rotationDegree by remember { mutableFloatStateOf(0f) }
    var rewardDialog by remember { mutableStateOf<Int?>(null) }
    
    val spinsLeft = maxOf(0, viewModel.spinLimit - viewModel.spinsToday)
    
    val rewards = listOf(10, 50, 0, 100, 20, 200, 5, 500)
    val colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5), Color(0xFF00BCD4), Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFF795548))
    
    val animateRotation by animateFloatAsState(
        targetValue = rotationDegree,
        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
        finishedListener = {
            isSpinning = false
            val normalizedRotation = it % 360
            val segmentAngle = 360f / rewards.size
            val pointingAngle = (360 - normalizedRotation + 270) % 360
            val index = (pointingAngle / segmentAngle).toInt() % rewards.size
            val reward = rewards[index]
            rewardDialog = reward
            if (reward > 0) {
                viewModel.addCoinsAndRecordSpin(reward)
            } else {
                viewModel.addCoinsAndRecordSpin(0) // Record spin even if no win
            }
        },
        label = "spin_wheel"
    )

    fun startSpin() {
        if (!isSpinning && !isAdLoading && spinsLeft > 0) {
            isAdLoading = true
            AdManager.showRewardedAd(
                activity = context as Activity,
                onRewarded = {
                    isAdLoading = false
                    isSpinning = true
                    rotationDegree += (360 * 5) + (Math.random() * 360).toFloat()
                },
                onFailed = {
                    isAdLoading = false
                    // Optionally, you can let them spin even if ad fails, or show an error
                    // We'll let them spin anyway to not block the user entirely
                    isSpinning = true
                    rotationDegree += (360 * 5) + (Math.random() * 360).toFloat()
                }
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B16))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Spin Wheel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Spin the wheel and\nwin exciting rewards",
            color = Color.White,
            fontSize = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().rotate(animateRotation)) {
                val segmentAngle = 360f / rewards.size
                for (i in rewards.indices) {
                    drawArc(
                        color = colors[i],
                        startAngle = i * segmentAngle,
                        sweepAngle = segmentAngle,
                        useCenter = true,
                        style = Fill
                    )
                }
            }
            
            // Indicator at top (270 degrees)
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Pointer",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = (-150).dp)
                    .rotate(180f)
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (spinsLeft > 0) Color(0xFFFFC107) else Color.Gray)
                    .clickable(enabled = !isSpinning && !isAdLoading && spinsLeft > 0) {
                        startSpin()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isAdLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text("SPIN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { startSpin() },
            enabled = !isSpinning && !isAdLoading && spinsLeft > 0,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF651FFF), contentColor = Color.White, disabledContainerColor = Color.Gray),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if (spinsLeft == 0) "Come back tomorrow" else if (isSpinning) "Spinning..." else if (isAdLoading) "Loading Ad..." else "Spin Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "You have $spinsLeft/10 spins left today",
            color = Color(0xFFAAAAB4),
            fontSize = 14.sp
        )
    }
    
    if (rewardDialog != null) {
        AlertDialog(
            onDismissRequest = { rewardDialog = null },
            icon = { if (rewardDialog != 0) MsCoinIcon(size = 36.dp) },
            title = { Text(if (rewardDialog == 0) "Better luck next time!" else "Congratulations!") },
            text = { Text(if (rewardDialog == 0) "You didn't win anything this time." else "You won $rewardDialog coins!") },
            confirmButton = {
                TextButton(onClick = { rewardDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}
