package com.tachyonmusic.data.model

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.tachyonmusic.domain.model.RewardAd
import com.tachyonmusic.logger.domain.Logger
import dagger.hilt.android.qualifiers.ApplicationContext

class GoogleRewardAd(
    @ApplicationContext private val context: Context,
    private val log: Logger
) : RewardAd {
    private var rewardedAd: RewardedAd? = null
    override var type: RewardAd.Type? = null

    override fun load() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            TEST_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    log.warning("[TAdRew] Load Error: " + adError.message)
                    rewardedAd = null
                    type = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    log.debug("[TAdRew] Reward ad loaded successfully")
                    rewardedAd = ad
                    type = RewardAd.Type.NewRemixes(ad.rewardItem.amount)

                    rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            // Called when a click is recorded for an ad.
                            log.debug("[TAdRew] Ad was clicked.")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            // Called when ad is dismissed.
                            // Set the ad reference to null so you don't show the ad a second time.
                            log.debug("[TAdRew] Ad dismissed fullscreen content.")
                            rewardedAd = null
                            type = null
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            // Called when ad fails to show.
                            log.debug("[TAdRew] Ad failed to show fullscreen content.")
                            rewardedAd = null
                            type = null
                        }

                        override fun onAdImpression() {
                            // Called when an impression is recorded for an ad.
                            log.debug("[TAdRew] Ad recorded an impression.")
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            log.debug("[TAdRew] Ad showed fullscreen content.")
                        }
                    }
                }
            })
    }

    override fun unload() {
        rewardedAd = null
        log.debug("[TAdRew] Ad unloaded successfully")
    }

    override fun show(activity: ComponentActivity, onRewardGranted: (RewardAd.Type, Int) -> Unit) {
        rewardedAd?.show(activity) { rewardItem ->
            onRewardGranted(type!!, rewardItem.amount)
        }
    }

    companion object {
        private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val AD_UNIT_ID = "ca-app-pub-7145716621236451~4657802755"
    }
}