package com.epfl.esl.a1_c_green_red_light

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentLoungeBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.wearable.*


class LoungeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerDragListener {

    private lateinit var binding: FragmentLoungeBinding
    private lateinit var viewModel: SharedViewModel

    private lateinit var markerGoal: Marker

    private val LOCATION_REQUEST_CODE = 101
    private lateinit var mMap: GoogleMap
    private var permission: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_lounge, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Initialise heart beat observer to keep sync with wear
        viewModel.heartBeat.observe(viewLifecycleOwner, Observer { time ->
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModel.sendStateMachineToWear(dataClient, "logged")
        })

        // Start timer for heartBeat
        viewModel.startHeartBeatTimer()

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : Lounge"

        binding.gotoWearButton.setOnClickListener{view: View ->
            if(permission){
                // Send start condition to wear
                println("declare Client")
                val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
                viewModel.sendCommandToWear(dataClient, "start")
                println("End SendStart to wear : Navigate")

                // Navigate to in progressFragment
                //val directions = LoungeFragmentDirections.actionLoungeFragmentToInProgressFragment(viewModel.goalPosition)
                //
                //
                // view.findNavController().navigate(directions)
                Navigation.findNavController(view).navigate(R.id.action_loungeFragment_to_inProgressFragment)
            }else{
                Toast.makeText(
                    this.requireActivity(),
                    "Oupsi... Localisation permission required",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Save the position of the goal and the position of the player
            viewModel.goalPosition = markerGoal.position
            println(markerGoal.position)
            //viewModel.playerPosition = viewModel.receivedPosition.value!!
        }

        // Initialise Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //var goal = LatLng(46.520444, 6.567717)

        permission = ActivityCompat.checkSelfPermission(
            this.requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this.requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (permission) {
            mMap.isMyLocationEnabled = true
            inflateMap(viewModel.goalPosition)
        } else {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE
            )
            permission = true
        }

        //map of the earth when the permission is not given
        // Map of the earth when the permission is not given

        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
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


    //Creation of the map with the goal position
    @SuppressLint("MissingPermission", "SuspiciousIndentation")
    private fun inflateMap(goalPosition : LatLng){
        mMap.clear()

        // Add goal position to Map
        markerGoal = mMap.addMarker(
            MarkerOptions()
                .position(goalPosition)
                .title("Goal Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                .draggable(true)
        ) as Marker

        mMap.setOnMarkerDragListener(this)
        //onMarkerDrag(markerGoal)

        // Move Camera to goal
        val cameraPosition = CameraPosition.Builder()
            .target(goalPosition)
            .zoom(17f)
            .build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }
    

    // Start HeartBeatTime
    override fun onStart() {
        super.onStart()
        println("Lounge started")
        viewModel.startHeartBeatTimer()
    }


    // Stop and destroy HeartBeatTimer
    override fun onStop() {
        super.onStop()
        println("Lounge stopped")
        viewModel.stopHeartBeatTimer()
    }


    override fun onMarkerDragStart(p0: Marker) {
    }


    override fun onMarkerDrag(marker: Marker) {
    }

        
    override fun onMarkerDragEnd(p0: Marker) {
        println("onMarkerDrag.  Current Position: " + p0.position)
        viewModel.goalPosition = p0.position
        print("saved position in view model is : ")
        println(viewModel.goalPosition)
    }

}


