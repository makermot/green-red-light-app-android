package com.epfl.esl.a1_c_green_red_light

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.epfl.esl.a1_c_green_red_light.databinding.ActivityMainBinding
import com.google.android.gms.wearable.*

class   MainActivity : Activity(), DataClient.OnDataChangedListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    }


    override fun onResume() {
        super.onResume()
        println("App resumed")
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        println("App paused")
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        println(" On Data Changed Called")

        dataEvents
            .filter {it.dataItem.uri.path == "/userInfo" }
            .forEach { event ->
                println("User Info Event")
                val receivedImage: ByteArray = DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray("profileImage")
                val receivedUsername: String = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("username")

                val receivedUsernameBitmap = BitmapFactory.decodeByteArray(receivedImage, 0, receivedImage.size)

                binding.userImage.setImageBitmap(receivedUsernameBitmap)
                binding.welcomeText.text = "Welcome"
                binding.userName.text = receivedUsername
                binding.waitingView.visibility = View.VISIBLE
                binding.startView.visibility = View.GONE
            }

        dataEvents
            .filter {it.dataItem.uri.path == "/command" }
            .forEach { event ->
                println("command event")
                val receivedTimeStamp: String = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("timeStamp")
                val receivedCommand: String = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("command")
                binding.welcomeText.setText(receivedTimeStamp)
                if (receivedCommand == "start"){
                    binding.waitingView.visibility = View.GONE
                    binding.startView.visibility = View.VISIBLE
                }
            }
    }
}