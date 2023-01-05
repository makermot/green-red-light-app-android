package com.epfl.esl.a1_c_green_red_light

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.system.Os.remove
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentInProgressBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import java.io.IOException
import java.util.*
import kotlin.concurrent.timerTask


class InProgressFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentInProgressBinding
    private lateinit var viewModel: SharedViewModel
    private lateinit var mMap: GoogleMap
    private val LOCATION_REQUEST_CODE = 101

    private var markerPlayer: Marker? = null

    private var timerHeartBeat: Timer? = null
    private var timerRace: Timer? = null
    //private var timerRaceDeconection: Timer? = null
    private var rand: Long = 0
    private var lightColor: String = "red"

    // Live data and Init Live data variable
    private var mapInitialised = MutableLiveData<Boolean>()
    init {
        mapInitialised.value = false
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_in_progress, container, false)

        //val args : InProgressFragmentArgs by navArgs()
        //val goalPosition = args.goalPosition

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : Race"

        // Start timer for heartBeat : private timer not from view model and Initialise heart beat to keep sync with wear
        startHeartBeatTimer()

        // Add listener to dataclient to be able to recieve data from wear
        Wearable.getDataClient(activity as MainActivity).addListener(viewModel)


        // Add observer on username
        mapInitialised.observe(viewLifecycleOwner) { isInitialised ->
            if(isInitialised){
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
            print("new position :")
            println(newPosition)
            if(mapInitialised.value == true){
                updatePlayerLocation(newPosition)
            }
        })

        // Launch random timer to send green and red command
        timerCeption()

        return binding.root
    }

    private fun findRand(): Long {
        return ((1..5).random())*1000.toLong()
    }


    // launch a random timer to send green and red command
    private fun timerCeption(){
        // reset timer if present
        if(timerRace != null) {
            timerRace!!.cancel()
            timerRace!!.purge()
            timerRace = null
        }

        // find random period
        rand = findRand()
        //print("je print le rand : ")
        //println(rand)

        // Launch timer with random period
        timerRace = Timer()
        timerRace!!.schedule(timerTask {
            //println("Timer Race")

            lightColor = if (lightColor == "red"){"green"} else {"red"}
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModel.sendCommandToWear(dataClient, lightColor)

            timerCeption()
        }, rand, 1000)

    }


    // Stop and destroy random timer for green and red light
    fun stopTimerCeption(){
        // reset timer if present
        if(timerRace != null) {
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


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
    private fun inflateMap(goalPosition : LatLng){
        print("We infalte the map with goal position : ")
        println(goalPosition)
        print("While goal pos is : ")
        println(viewModel.goalPosition)
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
    private fun updatePlayerLocation(playerPosition : LatLng) {
        // remove the last marker
        if (markerPlayer != null) {
            markerPlayer!!.remove()
            markerPlayer=null
        }

        // add player position to Map
        markerPlayer = mMap.addMarker(
            MarkerOptions()
                .position(playerPosition)
                .title("Goal Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .visible(true)
        )
    }


    /*
    override fun onResume() {
        super.onResume()
        //Wearable.getDataClient(activity as MainActivity).addListener(viewModel)
    }

    override fun onPause() {
        super.onPause()
        //Wearable.getDataClient(activity as MainActivity).removeListener(viewModel)
    }*/


    override fun onDestroy() {
        super.onDestroy()
        println("In progress : Everything destroyed")
        stopHeartBeatTimer()
        stopTimerCeption()
        Wearable.getDataClient(activity as MainActivity).removeListener(viewModel)
    }


    // Start thread to update heart beat
    fun startHeartBeatTimer(){
        // reset timer if present
        if(timerHeartBeat != null) {
            timerHeartBeat!!.cancel()
            timerHeartBeat!!.purge()
            timerHeartBeat = null
        }

        timerHeartBeat = Timer()
        timerHeartBeat?.schedule(timerTask {
            println("Heart Beat")
            val dataClient2: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModel.sendStateMachineToWear(dataClient2, "racing")
        }, 0, 3000)
    }


    // Stop heart beat thread
    fun stopHeartBeatTimer(){
        // reset timer if present
        if(timerHeartBeat != null) {
            timerHeartBeat!!.cancel()
            timerHeartBeat!!.purge()
            timerHeartBeat = null
        }
    }


}


