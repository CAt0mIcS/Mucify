package com.tachyonmusic.data.model

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.tachyonmusic.domain.model.RewardAd
import com.tachyonmusic.logger.domain.Logger
import dagger.hilt.android.qualifiers.ApplicationContext

class InterstitialRewardAd(
    @ApplicationContext private val context: Context,
    private val log: Logger
) : RewardAd {
    private var interstitialAd: RewardedInterstitialAd? = null
    override var type: RewardAd.Type? = null
        private set

    override fun load() {
        val adRequest = AdRequest.Builder().build()

        RewardedInterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    log.debug("[TAdInt] Load error: ${adError.message}")
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    log.debug("[TAdInt] Reward ad loaded successfully")
                    interstitialAd = ad
                    type = RewardAd.Type.NewRemixes(ad.rewardItem.amount)

                    interstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdClicked() {
                                // Called when a click is recorded for an ad.
                                log.debug("[TAdInt] Ad was clicked.")
                            }

                            override fun onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                log.debug("[TAdInt] Ad dismissed fullscreen content.")
                                interstitialAd = null
                                type = null
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Called when ad fails to show.
                                log.debug("[TAdInt] Ad failed to show fullscreen content.")
                                interstitialAd = null
                                type = null
                            }

                            override fun onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                log.debug("[TAdInt] Ad recorded an impression.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                log.debug("[TAdInt] Ad showed fullscreen content.")
                            }
                        }
                }
            })
    }

    override fun unload() {
        interstitialAd = null
        type = null
        log.debug("[TAdInt] Ad unloaded successfully")
    }

    override fun show(activity: ComponentActivity, onRewardGranted: (RewardAd.Type, Int) -> Unit) {
        interstitialAd?.show(activity) { rewardItem ->
            onRewardGranted(type!!, rewardItem.amount)
        }
    }

    companion object {
        private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"
        private const val AD_UNIT_ID = "ca-app-pub-7145716621236451/5191931827"
    }
}