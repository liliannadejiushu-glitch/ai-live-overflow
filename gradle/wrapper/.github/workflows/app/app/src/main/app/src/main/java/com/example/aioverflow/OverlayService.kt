package com.example.aioverflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var webView: WebView

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, buildNotification())
        setupOverlay()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "overlay_channel",
            "悬浮宠物",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("悬浮宠物运行中")
            .setContentText("点击返回主界面")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupOverlay() {
        webView = WebView(this).apply {
            setBackgroundColor(0)
            setWebViewClient(WebViewClient())
            loadUrl("file:///android_asset/pet.html")
        }

        val params = WindowManager.LayoutParams(
            180,
            240,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                stopSelf()
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                // long press - do nothing for now
            }
        })

        webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX - webView.width / 2).toInt()
                    params.y = (event.rawY - webView.height / 2).toInt()
                    windowManager.updateViewLayout(webView, params)
                }
            }
            gestureDetector.onTouchEvent(event)
            true
        }

        windowManager.addView(webView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::webView.isInitialized) {
            webView.destroy()
            windowManager.removeView(webView)
        }
    }
}
