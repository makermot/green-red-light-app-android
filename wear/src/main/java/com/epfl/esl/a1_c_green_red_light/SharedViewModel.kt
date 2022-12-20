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

class SharedViewModel: ViewModel(), DataClient.OnDataChangedListener, SensorEventListener,
MessageClient.OnMessageReceivedListener {

    // User Data
    //var receivedUsername: String = ""

    private val _receivedUsername = MutableLiveData<String>()
    val receivedUsername: LiveData<String>
        get() = _receivedUsername

    private val _receivedGameStatus = MutableLiveData<String>()
    val receivedGameStatus: LiveData<String>
        get() = _receivedGameStatus

    // Wearable.getDataClient(this).addListener(this)

    // Init variable
    init {
        _receivedGameStatus.value = "Wait"
    }


    // Message send to the watch
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if(messageEvent.path == "/command") {
            val receivedCommand: String = String(messageEvent.data)
            if (receivedCommand == "Start") {
                _receivedGameStatus.value = "Start"
            } else if (receivedCommand == "Stop") {
                _receivedGameStatus.value = "Stop"
            }
        }
        println("we received Command")
    }

    // Data send to the watch
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter {
                it.type == DataEvent.TYPE_CHANGED &&
                        it.dataItem.uri.path == "/userInfo"
            }
            .forEach { event ->
                _receivedUsername.value = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("username")!!
            }
        println("we received Username")
        //binding.myText.setText(receivedUsername)
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        TODO("Not yet implemented")
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }
}