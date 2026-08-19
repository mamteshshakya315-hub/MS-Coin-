package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.NotificationWorker
import java.util.concurrent.TimeUnit
import com.google.android.gms.ads.MobileAds
import com.example.ui.navigation.NavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.util.UnityAdsManager

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import com.example.ui.components.NetworkStatusOverlay

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    try {
      UnityAdsManager.initialize(this)
    } catch (e: Exception) {
      Log.e("MainActivity", "Failed to initialize UnityAds", e)
    }

    try {
      MobileAds.initialize(this)
    } catch (e: Exception) {
      Log.e("MainActivity", "Failed to initialize MobileAds / Play Services", e)
    }

    try {
      val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
      WorkManager.getInstance(this).enqueueUniquePeriodicWork(
          "NotificationWorker",
          ExistingPeriodicWorkPolicy.KEEP,
          workRequest
      )
    } catch (e: Exception) {
      Log.e("MainActivity", "Failed to enqueue NotificationWorker", e)
    }

    val preferencesManager = (application as MyApplication).preferencesManager
    
    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
          NetworkStatusOverlay {
            NavGraph()
          }
        }
      }
    }
  }
}
