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
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentMultPlayerBinding
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import java.util.*
import kotlin.concurrent.timerTask


class MultPlayerFragment : Fragment() {

    private lateinit var binding: FragmentMultPlayerBinding
    private lateinit var viewModel: SharedViewModel

    private var timerHeartBeat: Timer? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_mult_player, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Set title
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : MultiPlayer"

        viewModel.getPlayRequest()

        // Initialise heart beat observer to keep sync with wear
        viewModel.gameOwner.observe(viewLifecycleOwner, Observer { owner ->
            if(owner != null){
                println(" Game requested !!")
                binding.noFriendsLayout.visibility = View.GONE
                binding.ownerName.text = owner
                binding.ownerLayout.visibility = View.VISIBLE
            }
        })

        binding.acceptButton.setOnClickListener{view : View ->
            // Add listener to dataclient to be able to recieve data from wear
            Wearable.getDataClient(activity as MainActivity).addListener(viewModel)

            // Start timer for heartBeat : private timer not from view model and Initialise heart beat to keep sync with wear
            startHeartBeatTimer()

            // Observer on the received position
            viewModel.receivedPosition.observe(viewLifecycleOwner, Observer { newPosition ->
                viewModel.playerPosition = newPosition
                //viewModel.playerPosition = viewModel.receivedPosition.value!!
                viewModel.addPositionMultiplayer()
            })
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        println("In progress : Everything destroyed")
        stopHeartBeatTimer()
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