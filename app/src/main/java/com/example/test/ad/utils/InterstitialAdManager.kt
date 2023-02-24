package com.example.test.ad.utils

import android.os.CountDownTimer
import com.example.test.ad.data.ADListBean
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
    companion object {
        var interstitialAd: InterstitialAd? = null
    }

    var countdownTimer: CountDownTimer? = null
    var adIsLoading: Boolean = false
    var adIsImpression: Boolean = false
    private var interADTAG = AppConstant.TAG + "showInterstitial"

    fun showInterstitial(
        activity: BaseActivity,
        interListAD: MutableList<ADListBean.ADBean>,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    Timber.tag(interADTAG).e("Ad was onAdClicked.")
                    super.onAdClicked()
                }

                override fun onAdImpression() {
                    Timber.tag(interADTAG).e("Ad was onAdImpression.")
                    adIsImpression = true
                    super.onAdImpression()
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag(interADTAG).e("全屏内容消失")//用户点击后跳转google play，返回时，自己回调用此方法
//                    interstitialAd = null  //展示后不需要置为空，因为要缓存，不需要再去load一个ad对象
                    onShowAdCompleteListener.onShowAdComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag(interADTAG).e("Ad failed to show.")
//                    interstitialAd = null
                    loadAd(activity, interListAD) { }//todo 可以的话复现一下
                    onShowAdCompleteListener.onShowAdComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.tag(interADTAG).e("广告全屏展示.")
                }
            }
            interstitialAd?.show(activity)
        } else {
            Timber.tag(interADTAG).e("Ad wasn't loaded.")
            startInterstitialAD(activity, interListAD)
        }
    }

    fun loadAd(
        activity: BaseActivity,
        interListAd: MutableList<ADListBean.ADBean>,
        position: Int = 0,
        result: (Boolean) -> Unit
    ) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            interListAd[position].robvn_id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    adIsLoading = false
                    val error =
                        "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message} position$position"
                    Timber.tag(interADTAG).e("onAdFailedToLoad----$error")
                    if (position < interListAd.size) loadAd(activity, interListAd, position + 1) {
                        if (it) result.invoke(true)
                    }
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.tag(interADTAG)
                        .e("Ad was loaded.code ${interListAd[position].robvn_id} position $position")
                    interstitialAd = ad
                    adIsLoading = false
                    result.invoke(true)
                }
            })
    }


    private fun startInterstitialAD(
        activity: BaseActivity,
        interListAD: MutableList<ADListBean.ADBean>
    ) {
        if (!adIsLoading && interstitialAd == null) {
            adIsLoading = true
            loadAd(activity, interListAD) {}
        }
    }

}