package com.rayworks.droidweekly.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.rayworks.droidweekly.dashboard.ui.theme.DroidWeeklyTheme
import kotlinx.coroutines.launch

class DetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {
            DroidWeeklyTheme {
                MainContent(url = intent.getStringExtra("url_val") ?: "") { finish() }
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context, url: String) {
            val starter = Intent(context, DetailActivity::class.java)
            starter.putExtra("url_val", url)
            context.startActivity(starter)
        }
    }
}

@Composable
fun MainContent(url: String, onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebView", color = Color.White) },
                backgroundColor = Color(0xff0f9d58)
            )
        },
        content = { BodyContent(url, onClose) }
    )
}

@Composable
fun BodyContent(url: String, onClose: () -> Unit) {
    var rememberWebViewProgress: Int by remember { mutableStateOf(-1) }

    Box() {
        WebContent(modifier = Modifier.fillMaxSize(), url = url, onBack = { webView ->
            if (webView?.canGoBack() == true) {
                webView.goBack()
            } else {
                onClose.invoke()
            }
        }, initSettings = { settings ->
            settings?.apply {
                javaScriptEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = true
                javaScriptCanOpenWindowsAutomatically = true
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
        })

    }
}


@Composable
fun WebContent(
    modifier: Modifier = Modifier,
    url: String,
    onBack: (webView: WebView?) -> Unit,
    initSettings: (webSettings: WebSettings?) -> Unit = {}
) {

    val chromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            //onProgressChange(newProgress)
            super.onProgressChanged(view, newProgress)
        }
    }

    val client = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            super.onReceivedSslError(view, handler, error)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var webView: WebView? = null
    AndroidView(modifier = modifier, factory = {
        WebView(it).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.webChromeClient = chromeClient
            this.webViewClient = client

            webView = this

            initSettings(settings)
            loadUrl(url)
        }
    })

    BackHandler() {
        coroutineScope.launch {
            onBack.invoke(webView)
        }
    }
}

