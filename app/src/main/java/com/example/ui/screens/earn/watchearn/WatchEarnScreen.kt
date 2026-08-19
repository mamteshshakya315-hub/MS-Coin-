package com.example.ui.screens.earn.watchearn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.repository.UserRepository
import com.example.ui.theme.*
import com.example.ui.components.MsCoinIcon
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.example.util.AdManager
import kotlinx.coroutines.launch

import com.example.data.repository.ConfigRepository
import com.example.data.repository.AppConfig
import com.example.data.model.Transaction

class WatchEarnViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val configRepository = ConfigRepository()
    
    var videosWatched by mutableStateOf(0)
        private set
        
    var isWatchingAd by mutableStateOf(false)
        private set
        
    var rewardDialog by mutableStateOf(false)
        private set
        
    var rewardAmount by mutableStateOf(20)
        private set
    
    var videoLimit by mutableStateOf(10)
        private set
        
    init {
        loadUser()
        loadConfig()
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            val config = configRepository.getConfig()
            rewardAmount = config.video
            videoLimit = config.video_limit
        }
    }
    
    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUser(uid).onSuccess { user ->
                val calCurrent = java.util.Calendar.getInstance()
                calCurrent.timeInMillis = System.currentTimeMillis()
                
                val calLast = java.util.Calendar.getInstance()
                calLast.timeInMillis = user.lastVideoWatchedAt
                
                val isSameDay = calCurrent.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR) &&
                                calCurrent.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR)
                
                if (!isSameDay) {
                    userRepository.resetVideoLimit(uid)
                    videosWatched = 0
                } else {
                    videosWatched = user.videosWatchedToday
                }
            }
        }
    }
        
    fun watchAd(onShowAd: () -> Unit) {
        if (videosWatched >= videoLimit || isWatchingAd) return
        isWatchingAd = true
        onShowAd()
    }
    
    fun onAdWatched() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                userRepository.updateUserBalance(uid, rewardAmount)
                userRepository.updateVideoWatched(uid, 1)
                userRepository.addTransaction(Transaction(userId = uid, title = "Watch Video Reward", amount = rewardAmount, type = "credit", icon = "play"))
                videosWatched++
                rewardDialog = true
            }
            isWatchingAd = false
        }
    }
    
    fun onAdFailed() {
        isWatchingAd = false
    }
    
    fun dismissDialog() {
        rewardDialog = false
    }
}

@Composable
fun WatchEarnScreen(onBack: () -> Unit, viewModel: WatchEarnViewModel = viewModel()) {
    val context = LocalContext.current
    
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
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Watch & Earn", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF651FFF), Color(0xFF2C1558))))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Watch the videos and\nearn coins", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                MsCoinIcon(size = 44.dp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                val remainingVideos = maxOf(0, viewModel.videoLimit - viewModel.videosWatched)
                items(remainingVideos) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF151528))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF50057)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Watch Video", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Earn ${viewModel.rewardAmount} Coins", color = Color(0xFFAAAAB4), fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.watchAd {
                                    AdManager.showRewardedAd(
                                        activity = context as Activity,
                                        onRewarded = { viewModel.onAdWatched() },
                                        onFailed = { viewModel.onAdFailed() }
                                    )
                                }
                            },
                            enabled = !viewModel.isWatchingAd && viewModel.videosWatched < viewModel.videoLimit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color.Black),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (viewModel.isWatchingAd) "..." else "Watch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Daily Limit", color = Color(0xFFAAAAB4), fontSize = 12.sp)
                Text("${viewModel.videosWatched}/${viewModel.videoLimit} Videos", color = Color(0xFFAAAAB4), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (viewModel.videoLimit > 0) viewModel.videosWatched.toFloat() / viewModel.videoLimit else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFFC107),
                trackColor = Color(0xFF151528)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    if (viewModel.rewardDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            icon = { MsCoinIcon(size = 36.dp) },
            title = { Text("Reward Received!") },
            text = { Text("You successfully watched the ad and earned ${viewModel.rewardAmount} coins.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Awesome")
                }
            }
        )
    }
}
