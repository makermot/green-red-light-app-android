package com.epfl.esl.a1_c_green_red_light

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.*
import java.util.*
import kotlin.concurrent.timerTask
import com.google.android.gms.wearable.MessageEvent as MessageEvent

class SharedViewModel: ViewModel(),SensorEventListener{

    private val _receivedUsername = MutableLiveData<String>()
    val receivedUsername: LiveData<String>
        get() = _receivedUsername

    private val _receivedGameStatus = MutableLiveData<String>()
    val receivedGameStatus: LiveData<String>
        get() = _receivedGameStatus


    // Init variable
    init {
        _receivedGameStatus.value = "Wait"
        _receivedUsername.value = "***"
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        TODO("Not yet implemented")
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }
}