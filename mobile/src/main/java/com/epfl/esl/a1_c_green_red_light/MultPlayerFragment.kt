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


class MultPlayerFragment : Fragment() {

    private lateinit var binding: FragmentMultPlayerBinding
    private lateinit var viewModel: SharedViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_mult_player, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Inflate the layout for this fragment
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : MultiPlayer"

        viewModel.getPlayRequest()

        // Initialise heart beat observer to keep sync with wear
        viewModel.gameOwner.observe(viewLifecycleOwner, Observer { owner ->
            println(" Game requested !!")
        })


        return binding.root
    }
}