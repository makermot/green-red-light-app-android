package com.epfl.esl.a1_c_green_red_light

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        binding.addFriendButton.setOnClickListener{view: View ->
            view.let {
                if(binding.friendUsername.text.toString() != "Friend's username"){
                    viewModel.addFriend(binding.friendUsername.text.toString())
                }
                else{
                    Toast.makeText(context,"Enter friend's username", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.addFriendStatus.observe(viewLifecycleOwner, Observer { status ->
            print("addFriendStatus changed : status :")
            println(status)

            if(status == "Friend already present"){
                Toast.makeText(context,"Oupsi... You're already friends", Toast.LENGTH_LONG).show()
                viewModel.resetAddFriendStatus()
            }
            else if (status == "Friend profile don't exist"){
                Toast.makeText(context,"Oupsi... We don't find your friend's profile...", Toast.LENGTH_LONG).show()
                viewModel.resetAddFriendStatus()
            }
            else if (status == "Friend successfully added"){
                Toast.makeText(context,"Friend successfully added", Toast.LENGTH_LONG).show()
                viewModel.resetAddFriendStatus()
            }
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

        binding.refreshWatchButton.setOnClickListener{view: View ->
            view.let {
                // Send data to wear
                val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
                viewModel.sendUserNameAndImageToWear(dataClient)
            }
        }

        return binding.root
    }
}