package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentLoginBinding
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentMySpaceBinding

class MySpaceFragment : Fragment() {

    private lateinit var binding: FragmentMySpaceBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_space, container, false)
        // Inflate the layout for this fragment

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app)

        binding.statButton.setOnClickListener{view: View ->
            view.let {
                Navigation.findNavController(it)
                    .navigate(R.id.action_mySpaceFragment_to_statFragment)
            }
        }

        binding.loungeButton.setOnClickListener{view: View ->
            view.let {
                Navigation.findNavController(it)
                    .navigate(R.id.action_mySpaceFragment_to_loungeFragment)
            }
        }

        return binding.root
    }
}