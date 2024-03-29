package com.example.test.ad.utils

import android.app.Activity
import android.content.Context
import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.ADLoading
import com.example.test.ad.data.ADType
import com.example.test.ad.data.CheckADStatus
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.utils.TimberUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import timber.log.Timber
import java.util.*

class InterstitialAdManager {
    var interstitialAd: InterstitialAd? = null
    var adIsLoading: Boolean = false
    var adIsImpression: Boolean = false
    var loadTime: Long = 0


    fun showInterstitial(
        context: BaseActivity,
        interListAD: MutableList<ADListBean.ADBean>,
        type: String,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        if (context.isFinishing || context.isDestroyed) return
        adIsImpression = false
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                TimberUtils().printADClick(type)
                CheckADStatus().setShowAndClickCount(
                    context, isShow = false, isClick = true
                )
                super.onAdClicked()
            }

            override fun onAdImpression() {
                TimberUtils().printADImpression(type)
                adIsImpression = true
                AppVariable.cacheDataList?.forEach {
                    if (it[AppConstant.AD_TYPE].toString() == type) AppVariable.cacheDataList?.remove(
                        it
                    )
                }
                CheckADStatus().setShowAndClickCount(
                    context, isShow = true, isClick = false
                )
                if (type == ADType.INTER_OPEN.value) AppVariable.cacheSplashADData = null
                else loadAd(context, interListAD, 0, type) { it1, it2 -> }//展示成功做缓存

                super.onAdImpression()
            }


            override fun onAdDismissedFullScreenContent() {
                TimberUtils().printAdDismissedFullScreenContent(type)
                interstitialAd = null
                adIsImpression = false
                onShowAdCompleteListener.onShowAdComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                TimberUtils().printAdFailedToShowFullScreenContent(type, adError)
                AppVariable.cacheDataList?.forEach {
                    if (it[AppConstant.AD_TYPE].toString() == type) AppVariable.cacheDataList?.remove(
                        it
                    )
                }
                interstitialAd = null
                adIsImpression = false
                loadAd(context, interListAD, 0, type) { it1, _ ->
                }
            }

        }
        interstitialAd?.show(context)
    }

    fun showInterstitialWithData(
        context: BaseActivity,
        interListAD: MutableList<ADListBean.ADBean>,
        interstitialAdCache: InterstitialAd,
        type: String,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        if (context.isFinishing || context.isDestroyed) return
        adIsImpression = false
        interstitialAd = interstitialAdCache
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                TimberUtils().printADClick(type)
                CheckADStatus().setShowAndClickCount(
                    context, isShow = false, isClick = true
                )
                super.onAdClicked()
            }

            override fun onAdImpression() {
                TimberUtils().printADImpression(type)
                adIsImpression = true
                AppVariable.cacheDataList?.forEach {
                    if (it[AppConstant.AD_TYPE].toString() == type) AppVariable.cacheDataList?.remove(
                        it
                    )
                }
                CheckADStatus().setShowAndClickCount(
                    context, isShow = true, isClick = false
                )
                if (type == ADType.INTER_OPEN.value) AppVariable.cacheSplashADData = null
                else loadAd(context, interListAD, 0, type) { it1, it2 -> }//展示成功做缓存

                super.onAdImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                TimberUtils().printAdDismissedFullScreenContent(type)
                interstitialAd = null
                adIsImpression = false
                onShowAdCompleteListener.onShowAdComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                TimberUtils().printAdFailedToShowFullScreenContent(type, adError)
                AppVariable.cacheDataList?.forEach {
                    if (it[AppConstant.AD_TYPE].toString() == type) AppVariable.cacheDataList?.remove(
                        it
                    )
                }
                interstitialAd = null
                adIsImpression = false
                loadAd(context, interListAD, 0, type) { it1, _ ->
                }
            }

            override fun onAdShowedFullScreenContent() {
            }
        }
        interstitialAd?.show(context)
    }

    fun loadAd(
        context: Activity,
        interListAd: MutableList<ADListBean.ADBean>,
        position: Int = 0,
        type: String,
        result: (Boolean, Boolean) -> Unit
    ) {
        if (position < interListAd.size) {
            if (adIsLoading) {
                return
            }
            if (ADLoading.INTER.isLoading && type != ADType.INTER_OPEN.value) {
                Timber.tag(AppConstant.TAG).e("inter广告还没请求完，不发起新的一轮请求")
                return
            }
            if ((ADLoading.INTER_OPEN.isLoading) && type == ADType.INTER_OPEN.value) {
                Timber.tag(AppConstant.TAG).e("inter_open广告还没请求完，不发起新的一轮请求")
                return
            }
            TimberUtils().printADLoadLog(type, AppConstant.LOADING, interListAd[position])
            adIsLoading = true
            if (type != ADType.INTER_OPEN.value) ADLoading.INTER.isLoading = true
            else ADLoading.INTER_OPEN.isLoading = true
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(context,
                interListAd[position].robvn_id,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        interstitialAd = null
                        adIsLoading = false
                        TimberUtils().printADLoadLog(
                            type,
                            AppConstant.LOAD_FAIL,
                            interListAd[position],
                            adError
                        )
                        if (type == ADType.INTER_OPEN.value) {
                            ADLoading.INTER_OPEN.isLoading = false
                            result.invoke(false, false)
                            return
                        } else ADLoading.INTER.isLoading = false
                        if (position + 1 < interListAd.size) {
                            loadAd(
                                context, interListAd, position + 1, type
                            ) { it, _ ->
                                if (it) result.invoke(true, true)
                            }
                        } else {
                            result(false, false)
                        }
                    }

                    override fun onAdLoaded(ad: InterstitialAd) {
                        TimberUtils().printADLoadLog(
                            type,
                            AppConstant.LOAD_SUC,
                            interListAd[position]
                        )
                        if (type != ADType.INTER_OPEN.value) ADLoading.INTER.isLoading = false
                        else ADLoading.INTER_OPEN.isLoading = false
                        interstitialAd = ad
                        if (type == ADType.INTER_OPEN.value) AppVariable.cacheSplashADData =
                            interListAd[position]
                        AppVariable.cacheDataList?.add(HashMap<String, Any>().apply {
                            put(AppConstant.AD_TYPE, type)
                            put("value", interstitialAd!!)
                            put(AppConstant.LOAD_TIME, Date().time)
                        })
                        adIsLoading = false
                        if (context.isDestroyed || context.isFinishing) return
                        result.invoke(true, true)
                    }
                })
        }
    }

}
