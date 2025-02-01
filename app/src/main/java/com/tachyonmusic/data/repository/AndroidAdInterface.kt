package com.tachyonmusic.data.repository

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.tachyonmusic.domain.model.RewardAd
import com.tachyonmusic.domain.repository.AdInterface
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.util.delay
import com.tachyonmusic.util.min
import com.tachyonmusic.util.runOnUiThread
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.milliseconds

class AndroidAdInterface(
    private val log: Logger,
    private val rewardAd: RewardAd
) : AdInterface {
    private var rewardAdLoadHandler: Job? = null
    private var nativeInstallAdLoadHandler: Job? = null

    private var _nativeAppInstallAdCache = MutableStateFlow<List<NativeAd>>(emptyList())
    override val nativeAppInstallAdCache: Flow<List<NativeAd>> =
        _nativeAppInstallAdCache.debounce(300.milliseconds)

    override val rewardAdType: RewardAd.Type?
        get() = rewardAd.type

    private lateinit var nativeAppInstallAdLoader: AdLoader

    val isLoadingNativeInstallAd: Boolean
        get() = nativeAppInstallAdLoader.isLoading

    override fun initialize(activity: ComponentActivity) {
        MobileAds.initialize(activity)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(listOf("TEST_EMULATOR")).build()
        )

        nativeAppInstallAdLoader = AdLoader.Builder(
            activity,
            NATIVE_INSTALL_AD_ID
        ).forNativeAd { nativeAd ->
            // Ensure the ad is of type App Install Ad before displaying

            _nativeAppInstallAdCache.update { it + nativeAd }
            log.debug("[AdInterface] Native Install Ad loaded")

        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                log.error("[AdInterface] Ad failed to load: ${adError.message}")
            }
        }).withNativeAdOptions(
            NativeAdOptions.Builder()
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT) // Customize AdChoices icon placement
                .build()
        ).build()
    }

    override fun release() {
        rewardAdLoadHandler?.cancel()
        rewardAd.unload()
        unloadNativeInstallAds()
    }

    override fun <T> showRewardAd(
        activity: ComponentActivity,
        onUserReward: (RewardAd.Type, Int) -> T
    ): T {
        var ret: T? = null
        rewardAd.show(activity) { type, amount ->
            ret = onUserReward(type, amount)
        }
        restartRewardLoadHandler(activity)
        return ret!!
    }

    override suspend fun <T> showRewardAdSuspend(
        activity: ComponentActivity,
        onUserReward: suspend (RewardAd.Type, Int) -> T
    ): T {
        val pair = runOnUiThread {
            suspendCoroutine { cont ->
                rewardAd.show(activity) { type, amount ->
                    cont.resume(onUserReward to (type to amount))
                }
            }
        }

        return pair.first(pair.second.first, pair.second.second).apply {
            restartRewardLoadHandler(activity)
        }
    }

    override fun loadRewardAd(lifecycleOwner: LifecycleOwner) {
        restartRewardLoadHandler(lifecycleOwner)
    }

    override fun loadNativeInstallAds(lifecycleOwner: LifecycleOwner) {
        restartNativeInstallAdLoadHandler(lifecycleOwner)
    }

    override fun unloadRewardAd() {
        rewardAdLoadHandler?.cancel()
        rewardAdLoadHandler = null
        rewardAd.unload()
    }

    override fun unloadNativeInstallAds() {
        nativeInstallAdLoadHandler?.cancel()
        nativeInstallAdLoadHandler = null
        _nativeAppInstallAdCache.value.forEach { it.destroy() }
        _nativeAppInstallAdCache.update { emptyList() }
        log.debug("[AdInterface] Unloaded native app install ads")
    }


    private fun restartRewardLoadHandler(lifecycleOwner: LifecycleOwner) {
        rewardAdLoadHandler?.cancel()
        rewardAdLoadHandler = lifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                rewardAd.load()
                delay(60.min) // Ads time out after 60 minutes
            }
        }
    }

    private fun restartNativeInstallAdLoadHandler(lifecycleOwner: LifecycleOwner) {
        nativeInstallAdLoadHandler?.cancel()
        nativeInstallAdLoadHandler = lifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                loadNativeInstallAdsInternal()
                delay(60.min) // Ads time out after 60 minutes
            }
        }
    }

    private fun loadNativeInstallAdsInternal() {
        if (isLoadingNativeInstallAd || _nativeAppInstallAdCache.value.size == 5)
            return

        log.debug("[AdInterface] Loading 5 native install ads...")
        nativeAppInstallAdLoader.loadAds(AdRequest.Builder().build(), 5)
    }

    companion object {
        const val NATIVE_INSTALL_AD_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
        const val NATIVE_INSTALL_AD_ID = "ca-app-pub-7145716621236451/6099968554"
    }
}