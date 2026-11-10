package com.flipwallpaper.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnToggle = findViewById<Button>(R.id.btnToggle)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        btnToggle.setOnClickListener {
            val intent = Intent(this, FlipWallpaperService::class.java)
            if (!isRunning) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                isRunning = true
                tvStatus.text = "स्थिति: सर्विस चालू है (फ़ोन को दाएँ/बाएँ झुकाएँ)"
                btnToggle.text = "सर्विस बंद करें"
            } else {
                stopService(intent)
                isRunning = false
                tvStatus.text = "स्थिति: सर्विस बंद है"
                btnToggle.text = "सर्विस शुरू करें"
            }
        }
    }
}
