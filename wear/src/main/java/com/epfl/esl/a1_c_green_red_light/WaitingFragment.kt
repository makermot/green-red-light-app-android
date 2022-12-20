package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentWaitingBinding

class WaitingFragment : Fragment() {

    private lateinit var binding : FragmentWaitingBinding
    private lateinit var viewModel : SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_waiting, container, false)
        // Inflate the layout for this fragment
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        // Inflate the layout for this fragment
        binding.waitingText.text = viewModel.receivedUsername

        return binding.root
    }
}