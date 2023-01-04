package com.epfl.esl.a1_c_green_red_light

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.textclassifier.ConversationActions
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import com.epfl.esl.a1_c_green_red_light.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.sqrt


class MainActivity : Activity(), SensorEventListener, DataClient.OnDataChangedListener,
    LifecycleOwner {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var dataClient : DataClient
    private lateinit var lifecycleRegistry: LifecycleRegistry

    private var screen :String? = "waiting"

    // Live data
    private var stateMachine = MutableLiveData<String>()
    private var username  = MutableLiveData<String?>()
    private var userImage = MutableLiveData<Bitmap?>()
    // Init Live data variable
    init {
        stateMachine.value = "unlogged"
        username.value = null
        userImage.value = null
    }

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
    private var timerDeconection: Timer? = null
    private var startTime: Long = 0
    private var mHandler: Handler = object : Handler(){}

    private var light: String = "green"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.markState(Lifecycle.State.CREATED)

        //definition of the FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Instantiate dataclient
        dataClient = Wearable.getDataClient(this)

        // Initialise Binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Check if hardware has GPS
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

        // Add observer on state machine
        stateMachine.observe(this) { state ->
            updateState()
        }

        // Add observer on username
        username.observe(this) { name ->
            if(name == null){
                binding.welcomeText.text = this.getString(R.string.waiting_for_logging)
                binding.userName.text = this.getString(R.string.on_tablet)
            }
            else{
                binding.welcomeText.text = this.getString(R.string.welcome)
                binding.userName.text = name
            }
        }

        // Add observer on userImage
        userImage.observe(this) { image ->
            binding.userImage.setImageBitmap(image)
        }

    }


    // instantiate data client
    override fun onResume() {
        super.onResume()
        println("App resumed")
        Wearable.getDataClient(this).addListener(this)
        //mSensorManager!!.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }


    // remove data client
    override fun onPause() {
        super.onPause()
        println("App paused")
        Wearable.getDataClient(this).removeListener(this)
        //mSensorManager!!.unregisterListener(this)
    }


    // receive Message from mobile
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        println(" On Data Changed Called")

        dataEvents
            .filter {it.dataItem.uri.path == "/userInfo" }
            .forEach { event ->
                println("User Info Event")
                val receivedImage: ByteArray = DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray("profileImage")
                username.value = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("username")
                userImage.value = BitmapFactory.decodeByteArray(receivedImage, 0, receivedImage.size)

                binding.waitingView.visibility = View.VISIBLE
                binding.startView.visibility = View.GONE
                screen = "waiting"
                binding.container.setBackgroundColor(
                    ContextCompat.getColor(applicationContext, R.color.white))
            }

        dataEvents
            .filter {it.dataItem.uri.path == "/command" }
            .forEach { event ->
                println("command event")
                val receivedTimeStamp: String = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("timeStamp")
                val receivedCommand: String = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("command")
                binding.welcomeText.text = receivedTimeStamp
                if (receivedCommand == "start"){
                    println("est rentré")
                    startTime = System.currentTimeMillis() / 1000
                    binding.waitingView.visibility = View.GONE
                    binding.startView.visibility = View.VISIBLE
                    screen = "start"
                    light = "green"
                    println("before timer")
                    timer = Timer()
                    println("after timer")
                    println("Setting Timer")
                    timer.schedule(timerTask {
                        println("Timer timeout")
                        getGPSPositionandCallSendGPSToMobile()
                        mHandler.post( Runnable() {
                            runOnUiThread() {
                                updateTime()
                            }
                        })
                    }, 0, 1000)
                }
                else if(receivedCommand == "stop"){
                    timer.cancel()
                }
                else if(receivedCommand == "change_light"){
                    if (light == "green") {
                        binding.container.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.red))
                        binding.startView.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.red))
                        light = "red"
                        println("change light to red")
                    }
                    else{
                        binding.container.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.green))
                        binding.startView.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.green))
                        light = "green"
                        println("change light to green")
                    }
                }
            }

        dataEvents
            .filter {it.dataItem.uri.path == "/state" }
            .forEach { event ->
                print("Heart beat received :")
                val receivedStatemachine: String? = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("state")
                // To handle null case -> should never happen btw
                if(receivedStatemachine != null){
                    print("received state :")
                    println(receivedStatemachine)

                    // to avoid call the observer each time the value is reasigned
                    if(stateMachine.value != receivedStatemachine){
                        stateMachine.value = receivedStatemachine!!
                    }

                    // reset timer if present
                    if(timerDeconection != null) {
                        timerDeconection!!.cancel()
                        timerDeconection!!.purge()
                        timerDeconection = null
                    }

                    // Launch watch dog timer 10s
                    timerDeconection = Timer()
                    timerDeconection!!.schedule(timerTask {
                        println("Watch Dog Timer")
                        mHandler.post( Runnable() {
                            runOnUiThread() {
                                stateMachine.value = "unlogged"
                            }
                        })
                    }, 10000, 5000)
                }
            }
    }


    private fun updateState(){
        print("We update the state machine with state :")
        println(stateMachine.value)

        if(stateMachine.value == "unlogged"){
            username.value = null
            userImage.value = BitmapFactory.decodeResource(this.resources,R.drawable.ic_logo)
        }
        else if(stateMachine.value == "logged"){

        }
        else if(stateMachine.value == "racing"){

        }
        else{
            println("0h oh... wrong state machine")
        }

    }

    // Call detect shake
    override fun onSensorChanged(event: SensorEvent) {
        detectShake(event)
    }


    // TODO Implement
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        println("I am in onAccuracyChanged")
    }


    // Detect with acceleromter if the user moves
    private fun detectShake(event: SensorEvent) {
        // References:
        //  - http://jasonmcreynolds.com/?p=388
        //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
        val now = System.currentTimeMillis()
        //println("I am in detectShake")
        if (now - mShakeTime > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now
            val gX = event.values[0] / SensorManager.GRAVITY_EARTH
            val gY = event.values[1] / SensorManager.GRAVITY_EARTH
            val gZ = event.values[2] / SensorManager.GRAVITY_EARTH

            // gForce will be close to 1 when there is no movement
            val gForce: Float = sqrt(gX * gX + gY * gY + gZ * gZ)

            // Change boolean value if gForce exceeds threshold;
            if (light == "red"){
                if (gForce > SHAKE_THRESHOLD) {
                    binding.startView.visibility = View.GONE
                    binding.cheatingView.visibility = View.VISIBLE
                    binding.container.setBackgroundColor(
                        ContextCompat.getColor(applicationContext, R.color.yellow))
                }
                else{
                    binding.cheatingView.visibility = View.GONE
                    binding.startView.visibility = View.VISIBLE
                    if (light == "green"){
                        binding.container.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.green))}
                    else{
                        binding.container.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.red))
                    }
                }
            }
        }
    }


    // Get GPS position from wear and all sendGPSToMobile func if success
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


    // Send GPS coordonate to mobile
    fun sendGPSToMobile(position : Location) {
        println("We are in send GPS to Mobile")
        // Add a timestamp to the message, so its truly different each time !
        val tsLong = System.currentTimeMillis() / 1000
        val timestamp = tsLong.toString()

        val request: PutDataRequest = PutDataMapRequest.create("/GPS_data").run {
            dataMap.putString("timeStamp", timestamp)
            var LocationLat = position.latitude
            var LocationLong = position.longitude
            dataMap.putDouble("latitude", LocationLat)
            dataMap.putDouble("longitude", LocationLong)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
        putTask.addOnSuccessListener {
            println("Great Succes! : Command sent to wear")
        }.addOnFailureListener {
            println("Oupsi... On a pas envoyé les données GPS")
        }
    }


    // Function to request permision to position
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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


    // Verify if hardware has GPS capabilities
    private fun hasGps(context : Context): Boolean{
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }


    // update elapse time display
    private fun updateTime(){
        binding.elapsedTimeText.text = (System.currentTimeMillis() / 1000 - startTime).toString()
    }


    // update lifecycle -> needed for the observer
    public override fun onStart() {
        super.onStart()
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
    }


    // return lifecyle owner -> need for the observer
    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}




