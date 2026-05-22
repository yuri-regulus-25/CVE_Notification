package com.yuri.cve_notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : ComponentActivity() {

    private var androidBridge: AndroidBridge? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        schedulePeriodicCveCheck()
        createWebView()
    }

    private fun createWebView() {
        val swipeRefreshLayout = SwipeRefreshLayout(this)
        val webView = WebView(this)

        swipeRefreshLayout.addView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            allowFileAccess = true
            allowContentAccess = true
            loadsImagesAutomatically = true
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        val bridge = AndroidBridge(this, webView, swipeRefreshLayout)
        androidBridge = bridge
        webView.addJavascriptInterface(bridge, "AndroidBridge")

        swipeRefreshLayout.setOnRefreshListener {
            webView.evaluateJavascript(
                "window.refreshFromAndroid && window.refreshFromAndroid()",
                null
            )
            swipeRefreshLayout.isRefreshing = false
        }

        webView.loadUrl("file:///android_asset/www/index.html")

        setContentView(swipeRefreshLayout)
    }

    override fun onDestroy() {
        androidBridge?.close()
        androidBridge = null
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun schedulePeriodicCveCheck() {

        val repeatInterval =
            if (BuildConfig.DEBUG) 15L else 6L

        val repeatUnit =
            if (BuildConfig.DEBUG) TimeUnit.MINUTES else TimeUnit.HOURS

        val workRequest =
            PeriodicWorkRequestBuilder<CveCheckWorker>(
                repeatInterval,
                repeatUnit
            ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cve_periodic_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}

data class AlertResult(
    val generatedAt: String,
    val count: Int,
    val alerts: List<CveAlert>
)

data class CveAlert(
    val alertId: String,
    val source: String,
    val category: String,
    val priority: String,
    val cveId: String,
    val matched: String,
    val severity: String,
    val score: String,
    val published: String,
    val lastModified: String,
    val title: String,
    val description: String,
    val url: String
)
