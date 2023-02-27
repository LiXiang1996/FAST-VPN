package com.example.test.ad.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.example.test.R
import com.example.test.ad.data.ADListBean
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import timber.log.Timber
import java.text.FieldPosition


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
    val TAG = AppConstant.TAG + " NativeAD"
    var isLoadingAD = false

     fun populateNativeAdView(nativeAd: NativeAd, adView: View) {
        val nativeAdView: NativeAdView = adView.rootView as NativeAdView
        nativeAdView.mediaView = NativeAdView1.adMedia
        nativeAdView.bodyView = NativeAdView1.adBody
        nativeAdView.iconView = NativeAdView1.appIcon
        nativeAdView.headlineView = NativeAdView1.headLine
        nativeAdView.callToActionView = NativeAdView1.actionView
        NativeAdView1.headLine?.text = nativeAd.headline
        nativeAd.mediaContent?.let { NativeAdView1.adMedia?.setMediaContent(it) }

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
        result:(NativeAd)->Unit
    ) {
//        if (isLoadingAD) return
        Timber.tag(TAG).e("加载NativeAD $position 类型 $type")
        if (position < nativeListAD.size) {
            isLoadingAD = true
            val builder = AdLoader.Builder(activity, nativeListAD[position].robvn_id)
            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()

            builder.forNativeAd { nativeAd ->
                if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd
                result.invoke(nativeAd)
            }.withNativeAdOptions(adOptions)
           .withAdListener(object : AdListener() {
                override fun onAdClicked() {
                    Timber.tag(TAG).e("ad clicked")
                    super.onAdClicked()
                }

                override fun onAdClosed() {
                    Timber.tag(TAG).e("ad closed")
                    super.onAdClosed()
                }

                override fun onAdLoaded() {
                    isLoadingAD = false
                    Timber.tag(TAG).e("ad onLoaded 未展示 加入缓存 type $type")
                    val data = HashMap<String, Any>().apply {
                        put("type", type)
                        put("value", currentNativeAd!!)
                    }
                    AppVariable.cacheDataList?.add(data)
                    super.onAdLoaded()
                }

                override fun onAdOpened() {
                    Timber.tag(TAG).e("ad opened")
                    super.onAdOpened()
                }

                override fun onAdImpression() {
                    Timber.tag(TAG).e("ad onAdImpression,移除缓存")
                    val a = AppVariable.cacheDataList?.find { it["type"].toString() == type }
                    a?.remove(type)
                    super.onAdImpression()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAD = false
                    Timber.tag(TAG)
                        .e("domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}")
                    refreshAd(activity, frameLayout, type, position + 1, nativeListAD){
                        result.invoke(it)
                    }
                }
            }).build().loadAd(AdRequest.Builder().build())
        }
    }

}