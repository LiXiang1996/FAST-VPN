package com.example.test.ui.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.data.CountryUtils
import com.example.test.base.data.RemoteProfile
import com.example.test.base.data.ToProfile
import com.example.test.base.utils.DpUtils
import com.example.test.base.utils.LinearLayoutDivider
import com.example.test.base.utils.NetworkUtil
import com.example.test.base.utils.ScreenSizeUtils
import com.example.test.ui.activity.ServersListProfile.Companion.getServersList
import com.example.test.ui.activity.ServersListProfile.Companion.getSmartServerRandom
import com.example.test.ui.widget.TitleView
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import kotlinx.coroutines.*


class ServersListActivity : BaseActivity() {

    private lateinit var serverList: RecyclerView
    private lateinit var serverTitle: TitleView
    private var recyclerViewAdapter: RecyclerViewAdapter? = null
    private val serversListData: MutableList<Profile> = getServersList()

    override var layoutId: Int = R.layout.activity_server_list
    override fun initListener() {
        serverTitle.leftImg.setOnClickListener {
            finish()
        }
    }

    override fun initView() {
        serverTitle = findViewById(R.id.servers_title)
        serverList = findViewById(R.id.servers_recycle_view)
        recyclerViewAdapter = RecyclerViewAdapter(this)
        serverList.layoutManager = LinearLayoutManager(this)
        serverList.adapter = recyclerViewAdapter
        val decoration = LinearLayoutDivider(this, LinearLayoutManager.VERTICAL)
        decoration.mDividerHeight = DpUtils.dip2px(this, 12F)
        serverList.addItemDecoration(decoration)
        val list = mutableListOf<Profile>()
        GlobalScope.launch {
            coroutineScope {
                serversListData.forEach {
                    list.add(it.apply {
                        this.isChecked = false
                    })
                }
                if (AppVariable.state == BaseService.State.Connected) {
                    if (AppVariable.isFast) {
                        list[0].isChecked = true
                        for (i in 1 until list.size) {
                            if (list[i].isChecked) list[i].isChecked =
                                false
                        }
                    } else {
                        list[0].isChecked =
                            false//因为第一位smart配置是复制的下面普通配置的数据，可能会有数据isChecked为true
                        for (i in 1 until list.size) {
                            if (list[i].host == AppVariable.host) list[i].isChecked =
                                true
                        }
                    }
                }
            }
            recyclerViewAdapter?.setData(list)
            recyclerViewAdapter?.notifyDataSetChanged()
        }
    }
}


class RecyclerViewAdapter(context: Context?) : Adapter<RecyclerViewAdapter.MyViewHolder>() {
    private var data: MutableList<Profile>? = null
    private val inflater: LayoutInflater
    private var context: Context? = null

    init {
        inflater = LayoutInflater.from(context)
        this.context = context
    }

    fun setData(data: MutableList<Profile>) {
        this.data = data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView: View = inflater.inflate(R.layout.servers_list_item_layout, null)
        val layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        itemView.layoutParams = layoutParams
        return MyViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (!data?.get(position)?.name.isNullOrBlank() && position != 0) {
            holder.countryText.text = data?.get(position)?.name + "-" + data?.get(position)?.city
        } else holder.countryText.text = "Super Fast Server"
        context?.let { it ->
            if (position == 0) Glide.with(it).load(R.mipmap.server_default).circleCrop()
                .into(holder.iconImg)
            else Glide.with(it).load(data?.get(position)?.let { it1 ->
                it1.name.let { it2 -> CountryUtils.getCountrySource(it2 ?: "") }
            }).circleCrop().into(holder.iconImg)
        }
        if (data?.get(position)?.isChecked == true) {
            holder.iconIsChecked.isVisible = true
            holder.countryText.setTextColor(Color.parseColor("#333333"))
            holder.container.setBackgroundResource(R.drawable.server_item_checked_bg)
        } else {
            holder.iconIsChecked.isVisible = false
            holder.countryText.setTextColor(Color.parseColor("#FFFFFF"))
        }

        holder.container.setOnClickListener {
            val isHaveChecked: Int = data?.indexOfFirst { it.isChecked } ?: -1
            if (isHaveChecked > -1) {
                if (AppVariable.state == BaseService.State.Connected && isHaveChecked == position) return@setOnClickListener
                showTipsDialog(
                    holder.itemView.context, data?.get(position), data as MutableList<Profile>
                )
            } else {
                val isFastVpn = position == 0
                toJump(holder.itemView.context, data?.get(position), isFastVpn)
            }
        }
    }

