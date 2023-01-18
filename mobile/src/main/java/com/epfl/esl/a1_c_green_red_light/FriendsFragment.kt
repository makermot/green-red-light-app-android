package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentFriendsBinding
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentStatBinding
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import com.google.firebase.database.*

class FriendsFragment : Fragment() {
    private lateinit var viewModelFriends: FriendsViewModel
    private lateinit var viewModelShared: SharedViewModel

    var username: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentFriendsBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_friends, container, false
        )

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title =
            getString(R.string.name_app) + " : My friends"

        // Set the LayoutManager that this RecyclerView will use.
        //binding.recyclerViewItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewItems.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        // Instantiate the viewModels
        viewModelFriends = ViewModelProvider(this).get(FriendsViewModel::class.java)
        viewModelShared = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Initialise heart beat observer to keep sync with wear
        viewModelShared.heartBeat.observe(viewLifecycleOwner, Observer { time ->
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModelShared.sendStateMachineToWear(dataClient, "logged")
        })

        // add an observer to the shouldSendUserInfoRequest Image
        viewModelShared.shouldSendUserInfoToWear.observe(viewLifecycleOwner, Observer { request ->
            // Send data to wear
            println("We observed should send request !!!")
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModelShared.sendUserNameAndImageToWear(dataClient)
        })

        // Retrieve current user
        username = viewModelShared.username

        // Retrieve stat for that user
        viewModelFriends.listenForFriends(context, username)

        viewModelFriends.friendsUpdate.observe(viewLifecycleOwner, Observer { friendsUpdate ->
            if (friendsUpdate == true) {
                binding.recyclerViewItems.adapter = viewModelFriends.itemAdapterFriends
                viewModelFriends.resetUpdate()
            }
        })

        viewModelFriends.nbFriends.observe(viewLifecycleOwner, Observer { nbFriends ->
            if (nbFriends == viewModelFriends.numberFriends) {
                viewModelFriends.sendToItemAdapter(context)
            }
        })
        return binding.root
    }


    // Start HeartBeatTime
    override fun onStart() {
        super.onStart()
        viewModelShared.startHeartBeatTimer()
        Wearable.getDataClient(activity as MainActivity).addListener(viewModelShared)
    }


    // Stop and destroy HeartBeatTimer
    override fun onStop() {
        super.onStop()
        viewModelShared.stopHeartBeatTimer()
        Wearable.getDataClient(activity as MainActivity).removeListener(viewModelShared)
    }
}