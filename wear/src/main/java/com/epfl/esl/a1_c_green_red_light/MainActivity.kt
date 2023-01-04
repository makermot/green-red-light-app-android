package com.epfl.esl.a1_c_green_red_light

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.epfl.esl.a1_c_green_red_light.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.wearable.*
import androidx.lifecycle.ViewModelProvider

class   MainActivity : Activity(), DataClient.OnDataChangedListener {

    private lateinit var binding: ActivityMainBinding
    var mFusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var viewModel: WearViewModel

    private var screen :String? = "waiting"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //definition of the FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        viewModel = ViewModelProvider(this).get(WearViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)



        viewModel.isMoving.observe(viewLifecycleOwner, Observer { value ->
            println("isMoving changed")
            // watch is moving
            if (screen == "start") {
                if (value == true) {
                    binding.start_text.text = "Moving"
                }
                //watch is still
                else {
                    binding.start_text.text = "Still"
                }
            }
        })

        if (!hasGps(this)) {
            Log.d(TAG, "This hardware doesn't have GPS.")
            // Fall back to functionality that doesn't use location or
            // warn the user that location function isn't available.
        }
        else{
            Log.d(TAG, "This hardware has GPS.")
        }

        return binding.root
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
                screen = "waiting"
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
                    screen = "start"
                }
            }
    }


private fun hasGps(context : Context): Boolean{
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
}

private fun sendDataToMobile(mFusedLocationClient : LatLng) {
    val dataClient: DataClient = Wearable.getDataClient(this)
    val putDataReq: PutDataRequest = PutDataMapRequest.create("/GPS_data").run {
        var LocationLat = mFusedLocationClient.latitude
        var LocationLong = mFusedLocationClient.longitude
        dataMap.putDouble("latitude", LocationLat)
        dataMap.putDouble("longitude", LocationLong)
        asPutDataRequest()
    }
    dataClient.putDataItem(putDataReq)
}

}



