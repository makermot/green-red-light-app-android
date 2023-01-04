package com.epfl.esl.a1_c_green_red_light

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
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
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentInProgressBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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

    var currentLat: Double = 0.0
    var currentLng: Double = 0.0

    private var timer_race = Timer()
    private var rand: Long = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_in_progress, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : Race"

        // Initialise Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Add goal position to Map
        //inflateMap(LatLng(46.520444, 6.567717))

        // Add observer to playerPosition
        viewModel.receivedPosition.observe(viewLifecycleOwner, Observer { newPosition ->
            updatePlayerLocation(newPosition)
        })

        // changing green and red lights
        timer_race = Timer()
        rand = findRand()
        timer_race.schedule(timerTask {
            print("je print le rand : ")
            println(rand)
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModel.sendCommandToWear(dataClient, "change_light")
        }, 0, rand)

        return binding.root
    }

    private fun findRand(): Long {
        return ((1..5).random())*1000.toLong()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        /*
        val permission: Boolean = ActivityCompat.checkSelfPermission(
            this.requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this.requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (permission) {
            mMap.isMyLocationEnabled = true
            getLastLocation()
        } else {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE
        )
        }*/

        // Add goal position to Map
        inflateMap(LatLng(46.520444, 6.567717))

        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    )
    {
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
        // Create goal icon to display
        val goalIcon = BitmapDescriptorFactory.fromBitmap(
            BitmapFactory.decodeResource(
                this.resources,
                R.drawable.ic_pickup
            )
        )

        // add goal position to Map
        mMap.addMarker(
            MarkerOptions()
                .position(goalPosition)
                .title("Goal Location")
                .icon(goalIcon)
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
        // Create icon to display
        val playerIcon = BitmapDescriptorFactory.fromBitmap(
            BitmapFactory.decodeResource(
                this.resources,
                R.drawable.ic_pickup
            )
        )

        // add player position to Map
        mMap.addMarker(
            MarkerOptions()
                .position(playerPosition)
                .title("Goal Location")
                .icon(playerIcon)
        )
    }


    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(activity as MainActivity).addListener(viewModel)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(activity as MainActivity).removeListener(viewModel)
    }

}


