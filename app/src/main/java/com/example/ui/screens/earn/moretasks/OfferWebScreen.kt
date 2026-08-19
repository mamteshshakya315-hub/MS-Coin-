package com.example.ui.screens.earn.moretasks

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OfferWebViewModel : ViewModel() {
    private val userRepository = UserRepository()
    var timeRemaining by mutableStateOf(0)
    var isTimerRunning by mutableStateOf(false)
    var rewardClaimed by mutableStateOf(false)

    fun startTimer(seconds: Int, reward: Int) {
        if (isTimerRunning || rewardClaimed) return
        timeRemaining = seconds
        isTimerRunning = true
        viewModelScope.launch {
            while (timeRemaining > 0) {
                delay(1000)
                timeRemaining--
            }
            isTimerRunning = false
            if (!rewardClaimed) {
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    userRepository.updateUserBalance(
                        uid,
                        reward,
                        "Offer Completion",
                        "credit",
                        "local_offer"
                    )
                }
                rewardClaimed = true
            }
        }
    }
}

@Composable
fun OfferWebScreen(
    url: String,
    timer: Int,
    reward: Int,
    onBack: () -> Unit,
    viewModel: OfferWebViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.startTimer(timer, reward)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1B2E))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete Offer", color = Color.White, fontSize = 20.sp)
            Spacer(modifier = Modifier.weight(1f))
            if (viewModel.rewardClaimed) {
                Text("Reward Claimed!", color = Color(0xFF00BFA5))
            } else {
                Text("Wait: ${viewModel.timeRemaining}s", color = Color(0xFFFFC107))
            }
        }
        
        // WebView
        val context = LocalContext.current
        var intentLaunched by remember { mutableStateOf(false) }

        if (!intentLaunched && url.isNotEmpty()) {
            LaunchedEffect(url) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                    intentLaunched = true
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (viewModel.rewardClaimed) {
                Text("Reward Claimed!", color = Color(0xFF00BFA5), fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color.Black)) {
                    Text("Go Back")
                }
            } else {
                CircularProgressIndicator(color = Color(0xFFFFC107))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Please wait ${viewModel.timeRemaining} seconds...", color = Color.White, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Complete the task in the opened app/website.", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch(e: Exception) {}
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)) {
                    Text("Open Task Again")
                }
            }
    }
}
}
