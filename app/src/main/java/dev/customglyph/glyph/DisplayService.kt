package dev.customglyph.glyph

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.customglyph.GlyphApplication
import dev.customglyph.MainActivity
import dev.customglyph.R

class DisplayService : Service() {
    private val app
        get() = application as GlyphApplication

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "enabled" && !app.repository.enabled) stopSelf()
        }

    override fun onCreate() {
        super.onCreate()
        app.repository.prefs.registerOnSharedPreferenceChangeListener(listener)
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "Glyph display", NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = "Shown while your rear-display orientation monitor is running."
                }
        )
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val stop =
            PendingIntent.getService(
                this,
                1,
                Intent(this, DisplayService::class.java).setAction(STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Custom Glyph is enabled")
                .setContentText("Tap to edit. Use Stop to turn off your design.")
                .setContentIntent(open)
                .setOngoing(true)
                .setSilent(true)
                .addAction(0, "Stop", stop)
                .build()
        ServiceCompat.startForeground(
            this,
            12,
            notification,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            app.repository.enabled = false
            app.glyph.cancelPreview()
            app.glyph.refresh()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!app.repository.enabled || !app.repository.showUnlocked || !app.glyph.directDisplay) {
            stopSelf()
            return START_NOT_STICKY
        }
        app.glyph.attach(GlyphController.APP)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        app.repository.prefs.unregisterOnSharedPreferenceChangeListener(listener)
        app.glyph.detach(GlyphController.APP)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "glyph_display"
        const val STOP = "dev.customglyph.STOP"
        const val RETRY = "dev.customglyph.RETRY"

        fun start(context: Context, retry: Boolean = false) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DisplayService::class.java).apply { if (retry) action = RETRY },
            )
        }
    }
}
