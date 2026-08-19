package com.example.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.components.MsCoinIcon
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LeaderboardScreen(
    onBack: (() -> Unit)? = null,
    viewModel: LeaderboardViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val format = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Leaderboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = if (onBack == null) 8.dp else 0.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else if (users.isEmpty()) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found", color = TextSecondary)
            }
        } else {
            // Podium
            if (users.size >= 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val top3 = users.take(3)
                    // Rank 2
                    if (top3.size > 1) {
                        PodiumItem(rank = 2, user = top3[1], size = 64.dp, color = Color.LightGray, format = format)
                    }
                    // Rank 1
                    if (top3.isNotEmpty()) {
                        PodiumItem(rank = 1, user = top3[0], size = 80.dp, color = AccentYellow, format = format)
                    }
                    // Rank 3
                    if (top3.size > 2) {
                        PodiumItem(rank = 3, user = top3[2], size = 64.dp, color = Color(0xFFCD7F32), format = format) // Bronze
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            val listRanks = if (users.size > 3) users.drop(3) else emptyList()
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(listRanks) { index, user ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text((index + 4).toString(), color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        if (user.photoUrl.isNotEmpty()) {
                             AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryPurple), contentAlignment = Alignment.Center) {
                                Text(user.name.firstOrNull()?.toString()?.uppercase() ?: "U", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(user.name.ifBlank { "Guest" }, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MsCoinIcon(size = 14.dp, elevation = 0.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(format.format(user.balance), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.loadLeaderboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = AccentYellow),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Refresh Leaderboard \uD83D\uDD04", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun PodiumItem(rank: Int, user: User, size: androidx.compose.ui.unit.Dp, color: Color, format: NumberFormat) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (user.photoUrl.isNotEmpty()) {
             AsyncImage(
                model = user.photoUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.size(size).clip(CircleShape).background(PrimaryPurple), contentAlignment = Alignment.Center) {
                Text(user.name.firstOrNull()?.toString()?.uppercase() ?: "U", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(user.name.ifBlank { "Guest" }.take(10), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MsCoinIcon(size = 14.dp, elevation = 0.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(format.format(user.balance), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(rank.toString(), color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}
