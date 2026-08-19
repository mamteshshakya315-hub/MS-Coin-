package com.example.util

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    // AdMob rewarded ad unit ID
    private const val ADMOB_REWARDED_UNIT_ID = "ca-app-pub-1412389526076421/1907424670"
    private const val ADMOB_TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    
    // Switch variable
    private var useUnityAds = true

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        if (useUnityAds) {
            useUnityAds = false
            UnityAdsManager.showRewardedAd(activity, onComplete = onRewarded, onFailed = {
                // If unity fails, fallback to primary AdMob
                showAdMobRewardedAd(activity, ADMOB_REWARDED_UNIT_ID, onRewarded, onFailed = {
                    // If live AdMob fails (e.g. publisher data not found), try AdMob test ad
                    showAdMobRewardedAd(activity, ADMOB_TEST_REWARDED_UNIT_ID, onRewarded, onFailed)
                })
            })
        } else {
            useUnityAds = true
            showAdMobRewardedAd(activity, ADMOB_REWARDED_UNIT_ID, onRewarded, onFailed = {
                // If live AdMob fails, fallback to Unity
                UnityAdsManager.showRewardedAd(activity, onComplete = onRewarded, onFailed = {
                    // If Unity also fails, fallback to AdMob test ad
                    showAdMobRewardedAd(activity, ADMOB_TEST_REWARDED_UNIT_ID, onRewarded, onFailed)
                })
            })
        }
    }

    private fun showAdMobRewardedAd(
        activity: Activity,
        adUnitId: String,
        onRewarded: () -> Unit,
        onFailed: () -> Unit
    ) {
        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(activity, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w("AdManager", "AdMob Load failed for $adUnitId: ${loadAdError.message}")
                    onFailed()
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    var userEarnedReward = false
                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            if (userEarnedReward) {
                                onRewarded()
                            } else {
                                onFailed()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("AdManager", "AdMob Show failed: ${adError.message}")
                            onFailed()
                        }
                    }
                    
                    try {
                        rewardedAd.show(activity) { _ ->
                            userEarnedReward = true
                        }
                    } catch (e: Exception) {
                        Log.e("AdManager", "AdMob Show error", e)
                        onFailed()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("AdManager", "AdMob Load error", e)
            onFailed()
        }
    }
}
