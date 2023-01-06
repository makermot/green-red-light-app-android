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
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentStatBinding
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import com.google.firebase.database.*

class StatFragment : Fragment() {
    private lateinit var viewModelStat: StatViewModel
    private lateinit var viewModelShared: SharedViewModel

    var username: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentStatBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_stat, container, false
        )

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : My statistics"

        // Set the LayoutManager that this RecyclerView will use.
        //binding.recyclerViewItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewItems.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        // Instantiate the viewModels
        viewModelStat = ViewModelProvider(this).get(StatViewModel::class.java)
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
        viewModelStat.listenForStat(context,username)

        viewModelStat.statUpdate.observe(viewLifecycleOwner, Observer { statUpdate ->
            if(statUpdate == true){
                binding.recyclerViewItems.adapter = viewModelStat.itemAdapter
                viewModelStat.resetUpdate()
            }
        })
        return binding.root
    }


    // Start HeartBeatTime
    override fun onStart() {
        super.onStart()
        println("My space started")
        viewModelShared.startHeartBeatTimer()
        Wearable.getDataClient(activity as MainActivity).addListener(viewModelShared)
    }


    // Stop and destroy HeartBeatTimer
    override fun onStop() {
        super.onStop()
        println("My space stopped")
        viewModelShared.stopHeartBeatTimer()
        Wearable.getDataClient(activity as MainActivity).removeListener(viewModelShared)
    }
}