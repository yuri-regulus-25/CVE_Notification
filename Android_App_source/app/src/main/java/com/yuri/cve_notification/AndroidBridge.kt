package com.yuri.cve_notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone

class AndroidBridge(
    private val context: Context
) {
    private val alertsUrl =
        "https://yuri-regulus-25.github.io/Operate_CVE_Notification/alerts.json"

    private val prefsName = "cve_alert_prefs"
    private val knownIdsKey = "known_alert_ids"
    private val ignoredIdsKey = "ignored_alert_ids"
    private val latestJsonKey = "latest_alerts_json"
    private val lastStatusKey = "last_fetch_status"
    private val lastCheckedAtKey = "last_checked_at"
    private val lastErrorKey = "last_fetch_error"
    private val hasFetchedOnceKey = "has_fetched_once"

    @JavascriptInterface
    fun getAppState(): String {
        return try {
            buildAppStateJson().toString()
        } catch (e: Exception) {
            errorJson("状態取得エラー", e)
        }
    }

    @JavascriptInterface
    fun fetchAlerts(): String {
        return try {
            val jsonText = fetchText(alertsUrl)
            val root = JSONObject(jsonText)
            val alerts = parseAlerts(root)

            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val knownIds = prefs.getStringSet(knownIdsKey, emptySet()) ?: emptySet()
            val ignoredIds = prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()
            val hasFetchedOnce = prefs.getBoolean(hasFetchedOnceKey, false)

            val currentAlertIds = alerts.map { it.alertId }.toSet()
            val cleanedKnownIds = knownIds.filter { it in currentAlertIds }.toSet()

            val fetchedAt = formatMillisToJst(System.currentTimeMillis())
            val generatedAtJst = formatToJst(root.optString("generated_at", ""))

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

            val updatedKnownIds = knownIds.toMutableSet().apply {
                addAll(alerts.map { it.alertId })
            }

            if (isFirstRun) {
                NotificationHelper.showStatusNotification(
                    context = context,
                    title = "初回の脆弱性情報を取得しました",
                    message = "取得日時: $fetchedAt\n合計件数: ${alerts.size}",
                    iconRes = R.drawable.shield_check,
                    notificationId = 9003
                )
            } else {
                if (newAlerts.isNotEmpty()) {
                    newAlerts.forEach {
                        NotificationHelper.showNotification(
                            context = context,
                            alert = it,
                            fetchedAt = fetchedAt,
                            publishedAt = generatedAtJst
                        )
                    }
                } else {
                    NotificationHelper.showStatusNotification(
                        context = context,
                        title = "新しい脆弱性情報はありません",
                        message = "取得日時: $fetchedAt\n合計件数: ${alerts.size}",
                        iconRes = R.drawable.shield_check,
                        notificationId = 9001
                    )
                }
            }

            val now = System.currentTimeMillis()
            prefs.edit()
                .putBoolean(hasFetchedOnceKey, true)
                .putStringSet(knownIdsKey, currentAlertIds)
                .putStringSet(ignoredIdsKey, cleanedIgnoredIds)
                .putString(latestJsonKey, jsonText)
                .putString(lastStatusKey, if (isFirstRun) "初回取得完了" else "取得完了")
                .putLong(lastCheckedAtKey, now)
                .remove(lastErrorKey)
                .apply()

            buildAppStateJson(
                statusOverride = if (isFirstRun) {
                    "初回取得完了。既存 ${alerts.size} 件を既読登録"
                } else {
                    "取得完了。新規 ${newAlerts.size} 件 / 全体 ${alerts.size} 件"
                }
            ).toString()
        } catch (e: Exception) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(lastStatusKey, "取得エラー")
                .putString(lastErrorKey, e.message ?: e.javaClass.simpleName)
                .putLong(lastCheckedAtKey, System.currentTimeMillis())
                .apply()

            errorJson("取得エラー", e)
        }
    }

    @JavascriptInterface
    fun ignoreAlert(alertId: String): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val ignoredIds = prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()
        val updated = ignoredIds.toMutableSet().apply { add(alertId) }

        prefs.edit()
            .putStringSet(ignoredIdsKey, updated)
            .putString(lastStatusKey, "非通知設定済")
            .apply()

        return buildAppStateJson(statusOverride = "非通知設定済").toString()
    }

    @JavascriptInterface
    fun unignoreAlert(alertId: String): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val ignoredIds = prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()
        val updated = ignoredIds.toMutableSet().apply { remove(alertId) }

        prefs.edit()
            .putStringSet(ignoredIdsKey, updated)
            .putString(lastStatusKey, "非通知設定解除済")
            .apply()

        return buildAppStateJson(statusOverride = "非通知設定解除済").toString()
    }

    @JavascriptInterface
    fun clearIgnoredAlerts(): String {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(ignoredIdsKey, emptySet())
            .putString(lastStatusKey, "全ての非通知設定を解除済")
            .apply()

        return buildAppStateJson(statusOverride = "全ての非通知設定を解除済").toString()
    }

    @JavascriptInterface
    fun openUrl(url: String) {
        if (url.isBlank()) return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun buildAppStateJson(statusOverride: String? = null): JSONObject {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val jsonText = prefs.getString(latestJsonKey, null)
        val ignoredIds = prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()
        val lastCheckedAt = prefs.getLong(lastCheckedAtKey, 0L)
        val lastError = prefs.getString(lastErrorKey, "") ?: ""

        val root = if (jsonText.isNullOrBlank()) {
            JSONObject()
                .put("generated_at", "")
                .put("count", 0)
                .put("sources", JSONArray())
                .put("alerts", JSONArray())
        } else {
            JSONObject(jsonText)
        }

        val alerts = parseAlerts(root)
        val displayAlerts = JSONArray()
        val referenceMillis =
            if (lastCheckedAt > 0L) lastCheckedAt else System.currentTimeMillis()

        alerts.forEach { alert ->
            displayAlerts.put(
                JSONObject()
                    .put("alert_id", alert.alertId)
                    .put("source", alert.source)
                    .put("category", alert.category)
                    .put("priority", alert.priority)
                    .put("cve_id", alert.cveId)
                    .put("matched", alert.matched)
                    .put("severity", alert.severity)
                    .put("score", alert.score)
                    .put("published", alert.published)
                    .put("last_modified", alert.lastModified)
                    .put("is_new", isWithinOneDay(alert.published, referenceMillis))
                    .put("is_updated", isWithinOneDay(alert.lastModified, referenceMillis))
                    .put("title", alert.title)
                    .put("description", alert.description)
                    .put("url", alert.url)
                    .put("ignored", alert.alertId in ignoredIds)
            )
        }

        val generatedAt = root.optString("generated_at", "")
        val notificationCount = alerts.count { it.alertId !in ignoredIds }

        return JSONObject()
            .put("ok", true)
            .put("status", statusOverride ?: prefs.getString(lastStatusKey, "未取得"))
            .put("last_error", lastError)
            .put("generated_at", generatedAt)
            .put("generated_at_jst", formatToJst(generatedAt))
            .put("last_checked_at", lastCheckedAt)
            .put("last_checked_at_jst", formatMillisToJst(lastCheckedAt))
            .put("stale", isGeneratedAtStale(generatedAt))
            .put("count", root.optInt("count", alerts.size))
            .put("display_count", alerts.size)
            .put("notification_count", notificationCount)
            .put("ignored_count", ignoredIds.size)
            .put("sources", root.optJSONArray("sources") ?: JSONArray())
            .put("alerts", displayAlerts)
    }

    private fun parseAlerts(root: JSONObject): List<CveAlert> {
        val alertsArray = root.optJSONArray("alerts") ?: return emptyList()
        val alerts = mutableListOf<CveAlert>()

        for (i in 0 until alertsArray.length()) {
            val item = alertsArray.optJSONObject(i) ?: continue
            alerts.add(
                CveAlert(
                    alertId = item.optString("alert_id", "unknown-$i"),
                    source = item.optString("source", "UNKNOWN"),
                    category = item.optString("category", "UNKNOWN"),
                    priority = item.optString("priority", "INFO"),
                    cveId = item.optString("cve_id", "UNKNOWN"),
                    matched = item.optString("matched", ""),
                    severity = item.optString("severity", "UNKNOWN"),
                    score = item.optString("score", ""),
                    published = item.optString("published", ""),
                    lastModified = item.optString("last_modified", ""),
                    title = item.optString("title", item.optString("cve_id", "UNKNOWN")),
                    description = item.optString("description", ""),
                    url = item.optString("url", "")
                )
            )
        }

        return alerts
    }

    private fun fetchText(urlText: String): String {
        val connection = URL(urlText).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun formatToJst(utcText: String): String {
        if (utcText.isBlank()) return "-"

        return try {
            val sdf = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                Locale.US
            )
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val date = sdf.parse(utcText)

            val out = SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss",
                Locale.JAPAN
            )
            out.timeZone = TimeZone.getTimeZone("Asia/Tokyo")

            if (date != null) out.format(date) else utcText
        } catch (e: Exception) {
            utcText
        }
    }

    private fun formatMillisToJst(millis: Long): String {
        if (millis <= 0L) return "-"

        val out = SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss",
            Locale.JAPAN
        )
        out.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        return out.format(millis)
    }

    private fun isGeneratedAtStale(utcText: String): Boolean {
        if (utcText.isBlank()) return false

        return try {
            val sdf = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                Locale.US
            )
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val generatedDate = sdf.parse(utcText) ?: return true
            val diffMillis = System.currentTimeMillis() - generatedDate.time
            val staleThresholdMillis = 24L * 60L * 60L * 1000L

            diffMillis > staleThresholdMillis
        } catch (e: Exception) {
            true
        }
    }

    private fun isWithinOneDay(dateText: String, referenceMillis: Long): Boolean {
        val targetMillis = parseAlertMillis(dateText) ?: return false
        val diffMillis = referenceMillis - targetMillis

        return diffMillis in 0..(24L * 60L * 60L * 1000L)
    }

    private fun parseAlertMillis(dateText: String): Long? {
        if (dateText.isBlank()) return null

        return try {
            when {
                dateText.endsWith("Z") -> Instant.parse(dateText).toEpochMilli()
                dateText.contains("+") || dateText.matches(Regex(".*-\\d\\d:\\d\\d$")) ->
                    OffsetDateTime.parse(dateText).toInstant().toEpochMilli()
                else ->
                    LocalDateTime.parse(dateText).toInstant(ZoneOffset.UTC).toEpochMilli()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun errorJson(status: String, e: Exception): String {
        return JSONObject()
            .put("ok", false)
            .put("status", status)
            .put("message", e.message ?: e.javaClass.simpleName)
            .toString()
    }
}
