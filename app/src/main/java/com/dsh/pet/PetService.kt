package com.dsh.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class PetService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var petView: WebView
    private lateinit var lp: WindowManager.LayoutParams

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    /** 菜单打开时，触摸要放行给 WebView，否则按钮点不动 */
    private var menuOpen = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        petView = WebView(this).apply {
            // 必须在 loadUrl 之前设透明，否则白底（作者踩过的坑）
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            // file:// 页面里再加载 file:// 的 js/模型，不加这两条会被拦
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this@PetService))
                .build()

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?, request: WebResourceRequest?
                ): WebResourceResponse? {
                    request?.url?.let { return assetLoader.shouldInterceptRequest(it) }
                    return null
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    android.util.Log.i("DshPet", "页面加载完成: " + url)
                }
                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    // favicon 缺失是无害噪音，别拿它刷屏
                    val u = request?.url?.toString() ?: ""
                    if (u.contains("favicon")) return
                    val msg = "资源加载失败: " + (error?.description ?: "?") + " @ " + u
                    android.util.Log.e("DshPet", msg)
                    view?.post {
                        view.loadUrl("javascript:window.__showError&&__showError(" + quoteJs(msg) + ")")
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage?): Boolean {
                    android.util.Log.i("DshPet",
                        "console: " + (m?.message() ?: "") + " (" + (m?.lineNumber() ?: 0) + ")")
                    m?.message()?.let { text ->
                        if (text.contains("error", true) || text.contains("Uncaught", true)) {
                            petView.post { petView.loadUrl("javascript:window.__showError&&__showError(" + quoteJs(text) + ")") }
                        }
                    }
                    return true
                }
            }
            addJavascriptInterface(Bridge(), "DshPet")
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val (px, py) = Prefs.loadPos(this)
        val (winW, winH) = Prefs.loadSize(this)
        val winAlpha = Prefs.loadAlpha(this)
        lp = WindowManager.LayoutParams(
            dp(winW), dp(winH), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = px
            y = py
            // 不再用 lp.alpha：它会让整个窗口（含菜单）一起透明
            // 透明度改由页面内的 CSS 控制，只作用在形象上
        }

        // 拖动由服务端拦截（rawX/rawY 是屏幕绝对坐标，窗口移动时不会抖），
        // 点击则回吐给页面里的 onPetClick()
        val slop = dp(6)
        petView.setOnTouchListener { _, ev ->
            // 菜单/对话面板开着时，把触摸完全交给 WebView（否则按钮和滑块全废）
            if (menuOpen) return@setOnTouchListener false

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY
                    lastX = ev.rawX; lastY = ev.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging &&
                        (abs(ev.rawX - downX) > slop || abs(ev.rawY - downY) > slop)) {
                        dragging = true
                    }
                    if (dragging) {
                        lp.x += (ev.rawX - lastX).toInt()
                        lp.y += (ev.rawY - lastY).toInt()
                        try { wm.updateViewLayout(petView, lp) } catch (_: Throwable) {}
                    }
                    lastX = ev.rawX; lastY = ev.rawY
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        Prefs.savePos(this, lp.x, lp.y)
                    } else {
                        petView.evaluateJavascript("window.onPetClick&&onPetClick()", null)
                    }
                    dragging = false
                    true
                }
                else -> false
            }
        }

        wm.addView(petView, lp)
        petView.loadUrl("https://appassets.androidplatform.net/assets/pet.html")
    }

    override fun onDestroy() {
        try { if (::petView.isInitialized) wm.removeView(petView) } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_MIN)
            ch.setShowBadge(false)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    /** 暴露给 WebView 的接口 */
    inner class Bridge {

        /** 查 DeepSeek 余额，原样返回 JSON 文本（失败时返回 {"error":...}） */
        @JavascriptInterface
        fun balance(): String {
            val key = Prefs.getKey(this@PetService)
            if (key.isBlank()) return "{\"error\":\"no_key\"}"
            return try {
                val conn = (URL("https://api.deepseek.com/user/balance")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer " + key)
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = if (stream == null) "" else
                    BufferedReader(stream.reader()).use { it.readText() }
                if (code in 200..299) body
                else "{\"error\":\"http_" + code + "\"}"
            } catch (e: Exception) {
                "{\"error\":\"" + e.javaClass.simpleName + "\"}"
            }
        }

        /** 窗口按相对位移挪动（备用，主要拖动在服务端做） */
        @JavascriptInterface
        fun moveBy(dx: Int, dy: Int) {
            handler.post {
                lp.x += dx
                lp.y += dy
                try { wm.updateViewLayout(petView, lp) } catch (_: Throwable) {}
            }
        }

        @JavascriptInterface
        fun savePos() {
            handler.post { Prefs.savePos(this@PetService, lp.x, lp.y) }
        }

        @JavascriptInterface
        fun toast(msg: String) {
            handler.post {
                android.widget.Toast.makeText(this@PetService, msg,
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        /** 菜单开关状态：打开时 Android 层不再拦截触摸 */
        @JavascriptInterface
        fun setMenuOpen(open: Boolean) {
            menuOpen = open
        }

        /** 对话：先走 DeepSeek API（DSH 有信任栅栏，接入放下一版） */
        @JavascriptInterface
        fun ask(prompt: String): String {
            val key = Prefs.getKey(this@PetService)
            if (key.isBlank()) return "{\"error\":\"no_key\"}"
            return try {
                val conn = (URL("https://api.deepseek.com/chat/completions")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer " + key)
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 15000
                    readTimeout = 45000
                }
                val body = "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":" +
                        quoteJs(prompt) + "}],\"max_tokens\":300,\"temperature\":0.8}"
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = if (stream == null) "" else
                    BufferedReader(stream.reader()).use { it.readText() }
                if (code !in 200..299) {
                    "{\"error\":\"http_" + code + "\"}"
                } else {
                    val obj = org.json.JSONObject(text)
                    val reply = obj.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                    "{\"reply\":" + quoteJs(reply) + "}"
                }
            } catch (e: Exception) {
                "{\"error\":\"" + e.javaClass.simpleName + "\"}"
            }
        }

        /** 当前窗口配置（dp 尺寸 + 透明度） */
        @JavascriptInterface
        fun getConfig(): String {
            val dm = resources.displayMetrics
            val wdp = (lp.width / dm.density).toInt()
            val hdp = (lp.height / dm.density).toInt()
            val a = Prefs.loadAlpha(this@PetService)
            return "{\"w\":" + wdp + ",\"h\":" + hdp + ",\"alpha\":" + a + "}"
        }

        /** 调窗口大小，单位 dp */
        @JavascriptInterface
        fun setSize(w: Int, h: Int) {
            handler.post {
                val dm = resources.displayMetrics
                val nw = (w * dm.density).toInt()
                val nh = (h * dm.density).toInt()
                android.util.Log.i("DshPet", "setSize dp=" + w + "x" + h +
                    " px=" + nw + "x" + nh + " (旧 " + lp.width + "x" + lp.height + ")")
                lp.width = nw
                lp.height = nh
                val ok = try {
                    wm.updateViewLayout(petView, lp); true
                } catch (e: Exception) {
                    android.util.Log.e("DshPet", "updateViewLayout 失败: " + e)
                    false
                }
                android.util.Log.i("DshPet", "setSize 结果=" + ok +
                    " 实际=" + lp.width + "x" + lp.height)
                Prefs.saveSize(this@PetService, w, h)
            }
        }

        /** 调透明度 0.2 ~ 1.0 —— 只影响形象，不影响菜单 */
        @JavascriptInterface
        fun setAlpha(a: Float) {
            val v = if (a < 0.15f) 0.15f else if (a > 1f) 1f else a
            Prefs.saveAlpha(this@PetService, v)
        }

        /** 需要输入文字时，临时让窗口可聚焦（否则键盘弹不出来） */
        @JavascriptInterface
        fun setFocusable(on: Boolean) {
            handler.post {
                lp.flags = if (on)
                    lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                else
                    lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                try { wm.updateViewLayout(petView, lp) } catch (_: Throwable) {}
                if (on) {
                    petView.requestFocus()
                }
            }
        }

        /** 快捷对话：跳到 DSH 主界面 */
        @JavascriptInterface
        fun openChat() {
            handler.post {
                try {
                    val pkg = "com.deepseek.harness"
                    // 先试 launcher intent，拿不到就直接按组件名拉 MainActivity
                    var i = packageManager.getLaunchIntentForPackage(pkg)
                    if (i == null) {
                        i = Intent().apply {
                            setClassName(pkg, "com.deepseek.harness.MainActivity")
                        }
                    }
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                               Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    try {
                        startActivity(i)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this@PetService,
                            "打不开 DSH：" + e.javaClass.simpleName,
                            android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@PetService,
                        "打不开 DSH", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        /** 打开主界面（用于配置 Key） */
        @JavascriptInterface
        fun openSettings() {
            handler.post {
                val i = Intent(this@PetService, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
        }
    }

    /** 把任意字符串安全塞进 JS 调用 */
    private fun quoteJs(s: String): String {
        val esc = s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")
        return "\"" + esc + "\""
    }

    companion object {
        private const val CHANNEL = "dsh_pet"
        private const val NOTIF_ID = 9001
    }
}