    private fun toJump(context: Context, data: Profile?, isFast: Boolean) {
        if (NetworkUtil.get().isNetworkAvailable || NetworkUtil.isNetSystemUsable((context as ServersListActivity))) {
            val intent = Intent(context, MainActivity::class.java)
            AppVariable.host = data?.host ?: ""
            AppVariable.country = data?.name ?: ""
            AppVariable.isFast = isFast
            (context as ServersListActivity).setResult(100, intent)
            context.finish()
        } else Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_LONG)
            .show()
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iconImg: AppCompatImageView = itemView.findViewById(R.id.server_item_icon)
        var countryText = itemView.findViewById<AppCompatTextView>(R.id.server_item_name)
        var container = itemView.findViewById<RelativeLayout>(R.id.server_item_container)
        var iconIsChecked = itemView.findViewById<AppCompatImageView>(R.id.server_item_check_icon)
    }


    private fun showTipsDialog(
        context: Context, data: Profile?, serversListData: MutableList<Profile>
    ) {
        val dialog = Dialog(context, R.style.NormalDialogStyle)
        val localView = LayoutInflater.from(context)
            .inflate(R.layout.common_global_volume_dialog2, null) //设置自定义的弹窗UI
        dialog.setContentView(localView)
        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val localLayoutParams = window?.attributes
        localLayoutParams?.gravity = Gravity.CENTER
        localLayoutParams?.width = (ScreenSizeUtils.getInstance(context).screenWidth * 0.75).toInt()
        localLayoutParams?.height =
            (ScreenSizeUtils.getInstance(context).screenHeight * 0.3).toInt()
        window?.attributes = localLayoutParams
        val cancelTv = dialog.findViewById<AppCompatTextView>(R.id.volume_tv_cancel)
        val confirmTv = dialog.findViewById<AppCompatTextView>(R.id.volume_tv_confirm)
        cancelTv.setOnClickListener {
            dialog.dismiss()
        }
        confirmTv.setOnClickListener {
            val find = serversListData.find { it.isChecked }
            AppVariable.temporaryProfile = data
            toJump(context, find, false)
            dialog.dismiss()
        }
        dialog.show()
    }
}


class ServersListProfile {
    companion object {
        private val defaultProfile1 = ToProfile.remoteProfileToProfile(
            RemoteProfile(
                robvn_pwd = "tvX1v#NSFP_LG_bJ",
                robvn_account = "chacha20-ietf-poly1305",
                robvn_city = "Dallas",
                robvn_ip = "52.11.255.98",
                robvn_country = "United States",
                robvn_port = 812
            )
        )
        private val defaultProfile2 = ToProfile.remoteProfileToProfile(
            RemoteProfile(
                robvn_pwd = "tvX1v#NSFP_LG_bJ",
                robvn_account = "chacha20-ietf-poly1305",
                robvn_city = "Tokyo",
                robvn_ip = "52.11.255.91",
                robvn_country = "Japan",
                robvn_port = 811
            )
        )
        private val defaultProfile3 = ToProfile.remoteProfileToProfile(
            RemoteProfile(
                robvn_pwd = "tvX1v#NSFP_LG_bJ",
                robvn_account = "chacha20-ietf-poly1305",
                robvn_city = "Tokyo",
                robvn_ip = "52.11.255.92",
                robvn_country = "India",
                robvn_port = 811
            )
        )
        private val defaultSmartProfile1 = ToProfile.remoteProfileToProfile(
            RemoteProfile(
                robvn_pwd = "tvX1v#NSFP_LG_bJ",
                robvn_account = "chacha20-ietf-poly1305",
                robvn_city = "Dallas",
                robvn_ip = "52.11.255.98",
                robvn_country = "United States",
                robvn_port = 812
            )
        )

        //这是默认配置列表，若远程有数据，则更新此列表
        var defaultList = mutableListOf(defaultProfile1, defaultProfile2, defaultProfile3)

        //这是默认配置smart列表，若远程有数据，则更新此列表
        private var smartListProfile = mutableListOf(defaultSmartProfile1)

        fun getServersList(): MutableList<Profile> {
            val mutableList = mutableListOf<Profile>()
            mutableList.add(getSmartServerRandom())
            mutableList.addAll(defaultList)
            return mutableList
        }

        fun setSmartListProfile(listProfile: MutableList<Profile>) {
            this.smartListProfile = listProfile
        }

        fun getSmartServersList(): MutableList<Profile> {
            return smartListProfile
        }

        fun getSmartServerRandom(): Profile {
            return getSmartServersList().take(3).random().copy().apply {
                this.name = ""
            }
        }

        fun getServerProfile(host: String): Profile {
            val list = ArrayList(defaultList)
            val data = list.find { it.host == host }
            return if (data == null) list[0] else data
        }

    }


}


