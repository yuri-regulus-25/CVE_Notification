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

object NotificationHelper {

    private const val urgentChannelId = "cve_urgent_channel"
    private const val watchChannelId = "cve_watch_channel"

    fun showNotification(
        context: Context,
        alert: CveAlert,
        fetchedAt: String,
        publishedAt: String
    ) {
        createChannels(context)

        if (!canNotify(context)) return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(alert.url))

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = if (alert.priority == "URGENT" || alert.severity == "CRITICAL") {
            urgentChannelId
        } else {
            watchChannelId
        }

        val title =
            "新しい脆弱性情報があります: [${alert.severity}] ${alert.cveId}"

        val message = buildString {
            append("取得日時: ").append(fetchedAt).append("\n")
            append("公開日時: ").append(publishedAt).append("\n")
            append("カテゴリ: ").append(alert.category).append("\n")
            append("CVSS v3: ").append(alert.score.ifBlank { "-" }).append("\n")
            append(alert.description.ifBlank { "詳細情報はありません" })
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.shield_alert)
            .setContentTitle(title)
            .setContentText("${alert.category} / CVSS ${alert.score.ifBlank { "-" }}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(alert.alertId.hashCode(), notification)
    }

    fun showStatusNotification(
        context: Context,
        title: String,
        message: String,
        iconRes: Int = R.drawable.shield_check,
        notificationId: Int = 9999,
        useUrgentChannel: Boolean = false
    ) {
        createChannels(context)

        if (!canNotify(context)) return

        val intent = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = if (useUrgentChannel) urgentChannelId else watchChannelId

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun canNotify(context: Context): Boolean {
        return !(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                )
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val urgentChannel = NotificationChannel(
            urgentChannelId,
            "CVE URGENT",
            NotificationManager.IMPORTANCE_HIGH
        )

        val watchChannel = NotificationChannel(
            watchChannelId,
            "CVE WATCH",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(urgentChannel)
        manager.createNotificationChannel(watchChannel)
    }
}