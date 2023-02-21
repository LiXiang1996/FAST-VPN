package com.example.test.ui.activity

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.data.CountryUtils
import com.example.test.base.utils.DpUtils
import com.example.test.base.utils.LinearLayoutDivider
import com.example.test.base.utils.NetworkUtil
import com.example.test.base.utils.ScreenSizeUtils
import com.example.test.ui.activity.ServersListProfile.Companion.getServersList
import com.example.test.ui.widget.TitleView
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile


class ServersListActivity : BaseActivity() {

    private lateinit var serverList: RecyclerView
    private lateinit var serverTitle: TitleView
    private var recyclerViewAdapter: RecyclerViewAdapter? = null
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
        if (AppVariable.isFast && AppVariable.state == BaseService.State.Connected)
            getServersList()[0].isChecked = true
        else getServersList().find { it.host == AppVariable.host && AppVariable.state == BaseService.State.Connected }
            .apply { this?.isChecked = true }
        list.addAll(getServersList())
        recyclerViewAdapter?.setData(list)
        recyclerViewAdapter?.notifyDataSetChanged()
    }
}

/**
 * 本地配置节点列表 todo
 */
class ServersListProfile {
    companion object {
        private val fastProfile = Profile(name = "Fast Super Server")
        private val australiaProfile = Profile(
            name = "Dallas",
            host = "34.213.182.172",
            remotePort = 800,
            password = "tvX1v#NSFP_LG_bJ",
            method = "chacha20-ietf-poly1305",
            country_code = "as",
        )
        private val japanProfile = Profile(
            name = "Japan",
            host = "13.107.21.200",
            remotePort = 8389,
            password = "u1rRWTssNv0p",
            method = "aes-256-cfb",
            country_code = "jp",
        )
        private val norwayProfile = Profile(
            name = "Norway",
            remotePort = 8389,
            host = "13.107.21.201",
            password = "u1rRWTssNv0p",
            method = "aes-256-cfb",
            country_code = "jp",
        )

        private val chinaProfile = Profile(
            name = "China", host = "116.179.32.99"
        )
        private val indiaProfile = Profile(
            name = "India", host = "192.168.1.1"
        )
        private val irelandProfile = Profile(
            name = "IreLand", host = "116.179.32.98"
        )

        fun getServersList(): MutableList<Profile> {
            val mutableList = mutableListOf<Profile>()
            mutableList.add(fastProfile)
            mutableList.add(australiaProfile)
            mutableList.add(japanProfile)
            mutableList.add(norwayProfile)
            mutableList.add(chinaProfile)
            mutableList.add(indiaProfile)
            mutableList.add(irelandProfile)
            return mutableList
        }

        fun getServerProfile(host: String): Profile {
            val list = ArrayList(getServersList()).apply { removeAt(0) }
            val data = list.find { it.host == host }
            return if (data == null) list[0] else data
        }

    }


}

class RecyclerViewAdapter(context: Context?) : Adapter<RecyclerViewAdapter.MyViewHolder>() {
    private var data: List<Profile>? = null
    private val inflater: LayoutInflater
    private var context: Context? = null

    init {
        inflater = LayoutInflater.from(context)
        this.context = context
    }

    fun setData(data: List<Profile>) {
        this.data = data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView: View = inflater.inflate(R.layout.servers_list_item_layout, null)
        val layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        itemView.layoutParams = layoutParams
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.countryText.text = data?.get(position)?.name
        context?.let { it ->
            Glide.with(it).load(data?.get(position)?.let { it1 ->
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
                showTipsDialog(holder.itemView.context, data?.get(position))
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


    private fun showTipsDialog(context: Context, data: Profile?) {
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
            val find = getServersList().find { it.isChecked }
            AppVariable.temporaryProfile = data
            toJump(context, find, false)
            dialog.dismiss()
        }
        dialog.show()
    }
}


