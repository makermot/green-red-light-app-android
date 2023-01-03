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
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentLoginBinding
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentMySpaceBinding
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable

class MySpaceFragment : Fragment() {

    private lateinit var binding: FragmentMySpaceBinding
    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_space, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : My Space"

        // add an observer to the profile Image
        viewModel.imageBitmap.observe(viewLifecycleOwner, Observer { newImageBitmap ->
            binding.welcomeImage.setImageBitmap(newImageBitmap)
            binding.welcomeUsername.text = buildString {
                append("Welcome ")
                append(viewModel.username)
                append(" !") }
        })

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