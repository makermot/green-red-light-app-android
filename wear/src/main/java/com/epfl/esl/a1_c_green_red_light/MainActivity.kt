package com.epfl.esl.a1_c_green_red_light

import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.ActivityMainBinding
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.R
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : FragmentActivity(), DataClient.OnDataChangedListener, SensorEventListener,
    MessageClient.OnMessageReceivedListener {


    private var timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.epfl.esl.a1_c_green_red_light.R.layout.activity_main)

    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if(messageEvent.path == "/command") {
            val receivedCommand: String = String(messageEvent.data)
            if (receivedCommand == "Start") {
                timer = Timer()
                timer.schedule(timerTask {
                   // sendDataToMobile(heartrate)
                }, 0, 500)
            } else if (receivedCommand == "Stop") {
            timer.cancel()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter {
                it.type == DataEvent.TYPE_CHANGED &&
                        it.dataItem.uri.path == "/userInfo"
            }
            .forEach { event ->
                val receivedUsername: String = DataMapItem.fromDataItem(
                    event.dataItem
                ).dataMap.getString("username")!!
            }
        //binding.myText.setText(receivedUsername)
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        TODO("Not yet implemented")
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }
}