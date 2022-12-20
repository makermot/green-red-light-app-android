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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentLoungeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import java.io.IOException
import java.util.*

class LoungeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentLoungeBinding
    private lateinit var viewModel: SharedViewModel
    private lateinit var dataClient: DataClient

    private val LOCATION_REQUEST_CODE = 101
    private lateinit var mMap: GoogleMap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_lounge, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        dataClient = Wearable.getDataClient(activity as AppCompatActivity)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app)

        binding.gotoWearButton.setOnClickListener{view: View ->
            val dataClient: DataClient = Wearable
                .getDataClient(activity as AppCompatActivity)
            viewModel.sendDataToWear(activity?.applicationContext, dataClient)
            sendCommandToWear("Start")
            println("Alo")
            Navigation.findNavController(view).navigate(R.id.action_loungeFragment_to_inProgressFragment)
        }

        return binding.root
    }


    // Send Start and Stop command to the watch
    private fun sendCommandToWear(command: String) {
        Thread(Runnable {
            println("Started the Thread")
            val connectedNodes: List<String> = Tasks
                .await(
                    Wearable
                        .getNodeClient(activity as MainActivity).connectedNodes
                )
                .map { it.id }
            connectedNodes.forEach {
                val messageClient: MessageClient = Wearable
                    .getMessageClient(activity as AppCompatActivity)
                messageClient.sendMessage(it, "/command", command.toByteArray())
            }
            println("Finnished the Thread")
        }).start()
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
                        .target(LatLng(mLastLocation.latitude, mLastLocation.longitude))
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
