package com.epfl.esl.a1_c_green_red_light

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentInProgressBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.pow


class InProgressFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentInProgressBinding
    private lateinit var viewModel: SharedViewModel
    private lateinit var mMap: GoogleMap
    private val LOCATION_REQUEST_CODE = 101
    private var startTime: Long = 0
    private var alreadyTakenLaMer: Boolean = false

    private var markerPlayer: Marker? = null
    private lateinit var markers: MutableList<Marker>

    private var timerHeartBeat: Timer? = null
    private var timerRace: Timer? = null
    private var rand: Long = 0
    private var lightColor: String = "red"

    // Variable has won or not
    private var winner = MutableLiveData<Boolean>()

    init {
        winner.value = false
    }

    // Live data and Init Live data variable
    private var mapInitialised = MutableLiveData<Boolean>()

    init {
        mapInitialised.value = false
    }

    // Variable state the number of positions received on the watch
    private var isTheFirst: Boolean = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_in_progress, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        // Save start time to compute elapsed time
        startTime = System.currentTimeMillis() / 1000

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title =
            getString(R.string.name_app) + " : Race"

        // Start timer for heartBeat : private timer not from view model and Initialise heart beat to keep sync with wear
        startHeartBeatTimer()

        // Save start time to compute elapse time
        viewModel.startTime = System.currentTimeMillis() / 1000

        // Add listener to dataclient to be able to recieve data from wear
        Wearable.getDataClient(activity as MainActivity).addListener(viewModel)

        //Check for winner condition
        winner.observe(viewLifecycleOwner) { win ->
            if (win && !alreadyTakenLaMer) {
                // Save game data to firebase
                viewModel.addRaceToDataBase()

                viewModel.elapse_end = cleanElapse()

                alreadyTakenLaMer = true
                println("!!! WE NAVIGATE !!!")
                Navigation.findNavController(binding.root)
                    .navigate(R.id.action_inProgressFragment_to_resultFragment)

                onDestroy()
            }
        }

        // Check for multiple player and add listener to change position
        if (viewModel.playWithFriends > 0) {
            println("Game with multiple friends !!!")
            viewModel.getFriendUpdatePosition()
            viewModel.newFriendsPos.observe(viewLifecycleOwner) { newPosInt ->
                addMarkersOfFriends()
            }

        }


        // Add observer on username
        mapInitialised.observe(viewLifecycleOwner) { isInitialised ->
            if (isInitialised) {
                print("We've initialised the map !, saved pos is :")
                println(viewModel.goalPosition)
                // Add goal position to Map
                inflateMap(viewModel.goalPosition)
            }
        }

        // Initialise Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Add observer to playerPosition
        viewModel.receivedPosition.observe(viewLifecycleOwner, Observer { newPosition ->
            //print("new position :")
            //println(newPosition)
            //println("is the first " + isTheFirst)
            if (isTheFirst) {
                viewModel.playerPosition = viewModel.receivedPosition.value!!
                isTheFirst = false
                //println("la position du player is " + viewModel.playerPosition )
                //println("la position sauvegardée is " + viewModel.receivedPosition.value)
            }
            if (mapInitialised.value == true) {
                updatePlayerLocation(newPosition)
            }
            if (viewModel.cheating) {
                hasReturnedToStart(newPosition)
            }

        })

        // Launch random timer to send green and red command
        timerCeption()

        return binding.root
    }


    // Find random number for timer
    private fun findRand(min_frequency: Int, max_frequency: Int): Long {
        return ((min_frequency..max_frequency).random()) * 1000.toLong()
    }


    // launch a random timer to send green and red command
    private fun timerCeption() {
        // reset timer if present
        if (timerRace != null) {
            timerRace!!.cancel()
            timerRace!!.purge()
            timerRace = null
        }

        // get frequency of changing light
        val minFrequency = viewModel.minFreq
        val maxFrequency = viewModel.maxFreq

        // find random period
        rand = findRand(minFrequency, maxFrequency)

        // Launch timer with random period
        timerRace = Timer()
        timerRace!!.schedule(timerTask {
            if (!viewModel.cheating) {
                lightColor = if (lightColor == "red") {
                    "green"
                } else {
                    "red"
                }
                val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
                viewModel.sendCommandToWear(dataClient, lightColor)
            }

            timerCeption()
        }, rand, 1000)
    }


    // Stop and destroy random timer for green and red light
    fun stopTimerCeption() {
        // reset timer if present
        if (timerRace != null) {
            println("TimerRace in tablet is stopped")
            timerRace!!.cancel()
            timerRace!!.purge()
            timerRace = null
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        mapInitialised.value = true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(
                        this.requireActivity(),
                        "Unable to show location - permission required",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val mapFragment =
                        childFragmentManager.findFragmentById(R.id.map) as
                                SupportMapFragment
                    mapFragment.getMapAsync(this)
                }
            }
        }
    }


    // Add marker to goal position and center map around goal
    private fun inflateMap(goalPosition: LatLng) {
        // add goal position to Map
        mMap.addMarker(
            MarkerOptions()
                .position(goalPosition)
                .title("Goal Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        )
        // Move Camera to goal
        val cameraPosition = CameraPosition.Builder()
            .target(goalPosition)
            .zoom(17f)
            .build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }


    @SuppressLint("MissingPermission")
    // Update player location on Map
    private fun updatePlayerLocation(playerPosition: LatLng) {
        // remove the last marker
        if (markerPlayer != null) {
            markerPlayer!!.remove()
            markerPlayer = null
        }

        // add player position to Map
        markerPlayer = mMap.addMarker(
            MarkerOptions()
                .position(playerPosition)
                .title(viewModel.username)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .visible(true)
        )

        hasWon(playerPosition, viewModel.username)

    }


    override fun onDestroy() {
        super.onDestroy()
        stopHeartBeatTimer()
        stopTimerCeption()
        Wearable.getDataClient(activity as MainActivity).removeListener(viewModel)
    }


    // Start thread to update heart beat
    fun startHeartBeatTimer() {
        // reset timer if present
        if (timerHeartBeat != null) {
            timerHeartBeat!!.cancel()
            timerHeartBeat!!.purge()
            timerHeartBeat = null
        }
        timerHeartBeat = Timer()
        timerHeartBeat?.schedule(timerTask {
            //println("Heart Beat")
            val dataClient2: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModel.sendStateMachineToWear(dataClient2, "racing")
        }, 0, 3000)
    }


    // Stop heart beat thread
    fun stopHeartBeatTimer() {
        // reset timer if present
        if (timerHeartBeat != null) {
            timerHeartBeat!!.cancel()
            timerHeartBeat!!.purge()
            timerHeartBeat = null
        }
    }


    // Check for winning condition and set winner livedata
    private fun hasWon(position: LatLng, user: String) {
        var tolerance = 10.0
        tolerance = tolerance.pow(-9)
        val latitudeGoal = viewModel.goalPosition.latitude
        val longitudeGoal = viewModel.goalPosition.longitude
        val latitudeCurrent = position.latitude
        val longitudeCurrent = position.longitude

        val circle =
            (latitudeCurrent - latitudeGoal).pow(2) + (longitudeCurrent - longitudeGoal).pow(2)

        //println("circle = $circle")
        if (circle <= tolerance) {
            // Save stop time to compute elapse time
            viewModel.stopTime = System.currentTimeMillis() / 1000

            // Set the name of the winner
            viewModel.setWinner(user)
            viewModel.setWinnerAndTimeMultiPlayer(user)

            // Call the observer to navigate
            winner.value = true
        }
    }

    // Check for winning condition and set winner livedata
    private fun hasReturnedToStart(position: LatLng) {
        var tolerance = 10.0
        tolerance = tolerance.pow(-9)
        val latitudeStart = viewModel.playerPosition.latitude
        val longitudeStart = viewModel.playerPosition.longitude
        val latitudeCurrent = position.latitude
        val longitudeCurrent = position.longitude

        val circle =
            (latitudeCurrent - latitudeStart).pow(2) + (longitudeCurrent - longitudeStart).pow(2)

        if (circle <= tolerance) {
            // Call the observer to navigate
            viewModel.cheating = false
        }
    }


    // Add the marker of the friends during multiplayer race
    fun addMarkersOfFriends() {
        val colors = listOf(
            BitmapDescriptorFactory.HUE_BLUE, BitmapDescriptorFactory.HUE_CYAN,
            BitmapDescriptorFactory.HUE_GREEN, BitmapDescriptorFactory.HUE_ORANGE,
            BitmapDescriptorFactory.HUE_ROSE, BitmapDescriptorFactory.HUE_VIOLET,
            BitmapDescriptorFactory.HUE_YELLOW
        );

        var increment: Int = 0

        if (mapInitialised.value == true) {
            mMap.clear()

            for (friendPos in viewModel.friendsPos) {
                // Check for winning condition
                hasWon(friendPos, viewModel.friendsName.elementAt(increment))

                // add player position to Map
                var markerMulti: Marker = mMap.addMarker(
                    MarkerOptions()
                        .position(friendPos)
                        .title(viewModel.friendsName.elementAt(increment))
                        .icon(BitmapDescriptorFactory.defaultMarker(colors.elementAt(increment)))
                        .visible(true)
                )
                //markers.add(increment, markerMulti)
                increment += 1
            }

            if (viewModel.receivedPosition.value != null) {
                markerPlayer = mMap.addMarker(
                    MarkerOptions()
                        .position(viewModel.receivedPosition.value!!)
                        .title(viewModel.username)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .visible(true)
                )
            }

            mMap.addMarker(
                MarkerOptions()
                    .position(viewModel.goalPosition)
                    .title("Goal Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
            )


        }
    }


    // Get cleaner elapsed time
    fun cleanElapse(): String {
        //val elapsed_time = System.currentTimeMillis() / 1000 - startTime
        val elapsed_time_fun = System.currentTimeMillis() / 1000 - startTime
        val seconds = (elapsed_time_fun.rem(60))?.toInt()
        val minutes = (elapsed_time_fun / 60)?.toInt()
        var time_string: String = ""
        if (seconds != null) {
            if (seconds < 10) {
                time_string = ("00.0$minutes.0$seconds")
            } else {
                time_string = ("00.0$minutes.$seconds")
            }
        }
        return time_string
    }
}




