package com.zfblistener

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.util.Base64
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AlipayAccessibilityService : AccessibilityService() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName ?: return
        if (pkg != PACKAGE_ALIPAY) return

        val notification = event.parcelableData as? Notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val content = "$title $text $bigText".trim()
        if (content.isBlank()) return

        val snippet = if (content.length > 30) content.take(30) + "…" else content
        LogHelper.add(this, "收到: $snippet")

        if (!containsKeyword(content)) {
            LogHelper.add(this, "忽略: 无关 $snippet")
            return
        }

        val url = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("target_url", DEFAULT_URL) ?: DEFAULT_URL
        val targetUrl = url + URLEncoder.encode(
            Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP), "UTF-8")

        LogHelper.add(this, "发送… $snippet")

        try {
            val resp = client.newCall(Request.Builder().url(targetUrl).get().build()).execute()
            if (resp.isSuccessful && resp.body?.string() == "ok") {
                LogHelper.add(this, "成功: $snippet")
            } else {
                LogHelper.add(this, "服务器异常: ${resp.code}")
            }
            resp.close()
        } catch (e: Exception) {
            LogHelper.add(this, "失败: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onInterrupt() {}

    private fun containsKeyword(text: String): Boolean {
        return KEYWORDS.any { text.contains(it) }
    }

    companion object {
        var isRunning = false

        private const val PACKAGE_ALIPAY = "com.eg.android.AlipayGphone"
        private const val DEFAULT_URL = "http://hot583.com/shop/zfbdz/index.php?data="
        private val KEYWORDS = listOf("到账", "收款", "收到转账", "转账转入", "转入", "向你转了")
    }
}
