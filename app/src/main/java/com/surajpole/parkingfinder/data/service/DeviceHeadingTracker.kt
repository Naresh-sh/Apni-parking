package com.surajpole.parkingfinder.data.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * High-performance Gyroscope + Magnetometer sensor fusion tracker.
 * Calculates real-time 360° azimuth/compass bearing with low-pass dampening
 * to provide a smooth, 60-120 FPS directional beam like Google Maps.
 */
class DeviceHeadingTracker(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    var currentHeading = mutableStateOf<Float?>(null)
        private set

    private var smoothedHeading = 0f

    fun startListening() {
        if (sensorManager == null) return
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]
            var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            if (azimuthDeg < 0) azimuthDeg += 360f
            updateSmoothHeading(azimuthDeg)
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                lastAccelerometerSet = true
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                lastMagnetometerSet = true
            }

            if (lastAccelerometerSet && lastMagnetometerSet) {
                if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuthRad = orientationAngles[0]
                    var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                    if (azimuthDeg < 0) azimuthDeg += 360f
                    updateSmoothHeading(azimuthDeg)
                }
            }
        }
    }

    private fun updateSmoothHeading(rawDeg: Float) {
        // Handle wrap-around at 0/360 degrees
        var diff = rawDeg - smoothedHeading
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f

        // Apply low-pass filter for silky smooth compass rotation
        if (abs(diff) > 0.5f) {
            smoothedHeading += diff * 0.25f
            if (smoothedHeading < 0) smoothedHeading += 360f
            if (smoothedHeading >= 360f) smoothedHeading -= 360f
            currentHeading.value = smoothedHeading
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun rememberDeviceHeading(): State<Float?> {
    val context = LocalContext.current
    val tracker = remember { DeviceHeadingTracker(context) }

    DisposableEffect(Unit) {
        tracker.startListening()
        onDispose {
            tracker.stopListening()
        }
    }

    return tracker.currentHeading
}
