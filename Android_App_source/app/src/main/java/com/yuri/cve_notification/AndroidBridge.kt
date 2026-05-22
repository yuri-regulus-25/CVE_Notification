package com.yuri.cve_notification

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
import java.util.concurrent.Executors

class AndroidBridge(
    context: Context,
    private val webView: WebView,
    private val swipeRefreshLayout: SwipeRefreshLayout
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile
    private var closed = false

    private val alertsUrl =
        "https://yuri-regulus-25.github.io/Operate_CVE_Notification/alerts.json"

    private val prefsName = "cve_alert_prefs"
    private val knownIdsKey = "known_alert_ids"
    private val ignoredIdsKey = "ignored_alert_ids"
    private val pinnedIdsKey = "pinned_alert_ids"
    private val areaOverridesKey = "area_overrides_json"
    private val detectedDatesKey = "detected_dates_json"
    private val updatedDatesKey = "updated_dates_json"
    private val lastModifiedByIdKey = "last_modified_by_id_json"
    private val latestJsonKey = "latest_alerts_json"
    private val lastStatusKey = "last_fetch_status"
    private val lastCheckedAtKey = "last_checked_at"
    private val lastErrorKey = "last_fetch_error"
    private val hasFetchedOnceKey = "has_fetched_once"

    @JavascriptInterface
    fun getAppState(requestId: String) {
        executeBridgeCall(requestId, "状態取得エラー") {
            buildAppStateJson().toString()
        }
    }

    @JavascriptInterface
    fun fetchAlerts(requestId: String) {
        executeBridgeCall(requestId, "取得エラー") {
            fetchAlertsJson()
        }
    }

    @JavascriptInterface
    fun ignoreAlert(requestId: String, alertId: String) {
        executeBridgeCall(requestId, "非通知設定エラー") {
            updateIgnoredAlert(alertId, ignored = true)
        }
    }

    @JavascriptInterface
    fun unignoreAlert(requestId: String, alertId: String) {
        executeBridgeCall(requestId, "非通知設定解除エラー") {
            updateIgnoredAlert(alertId, ignored = false)
        }
    }

    @JavascriptInterface
    fun clearIgnoredAlerts(requestId: String) {
        executeBridgeCall(requestId, "非通知設定解除エラー") {
            val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit()
                .putStringSet(ignoredIdsKey, emptySet())
                .putString(lastStatusKey, "全ての非通知設定を解除済")
                .apply()

            buildAppStateJson(statusOverride = "全ての非通知設定を解除済").toString()
        }
    }

    @JavascriptInterface
    fun pinAlert(requestId: String, alertId: String) {
        executeBridgeCall(requestId, "ピン留め設定エラー") {
            updateDisplayState(alertId, "pinned", "ピン留め済")
        }
    }

    @JavascriptInterface
    fun unpinAlert(requestId: String, alertId: String) {
        executeBridgeCall(requestId, "ピン留め解除エラー") {
            updateDisplayState(alertId, "visible", "ピン留め解除済")
        }
    }

    @JavascriptInterface
    fun hideAlert(requestId: String, alertId: String) {
        executeBridgeCall(requestId, "非表示設定エラー") {
            updateDisplayState(alertId, "hidden", "非表示設定済")
        }
    }

    @JavascriptInterface
    fun showAlert(requestId: String, alertId: String) {
        executeBridgeCall(requestId, "表示設定エラー") {
            updateDisplayState(alertId, "visible", "表示設定済")
        }
    }

    @JavascriptInterface
    fun setAlertArea(requestId: String, alertId: String, area: String) {
        executeBridgeCall(requestId, "対応領域更新エラー") {
            updateAreaOverride(alertId, area)
        }
    }

    @JavascriptInterface
    fun setPullRefreshEnabled(enabled: Boolean) {
        mainHandler.post {
            swipeRefreshLayout.isEnabled = enabled
        }
    }

    @JavascriptInterface
    fun openUrl(url: String) {
        if (url.isBlank()) return

        mainHandler.post {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        }
    }

    private fun executeBridgeCall(
        requestId: String,
        errorStatus: String,
        action: () -> String
    ) {
        if (closed) return

        executor.execute {
            if (closed) return@execute

            val result = try {
                action()
            } catch (e: Exception) {
                errorJson(errorStatus, e)
            }

            if (!closed) {
                postBridgeResult(requestId, result)
            }
        }
    }

    private fun postBridgeResult(requestId: String, jsonText: String) {
        if (closed) return

        val script =
            "window.onAndroidBridgeResult && window.onAndroidBridgeResult(" +
                "${JSONObject.quote(requestId)}, ${JSONObject.quote(jsonText)})"

        mainHandler.post {
            if (!closed) {
                webView.evaluateJavascript(script, null)
            }
        }
    }

    fun close() {
        closed = true
        executor.shutdownNow()
    }

    private fun fetchAlertsJson(): String {
        return try {
            val jsonText = fetchText(alertsUrl)
            val root = JSONObject(jsonText)
            val alerts = parseAlerts(root)

            val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val knownIds = prefs.getStringSet(knownIdsKey, emptySet()) ?: emptySet()
            val ignoredIds = prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()
            val pinnedIds = prefs.getStringSet(pinnedIdsKey, emptySet()) ?: emptySet()
            val hasFetchedOnce = prefs.getBoolean(hasFetchedOnceKey, false)
            val detectedDates = readJsonObject(prefs, detectedDatesKey)
            val updatedDates = readJsonObject(prefs, updatedDatesKey)
            val lastModifiedById = readJsonObject(prefs, lastModifiedByIdKey)
            val now = System.currentTimeMillis()

            val currentAlertIds = alerts.map { it.alertId }.toSet()
            val cleanedKnownIds = knownIds.filter { it in currentAlertIds }.toSet()
            val cleanedPinnedIds = pinnedIds.filter { it in currentAlertIds }.toSet()

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

            alerts.forEach { alert ->
                val existedBefore = alert.alertId in knownIds

                if (!detectedDates.has(alert.alertId)) {
                    val detectedMillis =
                        if (existedBefore) {
                            parseAlertMillis(alert.published) ?: 0L
                        } else {
                            now
                        }
                    detectedDates.put(alert.alertId, detectedMillis)
                }

                val previousLastModified = lastModifiedById.optString(alert.alertId, "")
                if (previousLastModified.isBlank()) {
                    updatedDates.put(alert.alertId, parseAlertMillis(alert.lastModified) ?: 0L)
                } else if (previousLastModified != alert.lastModified) {
                    updatedDates.put(alert.alertId, now)
                }

                lastModifiedById.put(alert.alertId, alert.lastModified)
            }

            val cleanedDetectedDates = retainJsonKeys(detectedDates, currentAlertIds)
            val cleanedUpdatedDates = retainJsonKeys(updatedDates, currentAlertIds)
            val cleanedLastModifiedById = retainJsonKeys(lastModifiedById, currentAlertIds)

            if (isFirstRun) {
                NotificationHelper.showStatusNotification(
                    context = appContext,
                    title = "初回の脆弱性情報を取得しました",
                    message = "取得日時: $fetchedAt\n合計件数: ${alerts.size}",
                    iconRes = R.drawable.shield_check,
                    notificationId = 9003
                )
            } else {
                if (newAlerts.isNotEmpty()) {
                    newAlerts.forEach {
                        NotificationHelper.showNotification(
                            context = appContext,
                            alert = it,
                            fetchedAt = fetchedAt,
                            publishedAt = generatedAtJst
                        )
                    }
                } else {
                    NotificationHelper.showStatusNotification(
                        context = appContext,
                        title = "新しい脆弱性情報はありません",
                        message = "取得日時: $fetchedAt\n合計件数: ${alerts.size}",
                        iconRes = R.drawable.shield_check,
                        notificationId = 9001
                    )
                }
            }

            prefs.edit()
                .putBoolean(hasFetchedOnceKey, true)
                .putStringSet(knownIdsKey, currentAlertIds)
                .putStringSet(ignoredIdsKey, cleanedIgnoredIds)
                .putStringSet(pinnedIdsKey, cleanedPinnedIds)
                .putString(detectedDatesKey, cleanedDetectedDates.toString())
                .putString(updatedDatesKey, cleanedUpdatedDates.toString())
                .putString(lastModifiedByIdKey, cleanedLastModifiedById.toString())
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
            val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(lastStatusKey, "取得エラー")
                .putString(lastErrorKey, e.message ?: e.javaClass.simpleName)
                .putLong(lastCheckedAtKey, System.currentTimeMillis())
                .apply()

            errorJson("取得エラー", e)
        }
    }

    private fun updateIgnoredAlert(alertId: String, ignored: Boolean): String {
        val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val ignoredIds = prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()
        val updated = ignoredIds.toMutableSet().apply {
            if (ignored) {
                add(alertId)
            } else {
                remove(alertId)
            }
        }
        val status = if (ignored) "非通知設定済" else "非通知設定解除済"

        prefs.edit()
            .putStringSet(ignoredIdsKey, updated)
            .putString(lastStatusKey, status)
            .apply()

        return buildAppStateJson(statusOverride = status).toString()
    }

    private fun updateDisplayState(alertId: String, displayState: String, status: String): String {
        val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val hiddenIds = (prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()).toMutableSet()
        val pinnedIds = (prefs.getStringSet(pinnedIdsKey, emptySet()) ?: emptySet()).toMutableSet()

        when (displayState) {
            "pinned" -> {
                pinnedIds.add(alertId)
                hiddenIds.remove(alertId)
            }
            "hidden" -> {
                hiddenIds.add(alertId)
                pinnedIds.remove(alertId)
            }
            else -> {
                hiddenIds.remove(alertId)
                pinnedIds.remove(alertId)
            }
        }

        prefs.edit()
            .putStringSet(ignoredIdsKey, hiddenIds)
            .putStringSet(pinnedIdsKey, pinnedIds)
            .putString(lastStatusKey, status)
            .apply()

        return buildAppStateJson(statusOverride = status).toString()
    }

    private fun updateAreaOverride(alertId: String, area: String): String {
        val normalizedArea = normalizeArea(area)
        val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val overrides = readJsonObject(prefs, areaOverridesKey)

        if (normalizedArea.isBlank()) {
            overrides.remove(alertId)
        } else {
            overrides.put(alertId, normalizedArea)
        }

        prefs.edit()
            .putString(areaOverridesKey, overrides.toString())
            .putString(lastStatusKey, "対応領域更新済")
            .apply()

        return buildAppStateJson(statusOverride = "対応領域更新済").toString()
    }

    private fun buildAppStateJson(statusOverride: String? = null): JSONObject {
        val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val jsonText = prefs.getString(latestJsonKey, null)
        val ignoredIds = prefs.getStringSet(ignoredIdsKey, emptySet()) ?: emptySet()
        val pinnedIds = prefs.getStringSet(pinnedIdsKey, emptySet()) ?: emptySet()
        val areaOverrides = readJsonObject(prefs, areaOverridesKey)
        val detectedDates = readJsonObject(prefs, detectedDatesKey)
        val updatedDates = readJsonObject(prefs, updatedDatesKey)
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
        val referenceMillis = System.currentTimeMillis()

        alerts.forEach { alert ->
            val displayState = when {
                alert.alertId in pinnedIds -> "pinned"
                alert.alertId in ignoredIds -> "hidden"
                else -> "visible"
            }
            val area = areaOverrides.optString(alert.alertId, detectArea(alert))
            val detectedMillis = detectedDates.optLong(
                alert.alertId,
                parseAlertMillis(alert.published) ?: 0L
            )
            val updatedMillis = updatedDates.optLong(
                alert.alertId,
                parseAlertMillis(alert.lastModified) ?: 0L
            )
            val isNew = isWithinOneDay(detectedMillis, referenceMillis)
            val isUpdated = !isNew && isWithinOneDay(updatedMillis, referenceMillis)

            displayAlerts.put(
                JSONObject()
                    .put("alert_id", alert.alertId)
                    .put("source", alert.source)
                    .put("category", alert.category)
                    .put("area", area)
                    .put("area_label", areaLabel(area))
                    .put("display_state", displayState)
                    .put("priority", alert.priority)
                    .put("cve_id", alert.cveId)
                    .put("matched", alert.matched)
                    .put("severity", alert.severity)
                    .put("score", alert.score)
                    .put("published", alert.published)
                    .put("last_modified", alert.lastModified)
                    .put("detected_date", formatMillisToIso(detectedMillis))
                    .put("updated_date", formatMillisToIso(updatedMillis))
                    .put("is_new", isNew)
                    .put("is_updated", isUpdated)
                    .put("title", alert.title)
                    .put("description", alert.description)
                    .put("url", alert.url)
                    .put("pinned", displayState == "pinned")
                    .put("hidden", displayState == "hidden")
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
            .put("hidden_count", ignoredIds.size)
            .put("pinned_count", pinnedIds.size)
            .put("sources", root.optJSONArray("sources") ?: JSONArray())
            .put("alerts", displayAlerts)
    }

    private fun readJsonObject(prefs: SharedPreferences, key: String): JSONObject {
        val jsonText = prefs.getString(key, null)
        if (jsonText.isNullOrBlank()) return JSONObject()

        return try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun retainJsonKeys(json: JSONObject, allowedKeys: Set<String>): JSONObject {
        val retained = JSONObject()

        json.keys().forEach { key ->
            if (key in allowedKeys) {
                retained.put(key, json.opt(key))
            }
        }

        return retained
    }

    private fun detectArea(alert: CveAlert): String {
        val text = listOf(
            alert.matched,
            alert.title,
            alert.description,
            alert.category
        ).joinToString(" ").lowercase(Locale.US)

        return when {
            containsAny(
                text,
                listOf("windows", "linux", "apache", "iis", "openssl", "kernel", "nginx", "openssh", "ubuntu", "debian", "red hat")
            ) -> "infra"
            containsAny(
                text,
                listOf("php", "laravel", "npm", "vue", "node.js", "nodejs", "node ", "javascript", "react", "angular", "spring")
            ) -> "dev"
            containsAny(
                text,
                listOf("postgresql", "postgres", "redis", "docker", "mysql", "mariadb", "mongodb", "kubernetes", "container")
            ) -> "common"
            else -> "uncategorized"
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun normalizeArea(area: String): String {
        return when (area) {
            "infra", "dev", "common", "uncategorized", "out_of_scope" -> area
            else -> ""
        }
    }

    private fun areaLabel(area: String): String {
        return when (area) {
            "infra" -> "インフラ"
            "dev" -> "開発"
            "common" -> "共通"
            "out_of_scope" -> "領域外"
            else -> "未分類"
        }
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

    private fun formatMillisToIso(millis: Long): String {
        if (millis <= 0L) return ""

        return Instant.ofEpochMilli(millis).toString()
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
        return isWithinOneDay(targetMillis, referenceMillis)
    }

    private fun isWithinOneDay(targetMillis: Long, referenceMillis: Long): Boolean {
        if (targetMillis <= 0L) return false

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
