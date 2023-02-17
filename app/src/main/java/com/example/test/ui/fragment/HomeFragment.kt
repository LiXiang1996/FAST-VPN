package com.example.test.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.RemoteException
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.data.CountryUtils
import com.example.test.base.utils.NetworkPing
import com.example.test.base.utils.NetworkUtil
import com.example.test.ui.activity.MainActivity
import com.example.test.ui.activity.ServersListActivity
import com.example.test.ui.activity.ServersListProfile
import com.example.test.ui.activity.SeverConnectStateActivity
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.utils.StartService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class HomeFragment : Fragment(), ShadowsocksConnection.Callback {

    private lateinit var serversContainer: RelativeLayout
    private lateinit var connectStateImg: AppCompatImageView
    private lateinit var countryIcon: AppCompatImageView
    private lateinit var connectClickBtn: LinearLayout
    private lateinit var countryName: AppCompatTextView
    lateinit var connectTimeTv: Chronometer
    private lateinit var context: MainActivity
    private val connection = ShadowsocksConnection(true)
    var isJump = false


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
        super.onResume()
        countryName.text =
            if (AppVariable.country == AppConstant.DEFAULT || AppVariable.country.isBlank()) "Super Fast Server" else AppVariable.country
        Glide.with(this).load(CountryUtils.getCountrySource(AppVariable.country)).circleCrop()
            .into(countryIcon)
    }

    private fun initView(view: View) {
        connectClickBtn = view.findViewById(R.id.main_connection_toggle_btn)
        serversContainer = view.findViewById(R.id.server_connect_to_servers_container)
        connectStateImg = view.findViewById(R.id.main_connection_toggle_img)
        connectTimeTv = view.findViewById(R.id.main_connection_time_tv)
        countryIcon = view.findViewById(R.id.service_country_icon)
        connectTimeTv.text = "00:00:00"
        countryName = view.findViewById(R.id.service_country_name)
    }

    private fun initListener() {
        connectClickBtn.setOnClickListener {
            if (NetworkUtil.get().isNetworkAvailable || NetworkUtil.isNetSystemUsable(activity)) toggle()
            else Toast.makeText(activity, getString(R.string.network_error), Toast.LENGTH_LONG)
                .show()
        }
        serversContainer.setOnClickListener {
            val intent = Intent(activity, ServersListActivity::class.java)
            intentResult.launch(intent)

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
        (activity as MainActivity).frameLayout.visibility = View.VISIBLE
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

    private val permission = registerForActivityResult(StartService()) {
        if (it) Toast.makeText(
            context,
            getString(com.github.shadowsocks.core.R.string.vpn_permission_denied),
            Toast.LENGTH_LONG
        ).show() else {
            val data = ServersListProfile.getServerProfile(AppVariable.host).copy()
            val find =
                ProfileManager.getAllProfiles()?.find { it1 -> it1.host == AppVariable.host }
                    ?: Profile()
            val id: Long
            if (find.id == 0L) {
                id = ProfileManager.createProfile(find).id
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
        val animation: Animation? = AnimationUtils.loadAnimation(activity, R.anim.loading_rotate)
        when (state) {
            BaseService.State.Connecting -> {
                connectStateImg.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.mipmap.home_toggle_btn_loading
                    )
                )
                animation?.repeatCount = -1
                connectStateImg.startAnimation(animation)
            }
            BaseService.State.Connected -> {
                animation?.cancel()
                connectStateImg.clearAnimation()
                connectClickBtn.setBackgroundResource(R.mipmap.main_btn_open_bg)
                connectStateImg.setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.mipmap.home_toggle_btn_open
                    )
                )
                if (isJump) result()
            }
            BaseService.State.Stopped -> {
                connectStateImg.clearAnimation()
                animation?.cancel()
                connectClickBtn.setBackgroundResource(R.mipmap.main_btn_stop_bg)
                connectStateImg.setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.mipmap.home_toggle_btn_close
                    )
                )
                if (isJump) result()
            }
            BaseService.State.Stopping -> {
                connectStateImg.setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.mipmap.home_toggle_btn_loading
                    )
                )
                animation?.repeatCount = -1
                connectStateImg.startAnimation(animation)
            }
            else -> {

            }

        }

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
                if (AppVariable.state == BaseService.State.Connected) {
                    Toast.makeText(activity, "Disconnecting", Toast.LENGTH_LONG).show()
                } else if (AppVariable.state == BaseService.State.Stopped) {
                    Toast.makeText(activity, "Connecting", Toast.LENGTH_LONG).show()
                }
                val isToConnect = AppVariable.state == BaseService.State.Stopped
                lifecycleScope.launch {
                    if (isToConnect && AppVariable.isFast) launch {
                        NetworkPing.toFastToggle { ip ->
                            AppVariable.host = ip
                        }
                    }
                    launch {
                        delay(3000)
                        toggle()
                    }
                }
            }
        }


    private fun result() {
        (activity as MainActivity).frameLayout.visibility = View.GONE
        val intent = Intent(activity, SeverConnectStateActivity::class.java)
        activity?.startActivity(intent)
    }

}
