package com.zfblistener

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import androidx.preference.PreferenceManager
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

        val tag = sbn.tag ?: ""
        val id = sbn.id
        val key = "$tag:$id"

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sentSet = getSentSet(prefs)
        if (sentSet.contains(key)) return

        val notification = sbn.notification
        val extras = notification.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val content = "$title $text $bigText".trim()
        if (content.isBlank()) return
        if (!containsKeyword(content)) return

        val encoded = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
        val baseUrl = prefs.getString("target_url", DEFAULT_URL) ?: DEFAULT_URL
        val targetUrl = baseUrl + encoded

        Log.d(TAG, "Sending: $targetUrl")

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.close()
            addToSentSet(prefs, sentSet, key)
            Log.d(TAG, "Sent successfully, key=$key")
        } catch (e: Exception) {
            Log.e(TAG, "Send failed: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun containsKeyword(text: String): Boolean {
        val keywords = listOf("到账", "收款", "收到转账", "转账转入", "转入", "向你转了")
        return keywords.any { text.contains(it) }
    }

    private fun getSentSet(prefs: android.content.SharedPreferences): MutableSet<String> {
        return prefs.getStringSet(SENT_KEYS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    private fun addToSentSet(
        prefs: android.content.SharedPreferences,
        set: MutableSet<String>,
        key: String
    ) {
        set.add(key)
        if (set.size > 200) {
            val excess = set.size - 200
            val iter = set.iterator()
            repeat(excess) {
                if (iter.hasNext()) {
                    iter.next()
                    iter.remove()
                }
            }
        }
        prefs.edit().putStringSet(SENT_KEYS, set).apply()
    }

    companion object {
        private const val TAG = "ZFBNotification"
        private const val PACKAGE_ALIPAY = "com.eg.android.AlipayGphone"
        private const val SENT_KEYS = "sent_notification_keys"
        private const val DEFAULT_URL = "http://hot583.com/shop/zfbdz/index.php?data="

        fun isNotificationListenerEnabled(context: Context): Boolean {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val cn = ComponentName(context, NotificationListener::class.java)
            return nm.isNotificationListenerAccessGranted(cn)
        }
    }
}
