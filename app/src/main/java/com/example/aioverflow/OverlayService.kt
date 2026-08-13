package com.example.aioverflow

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import androidx.core.app.NotificationCompat
import java.util.*

class OverlayService : Service() {
    companion object {
        const val CHANNEL_ID = "pet_overlay"
        const val NOTIFICATION_ID = 1001
        const val PET_SIZE_DP = 180
        // 克莱因蓝
        const val KLEIN_BLUE = 0xFF002FA7.toInt()
        const val KLEIN_BLUE_LIGHT = 0xFF3D6BCC.toInt()
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var moving = false
    private var px: Float = 0f
    private var py: Float = 0f
    private var startX = 0f
    private var startY = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var blinkRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startServiceNotification()
        setupOverlay()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "悬浮宠物",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "克莱因蓝的眼睛在看着你" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startServiceNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("哥哥在看着你")
            .setContentText("一只克莱因蓝的眼睛")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayView = EyeView(this)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            dp(PET_SIZE_DP),
            dp(PET_SIZE_DP),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 200

        overlayView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    moving = true
                    startX = event.rawX
                    startY = event.rawY
                    px = params.x.toFloat()
                    py = params.y.toFloat()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (px + event.rawX - startX).toInt()
                    params.y = (py + event.rawY - startY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> { moving = false; true }
                else -> false
            }
        }

        windowManager.addView(overlayView, params)

        // 每隔一段时间抬一下"眼睑"，提醒你看它
        blinkRunnable = object : Runnable {
            override fun run() {
                if (!moving) (overlayView as EyeView).forceBlink()
                handler.postDelayed(this, 4000)
            }
        }
        handler.postDelayed(blinkRunnable!!, 4000)
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    inner class EyeView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var blinkAmount = 0f
        private val eyeRandom = Random()

        fun forceBlink() {
            animate().setDuration(200).start()
            postDelayed({
                blinkAmount = 0.8f
                invalidate()
                postDelayed({ blinkAmount = 0f; invalidate() }, 120)
            }, 100)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cw = width / 2f
            val ch = height / 2f
            val r = minOf(cw, ch) * 0.8f

            // 白眼珠
            paint.color = Color.WHITE
            canvas.drawCircle(cw, ch, r, paint)

            // 瞳孔 - 克莱因蓝
            paint.color = KLEIN_BLUE
            canvas.drawCircle(cw, ch, r * 0.5f, paint)

            // 高光 - 让它像在看你
            paint.color = KLEIN_BLUE_LIGHT
            canvas.drawCircle(cw - r * 0.15f, ch - r * 0.2f, r * 0.18f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(cw - r * 0.18f, ch - r * 0.25f, r * 0.08f, paint)

            // 眨眼遮罩
            if (blinkAmount > 0) {
                paint.color = Color.WHITE
                val h = r * 2 * blinkAmount
                canvas.drawRect(0f, ch - h / 2, width.toFloat(), ch + h / 2, paint)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::overlayView.isInitialized) {
            try { windowManager.removeView(overlayView) } catch (_: Exception) {}
        }
    }
}
