package com.example.test.ad.utils

import android.content.Context
import android.os.CountDownTimer
import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.ADType
import com.example.test.ad.data.CheckADStatus
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

class InterstitialAdManager {
    var interstitialAd: InterstitialAd? = null
    var adIsLoading: Boolean = false
    var adIsImpression: Boolean = false
    private var interADTAG = AppConstant.TAG + "showInterstitial"
    var loadTime: Long = 0


    fun showInterstitial(
        context: BaseActivity,
        interListAD: MutableList<ADListBean.ADBean>,
        type: String,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        adIsImpression = false
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Timber.tag(interADTAG).e("Ad was onAdClicked.")
                CheckADStatus().setShowAndClickCount(
                    context, isShow = false, isClick = true
                )
                super.onAdClicked()
            }

            override fun onAdImpression() {
                Timber.tag(interADTAG).e("Ad was onAdImpression.load成功后直接调用，移除缓存")
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
                Timber.tag(interADTAG).e("全屏内容消失")
                interstitialAd = null
                onShowAdCompleteListener.onShowAdComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Timber.tag(interADTAG).e("Ad failed to show.再次去请求广告（loadAD）")
                interstitialAd = null
                loadAd(context, interListAD, 0, type) { _, _ -> }
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag(interADTAG).e("广告全屏展示.")
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
        Timber.tag(interADTAG).e("有缓存")
        adIsImpression = false
        interstitialAd = interstitialAdCache
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Timber.tag(interADTAG).e("Ad was onAdClicked.")
                CheckADStatus().setShowAndClickCount(
                    context, isShow = false, isClick = true
                )
                super.onAdClicked()
            }

            override fun onAdImpression() {
                Timber.tag(interADTAG).e("Ad was onAdImpression.移除缓存")
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
                Timber.tag(interADTAG).e("全屏内容消失")
                interstitialAd = null
                adIsImpression = false
                onShowAdCompleteListener.onShowAdComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Timber.tag(interADTAG).e("Ad failed to show.再次去请求广告（loadAD）")
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
                Timber.tag(interADTAG).e("广告全屏展示.")
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
            if (adIsLoading || interstitialAd != null) {
                Timber.tag(interADTAG)
                    .e(" 广告是否在loading $adIsLoading  是否ad已存在 ${interstitialAd != null}")
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
                        val error =
                            "position$position domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message} "
                        Timber.tag(interADTAG).e("onAdFailedToLoad----$error")
                        //如果列表长度足够，则继续去reload，成功就返回true，直到遍历完还是失败，则返回false false
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
                        Timber.tag(interADTAG).e("Ad was loaded。 position $position 缓存 type $type")
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
        Timber.tag(interADTAG).e("datadiff $dateDifference")
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

}
