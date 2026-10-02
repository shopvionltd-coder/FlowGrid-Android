package com.shopvion.flowgridgame

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

/**
 * AdManager:
 * Production-ready AdMob + Firebase Remote Config manager for FlowGrid.
 * Dynamically fetches, initializes, and manages all Ad Unit IDs from the cloud
 * without requiring Google Play Store app updates.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Backwards-compatible constants
    val BANNER_TEST_UNIT_ID: String get() = getBannerAdUnitId()
    val INTERSTITIAL_TEST_UNIT_ID: String get() = getInterstitialAdUnitId()
    val REWARDED_TEST_UNIT_ID: String get() = getRewardedAdUnitId()

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private var isMobileAdsInitialized = false
    private var isRemoteConfigInitialized = false

    // State holder for Compose reactivity when Remote Config updates live
    val currentBannerAdUnitIdState = mutableStateOf(GameConfig.DEFAULT_BANNER_AD_ID)

    /**
     * Initializes Firebase Remote Config and Google Mobile Ads SDK.
     * Fetches live Ad Unit IDs from cloud; falls back seamlessly to defaults.
     */
    fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
        val appContext = context.applicationContext

        // 1. Initialize Firebase Remote Config
        initRemoteConfig(appContext) {
            // 2. Initialize MobileAds SDK once Remote Config defaults/live values are active
            initMobileAds(appContext)
            onComplete?.invoke()
        }
    }

    /**
     * Sets up Firebase Remote Config with default fallback parameters and fetches cloud overrides.
     */
    private fun initRemoteConfig(context: Context, onReady: () -> Unit) {
        try {
            val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

            // Configure fetch intervals: 0 seconds in development / fast refresh
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0
                fetchTimeoutInSeconds = 10
            }
            remoteConfig.setConfigSettingsAsync(configSettings)

            // Register local fallback defaults
            remoteConfig.setDefaultsAsync(GameConfig.REMOTE_CONFIG_DEFAULTS)
                .addOnCompleteListener {
                    updateCachedAdUnitIds()
                }

            // Fetch and activate cloud parameters
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val isUpdated = task.result
                        Log.i(TAG, "Firebase Remote Config fetchAndActivate succeeded (updated: $isUpdated)")
                    } else {
                        Log.w(TAG, "Firebase Remote Config fetchAndActivate failed, using defaults", task.exception)
                    }
                    updateCachedAdUnitIds()
                    isRemoteConfigInitialized = true
                    onReady()
                }

            // Real-time Remote Config updates listener
            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.i(TAG, "Remote Config updated keys: ${configUpdate.updatedKeys}")
                    remoteConfig.activate().addOnCompleteListener {
                        updateCachedAdUnitIds()
                        // Reload preloaded ads if their unit IDs changed
                        if (configUpdate.updatedKeys.contains(GameConfig.REMOTE_KEY_INTERSTITIAL_AD_ID)) {
                            interstitialAd = null
                            loadInterstitial(context)
                        }
                        if (configUpdate.updatedKeys.contains(GameConfig.REMOTE_KEY_REWARDED_AD_ID)) {
                            rewardedAd = null
                            loadRewarded(context)
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Remote Config real-time update listener error: ${error.message}", error)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Remote Config, falling back to local defaults", e)
            updateCachedAdUnitIds()
            onReady()
        }
    }

    /**
     * Initializes Google Mobile Ads SDK and pre-loads interstitial and rewarded ads.
     */
    private fun initMobileAds(context: Context) {
        if (isMobileAdsInitialized) return
        try {
            val requestConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfig)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: ${status.adapterStatusMap}")
                isMobileAdsInitialized = true
                loadInterstitial(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob MobileAds", e)
        }
    }

    private fun updateCachedAdUnitIds() {
        val appId = getAdMobAppId()
        val bannerId = getBannerAdUnitId()
        val interstitialId = getInterstitialAdUnitId()
        val rewardedId = getRewardedAdUnitId()
        currentBannerAdUnitIdState.value = bannerId
        Log.i(TAG, "Connected Firebase Remote Config parameters -> admob_app_id=$appId, banner_ad_id=$bannerId, interstitial_ad_id=$interstitialId, rewarded_ad_id=$rewardedId")
        printAllAdIds(TAG)
    }

    /**
     * Data holder representing current Ad IDs diagnostics.
     */
    data class AdIdsInfo(
        val admobAppId: String,
        val bannerAdId: String,
        val interstitialAdId: String,
        val rewardedAdId: String,
        val isAppIdRemote: Boolean,
        val isBannerRemote: Boolean,
        val isInterstitialRemote: Boolean,
        val isRewardedRemote: Boolean,
        val isRemoteConfigActive: Boolean,
        val isMobileAdsActive: Boolean
    )

    fun getAdIdsInfo(): AdIdsInfo {
        val appId = getAdMobAppId()
        val bannerId = getBannerAdUnitId()
        val interstitialId = getInterstitialAdUnitId()
        val rewardedId = getRewardedAdUnitId()
        return AdIdsInfo(
            admobAppId = appId,
            bannerAdId = bannerId,
            interstitialAdId = interstitialId,
            rewardedAdId = rewardedId,
            isAppIdRemote = appId != GameConfig.DEFAULT_ADMOB_APP_ID,
            isBannerRemote = bannerId != GameConfig.DEFAULT_BANNER_AD_ID,
            isInterstitialRemote = interstitialId != GameConfig.DEFAULT_INTERSTITIAL_AD_ID,
            isRewardedRemote = rewardedId != GameConfig.DEFAULT_REWARDED_AD_ID,
            isRemoteConfigActive = isRemoteConfigInitialized,
            isMobileAdsActive = isMobileAdsInitialized
        )
    }

    /**
     * Prints all active Ad IDs and configuration parameters in debug mode
     * with an eye-catching ASCII box format in Logcat, and returns the formatted text.
     */
    fun printAllAdIds(tag: String = TAG, forcePrint: Boolean = false): String {
        val isDebug = com.example.BuildConfig.DEBUG || forcePrint
        val info = getAdIdsInfo()

        val bannerSource = if (info.isBannerRemote) "Firebase Remote Config" else "Default Test ID"
        val interstitialSource = if (info.isInterstitialRemote) "Firebase Remote Config" else "Default Test ID"
        val rewardedSource = if (info.isRewardedRemote) "Firebase Remote Config" else "Default Test ID"
        val appSource = if (info.isAppIdRemote) "Firebase Remote Config" else "Default Test ID"

        val formattedLog = buildString {
            appendLine("╔════════════════════════════════════════════════════════════════════════════════════╗")
            appendLine("║                        FLOWGRID DEBUG - ACTIVE ADMOB AD IDS                        ║")
            appendLine("╠════════════════════════════════════════════════════════════════════════════════════╣")
            appendLine("║ Status: RemoteConfig Init = ${info.isRemoteConfigActive} | MobileAds Init = ${info.isMobileAdsActive}")
            appendLine("║ Package: ${GameConfig.PACKAGE_NAME} | Debug Build: $isDebug")
            appendLine("╟────────────────────────────────────────────────────────────────────────────────────╢")
            appendLine("║ [1] ADMOB APP ID:")
            appendLine("║     ${info.admobAppId}")
            appendLine("║     Source: $appSource")
            appendLine("║ [2] BANNER AD ID (Inline Adaptive):")
            appendLine("║     ${info.bannerAdId}")
            appendLine("║     Source: $bannerSource")
            appendLine("║ [3] INTERSTITIAL AD ID (Every 3 Levels):")
            appendLine("║     ${info.interstitialAdId}")
            appendLine("║     Source: $interstitialSource")
            appendLine("║ [4] REWARDED AD ID (Free Hints Unlock):")
            appendLine("║     ${info.rewardedAdId}")
            appendLine("║     Source: $rewardedSource")
            appendLine("╚════════════════════════════════════════════════════════════════════════════════════╝")
        }

        if (isDebug) {
            Log.i(tag, "\n$formattedLog")
            // Also print single-line records for quick logcat grep/filtering
            Log.d(tag, "[DEBUG_AD_ID] admob_app_id       = ${info.admobAppId} ($appSource)")
            Log.d(tag, "[DEBUG_AD_ID] banner_ad_id      = ${info.bannerAdId} ($bannerSource)")
            Log.d(tag, "[DEBUG_AD_ID] interstitial_ad_id = ${info.interstitialAdId} ($interstitialSource)")
            Log.d(tag, "[DEBUG_AD_ID] rewarded_ad_id     = ${info.rewardedAdId} ($rewardedSource)")
        }

        return formattedLog
    }

    // -------------------------------------------------------------
    // DYNAMIC REMOTE CONFIG AD UNIT GETTERS
    // -------------------------------------------------------------

    /**
     * Returns dynamic AdMob Application ID from Firebase Remote Config or local default.
     */
    fun getAdMobAppId(): String {
        return try {
            val remoteId = Firebase.remoteConfig.getString(GameConfig.REMOTE_KEY_ADMOB_APP_ID)
            if (remoteId.isNotBlank()) remoteId else GameConfig.DEFAULT_ADMOB_APP_ID
        } catch (_: Exception) {
            GameConfig.DEFAULT_ADMOB_APP_ID
        }
    }

    /**
     * Returns dynamic Banner Ad Unit ID from Firebase Remote Config or local default.
     */
    fun getBannerAdUnitId(): String {
        return try {
            val remoteId = Firebase.remoteConfig.getString(GameConfig.REMOTE_KEY_BANNER_AD_ID)
            if (remoteId.isNotBlank()) remoteId else GameConfig.DEFAULT_BANNER_AD_ID
        } catch (_: Exception) {
            GameConfig.DEFAULT_BANNER_AD_ID
        }
    }

    /**
     * Returns dynamic Interstitial Ad Unit ID from Firebase Remote Config or local default.
     */
    fun getInterstitialAdUnitId(): String {
        return try {
            val remoteId = Firebase.remoteConfig.getString(GameConfig.REMOTE_KEY_INTERSTITIAL_AD_ID)
            if (remoteId.isNotBlank()) remoteId else GameConfig.DEFAULT_INTERSTITIAL_AD_ID
        } catch (_: Exception) {
            GameConfig.DEFAULT_INTERSTITIAL_AD_ID
        }
    }

    /**
     * Returns dynamic Rewarded Ad Unit ID from Firebase Remote Config or local default.
     */
    fun getRewardedAdUnitId(): String {
        return try {
            val remoteId = Firebase.remoteConfig.getString(GameConfig.REMOTE_KEY_REWARDED_AD_ID)
            if (remoteId.isNotBlank()) remoteId else GameConfig.DEFAULT_REWARDED_AD_ID
        } catch (_: Exception) {
            GameConfig.DEFAULT_REWARDED_AD_ID
        }
    }

    // -------------------------------------------------------------
    // INTERSTITIAL AD
    // -------------------------------------------------------------

    /**
     * Pre-loads an Interstitial Ad using dynamic interstitial_ad_id from Remote Config.
     */
    fun loadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adUnitId = getInterstitialAdUnitId()
        val adRequest = AdRequest.Builder().build()

        try {
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "InterstitialAd loaded successfully with unitId: $adUnitId")
                        interstitialAd = ad
                        isInterstitialLoading = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "InterstitialAd failed to load ($adUnitId): ${error.message}")
                        interstitialAd = null
                        isInterstitialLoading = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during loadInterstitial with unitId: $adUnitId", e)
            interstitialAd = null
            isInterstitialLoading = false
        }
    }

    /**
     * Shows the pre-loaded Interstitial Ad if available, then runs onDismissed.
     * Safely executes onDismissed if ad is not ready or fails to show.
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd dismissed")
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.w(TAG, "InterstitialAd failed to show: ${error.message}")
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "InterstitialAd not ready; proceeding with game and pre-loading in background")
            loadInterstitial(activity.applicationContext)
            onDismissed()
        }
    }

    // -------------------------------------------------------------
    // REWARDED AD
    // -------------------------------------------------------------

    /**
     * Pre-loads a Rewarded Ad using dynamic rewarded_ad_id from Remote Config.
     */
    fun loadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adUnitId = getRewardedAdUnitId()
        val adRequest = AdRequest.Builder().build()

        try {
            RewardedAd.load(
                context,
                adUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "RewardedAd loaded successfully with unitId: $adUnitId")
                        rewardedAd = ad
                        isRewardedLoading = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "RewardedAd failed to load ($adUnitId): ${error.message}")
                        rewardedAd = null
                        isRewardedLoading = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during loadRewarded with unitId: $adUnitId", e)
            rewardedAd = null
            isRewardedLoading = false
        }
    }

    /**
     * Shows the pre-loaded Rewarded Ad.
     * Invokes onRewardEarned when the user completes the video.
     * Fallback logic ensures player is never blocked even if ad fails.
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "RewardedAd dismissed, rewardEarned=$rewardEarned")
                    rewardedAd = null
                    loadRewarded(activity.applicationContext)
                    if (rewardEarned) {
                        onRewardEarned()
                    }
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.w(TAG, "RewardedAd failed to show: ${error.message}")
                    rewardedAd = null
                    loadRewarded(activity.applicationContext)
                    // Grant reward on failure so gameplay is never blocked
                    onRewardEarned()
                    onDismissed()
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardEarned = true
            }
        } else {
            Log.d(TAG, "RewardedAd not ready yet; granting fallback reward and triggering pre-load")
            loadRewarded(activity.applicationContext)
            onRewardEarned()
            onDismissed()
        }
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null
}

/**
 * Standard AdMob Banner Ad View wrapped in Compose AndroidView.
 * Uses dynamic Banner Ad Unit ID fetched from Firebase Remote Config.
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier
) {
    val dynamicBannerId by AdManager.currentBannerAdUnitIdState

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        key(dynamicBannerId) {
            AndroidView(
                modifier = Modifier.wrapContentSize(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = dynamicBannerId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("AdMobBannerView", "Banner ad loaded successfully ($dynamicBannerId)")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.w("AdMobBannerView", "Banner ad failed to load ($dynamicBannerId): ${error.message}")
                            }
                        }
                        try {
                            loadAd(AdRequest.Builder().build())
                        } catch (e: Exception) {
                            Log.w("AdMobBannerView", "Error loading banner ad", e)
                        }
                    }
                },
                onRelease = { adView ->
                    try {
                        adView.destroy()
                    } catch (_: Exception) {}
                }
            )
        }
    }
}
