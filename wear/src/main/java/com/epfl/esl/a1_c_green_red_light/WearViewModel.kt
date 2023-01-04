package com.epfl.esl.a1_c_green_red_light

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.*

//SensorEventListener
class WearViewModel : ViewModel() {

    // Constants
    private val SHAKE_THRESHOLD = 1.1f
    private val SHAKE_WAIT_TIME_MS = 250

    // Variables
    private var mSensorManager: SensorManager? = null
    private var mSensor: Sensor? = null
    private var mSensorType = 0
    private var mShakeTime: Long = 0
    private val mRotationTime: Long = 0

    // Live data
    // validLogin : True is valid, false if not, null otherwise
    private val _isMoving = MutableLiveData<Boolean?>()
    val validLogin: LiveData<Boolean?>
        get() = _isMoving

    // Init variable
    init {
        _isMoving.value = null
    }


    fun onSensorChanged(event: SensorEvent) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event)
        }
    }

    fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}


    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private fun detectShake(event: SensorEvent) {
        val now = System.currentTimeMillis()
        if (now - mShakeTime > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now
            val gX = event.values[0] / SensorManager.GRAVITY_EARTH
            val gY = event.values[1] / SensorManager.GRAVITY_EARTH
            val gZ = event.values[2] / SensorManager.GRAVITY_EARTH

            // gForce will be close to 1 when there is no movement
            val gForce: Float = sqrt(gX * gX + gY * gY + gZ * gZ)

            // Change boolean value if gForce exceeds threshold;
            _isMoving.value = gForce > SHAKE_THRESHOLD
        }
    }
}