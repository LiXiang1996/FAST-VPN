package com.example.test.ad.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.example.test.R
import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.ADLoading
import com.example.test.ad.data.ADType
import com.example.test.ad.data.CheckADStatus
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.utils.TimberUtils
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
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
    var currentNativeAd: NativeAd? = null
    val nativeTAG = AppConstant.TAG + " NativeAD"
    var isLoadingAD = false

    fun populateNativeAdView(nativeAd: NativeAd, adView: View) {
        val nativeAdView: NativeAdView = adView.rootView as NativeAdView
        nativeAdView.mediaView = NativeAdView1.adMedia
        nativeAdView.bodyView = NativeAdView1.adBody
        nativeAdView.iconView = NativeAdView1.appIcon
        nativeAdView.headlineView = NativeAdView1.headLine
        nativeAdView.callToActionView = NativeAdView1.actionView
        NativeAdView1.headLine?.text = nativeAd.headline
        nativeAd.mediaContent?.let {
            NativeAdView1.adMedia?.setMediaContent(it)
        }

        if (nativeAdView.mediaView?.mediaContent == null) {
            Timber.tag(AppConstant.TAG).e("nativeAdView.mediaView?.mediaContent 为空")
        } else {
            Timber.tag(AppConstant.TAG).e(
                "mediaContent ${nativeAdView.mediaView?.mediaContent}  body是否为空 ${nativeAd.body.isNullOrEmpty()}" +
                        " nativeAd callToAction ${nativeAd.callToAction.toString()} nativeAd icon  ${nativeAd.icon}   "
            )
        }
        if (nativeAd.body == null) {
            NativeAdView1.adBody?.visibility = View.INVISIBLE
        } else {
            NativeAdView1.adBody?.visibility = View.VISIBLE
            NativeAdView1.adBody?.text = nativeAd.body
        }

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
        nativeAdView.setNativeAd(nativeAd)

        //可能有视频资源
        val mediaContent = nativeAd.mediaContent
        val vc = mediaContent?.videoController
        //更新界面，说明此广告是否包含视频素材资源。
        if (vc != null && mediaContent.hasVideoContent()) {
            // 创建一个新的视频生命周期回调对象，并将其传递给视频控制器。这
            //       当视频中发生事件时，视频控制器将调用此对象上的方法
            //       生命周期。
            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    super.onVideoEnd()
                }
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
//            if (isLoadingAD) return
//        if (isLoadingAD || activity.isFinishing || activity.isDestroyed) return
            if (ADLoading.NATIVE_HOME.isLoading && type == ADType.NATIVE_HOME.value) {
                Timber.tag(AppConstant.TAG).e("home原生广告还没load完，不发起新的一轮请求")
                return
            }
            if (ADLoading.NATIVE_RESULT.isLoading && type == ADType.NATIVE_RESULT.value) {
                Timber.tag(AppConstant.TAG).e("result原生广告还没load完，不发起新的一轮请求")
                return
            }
            isLoadingAD = true
            if (type == ADType.NATIVE_HOME.value) ADLoading.NATIVE_HOME.isLoading = true
            if (type == ADType.NATIVE_RESULT.value) {
                ADLoading.NATIVE_RESULT.isLoading = true
                Timber.tag(AppConstant.TAG + "nativeloading")
                    .e(" loading loading ${ADLoading.NATIVE_RESULT.isLoading}")
            }
            TimberUtils().printADLoadLog(type, AppConstant.LOADING, nativeListAD[position])
            val builder = AdLoader.Builder(activity, nativeListAD[position].robvn_id)
            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val adOptions = NativeAdOptions.Builder()
                .setReturnUrlsForImageAssets(false)//修复admob广告接入media view不显示图片
                .setVideoOptions(videoOptions).build()

            builder.forNativeAd { nativeAd ->
                if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd
                result.invoke(nativeAd)
            }
                .withNativeAdOptions(adOptions)
                .withAdListener(object : AdListener() {
                    override fun onAdClicked() {
                        TimberUtils().printADClick(type)
                        CheckADStatus().setShowAndClickCount(
                            activity, isShow = false, isClick = true
                        )
                        super.onAdClicked()
                    }

                    override fun onAdClosed() {
                        Timber.tag(nativeTAG).e("关闭广告")
                        super.onAdClosed()
                    }

                    override fun onAdLoaded() {
                        AppVariable.cacheDataList?.add(HashMap<String, Any>().apply {
                            put(AppConstant.AD_TYPE, type)
                            put("value", currentNativeAd!!)
                            put(AppConstant.LOAD_TIME, Date().time)
                        })
                        TimberUtils().printADLoadLog(
                            type,
                            AppConstant.LOAD_SUC,
                            nativeListAD[position]
                        )
                        if (type == ADType.NATIVE_HOME.value) {
                            ADLoading.NATIVE_HOME.isLoading = false
                        }
                        if (type == ADType.NATIVE_RESULT.value) {
                            ADLoading.NATIVE_RESULT.isLoading = false
                        }
                        isLoadingAD = false
                        super.onAdLoaded()
                    }

                    override fun onAdOpened() {
                        Timber.tag(nativeTAG).e("ad opened")
                        super.onAdOpened()
                    }

                    override fun onAdImpression() {
                        if (type == ADType.NATIVE_RESULT.value) AppVariable.isNativeResultImpression =
                            true
                        if (type == ADType.NATIVE_HOME.value) AppVariable.isNativeHomeImpression =
                            true
                        TimberUtils().printADImpression(type)
                        AppVariable.cacheDataList?.forEach {
                            if (it[AppConstant.AD_TYPE].toString() == type) AppVariable.cacheDataList?.remove(it)
                        }
                        CheckADStatus().setShowAndClickCount(
                            activity, isShow = true, isClick = false
                        )
                        refreshAd(activity, frameLayout, type, 0, nativeListAD) {}
                        super.onAdImpression()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        TimberUtils().printADLoadLog(
                            type,
                            AppConstant.LOAD_FAIL,
                            nativeListAD[position], loadAdError
                        )
                        if (type == ADType.NATIVE_HOME.value) ADLoading.NATIVE_HOME.isLoading =
                            false
                        if (type == ADType.NATIVE_RESULT.value) {
                            ADLoading.NATIVE_RESULT.isLoading = false
                            Timber.tag(AppConstant.TAG + "nativeloading")
                                .e(" failed loading ${ADLoading.NATIVE_RESULT.isLoading}")
                        }
                        isLoadingAD = false
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
