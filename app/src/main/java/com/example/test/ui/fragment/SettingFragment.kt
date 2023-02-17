package com.example.test.ui.fragment

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.test.R
import com.example.test.ui.activity.MainActivity
import com.example.test.ui.activity.WebViewActivity
import com.example.test.ui.widget.SettingItemView


class SettingFragment : Fragment() {

    private lateinit var contactUs: SettingItemView
    lateinit var privacyPolicy: SettingItemView
    lateinit var share: SettingItemView
    lateinit var upgrade: SettingItemView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)
        contactUs = view.findViewById(R.id.setting_contact_us)
        privacyPolicy = view.findViewById(R.id.setting_privacy_policy)
        share = view.findViewById(R.id.setting_share)
        upgrade = view.findViewById(R.id.setting_upgrade)
        initListener()
        return view
    }

    private fun initListener() {
        contactUs.setOnClickListener {
            toSendMail()
        }
        privacyPolicy.setOnClickListener {
            toPrivacyPolicy()
        }
        share.setOnClickListener {
            toShare()
        }
        upgrade.setOnClickListener {
            showMarket()
        }

    }

    /**
     * Call the system mailbox to contact us
     */
    private fun toSendMail() {
        val uri: Uri = Uri.parse("mailto:2459141797@qq.com")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        intent.putExtra(Intent.EXTRA_SUBJECT, "这是邮件的主题部分") // 主题
        intent.putExtra(Intent.EXTRA_TEXT, "这是邮件的正文部分") // 正文
        startActivity(Intent.createChooser(intent, ""))
    }

    /**
     * jump to Privacy Policy
     */
    private fun toPrivacyPolicy() {
        activity?.let {
            this.startActivity(Intent(it,WebViewActivity::class.java))
        }
    }

    private fun toShare() {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "要分享的文本")
        sendIntent.putExtra(Intent.EXTRA_TITLE, "我是标题")
        sendIntent.type = "text/plain"
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }


    private fun showMarket() {
        // TODO: 更换包名 
        val appPackageName = "com.example.test"
        try {
            val launchIntent = Intent()
            launchIntent.data = Uri.parse("market://details?id=$appPackageName")
            startActivity(launchIntent)
        } catch (anfe: ActivityNotFoundException) {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=$appPackageName"))
            )
        }
    }

}