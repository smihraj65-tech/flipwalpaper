package com.flipwallpaper.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class FlipWallpaperService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var wallpaperManager: WallpaperManager

    private var lastFlipTime: Long = 0
    private val flipCooldownMs: Long = 1200 // 1.2 सेकंड का कूलडाउन ताकि बार-बार न बदले
    private val tiltThreshold: Float = 5.5f // टिल्ट सेंसिटिविटी

    // डेमो वॉलपेपर्स (सॉलिड कलर्स/ग्रेडिएंट्स जिसे यूजर बाद में गैलरी से बदल सकता है)
    private val colors = listOf(
        Color.parseColor("#1E1E2E"), // डार्क ब्लू
        Color.parseColor("#2E1E38"), // डार्क पर्पल
        Color.parseColor("#1B2A4A"), // नेवी ब्लू
        Color.parseColor("#183D3D"), // एमराल्ड डार्क
        Color.parseColor("#3F2E56")  // डीप वॉयलेट
    )
    private var currentIndex = 0

    override fun onCreate() {
        super.onCreate()
        wallpaperManager = WallpaperManager.getInstance(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // हर Android फोन में चलने वाला एक्सेलेरोमीटर सेंसर
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        startForegroundServiceNotification()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "flip_wallpaper_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Flip Wallpaper Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Flip Wallpaper Active")
            .setContentText("फोन को दाएँ या बाएँ झुकाने पर वॉलपेपर बदलेगा")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .build()

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0] // दायाँ-बायाँ झुकाव
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastFlipTime > flipCooldownMs) {
            if (x < -tiltThreshold) {
                // दाएँ तरफ टिल्ट (Right Flip) -> अगला वॉलपेपर
                lastFlipTime = currentTime
                nextWallpaper()
            } else if (x > tiltThreshold) {
                // बाएँ तरफ टिल्ट (Left Flip) -> पिछला वॉलपेपर
                lastFlipTime = currentTime
                previousWallpaper()
            }
        }
    }

    private fun nextWallpaper() {
        currentIndex = (currentIndex + 1) % colors.size
        applyWallpaper(colors[currentIndex])
    }

    private fun previousWallpaper() {
        currentIndex = if (currentIndex - 1 < 0) colors.size - 1 else currentIndex - 1
        applyWallpaper(colors[currentIndex])
    }

    private fun applyWallpaper(color: Int) {
        Thread {
            try {
                // स्क्रीन साइज़ का बिटमैप बनाकर वॉलपेपर सेट करें
                val metrics = resources.displayMetrics
                val width = metrics.widthPixels
                val height = metrics.heightPixels

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(color)

                // थोड़ा डिज़ाइन (OriginOS स्टाइल इंडिकेटर)
                val paint = Paint().apply {
                    this.color = Color.WHITE
                    textSize = 60f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Flip Wallpaper #${currentIndex + 1}", width / 2f, height / 2f, paint)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
