package com.rayworks.droidweekly.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rayworks.droidweekly.ui.theme.DroidWeeklyTheme
import com.rayworks.droidweekly.ui.theme.LightBlue
import kotlinx.coroutines.launch

/***
 * The internal web page using embedded [WebView]
 */
class DetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DroidWeeklyTheme {
                MainContent(
                    url = intent.getStringExtra("url_val") ?: "",
                    title = intent.getStringExtra("str_title") ?: "",
                    onClose = { finish() },

                ) { url ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context, url: String, title: String? = "") {
            val starter = Intent(context, DetailActivity::class.java)
            starter.putExtra("url_val", url)
            if (!title.isNullOrEmpty()) {
                starter.putExtra("str_title", title)
            }
            context.startActivity(starter)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainContent(url: String, title: String?, onClose: () -> Unit, onShare: (url: String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (title.isNullOrEmpty()) "WebView" else title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        softWrap = true,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBlue),
                navigationIcon = {
                    IconButton(onClick = { onClose.invoke() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onShare.invoke(url)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                        )
                    }
                },
            )
        },
        content = { BodyContent(url, onClose) },
    )
}

@Composable
fun BodyContent(url: String, onClose: () -> Unit) {
    var rememberWebViewProgress: Float by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .background(colorResource(android.R.color.white))
            .fillMaxSize(),
    ) {
        // web page loading progress
        AnimatedVisibility(visible = (rememberWebViewProgress > 0f && rememberWebViewProgress < 1f)) {
            LinearProgressIndicator(
                progress = rememberWebViewProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(colorResource(android.R.color.white)),
                color = Color.Cyan,
            )
        }

        WebContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            url = url,
            onProgressChange = { progress ->
                rememberWebViewProgress = (progress / 100f).coerceIn(0f, 1f)
            },
            onBack = { webView ->
                if (webView?.canGoBack() == true) {
                    webView.goBack()
                } else {
                    onClose.invoke()
                }
            },
            initSettings = { settings ->
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
            },
        )
    }
}

@Composable
fun WebContent(
    modifier: Modifier = Modifier,
    url: String,
    onProgressChange: (progress: Int) -> Unit = {},
    onBack: (webView: WebView?) -> Unit,
    initSettings: (webSettings: WebSettings?) -> Unit = {},
) {
    val chromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            onProgressChange(newProgress)
            super.onProgressChanged(view, newProgress)
        }
    }

    val client = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onProgressChange.invoke(0)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onProgressChange.invoke(100)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?,
        ) {
            super.onReceivedSslError(view, handler, error)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            super.onReceivedError(view, request, error)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var webView: WebView? = null
    AndroidView(modifier = modifier, factory = {
        WebView(it).apply {
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
