package com.zfblistener

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var urlEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlEditText = findViewById(R.id.urlEditText)
        saveButton = findViewById(R.id.saveButton)
        statusText = findViewById(R.id.statusText)
        permissionButton = findViewById(R.id.permissionButton)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        urlEditText.setText(prefs.getString("target_url", "http://hot583.com/shop/zfbdz/index.php?data="))

        saveButton.setOnClickListener {
            val url = urlEditText.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "URL 不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("target_url", url).apply()
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        }

        permissionButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = NotificationListener.isNotificationListenerEnabled(this)
        statusText.text = if (enabled) "监听状态: 已开启" else "监听状态: 未开启"
        statusText.setTextColor(
            if (enabled) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt()
        )
    }
}
