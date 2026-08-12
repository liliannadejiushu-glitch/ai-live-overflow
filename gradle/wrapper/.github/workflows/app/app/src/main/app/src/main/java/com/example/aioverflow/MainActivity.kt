package com.example.aioverflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        layout.addView(Button(this).apply {
            text = "启动悬浮宠物"
            setOnClickListener {
                if (canDrawOverlays()) {
                    startOverlayService()
                } else {
                    requestOverlayPermission()
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "开启通知权限"
            setOnClickListener { requestNotificationPermission() }
        })

        setContentView(layout)
    }

    private fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            Toast.makeText(this, "通知权限已开启", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startOverlayService() {
        startForegroundService(Intent(this, OverlayService::class.java))
    }
}
