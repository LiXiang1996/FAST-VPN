package com.example.test.ui.activity

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import com.example.test.ui.widget.TitleView

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    lateinit var titleView: TitleView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_layout)
        webView = findViewById(R.id.activity_web_view)
        titleView = findViewById(R.id.web_view_title)
        titleView.centerTitle.text = "Loading..."
        titleView.leftImg.setOnClickListener { finish() }
        webView.canGoBack()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                titleView.centerTitle.visibility = View.GONE
                super.onPageFinished(view, url)
            }

            override fun onReceivedSslError(
                view: WebView?, handler: SslErrorHandler?, error: SslError?
            ) {
                handler?.proceed();
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl("https://bbs.csdn.net/topics/390442564")
                return true
            }
        }
        webView.loadUrl("https://bbs.csdn.net/topics/390442564")
    }


    override fun onDestroy() {
        webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        webView.clearHistory()
        (webView.parent as ViewGroup).removeView(webView)
        webView.destroy()
        super.onDestroy()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
