package com.example.test.ad.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.test.R
import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.ADLoading
import com.example.test.ad.data.ADType
import com.example.test.ad.data.CheckADStatus
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.utils.TimberUtils
import com.example.test.ui.activity.MainActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*


object NativeAdView1 {
    var adMedia: MediaView? = null
    var appIcon: AppCompatImageView? = null
    var headLine: AppCompatTextView? = null
    var adBody: AppCompatTextView? = null
    var actionView: AppCompatTextView? = null
    fun getView(context: Context): NativeAdView {
        val adView = LayoutInflater.from(context).inflate(R.layout.native_ad_view, null)
        adMedia = adView.findViewById(R.id.native_ad_media)
        appIcon = adView.findViewById(R.id.native_ad_header)
        headLine = adView.findViewById(R.id.ad_title1)
        adBody = adView.findViewById(R.id.ad_title2)
        actionView = adView.findViewById(R.id.ad_call_to_action)
        return adView as NativeAdView
    }
}

class NativeAdManager {

    fun populateNativeAdView(activity: Activity,nativeAd: NativeAd, adView: View,type: String) {
        val nativeAdView: NativeAdView = adView.rootView as NativeAdView
//        if (activity is MainActivity){
//            val h = AppVariable.connectClBottom-50
//            Timber.tag(AppConstant.TAG).e("${AppVariable.connectClBottom-50}")
//            val lp = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,h)
//            lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
//            lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
//            NativeAdView1.adMedia?.layoutParams = lp
//        }
        NativeAdView1.headLine?.text = nativeAd.headline ?: ""
        NativeAdView1.adBody?.text = nativeAd.body ?: ""
        nativeAd.mediaContent?.let { NativeAdView1.adMedia?.setMediaContent(it) }
        if (nativeAd.callToAction == null) {
            NativeAdView1.actionView?.visibility = View.INVISIBLE
        } else {
            NativeAdView1.actionView?.visibility = View.VISIBLE
            NativeAdView1.actionView?.text = nativeAd.callToAction
        }
        if (nativeAd.icon == null) {
            NativeAdView1.appIcon?.visibility = View.GONE
        } else {
            NativeAdView1.appIcon?.setImageDrawable(nativeAd.icon?.drawable)
            NativeAdView1.appIcon?.visibility = View.VISIBLE
        }
        nativeAdView.mediaView = NativeAdView1.adMedia
        nativeAdView.bodyView = NativeAdView1.adBody
        nativeAdView.iconView = NativeAdView1.appIcon
        nativeAdView.headlineView = NativeAdView1.headLine
        nativeAdView.callToActionView = NativeAdView1.actionView
        nativeAdView.setNativeAd(nativeAd)

        TimberUtils().printADImpression(type)
        AppVariable.cacheDataList?.let {
            AppVariable.cacheDataList?.forEach { it1 ->
                if (it1[AppConstant.AD_TYPE].toString() == type) AppVariable.cacheDataList?.remove(
                    it1
                )
            }

        }
    }


    fun refreshAd(
        activity: Activity,
        frameLayout: FrameLayout?,
        type: String,
        position: Int = 0,
        nativeListAD: MutableList<ADListBean.ADBean>,
        result: (NativeAd) -> Unit
    ) {
        if (position < nativeListAD.size) {
            if (ADLoading.NATIVE_HOME.isLoading && type == ADType.NATIVE_HOME.value) {
                Timber.tag(AppConstant.TAG).e("home原生广告还没load完，不发起新的一轮请求")
                return
            }
            if (ADLoading.NATIVE_RESULT.isLoading && type == ADType.NATIVE_RESULT.value) {
                Timber.tag(AppConstant.TAG).e("result原生广告还没load完，不发起新的一轮请求")
                return
            }
            if (type == ADType.NATIVE_HOME.value) ADLoading.NATIVE_HOME.isLoading = true
            if (type == ADType.NATIVE_RESULT.value) ADLoading.NATIVE_RESULT.isLoading = true
            TimberUtils().printADLoadLog(type, AppConstant.LOADING, nativeListAD[position])
            val builder = AdLoader.Builder(activity, nativeListAD[position].robvn_id)
            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val adOptions =
                NativeAdOptions.Builder()
                    .setReturnUrlsForImageAssets(false)//修复admob广告接入media view不显示图片
                    .setVideoOptions(videoOptions).build()

            builder.forNativeAd { nativeAd ->
                AppVariable.cacheDataList?.add(HashMap<String, Any>().apply {
                    put(AppConstant.AD_TYPE, type)
                    put("value", nativeAd)
                    put(AppConstant.LOAD_TIME, Date().time)
                })
                TimberUtils().printADLoadLog(
                    type, AppConstant.LOAD_SUC, nativeListAD[position]
                )
                if (type == ADType.NATIVE_HOME.value) {
                    ADLoading.NATIVE_HOME.isLoading = false
                }
                if (type == ADType.NATIVE_RESULT.value) {
                    ADLoading.NATIVE_RESULT.isLoading = false
                }
                result.invoke(nativeAd)
            }.withNativeAdOptions(adOptions).withAdListener(object : AdListener() {
                override fun onAdClicked() {
                    TimberUtils().printADClick(type)
                    CheckADStatus().setShowAndClickCount(
                        activity, isShow = false, isClick = true
                    )
                    super.onAdClicked()
                }


                override fun onAdImpression() {
//                    if (type == ADType.NATIVE_RESULT.value) AppVariable.isNativeResultImpression =
//                        true
//                    if (type == ADType.NATIVE_HOME.value) AppVariable.isNativeHomeImpression = true
//                    TimberUtils().printADImpression(type)
//                    AppVariable.cacheDataList?.forEach {
//                        if (it[AppConstant.AD_TYPE].toString() == type) AppVariable.cacheDataList?.remove(
//                            it
//                        )
//                    }
                    CheckADStatus().setShowAndClickCount(
                        activity, isShow = true, isClick = false
                    )
                    refreshAd(activity, frameLayout, type, 0, nativeListAD) {}
                    super.onAdImpression()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    TimberUtils().printADLoadLog(
                        type, AppConstant.LOAD_FAIL, nativeListAD[position], loadAdError
                    )
                    if (type == ADType.NATIVE_HOME.value) ADLoading.NATIVE_HOME.isLoading = false
                    if (type == ADType.NATIVE_RESULT.value) {
                        ADLoading.NATIVE_RESULT.isLoading = false
                        Timber.tag(AppConstant.TAG + "nativeloading")
                            .e(" failed loading ${ADLoading.NATIVE_RESULT.isLoading}")
                    }
                    if (position < nativeListAD.size) {
                        refreshAd(activity, frameLayout, type, position + 1, nativeListAD) {
                            result.invoke(it)
                        }
                    }
                }
            }).build().loadAd(AdRequest.Builder().build())
        }
    }

}
