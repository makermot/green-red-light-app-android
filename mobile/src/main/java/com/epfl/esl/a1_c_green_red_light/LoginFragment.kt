package com.epfl.esl.a1_c_green_red_light

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private lateinit var viewModel: SharedViewModel
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        // Inflate the layout for this fragment

        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        // viewModel.key = viewModel.profileRef.push().key.toString()

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app)

        binding.SignUp.setOnClickListener { view : View ->

            if (binding.Username.text.toString() == "") {
                Toast.makeText(context,"Enter username", Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.imageUri == null) {
                Toast.makeText(context,"Pick an image", Toast.LENGTH_SHORT).show()
            }
            else {
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()
                // val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
                // viewModel.sendDataToWear(activity?.applicationContext, dataClient)
                // viewModel.sendDataToFireBase(activity?.applicationContext)

                //(activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
            }
        }

        viewModel.profilePresent.observe(viewLifecycleOwner, Observer { success ->
            if (success == false){
                Toast.makeText(context,"Incorrect password/username",
                    Toast.LENGTH_LONG).show()
            }
            //else if (success == true){
                //(activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
            //}
        })

        binding.SignIn.setOnClickListener { view : View ->
            if (binding.Username.text.toString() == "") {
                Toast.makeText(context,"Enter username", Toast.LENGTH_SHORT).show()
            }
            else {
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()

                viewModel.fetchProfile()

            }
        }

        binding.Userimage.setOnClickListener {
            val imgIntent = Intent(Intent.ACTION_GET_CONTENT)
            imgIntent.setType("image/*")
            //resultLauncher.launch(imgIntent)
        }


        return binding.root
    }

}