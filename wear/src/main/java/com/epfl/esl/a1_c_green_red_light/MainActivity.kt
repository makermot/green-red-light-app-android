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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.ActivityMainBinding
import com.google.android.gms.wearable.*
import java.util.*
import kotlin.concurrent.timerTask

class   MainActivity : Activity(), DataClient.OnDataChangedListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.myText.setText("hello_world")
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
            .filter { it.type == DataEvent.TYPE_CHANGED &&
                    it.dataItem.uri.path == "/Test" }
            .forEach { event ->
                val receivedTestText: String = DataMapItem.fromDataItem(
                    event.dataItem).dataMap.getString("TestString")
                binding.myText.text = receivedTestText
            }
        binding.testtext.text = "Youston this a test"
    }
}

/*
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

class   MainActivity : FragmentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.epfl.esl.a1_c_green_red_light.R.layout.activity_main)

    }

    /*
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
            .filter { it.type == DataEvent.TYPE_CHANGED &&
                    it.dataItem.uri.path == "/userInfo" }
            .forEach { event ->
                val receivedImage: ByteArray = DataMapItem.fromDataItem(
                    event.dataItem).dataMap.getByteArray("profileImage")
                val receivedUsername: String = DataMapItem.fromDataItem(
                    event.dataItem).dataMap.getString("username")
                val receivedUsernameBitmap = BitmapFactory.
                decodeByteArray(receivedImage, 0, receivedImage.size)
            }
    }
    */
}
 */