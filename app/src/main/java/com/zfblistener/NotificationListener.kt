package com.zfblistener

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import androidx.preference.PreferenceManager
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NotificationListener : NotificationListenerService() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != PACKAGE_ALIPAY) return

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sentSet = prefs.getStringSet(SENT_KEYS, mutableSetOf()) ?: mutableSetOf()
        val key = "${sbn.tag ?: ""}:${sbn.id}"
        if (sentSet.contains(key)) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val content = "$title $text $bigText".trim()
        if (content.isBlank()) return

        val snippet = if (content.length > 30) content.take(30) + "…" else content
        LogHelper.add(this, "收到: $snippet")

        if (!KEYWORDS.any { content.contains(it) }) {
            LogHelper.add(this, "忽略: 无关 $snippet")
            return
        }

        val url = prefs.getString("target_url", DEFAULT_URL) ?: DEFAULT_URL
        val targetUrl = url + URLEncoder.encode(
            Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP), "UTF-8")

        LogHelper.add(this, "发送… $snippet")

        try {
            val resp = client.newCall(Request.Builder().url(targetUrl).get().build()).execute()
            if (resp.isSuccessful && resp.body?.string() == "ok") {
                sentSet.add(key)
                if (sentSet.size > 200) {
                    sentSet.remove(sentSet.first())
                }
                prefs.edit().putStringSet(SENT_KEYS, sentSet).apply()
                LogHelper.add(this, "成功: $snippet")
            } else {
                LogHelper.add(this, "服务器异常: ${resp.code}")
            }
            resp.close()
        } catch (e: Exception) {
            LogHelper.add(this, "失败: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    companion object {
        private const val PACKAGE_ALIPAY = "com.eg.android.AlipayGphone"
        private const val SENT_KEYS = "sent_notification_keys"
        private const val DEFAULT_URL = "http://hot583.com/shop/zfbdz/index.php?data="
        private val KEYWORDS = listOf("到账", "收款", "收到转账", "转账转入", "转入", "向你转了")

        fun isNotificationListenerEnabled(context: Context): Boolean {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val cn = ComponentName(context, NotificationListener::class.java)
            return nm.isNotificationListenerAccessGranted(cn)
        }
    }
}
