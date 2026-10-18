package dev.customglyph.glyph

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

class OrientationMonitor(context: Context, private val changed: (Boolean) -> Unit) :
    SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private var detector = FaceDownDetector()
    private var lastReading = 0L
    val fresh: Boolean
        get() = known && SystemClock.elapsedRealtime() - lastReading < 2000

    var available = false
        private set

    var known = false
        private set

    var faceDown = false
        private set

    fun start() {
        detector = FaceDownDetector()
        known = false
        lastReading = 0L
        faceDown = false
        val sensor =
            manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
                ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        available = sensor != null && manager.registerListener(this, sensor, 200_000)
    }

    fun stop() {
        manager.unregisterListener(this)
        known = false
        lastReading = 0L
        faceDown = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val next = detector.update(event.values[2], SystemClock.elapsedRealtime())
        val notify = !fresh || next != faceDown
        lastReading = event.timestamp / 1_000_000L
        known = true
        faceDown = next
        if (notify) changed(next)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
