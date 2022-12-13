package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentStartBinding
import com.google.android.gms.wearable.Wearable


class StartWearFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //binding = DataBindingUtil.inflate(inflater, R.layout.fragment_start, container, false)
        binding.StartRace.setOnClickListener {view: View ->
            Navigation.findNavController(view)
                .navigate(R.id.action_startFragment_to_raceFragment)
        }
        return binding.root
    }




}