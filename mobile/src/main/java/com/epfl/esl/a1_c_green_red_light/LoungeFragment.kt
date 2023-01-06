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
import androidx.lifecycle.MutableLiveData
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
    //private var permission: Boolean = false

    private var permission = MutableLiveData<Boolean>()
    init {
        permission.value = false
    }

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

        // Initialise friends response observer to navigate to fragment when all friend responded
        viewModel.friendsResponse.observe(viewLifecycleOwner, Observer { friendsResponse ->
            if(viewModel.playWithFriends == friendsResponse){
                // Save the position of the goal and the position of the player
                viewModel.goalPosition = markerGoal.position
                println(markerGoal.position)

                // Navigate to race fragment
                Navigation.findNavController(requireView()).navigate(R.id.action_loungeFragment_to_inProgressFragment)
            }else{
                Toast.makeText(
                    this.requireActivity(),
                    "Oupsi... Not All Friend responded",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        // Initialise friends response observer to navigate to fragment when all friend responded
        viewModel.playWithFriendStatus.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                "send play request successfully added" -> {
                    Toast.makeText(context,"play request successfully sent", Toast.LENGTH_SHORT).show()
                }
                "send request already sent" -> {
                    Toast.makeText(context,"You already asked him to play...", Toast.LENGTH_SHORT).show()
                }
                "Friend profile don't exist" -> {
                    Toast.makeText(context,"Friend profile don't exist", Toast.LENGTH_SHORT).show()
                }
                "you can't play with yourself" -> {
                    Toast.makeText(context,"you can't play with yourself", Toast.LENGTH_SHORT).show()
                }
                "you're not friends" -> {
                    Toast.makeText(context,"Oupsi... you're not friends", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Start timer for heartBeat
        viewModel.startHeartBeatTimer()

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : Lounge"

        // Start race functionality
        binding.gotoWearButton.setOnClickListener{view: View ->
            // Check if localisation permission was granted
            permission.value = ActivityCompat.checkSelfPermission(
                this.requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this.requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            // If not -> request it again
            if(permission.value == false){
                Toast.makeText(
                    this.requireActivity(),
                    "Oupsi... Localisation permission required",
                    Toast.LENGTH_LONG
                ).show()

                ActivityCompat.requestPermissions(
                    this.requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
            }
            //Else navigate to race fragment
            else{
                if(viewModel.playWithFriends > 0){
                    viewModel.getFriendsResponse()
                }else{
                    // Save the position of the goal and the position of the player
                    viewModel.goalPosition = markerGoal.position
                    println(markerGoal.position)

                    // Navigate to race fragment
                    Navigation.findNavController(view).navigate(R.id.action_loungeFragment_to_inProgressFragment)
                }
            }
        }

        // Add Friend to race functionality
        binding.playWithFriend.setOnClickListener{view: View ->
            if(viewModel.playWithFriends < 7){
                if(binding.friendUsername.text.toString() != "Friend's username"){
                    viewModel.requestFriendToPlayWith(binding.friendUsername.text.toString())
                }
                else{
                    Toast.makeText(context,"Enter friend's username", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(context,"You've reached max player for this race", Toast.LENGTH_SHORT).show()
            }

        }

        // Add observer on userImage
        permission.observe(viewLifecycleOwner) { permissionGranted ->
            if(permissionGranted == true){
                inflateMap(viewModel.goalPosition)
            }
        }

        // Initialise Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //var goal = LatLng(46.520444, 6.567717)

        permission.value = ActivityCompat.checkSelfPermission(
            this.requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this.requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (permission.value == true) {
            mMap.isMyLocationEnabled = true
            inflateMap(viewModel.goalPosition)
        } else {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE
            )
        }

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

                    permission.value = true
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
        Wearable.getDataClient(activity as MainActivity).addListener(viewModel)
        viewModel.resetFriendsPlayDemand()
    }


    // Stop and destroy HeartBeatTimer
    override fun onStop() {
        super.onStop()
        println("Lounge stopped")
        viewModel.stopHeartBeatTimer()
        Wearable.getDataClient(activity as MainActivity).removeListener(viewModel)
        viewModel.resetPlayWithFriendStatus()
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


