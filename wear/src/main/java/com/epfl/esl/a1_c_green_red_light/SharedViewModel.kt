package com.epfl.esl.a1_c_green_red_light

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.*
import java.util.*
import kotlin.concurrent.timerTask
import com.google.android.gms.wearable.MessageEvent as MessageEvent

class SharedViewModel: ViewModel(), DataClient.OnDataChangedListener, SensorEventListener,
MessageClient.OnMessageReceivedListener {

    var receivedUsername: String = ""
    var receivedGameStatus: String = "Wait"
    // Wearable.getDataClient(this).addListener(this)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if(messageEvent.path == "/command") {
            val receivedCommand: String = String(messageEvent.data)
            if (receivedCommand == "Start") {
                receivedGameStatus = "Start"
            } else if (receivedCommand == "Stop") {
                receivedGameStatus = "Stop"
            }
        }
    }
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter {
                it.type == DataEvent.TYPE_CHANGED &&
                        it.dataItem.uri.path == "/userInfo"
            }
            .forEach { event ->
                receivedUsername = DataMapItem.fromDataItem(
                    event.dataItem
                ).dataMap.getString("username")!!
            }
        println(receivedUsername)
        //binding.myText.setText(receivedUsername)
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        TODO("Not yet implemented")
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }
}