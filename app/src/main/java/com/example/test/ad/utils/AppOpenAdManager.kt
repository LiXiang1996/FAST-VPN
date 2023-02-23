package com.example.test.ad.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.test.base.AppConstant
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import timber.log.Timber
import java.util.*




class AppOpenAdManager {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
    private var AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"
    private var TAG = AppConstant.TAG +"OpenAD"

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0

    /**
     * Load an ad.
     * @param context the context of the activity that loads the ad
     */
    fun loadAd(context: Context) {
        if(isLoadingAd)
            Timber.tag(TAG).e("AppOpen 没有正在loading的广告 $isLoadingAd  ")
        // 如果有未使用的广告或已在加载的广告，不加载广告
        if (isLoadingAd || isAdAvailable()) {
            Timber.tag(TAG).e("AppOpen 是否有正在loading的广告 $isLoadingAd  isAdAvailable ${isAdAvailable()}")
            return
        }
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(context, AD_UNIT_ID, request, object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Timber.tag(TAG).d("onAdLoaded.")
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Timber.tag(TAG).d("onAdFailedToLoad: %s", loadAdError.message +"code"  +loadAdError.code)
                }
            }
        )
    }

    /** 检查广告是否在 n 小时前加载. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** 检查广告是否存在并且可以展示. */
    private fun isAdAvailable(): Boolean {
//        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
        return appOpenAd != null
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     */
    fun showAdIfAvailable(activity: Activity) {
        showAdIfAvailable(
            activity,
            object : OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                }
            }
        )
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        if (isShowingAd) {
            Timber.tag(TAG).d("The app open ad is already showing.")
            return
        }
        if (appOpenAd == null) {
            Timber.tag(TAG).d("The app open ad is not ready yet.")
            loadAd(activity)
            return
        }
        Timber.tag(TAG).d("Will show ad.")
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                onShowAdCompleteListener.onShowAdComplete()
                super.onAdClicked()
            }

            override fun onAdImpression() {
                Timber.tag(TAG).d(" AD onAdImpression.")
                super.onAdImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                Timber.tag(TAG).d("onAdDismissedFullScreenContent.")
                loadAd(activity)
            }

            @SuppressLint("BinaryOperationInTimber")
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Timber.tag(TAG).d("onAdFailedToShowFullScreenContent: %s", adError.message+"code"  +adError.code)
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag(TAG).d("onAdShowedFullScreenContent.")
            }
        }
        isShowingAd = true
        appOpenAd?.show(activity)
    }
}

/**
 * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
 * dismissed or fails to show).
 */
interface OnShowAdCompleteListener {
    fun onShowAdComplete()
}