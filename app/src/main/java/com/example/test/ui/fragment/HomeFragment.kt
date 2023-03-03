package com.example.test.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.RemoteException
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import android.widget.Chronometer.OnChronometerTickListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.ad.data.ADType
import com.example.test.ad.data.CheckADStatus
import com.example.test.ad.data.GetADData
import com.example.test.ad.utils.*
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.data.CountryUtils
import com.example.test.base.utils.NetworkPing
import com.example.test.base.utils.NetworkUtil
import com.example.test.ui.activity.MainActivity
import com.example.test.ui.activity.ServersListActivity
import com.example.test.ui.activity.ServersListProfile
import com.example.test.ui.activity.SeverConnectStateActivity
import com.example.test.ui.widget.NativeFrameLayout
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.utils.StartService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class HomeFragment : Fragment(), ShadowsocksConnection.Callback {

    lateinit var serversContainer: RelativeLayout
    private lateinit var connectStateImg: AppCompatImageView
    private lateinit var connectRobotImg: AppCompatImageView
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var countryIcon: AppCompatImageView
    lateinit var connectClickBtn: LinearLayout
    lateinit var connectClickGuideLottie: LottieAnimationView
    private lateinit var countryName: AppCompatTextView
    lateinit var connectTimeTv: Chronometer
    private lateinit var context: MainActivity
    var animationRotate: RotateAnimation? = null
    private val connection = ShadowsocksConnection(true)
    private var isJump = false
    var isToConnect = false
    var countDownTimer: CountDownTimer? = null
    var isShowGuideDialog = false

    private lateinit var interstitialAdManager: InterstitialAdManager
    private lateinit var nativeAdManager: NativeAdManager
     lateinit var nativeAdContainer: NativeFrameLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        this.context = activity as MainActivity
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)
        initView(view)
        initListener()
        connection.connect(context, this)
        return view
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }


    override fun onResume() {
        isToConnect = AppVariable.state == BaseService.State.Stopped
        setData()
        showNativeAD(activity as BaseActivity)
        nativeAdContainer.setOnTouchListener(object :View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return isShowGuideDialog
            }

        })
        super.onResume()
    }

    private fun initView(view: View) {
        nativeAdContainer = view.findViewById(R.id.main_native_ad_frame)
        connectClickBtn = view.findViewById(R.id.main_connection_toggle_btn)
        connectClickGuideLottie = view.findViewById(R.id.main_connection_toggle_bg)
        serversContainer = view.findViewById(R.id.server_connect_to_servers_container)
        connectStateImg = view.findViewById(R.id.main_connection_toggle_img)
        connectRobotImg = view.findViewById(R.id.main_connection_animate)
        lottieAnimationView = view.findViewById(R.id.main_connection_animate_lottie)
        connectTimeTv = view.findViewById(R.id.main_connection_time_tv)
        countryIcon = view.findViewById(R.id.service_country_icon)
        connectTimeTv.text = "00:00:00"
        countryName = view.findViewById(R.id.service_country_name)
        lottieAnimationView.imageAssetsFolder = "images"
        lottieAnimationView.setAnimation("data.json")
        lottieAnimationView.loop(true)
        connectClickGuideLottie.imageAssetsFolder = "images"
        connectClickGuideLottie.setAnimation("guide.json")
        connectClickGuideLottie.loop(true)
        connectClickGuideLottie.playAnimation()
        animationRotate = RotateAnimation(
            0f,
            360f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        animationRotate?.interpolator = object : LinearInterpolator() {}
        animationRotate?.repeatCount = Animation.INFINITE
        animationRotate?.duration = 3000
        animationRotate?.fillBefore = true
        nativeAdContainer.setHomeFragment(this)

        if (isShowGuideDialog && AppVariable.state != BaseService.State.Connected) {
            (activity as MainActivity).showGuideView()
        }
        interstitialAdManager = InterstitialAdManager()
        nativeAdManager = NativeAdManager()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {

        connectClickBtn.setOnClickListener {
            if (isShowGuideDialog) {
                //引导页消失操作
                isShowGuideDialog = false
                connectClickGuideLottie.visibility = View.GONE
                connectClickGuideLottie.cancelAnimation()
                (activity as MainActivity).guide?.dismiss()
                (activity as MainActivity).viewPager.setCanScroll(true)
                val tabStrip = (activity as MainActivity).tabLayout.getChildAt(0) as LinearLayout
                for (i in 0 until tabStrip.childCount) {
                    tabStrip.getChildAt(i).setOnTouchListener { _, _ -> false }
                }
            }
            if (NetworkUtil.get().isNetworkAvailable || NetworkUtil.isNetSystemUsable(activity)) {
                if (activity is MainActivity) {
                    if (CheckADStatus().canShowAD(activity as MainActivity))
                        activity?.let { it1 -> loadInterAd(it1, ADType.INTER_CONNECT.value) }
                    else {
                        (activity as MainActivity).frameLayout.visibility = View.VISIBLE//不可点击
                        MainScope().launch {
                            delay(3000)
                            toggle()
                        }
                    }
                }
            } else Toast.makeText(activity, getString(R.string.network_error), Toast.LENGTH_LONG)
                .show()
        }
        serversContainer.setOnClickListener {
            if (!isShowGuideDialog) {
                val intent = Intent(activity, ServersListActivity::class.java)
                intentResult.launch(intent)
            }
        }
        connectTimeTv.onChronometerTickListener = OnChronometerTickListener { cArg ->
            val time = System.currentTimeMillis() - cArg.base
            val d = Date(time)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            connectTimeTv.text = sdf.format(d)
        }
    }

    private fun toggle() {
        isJump = true
        if (AppVariable.state.canStop) Core.stopService()
        else {
            lifecycleScope.launch {
                if (AppVariable.isFast) launch {
                    NetworkPing.toFastToggle { ip ->
                        AppVariable.host = ip
                    }
                }
                permission.launch(null)
            }
        }
    }

    private val permission = registerForActivityResult(StartService()) { it ->
        if (it) {
            Toast.makeText(
                context,
                getString(com.github.shadowsocks.core.R.string.vpn_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            (activity as MainActivity)?.let { it2 -> it2.frameLayout.visibility = View.GONE }
            connectRobotImg.visibility = View.VISIBLE
            lottieAnimationView.visibility = View.INVISIBLE
            lottieAnimationView.clearAnimation()
            animationRotate?.cancel()
            connectClickBtn.setBackgroundResource(R.mipmap.main_btn_stop_bg)
            connectStateImg.setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.mipmap.home_toggle_btn_close
                )
            )
            connectRobotImg.setImageDrawable(context.getDrawable(R.mipmap.home_robot_disconnect))
        } else {
            val data = ServersListProfile.getServerProfile(AppVariable.host).copy()
            val find = ProfileManager.getAllProfiles()?.find { it1 -> it1.host == AppVariable.host }
                ?: Profile()
            val id: Long
            if (find.id == 0L) {
                id = ProfileManager.createProfile(data).id
            } else {
                id = find.id
                data.id = id
                ProfileManager.updateProfile(data)
            }
            Core.switchProfile(id)
            Core.startService()
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state, profileName)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun changeState(
        state: BaseService.State, profileName: String?
    ) {
        AppVariable.state = state
        AppVariable.connectTotalTime = connectTimeTv.text.toString()
        setConnectTime(state)
        Timber.tag(AppConstant.TAG).e("-ServiceState-$state")
        when (state) {
            BaseService.State.Connecting -> {
                connectingAndStoppingAnimation()
            }
            BaseService.State.Connected -> {
                connectClickBtn.setBackgroundResource(R.mipmap.main_btn_open_bg)
                connectStateImg.setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.mipmap.home_toggle_btn_open
                    )
                )
                connectRobotImg.setImageDrawable(context.getDrawable(R.mipmap.home_robot_connect))
                connectedAndStoppedAnimation()
            }
            BaseService.State.Stopped -> {
                connectClickBtn.setBackgroundResource(R.mipmap.main_btn_stop_bg)
                connectStateImg.setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.mipmap.home_toggle_btn_close
                    )
                )
                connectRobotImg.setImageDrawable(context.getDrawable(R.mipmap.home_robot_disconnect))
                connectedAndStoppedAnimation()

            }
            BaseService.State.Stopping -> {
                connectingAndStoppingAnimation()
            }
            else -> {

            }
        }
    }

    private fun connectingAndStoppingAnimation() {
        connectRobotImg.visibility = View.INVISIBLE
        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.playAnimation()
        connectStateImg.setImageDrawable(
            ContextCompat.getDrawable(
                context, R.mipmap.home_toggle_btn_loading
            )
        )
        connectStateImg.animation = animationRotate
        connectStateImg.animation.start()
        animationRotate?.start()

    }

    private fun connectedAndStoppedAnimation() {
        connectRobotImg.visibility = View.VISIBLE
        lottieAnimationView.visibility = View.INVISIBLE
        lottieAnimationView.clearAnimation()
        animationRotate?.cancel()
        if (isJump) result()
    }


    override fun onServiceConnected(service: IShadowsocksService) {
        Timber.tag(AppConstant.TAG).e("-onServiceConnected-${service.state}")
        changeState(
            try {//传入连接状态
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }, ""
        )
    }

    private fun setConnectTime(state: BaseService.State) {
        when (state) {
            BaseService.State.Connected -> {
                connectTimeTv.setTextColor(Color.parseColor("#FFFFFF"))
                connectTimeTv.base = System.currentTimeMillis()
                connectTimeTv.start()
            }
            BaseService.State.Stopped -> {
                connectTimeTv.setTextColor(Color.parseColor("#80FFFFFF"))
                connectTimeTv.stop()
                connectTimeTv.base = SystemClock.elapsedRealtime()
            }
            else -> {}
        }

    }

    private val intentResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == 100) {
                (activity as MainActivity).frameLayout.visibility = View.VISIBLE
                if (AppVariable.state == BaseService.State.Connected) {
                    Toast.makeText(activity, "Disconnecting", Toast.LENGTH_SHORT).show()
                } else if (AppVariable.state == BaseService.State.Stopped) {
                    Toast.makeText(activity, "Connecting", Toast.LENGTH_SHORT).show()
                }
                lifecycleScope.launch {
                    if (isToConnect && AppVariable.isFast)
                        launch {
                            NetworkPing.toFastToggle { ip ->
                                AppVariable.host = ip
                            }
                        }
                    launch {
                        if (activity is MainActivity) {
                            if (CheckADStatus().canShowAD(activity as MainActivity)) {
//                                loadInterAd(activity as Activity, ADType.INTER_SERVER.value)
                                loadInterAd(activity as Activity, ADType.INTER_CONNECT.value)
                            } else {
                                delay(3000)
                                toggle()
                            }
                        }
                    }
                }
            }
        }

    private fun loadInterAd(activity: Activity, type: String) {
        (activity as MainActivity).frameLayout.visibility = View.VISIBLE
        connectingAndStoppingAnimation()
        MainScope().launch {
            delay(1000)//延迟一秒后再执行下面代码
        }
        countDownTimer = null
        showInterAD(activity, type)
        countDownTimer = object : CountDownTimer(9000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                //这儿用是否展示广告来判断倒计时结束时是否跳转
                if (!interstitialAdManager.adIsImpression) {
                    interstitialAdManager.interstitialAd = null
                    toggle()
                }
            }
        }
        countDownTimer?.start()
    }


    private fun result() {
        if (activity is MainActivity) {
            (activity as MainActivity).frameLayout.visibility = View.GONE
            val intent = Intent(activity, SeverConnectStateActivity::class.java)
            activity?.startActivity(intent)
        }
    }

    private fun setData() {
        if (AppVariable.temporaryProfile != null && AppVariable.state == BaseService.State.Stopped) {
            AppVariable.host = AppVariable.temporaryProfile?.host ?: AppConstant.DEFAULT
            AppVariable.country = AppVariable.temporaryProfile?.name ?: AppConstant.DEFAULT
            AppVariable.temporaryProfile = null
        }
        countryName.text =
            if (AppVariable.country == AppConstant.DEFAULT || AppVariable.country.isBlank()) "Super Fast Server" else AppVariable.country
        Glide.with(this).load(
            CountryUtils.getCountrySource(
                AppVariable.country
            )
        ).circleCrop()
            .into(countryIcon)
    }

    override fun onStop() {
        countDownTimer?.cancel()
        super.onStop()
    }

    private fun showNativeAD(activity: BaseActivity) {
        AppVariable.nativeHomeADList?.let {
            GetADData.getFindData(activity, context, ADType.NATIVE_HOME.value, nativeAdManager,
                it, nativeAdContainer, object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                    }
                })
        }

    }

    private fun showInterAD(activity: BaseActivity, type: String) {
        AppVariable.interADList?.let { data ->
            GetADData.getFindData(
                activity,
                context,
                type,
                interstitialAdManager,
                data,
                null,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        countDownTimer?.cancel()
                        toggle()
                    }
                })
        }
    }
}