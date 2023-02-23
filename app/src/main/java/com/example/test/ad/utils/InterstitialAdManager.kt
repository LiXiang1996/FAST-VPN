package com.example.test.ad.utils

import android.os.CountDownTimer
import com.example.test.base.AppConstant
import com.example.test.base.BaseActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import timber.log.Timber

class InterstitialAdManager {

    private var interstitialAd: InterstitialAd? = null
    private val testADid = "ca-app-pub-3940256099942544/1033173712"
    var countdownTimer: CountDownTimer? = null
    private var adIsLoading: Boolean = false
    private var interADTAG = AppConstant.TAG + "showInterstitial"

    fun showInterstitial(
        activity: BaseActivity, onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdClicked() {
                    Timber.tag(interADTAG).e("Ad was onAdClicked.")
                    super.onAdClicked()
                }

                override fun onAdImpression() {
                    Timber.tag(interADTAG).e("Ad was dismissed.")
                    super.onAdImpression()
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag(interADTAG).e("Ad was onAdImpression.")
                    interstitialAd = null
                    loadAd(activity)
                    onShowAdCompleteListener.onShowAdComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag(interADTAG).e("Ad failed to show.")
                    interstitialAd = null
                    onShowAdCompleteListener.onShowAdComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.tag(interADTAG).e("Ad showed fullscreen content.")
                }
            }
            interstitialAd?.show(activity)
        } else {
            Timber.tag(interADTAG).e("Ad wasn't loaded.")
            startInterstitialAD(activity)
        }
    }

    private fun loadAd(activity: BaseActivity) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, testADid, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Timber.tag(interADTAG).e(adError.message)
                interstitialAd = null
                adIsLoading = false
                val error =
                    "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message}"
                Timber.tag(interADTAG).e(error)
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Timber.tag(interADTAG).e("Ad was loaded.")
                interstitialAd = ad
                adIsLoading = false
            }
        })
    }


    private fun startInterstitialAD(activity: BaseActivity) {
        if (!adIsLoading && interstitialAd == null) {
            adIsLoading = true
            loadAd(activity)
        }
    }

}