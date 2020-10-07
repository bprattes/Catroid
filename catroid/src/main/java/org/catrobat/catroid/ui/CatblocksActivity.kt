package org.catrobat.catroid.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.webkit.WebViewAssetLoader

class CatblocksActivity : BaseActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val catblocksWebview = WebView(this)
        catblocksWebview.settings.javaScriptEnabled = true
        WebView.setWebContentsDebuggingEnabled(true);
        setContentView(catblocksWebview)

        val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this))
            .build()

        catblocksWebview.webViewClient = object : WebViewClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        catblocksWebview.loadUrl("https://appassets.androidplatform.net/assets/catblocks/index.html");
    }
}