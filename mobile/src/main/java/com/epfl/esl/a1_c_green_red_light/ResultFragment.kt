package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentResultBinding
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import java.util.*


class ResultFragment : Fragment() {

    private lateinit var binding: FragmentResultBinding
    private lateinit var viewModel: SharedViewModel

    private var timerHeartBeat: Timer? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_result, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Inflate the layout for this fragment
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : Results"

        // Initialise heart beat observer to keep sync with wear
        viewModel.heartBeat.observe(viewLifecycleOwner, Observer { time ->
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModel.sendStateMachineToWear(dataClient, "logged")
        })

        binding.ReturnHome.setOnClickListener { view: View ->
            Navigation.findNavController(view).navigate(R.id.action_resultFragment_to_mySpaceFragment)
        }

        // add an observer to the shouldSendUserInfoRequest Image
        viewModel.shouldSendUserInfoToWear.observe(viewLifecycleOwner, Observer { request ->
            // Send data to wear
            println("We observed should send request !!!")
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModel.sendUserNameAndImageToWear(dataClient)
        })

        binding.winner.text = viewModel.winner.value
        val elapse = viewModel.elapse_end
        binding.time.text = elapse


        return binding.root
    }

    // Start HeartBeatTime
    override fun onStart() {
        super.onStart()
        println("Result started")
        viewModel.startHeartBeatTimer()
        Wearable.getDataClient(activity as MainActivity).addListener(viewModel)
    }


    // Stop and destroy HeartBeatTimer
    override fun onStop() {
        super.onStop()
        println("Result stopped")
        viewModel.stopHeartBeatTimer()
        Wearable.getDataClient(activity as MainActivity).removeListener(viewModel)
    }
}