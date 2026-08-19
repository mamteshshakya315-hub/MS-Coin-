package com.example.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AppConfig(
    val daily_bonus: Int = 100,
    val referral_referrer: Int = 5000,
    val referral_referee: Int = 100,
    val video: Int = 20,
    val video_limit: Int = 10,
    val spin_reward: Int = 10,
    val spin_limit: Int = 10,
    val scratch_reward: Int = 15,
    val scratch_limit: Int = 10,
    val rate: Int = 100,
    val min_withdraw: Int = 100,
    val unity_id: String = "6056319",
    val privacy_policy: String = "https://example.com/privacy",
    val terms: String = "https://example.com/terms",
    val daily_streak_coins: List<Int> = listOf(10, 20, 30, 40, 50, 60, 100),
    val telegram_link: String = "",
    val youtube_link: String = "",
    val instagram_link: String = "",
    val ads_enabled: Boolean = true,
    val unity_ads_enabled: Boolean = true,
    val admob_ads_enabled: Boolean = false,
    val home_banner_url: String = "",
    val home_banner_target: String = ""
)

class ConfigRepository {
    private val db = FirebaseFirestore.getInstance()
    
    suspend fun getConfig(): AppConfig {
        return try {
            val doc = db.collection("settings").document("config").get().await()
            doc.toObject(AppConfig::class.java) ?: AppConfig()
        } catch (e: Exception) {
            AppConfig()
        }
    }
}
