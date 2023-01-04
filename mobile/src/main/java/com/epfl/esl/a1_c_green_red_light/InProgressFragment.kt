package com.epfl.esl.a1_c_green_red_light

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
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


class InProgressFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentInProgressBinding
    private lateinit var viewModel: SharedViewModel
    private lateinit var mMap: GoogleMap
    private val LOCATION_REQUEST_CODE = 101

    var currentLat: Double = 0.0
    var currentLng: Double = 0.0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_in_progress, container, false)
        // Inflate the layout for this fragment

        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        viewModel.receivedLatitude.observe(viewLifecycleOwner, Observer { newLatitude ->
            currentLat = newLatitude
            getLastLocation()
        })
        viewModel.receivedLongitude.observe(viewLifecycleOwner, Observer { newLongitude ->
            currentLng = newLongitude
            getLastLocation()
        })

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app)

        return binding.root
    }


    private fun newAdress(newLat: Double, newLong: Double): LatLng {
        return LatLng(newLat, newLong)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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
        }
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

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val fusedLocationProviderClient = FusedLocationProviderClient(this.requireActivity())
        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener(this.requireActivity()) { task ->
                if (task.isSuccessful && task.result != null) {
                    val mLastLocation = task.result
                    var address = "No known address"
                    val gcd = Geocoder(this.requireActivity(), Locale.getDefault())
                    val addresses: List<Address>
                    try {
                        addresses = gcd.getFromLocation(
                            mLastLocation.latitude,
                            mLastLocation.longitude,
                            1
                        )
                        if (addresses.isNotEmpty()) {
                            address = addresses[0].getAddressLine(0)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    val icon = BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(
                            this.resources,
                            R.drawable.ic_pickup
                        )
                    )
                    val sat = LatLng(46.520444, 6.567717)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(sat)
                            .title("Current Location")
                            .snippet(address)
                            .icon(icon)
                    )
                    val cameraPosition = CameraPosition.Builder()
                        .target(newAdress(currentLat, currentLng))
                        .zoom(17f)
                        .build()
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                } else {
                    Toast.makeText(
                        this.requireActivity(),
                        "No current location found",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        return
    }

}


