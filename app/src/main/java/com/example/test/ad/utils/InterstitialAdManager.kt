package com.example.test.ad.utils

import android.content.Context
import com.example.test.ad.data.ADListBean
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
        if (context.isFinishing||context.isDestroyed) return
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
                val a = AppVariable.cacheDataList?.find { it["type"].toString() == type }
                a?.remove(type)
                if (type == ADType.INTER_OPEN.value) AppVariable.cacheSplashADData = null
                CheckADStatus().setShowAndClickCount(
                    context, isShow = true, isClick = false
                )
                super.onAdImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                TimberUtils().printAdDismissedFullScreenContent(type)
                interstitialAd = null
                onShowAdCompleteListener.onShowAdComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                TimberUtils().printAdFailedToShowFullScreenContent(type)
                interstitialAd = null
                loadAd(context, interListAD, 0, type) { it1, _ ->
                    if (it1) {
                        val cacheData =
                            AppVariable.cacheDataList?.find { it["type"].toString() == type }
                        cacheData?.let {
                            if (it["value"] is InterstitialAd) showInterstitialWithData(
                                context,
                                interListAD,
                                it["value"] as InterstitialAd,
                                type,
                                onShowAdCompleteListener
                            )
                        }
                    }
                }
            }

            override fun onAdShowedFullScreenContent() {
//                TimberUtils().printAdShowedFullScreenContent(type)
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
        if (context.isFinishing||context.isDestroyed||!context.canJump) return
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
                val a = AppVariable.cacheDataList?.find { it["type"].toString() == type }
                a?.remove(type)
                CheckADStatus().setShowAndClickCount(
                    context, isShow = true, isClick = false
                )
                if (type == ADType.INTER_OPEN.value) AppVariable.cacheSplashADData = null

                super.onAdImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                TimberUtils().printAdDismissedFullScreenContent(type)
                interstitialAd = null
                adIsImpression = false
                onShowAdCompleteListener.onShowAdComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                TimberUtils().printAdFailedToShowFullScreenContent(type)
                interstitialAd = null
                loadAd(context, interListAD, 0, type) { it1, _ ->
                    if (it1) {
                        val cacheData =
                            AppVariable.cacheDataList?.find { it["type"].toString() == type }
                        cacheData?.let {
                            if (it["value"] is InterstitialAd) showInterstitialWithData(
                                context,
                                interListAD,
                                it["value"] as InterstitialAd,
                                type,
                                onShowAdCompleteListener
                            )
                        }
                    }
                }
            }

            override fun onAdShowedFullScreenContent() {
//                TimberUtils().printAdShowedFullScreenContent(type)
            }
        }
        interstitialAd?.show(context)
    }

    fun loadAd(
        context: Context,
        interListAd: MutableList<ADListBean.ADBean>,
        position: Int = 0,
        type: String,
        result: (Boolean, Boolean) -> Unit
    ) {
        if (position < interListAd.size) {
            TimberUtils().printADLoadLog(type, AppConstant.LOADING, interListAd[position])
            if (adIsLoading || interstitialAd != null) {
                return
            }
            adIsLoading = true
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
                        //如果列表长度足够，则继续去reload，成功就返回true，直到遍历完还是失败，则返回false false
                        if (context is BaseActivity){ if (!context.canJump) return }
                        if (position + 1 < interListAd.size) loadAd(
                            context, interListAd, position + 1, type
                        ) { it, _ ->
                            if (it) result.invoke(true, true)
                        } else {
                            result(false, false)
                        }
                        if (type == ADType.INTER_OPEN.value) result.invoke(false, false)
                    }

                    override fun onAdLoaded(ad: InterstitialAd) {
                        TimberUtils().printADLoadLog(
                            type,
                            AppConstant.LOAD_SUC,
                            interListAd[position]
                        )
                        interstitialAd = ad
                        loadTime = Date().time
                        val cacheData =
                            AppVariable.cacheDataList?.find { it["type"].toString() == type }
                        if (cacheData != null) {
                            AppVariable.cacheDataList?.remove(cacheData)
                        }
                        val data = HashMap<String, Any>().apply {
                            put("type", type)
                            put("value", interstitialAd!!)
                            put(AppConstant.LOAD_TIME, Date().time)
                        }
                        AppVariable.cacheDataList?.add(data)
                        if (type == ADType.INTER_OPEN.value) AppVariable.cacheSplashADData =
                            interListAd[position]
                        adIsLoading = false
                        result.invoke(true, true)
                    }
                })
        }
    }

    private fun isAdAvailable(): Boolean {
        return interstitialAd != null && wasLoadTimeLessThanNHoursAgo(1)
    }

    /** 检查广告是否在 n 小时前加载. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
//        Timber.tag(interADTAG).e("datadiff $dateDifference")
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

}
