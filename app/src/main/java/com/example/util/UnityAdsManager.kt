package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

object UnityAdsManager {
    private const val GAME_ID = "800358325"
    private const val AD_UNIT_ID = "Rewarded_Android"
    private const val AD_UNIT_ID_FALLBACK = "rewardedVideo"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        UnityAds.initialize(context, GAME_ID, false, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                isInitialized = true
                Log.d("UnityAds", "Initialization Complete with Game ID: $GAME_ID")
            }

            override fun onInitializationFailed(
                error: UnityAds.UnityAdsInitializationError?,
                message: String?
            ) {
                Log.e("UnityAds", "Initialization Failed: $message")
            }
        })
    }

    fun showRewardedAd(activity: Activity, onComplete: () -> Unit, onFailed: () -> Unit) {
        if (!isInitialized) {
            onFailed()
            return
        }

        fun tryLoadPlacement(placement: String, fallback: (() -> Unit)? = null) {
            UnityAds.load(placement, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    val targetPlacement = placementId ?: placement
                    UnityAds.show(activity, targetPlacement, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                        override fun onUnityAdsShowFailure(
                            placementId: String?,
                            error: UnityAds.UnityAdsShowError?,
                            message: String?
                        ) {
                            Log.e("UnityAds", "Show Failure: $message")
                            onFailed()
                        }

                        override fun onUnityAdsShowStart(placementId: String?) {
                        }

                        override fun onUnityAdsShowClick(placementId: String?) {
                        }

                        override fun onUnityAdsShowComplete(
                            placementId: String?,
                            state: UnityAds.UnityAdsShowCompletionState?
                        ) {
                            if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                onComplete()
                            } else {
                                onFailed()
                            }
                        }
                    })
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String?,
                    error: UnityAds.UnityAdsLoadError?,
                    message: String?
                ) {
                    Log.w("UnityAds", "Load Failure for $placement: $message")
                    if (fallback != null) {
                        fallback()
                    } else {
                        onFailed()
                    }
                }
            })
        }

        tryLoadPlacement(AD_UNIT_ID, fallback = {
            tryLoadPlacement(AD_UNIT_ID_FALLBACK, fallback = {
                onFailed()
            })
        })
    }
}
