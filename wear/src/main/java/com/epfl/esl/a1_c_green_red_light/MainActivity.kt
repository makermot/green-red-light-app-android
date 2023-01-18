package com.epfl.esl.a1_c_green_red_light

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color.blue
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import com.epfl.esl.a1_c_green_red_light.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.sqrt
import android.content.Context as Context1


class MainActivity : Activity(), SensorEventListener, DataClient.OnDataChangedListener,
    LifecycleOwner {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var dataClient: DataClient
    private lateinit var lifecycleRegistry: LifecycleRegistry

    // Live data
    private var stateMachine = MutableLiveData<String>()
    private var username = MutableLiveData<String?>()
    private var userImage = MutableLiveData<Bitmap?>()
    private var light = MutableLiveData<String>()
    private var shouldSendInfoRequest = MutableLiveData<Boolean>()

    // Init Live data variable
    init {
        stateMachine.value = "unlogged"
        username.value = null
        userImage.value = null
        light.value = "green"
        shouldSendInfoRequest.value = false
    }

    // Constants
    private val SHAKE_THRESHOLD = 1.1f                         // reference value : 1.1f
    private val SHAKE_WAIT_TIME_MS = 250
    private val LOCATION_REQUEST_CODE = 101

    // Variables
    private var mSensorManager: SensorManager? = null
    private var mSensor: Sensor? = null
    private var mSensorType = 0
    private var mShakeTime: Long = 0
    private var raceTimer: Timer? = null
    private var watchdogTimer: Timer? = null
    private var startTime: Long = 0
    private var mHandler: Handler = object : Handler() {}
    private var cheating: Boolean = false


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
        } else {
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
            binding.userName.text = name
        }

        // Add observer on userImage
        userImage.observe(this) { image ->
            binding.userImage.setImageBitmap(image)
        }


        // Add observer on shouldSendInfoRequest
        shouldSendInfoRequest.observe(this) { request ->
            if (request) {
                sendUserInfoRequestToMobile()
            }
        }

        light.observe(this) { color ->
            if ((stateMachine.value == "racing") && (cheating == false)) {

                vibrateWear(this)

                when (color) {
                    "green" -> {
                        binding.cheatingView.visibility = View.GONE
                        binding.startView.visibility = View.VISIBLE
                        binding.container.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.green)
                        )
                        binding.startView.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.green)
                        )
                        println("change light to green")
                    }
                    "red" -> {
                        binding.container.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.red)
                        )
                        binding.startView.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.red)
                        )
                        println("change light to red")
                    }
                }
            }
        }
    }


    // receive Message from mobile
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        //print(" User name : ")
        //println(username.value)
        //print(" On Data Changed Called : ")

        dataEvents
            .filter { it.dataItem.uri.path == "/userInfo" }
            .forEach { event ->
                //println("with User Info Event")
                val receivedImage: ByteArray =
                    DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray("profileImage")
                username.value =
                    DataMapItem.fromDataItem(event.dataItem).dataMap.getString("username")
                userImage.value =
                    BitmapFactory.decodeByteArray(receivedImage, 0, receivedImage.size)
                //screen = "waiting"
            }

        dataEvents
            .filter { it.dataItem.uri.path == "/command" }
            .forEach { event ->
                println(" Light change command event : ")
                val receivedCommand: String =
                    DataMapItem.fromDataItem(event.dataItem).dataMap.getString("command")

                when (receivedCommand) {
                    "green" -> {
                        light.value = "green"
                    }
                    "red" -> {
                        light.value = "red"
                    }
                }
            }

        dataEvents
            .filter { it.dataItem.uri.path == "/state" }
            .forEach { event ->
                //print(" with Heart beat received :")
                val receivedStatemachine: String? =
                    DataMapItem.fromDataItem(event.dataItem).dataMap.getString("state")
                // To handle null case -> should never happen btw
                if (receivedStatemachine != null) {
                    //print("received state :")
                    //print(receivedStatemachine)
                    //print(", While actual state is :")
                    //print(stateMachine.value)
                    //print(", User name is :")
                    //println(username.value)

                    // to avoid call the observer each time the value is reasigned
                    if (stateMachine.value != receivedStatemachine) {
                        stateMachine.value = receivedStatemachine!!
                    }

                    if (stateMachine.value == "logged" && username.value == null) {
                        shouldSendInfoRequest.value = true
                    }

                    // Re-start watchdog timer
                    startWatchDogTimer()
                }
            }
    }


    private fun updateState() {
        print("We update the state machine with state :")
        println(stateMachine.value)

        // clearing everything on the screen
        binding.waitingView.visibility = View.GONE
        binding.loggedView.visibility = View.GONE
        binding.startView.visibility = View.GONE
        binding.resultView.visibility = View.GONE
        binding.container.setBackgroundColor(
            ContextCompat.getColor(applicationContext, R.color.white)
        )

        stopRaceTimer()

        when (stateMachine.value) {
            "unlogged" -> {
                username.value = null
                userImage.value = BitmapFactory.decodeResource(this.resources, R.drawable.ic_logo)
                binding.waitingView.visibility = View.VISIBLE
            }
            "logged" -> {
                if (username.value == null) {
                    // request user info to mobile
                    sendUserInfoRequestToMobile()
                }
                binding.loggedView.visibility = View.VISIBLE
            }
            "racing" -> {
                // Set up view visibility
                binding.container.setBackgroundColor(
                    ContextCompat.getColor(applicationContext, R.color.red)
                )
                binding.startView.visibility = View.VISIBLE
                binding.startView.setBackgroundColor(
                    ContextCompat.getColor(applicationContext, R.color.red)
                )

                // Save start time to compute elapsed time
                startTime = System.currentTimeMillis() / 1000

                // start timer to send GPS position and update elapse time
                startRaceTimer()
            }
            "result" -> {
                binding.container.setBackgroundColor(
                    ContextCompat.getColor(applicationContext, R.color.blue)
                )
                binding.resultView.visibility = View.VISIBLE
            }
            else -> {
                println("0h oh... wrong state machine")
            }
        }
    }

    // Call detect shake
    override fun onSensorChanged(event: SensorEvent) {
        detectShake(event)
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        println("I am in onAccuracyChanged")
    }


    // Detect with accelerometer if the user moves
    private fun detectShake(event: SensorEvent) {
        // References:
        //  - http://jasonmcreynolds.com/?p=388
        //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125

        // Return if we are not in racing or if we already cheating
        if (stateMachine.value != "racing" || cheating) {
            return
        }

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
            if (light.value == "red") {
                if (gForce > SHAKE_THRESHOLD) {
                    binding.startView.visibility = View.GONE
                    binding.cheatingView.visibility = View.VISIBLE
                    binding.container.setBackgroundColor(
                        ContextCompat.getColor(applicationContext, R.color.yellow)
                    )
                    cheating = true
                    sendCheatingCommandToMobile()
                }
                /*else{
                    binding.cheatingView.visibility = View.GONE
                    binding.startView.visibility = View.VISIBLE
                    if (light.value == "green"){
                        binding.container.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.green))}
                    else{
                        binding.container.setBackgroundColor(
                            ContextCompat.getColor(applicationContext, R.color.red))
                    }
                }*/
            }
        }
    }


    // Get GPS position from wear and all sendGPSToMobile func if success
    private fun getGPSPositionAndCallSendGPSToMobile() {
        //println("We are getGPSPositionAndCallSendGPSToMobile")

        val permission: Boolean = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (permission) {
            mFusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { position ->
                    //print("We find the position : ")
                    //println(position)
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
    fun sendGPSToMobile(position: Location) {
        //println("We are in send GPS to Mobile")
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
            //println("Great Succes! : Command sent to wear")
        }.addOnFailureListener {
            println("Oupsi... On a pas envoyé les données GPS")
        }
    }


    // request  userInfo to mobile to mobile
    fun sendUserInfoRequestToMobile() {
        //println("We are in send requestInfo to Mobile")
        // Add a timestamp to the message, so its truly different each time !
        val tsLong = System.currentTimeMillis() / 1000
        val timestamp = tsLong.toString()

        val request: PutDataRequest = PutDataMapRequest.create("/request_user_info").run {
            dataMap.putString("timeStamp", timestamp)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
        putTask.addOnSuccessListener {
            //println("Great Succes! : Command request user info sent to wear")
            shouldSendInfoRequest.value = false
        }.addOnFailureListener {
            println("Oupsi... On a pas envoyé les données request user info")
        }
    }

    // request  userInfo to mobile to mobile
    fun sendCheatingCommandToMobile() {
        println("We are in send sendCheatingCommandToMobile")
        // Add a timestamp to the message, so its truly different each time !
        val tsLong = System.currentTimeMillis() / 1000
        val timestamp = tsLong.toString()

        val request: PutDataRequest = PutDataMapRequest.create("/cheating").run {
            dataMap.putString("timeStamp", timestamp)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
        putTask.addOnSuccessListener {
            println("Great Succes! : cheating request successfuly send to mobile")
            cheating = false
        }.addOnFailureListener {
            println("Oupsi... On a pas envoyé la cheatimg request...")
        }
    }


    // Function to request permision to position
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
    private fun hasGps(context: Context1): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }


    // update elapse time display
    private fun updateTime() {
        val elapsed_time = System.currentTimeMillis() / 1000 - startTime
        val seconds = (elapsed_time % 60).toInt()
        val minutes = (elapsed_time / 60).toInt()
        if (seconds < 10) {
            binding.elapsedTimeText.text = ("00.0$minutes.0$seconds")
        } else {
            binding.elapsedTimeText.text = ("00.0$minutes.$seconds")
        }
    }


    // update lifecycle -> needed for the observer
    public override fun onStart() {
        super.onStart()
        println("Application started")
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
    }


    // Restart race timer
    private fun startRaceTimer() {
        // reset timer if present : should never be the case
        if (raceTimer != null) {
            raceTimer!!.cancel()
            raceTimer!!.purge()
            raceTimer = null
        }

        // start timer to send GPS position and update elapse time
        raceTimer = Timer()
        raceTimer!!.schedule(timerTask {
            //println("race Timer timeout")
            getGPSPositionAndCallSendGPSToMobile()
            mHandler.post(Runnable() {
                runOnUiThread() {
                    updateTime()
                }
            })
        }, 0, 1000)
    }


    // Destroy race timer
    private fun stopRaceTimer() {
        // reset timer if present
        if (raceTimer != null) {
            raceTimer!!.cancel()
            raceTimer!!.purge()
            raceTimer = null
        }
    }


    // Restart watchdog timer
    private fun startWatchDogTimer() {
        // reset timer if present
        if (watchdogTimer != null) {
            watchdogTimer!!.cancel()
            watchdogTimer!!.purge()
            watchdogTimer = null
        }

        // Launch watch dog timer 10s
        watchdogTimer = Timer()
        watchdogTimer!!.schedule(timerTask {
            println("Watch Dog Timer")
            mHandler.post(Runnable() {
                runOnUiThread() {
                    stateMachine.value = "unlogged"
                }
            })
        }, 10000, 5000)
    }


    // Destroy watchdog timer
    private fun stopWatchDogTimer() {
        // reset timer if present
        if (watchdogTimer != null) {
            watchdogTimer!!.cancel()
            watchdogTimer!!.purge()
            watchdogTimer = null
        }
    }


    // instantiate data client
    override fun onResume() {
        super.onResume()
        println("App resumed")
        Wearable.getDataClient(this).addListener(this)
        mSensorManager!!.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)

        startWatchDogTimer()
    }


    // remove data client
    override fun onPause() {
        super.onPause()
        println("App paused")
        Wearable.getDataClient(this).removeListener(this)
        mSensorManager!!.unregisterListener(this)

        //Reset App state
        stateMachine.value = "unlogged"
        username.value = null
        userImage.value = null
        light.value = "green"
        shouldSendInfoRequest.value = false

        // Stop all Timer
        stopRaceTimer()
        stopWatchDogTimer()
    }


    // update lifecycle -> needed for the observer
    public override fun onStop() {
        super.onStop()
        println("Application Stopped")
        //lifecycleRegistry.markState(Lifecycle.State.DESTROYED)

    }


    public override fun onDestroy() {
        super.onDestroy()
        println("Application Destroyed")

        stateMachine.value = "unlogged"
        username.value = null
        userImage.value = null
        light.value = "green"
        shouldSendInfoRequest.value = false

        // Stop all Timer
        stopRaceTimer()
        stopWatchDogTimer()
    }


    // return lifecyle owner -> need for the observer
    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    // vibrator function
    private fun vibrateWear(context: Context1) {
        val vibrator = context.getSystemService(Context1.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }
}




