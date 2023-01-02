package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentWaitingBinding
import com.google.android.gms.wearable.*

class WaitingFragment : Fragment(), DataClient.OnDataChangedListener {

    private lateinit var binding : FragmentWaitingBinding
    private lateinit var viewModel : SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_waiting, container, false)
        // Inflate the layout for this fragment
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        // Inflate the layout for this fragment
        //binding.waitingText.text = viewModel.receivedUsername.value

        // add Observer to the MESSAGE received from mobile
        //viewModel.receivedGameStatus.observe(viewLifecycleOwner, Observer {newStatus -> binding.stillText.text = newStatus})

        // add Observer to the DATA received from mobile
        //viewModel.receivedGameStatus.observe(viewLifecycleOwner, Observer {newStatus -> binding.stillText.text = newStatus})

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(requireActivity().getApplicationContext()).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(requireActivity().getApplicationContext()).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED &&
                    it.dataItem.uri.path == "/Test" }
            .forEach { event ->
                val receivedTestText: String = DataMapItem.fromDataItem(
                    event.dataItem).dataMap.getString("TestString")
                binding.testText.text = receivedTestText
            }
    }
}