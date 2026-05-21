package com.yuri.cve_notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

class CveCheckWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val alertsUrl =
        "https://yuri-regulus-25.github.io/Operate_CVE_Notification/alerts.json"

    private val prefsName = "cve_alert_prefs"
    private val knownIdsKey = "known_alert_ids"
    private val channelId = "cve_alert_channel"
    private val urgentChannelId = "cve_urgent_channel"
    private val watchChannelId = "cve_watch_channel"
    private val ignoredIdsKey = "ignored_alert_ids"
    private val latestJsonKey = "latest_alerts_json"
    private val lastStatusKey = "last_fetch_status"
    private val lastCheckedAtKey = "last_checked_at"
    private val lastErrorKey = "last_fetch_error"
    private val hasFetchedOnceKey = "has_fetched_once"

    override fun doWork(): Result {
        return try {
            val fetchedAt = formatNow()

            val jsonText = fetchText(alertsUrl)
            val root = JSONObject(jsonText)
            val alerts = parseAlerts(jsonText)

            val generatedAtJst = formatGeneratedAtToJst(root.optString("generated_at", ""))

            val prefs = applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val knownIds = prefs.getStringSet(knownIdsKey, emptySet()) ?: emptySet()
            val ignoredIds = prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()
            val hasFetchedOnce = prefs.getBoolean(hasFetchedOnceKey, false)

            val currentAlertIds = alerts.map { it.alertId }.toSet()
            val cleanedKnownIds = knownIds.filter { it in currentAlertIds }.toSet()

            val isFirstRun = !hasFetchedOnce

            val cleanedIgnoredIds =
                if (isFirstRun) {
                    emptySet()
                } else {
                    ignoredIds.filter { it in currentAlertIds }.toSet()
                }

            val newAlerts = alerts.filter {
                it.alertId !in cleanedKnownIds && it.alertId !in cleanedIgnoredIds
            }

            prefs.edit()
                .putBoolean(hasFetchedOnceKey, true)
                .putStringSet(knownIdsKey, currentAlertIds)
                .putStringSet(ignoredIdsKey, cleanedIgnoredIds)
                .putString(latestJsonKey, jsonText)
                .putString(
                    lastStatusKey,
                    if (isFirstRun) {
                        "[$fetchedAt] 定期初回取得完了"
                    } else {
                        "[$fetchedAt] 定期取得完了"
                    }
                )
                .putLong(lastCheckedAtKey, System.currentTimeMillis())
                .remove(lastErrorKey)
                .apply()

            if (isFirstRun) {
                NotificationHelper.showStatusNotification(
                    context = applicationContext,
                    title = "初回の脆弱性情報を取得しました",
                    message = "取得日時: $fetchedAt\n合計件数: ${alerts.size}",
                    iconRes = R.drawable.shield_check,
                    notificationId = 9003
                )
            } else {
                if (newAlerts.isNotEmpty()) {
                    newAlerts.forEach {
                        NotificationHelper.showNotification(
                            context = applicationContext,
                            alert = it,
                            fetchedAt = fetchedAt,
                            publishedAt = generatedAtJst
                        )
                    }
                } else {
                    NotificationHelper.showStatusNotification(
                        context = applicationContext,
                        title = "新しい脆弱性情報はありません",
                        message = "取得日時: $fetchedAt\n合計件数: ${alerts.size}",
                        iconRes = R.drawable.shield_check,
                        notificationId = 9001
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            val fetchedAt = formatNow()

            val prefs = applicationContext.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE
            )

            prefs.edit()
                .putString(lastStatusKey, "[$fetchedAt] 定期取得エラー")
                .putString(lastErrorKey, e.message ?: e.javaClass.simpleName)
                .putLong(lastCheckedAtKey, System.currentTimeMillis())
                .apply()

            NotificationHelper.showStatusNotification(
                context = applicationContext,
                title = "脆弱性情報の取得に失敗しました",
                message = "アプリまたはGitHubで問題が発生したため、取得に失敗しました",
                iconRes = R.drawable.alert_circle,
                notificationId = 9002,
                useUrgentChannel = true
            )

            Result.retry()
        }
    }

    private fun fetchText(urlText: String): String {
        val connection = URL(urlText).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseAlerts(jsonText: String): List<CveAlert> {
        val root = JSONObject(jsonText)
        val alertsArray = root.getJSONArray("alerts")
        val alerts = mutableListOf<CveAlert>()

        for (i in 0 until alertsArray.length()) {
            val item = alertsArray.getJSONObject(i)

            alerts.add(
                CveAlert(
                    alertId = item.optString("alert_id"),
                    source = item.optString("source"),
                    category = item.optString("category"),
                    priority = item.optString("priority"),
                    cveId = item.optString("cve_id"),
                    matched = item.optString("matched"),
                    severity = item.optString("severity"),
                    score = item.optString("score"),
                    published = item.optString("published"),
                    lastModified = item.optString("last_modified"),
                    title = item.optString("title", item.optString("cve_id")),
                    description = item.optString("description"),
                    url = item.optString("url")
                )
            )
        }

        return alerts
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val urgentChannel = NotificationChannel(
            urgentChannelId,
            "緊急CVE通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "CRITICALまたは最優先で確認が必要なCVEを通知します"
        }

        val watchChannel = NotificationChannel(
            watchChannelId,
            "重要CVE通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "HIGH相当など確認が必要なCVEと取得状況を通知します"
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(urgentChannel)
        manager.createNotificationChannel(watchChannel)
    }

    private fun formatNow(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        return formatter.format(LocalDateTime.now())
    }

    private fun formatGeneratedAtToJst(utcText: String): String {
        if (utcText.isBlank()) return "-"

        return try {
            val input = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                java.util.Locale.US
            )
            input.timeZone = java.util.TimeZone.getTimeZone("UTC")

            val date = input.parse(utcText)

            val output = java.text.SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss",
                java.util.Locale.JAPAN
            )
            output.timeZone = java.util.TimeZone.getTimeZone("Asia/Tokyo")

            if (date != null) output.format(date) else utcText
        } catch (e: Exception) {
            utcText
        }
    }
}
