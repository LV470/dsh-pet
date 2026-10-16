package com.dsh.pet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var keyInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (resources.displayMetrics.density * 20).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        val title = TextView(this).apply {
            text = "🐋 DSH 桌宠"
            textSize = 22f
        }
        root.addView(title)

        statusView = TextView(this).apply { textSize = 14f; setPadding(0, pad, 0, pad) }
        root.addView(statusView)

        val keyLabel = TextView(this).apply { text = "DeepSeek API Key（用于查余额）" }
        root.addView(keyLabel)

        keyInput = EditText(this).apply {
            hint = "sk-..."
            setSingleLine()
        }
        keyInput.setText(Prefs.getKey(this))
        root.addView(keyInput)

        val saveBtn = Button(this).apply { text = "保存 Key" }
        saveBtn.setOnClickListener {
            Prefs.setKey(this, keyInput.text.toString().trim())
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            refresh()
        }
        root.addView(saveBtn)

        val startBtn = Button(this).apply { text = "启动桌宠" }
        startBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                Toast.makeText(this, "请先允许「显示在其他应用上层」", Toast.LENGTH_LONG).show()
            } else {
                startService(Intent(this, PetService::class.java))
                Toast.makeText(this, "桌宠已启动", Toast.LENGTH_SHORT).show()
                refresh()
            }
        }
        root.addView(startBtn)

        val stopBtn = Button(this).apply { text = "停止桌宠" }
        stopBtn.setOnClickListener {
            stopService(Intent(this, PetService::class.java))
            refresh()
        }
        root.addView(stopBtn)

        val notifBtn = Button(this).apply { text = "申请通知权限（保活用）" }
        notifBtn.setOnClickListener { askNotification() }
        root.addView(notifBtn)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val overlay = if (Settings.canDrawOverlays(this)) "✅ 已授权" else "❌ 未授权"
        val key = if (Prefs.getKey(this).isNullOrBlank()) "❌ 未填" else "✅ 已填"
        statusView.text = "悬浮窗权限：$overlay\nAPI Key：$key"
    }

    private fun askNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1001)
        } else {
            Toast.makeText(this, "当前系统无需申请", Toast.LENGTH_SHORT).show()
        }
    }
}
