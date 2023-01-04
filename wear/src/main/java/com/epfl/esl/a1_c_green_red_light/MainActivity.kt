package com.epfl.esl.a1_c_green_red_light

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.epfl.esl.a1_c_green_red_light.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.wearable.*
import kotlin.math.sqrt
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : Activity(), SensorEventListener, DataClient.OnDataChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var screen :String? = "waiting"

    // Constants
    private val SHAKE_THRESHOLD = 1.1f
    private val SHAKE_WAIT_TIME_MS = 250
    private val LOCATION_REQUEST_CODE = 101

    // Variables
    private var mSensorManager: SensorManager? = null
    private var mSensor: Sensor? = null
    private var mSensorType = 0
    private var mShakeTime: Long = 0
    private var timer = Timer()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //definition of the FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        if (!hasGps(this)) {
            Log.d(TAG, "This hardware doesn't have GPS.")
            // Fall back to functionality that doesn't use location or
            // warn the user that location function isn't available.
        }
        else{
            Log.d(TAG, "This hardware has GPS.")
        }

        // accelerometer variables
        mSensorType = Sensor.TYPE_ACCELEROMETER
        mSensorManager = this.getSystemService(SENSOR_SERVICE) as SensorManager?
        mSensor = mSensorManager!!.getDefaultSensor(mSensorType)

    }


    override fun onResume() {
        super.onResume()
        println("App resumed")
        Wearable.getDataClient(this).addListener(this)
        mSensorManager!!.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        println("App paused")
        Wearable.getDataClient(this).removeListener(this)
        mSensorManager!!.unregisterListener(this)
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
                    timer = Timer()
                    println("Setting Timer")
                    timer.schedule(timerTask {
                        println("Timer timeout")
                        getGPSPositionandCallSendGPSToMobile()
                    }, 0, 500)
                }
                else if(receivedCommand == "stop"){
                    timer.cancel()
                }
            }
    }

    override fun onSensorChanged(event: SensorEvent) {
        detectShake(event)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        println("I am in onAccuracyChanged")
    }

    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private fun detectShake(event: SensorEvent) {
        val now = System.currentTimeMillis()
        println("I am in detectShake")
        if (now - mShakeTime > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now
            val gX = event.values[0] / SensorManager.GRAVITY_EARTH
            val gY = event.values[1] / SensorManager.GRAVITY_EARTH
            val gZ = event.values[2] / SensorManager.GRAVITY_EARTH

            // gForce will be close to 1 when there is no movement
            val gForce: Float = sqrt(gX * gX + gY * gY + gZ * gZ)

            // Change boolean value if gForce exceeds threshold;
            if (gForce > SHAKE_THRESHOLD){
                binding.welcomeText.text = "Moving"
                println("You are moving")
            }
            else {
                binding.welcomeText.text = "Still"
            println("You are still")
            }
        }
    }

    private fun isMoving(){
        println("isMoving changed")
        // watch is moving
        if (screen == "start") {
            if (screen == "start") {
                binding.startText.text = "Moving"
            }
            //watch is still
            else {
                binding.startText.text = "Still"
            }
        }
        else if (screen == "waiting") {
            if (screen == "waiting") {
                binding.welcomeText.text = "Moving"
            }
            //watch is still
            else {
                binding.welcomeText.text = "Still"
            }
        }
    }


    private fun hasGps(context : Context): Boolean{
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }


    private fun getGPSPositionandCallSendGPSToMobile(){
        println("We are getGPSPositionandCallSendGPSToMobile")

        val permission: Boolean = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (permission) {
            mFusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { position ->
                println("We find the position")
                println(position)
                sendGPSToMobile(position)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    )
    {
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Unable to show location - permission required",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    println("Wow, we have permission now")
                }
            }
        }
    }


    // Send GPS coordonate to mobile
    private fun sendGPSToMobile(position : Location) {
        println("We are in send GPS to Mobile")
        val dataClient: DataClient = Wearable.getDataClient(this)
        val putDataReq: PutDataRequest = PutDataMapRequest.create("/GPS_data").run {
            var LocationLat = position.latitude
            var LocationLong = position.longitude
            dataMap.putDouble("latitude", LocationLat)
            dataMap.putDouble("longitude", LocationLong)
            asPutDataRequest()
        }
        dataClient.putDataItem(putDataReq)
    }

}



