package com.epfl.esl.a1_c_green_red_light

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
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
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentLoginBinding
import com.google.android.gms.wearable.*

class LoginFragment : Fragment() {

    private lateinit var viewModel: SharedViewModel
    private lateinit var binding: FragmentLoginBinding

    // TEST
    var imageUri: Uri? = null
    var username: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialise Binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Set title on the Top Bar
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app) + " : Login page"

        // Reset authentification variable when page is recreated
        viewModel.resetAuthentification()

        // Initialise heart beat to keep sync with wear
        viewModel.heartBeat.observe(viewLifecycleOwner, Observer { time ->
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            viewModel.sendStateMachineToWear(dataClient, "unlogged")
        })

        // Start timer for heartBeat
        viewModel.startHeartBeatTimer()

        binding.SignUp.setOnClickListener { view: View ->

            if (checkForInternet(context)) {
                Toast.makeText(context, "Internet connection is off, please connect your device", Toast.LENGTH_SHORT).show()
            }

            if (binding.Username.text.toString() == "") {
                Toast.makeText(context,"Enter username", Toast.LENGTH_SHORT).show()
            }
            else if (binding.Password.text.toString() == "") {
                Toast.makeText(context,"Enter password", Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.imageUri == null) {
                Toast.makeText(context,"Pick an image", Toast.LENGTH_SHORT).show()
            }
            else {
                println("Signing up with image, password and username")
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()
                println("checkprofile Call")
                viewModel.checkProfile()
                println("checkprofile End")
            }
        }

        binding.SignIn.setOnClickListener { view : View ->
            if (checkForInternet(context)) {
                Toast.makeText(context, "Internet connection is on", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Internet connection is off, please connect your device", Toast.LENGTH_SHORT).show()
            }

            if (binding.Username.text.toString() == "") {
                Toast.makeText(context,"Enter username", Toast.LENGTH_SHORT).show()
            }
            else if (binding.Password.text.toString() == "") {
                Toast.makeText(context,"Enter password dumbass", Toast.LENGTH_SHORT).show()
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
            resultLauncher.launch(imgIntent)
        }

        viewModel.authentification.observe(viewLifecycleOwner, Observer { code ->
            print("Authentification changed : code :")
            println(code)

            if(code == "Invalid login"){
                Toast.makeText(context,"Incorrect password/username", Toast.LENGTH_LONG).show()
                viewModel.resetUserData()
            }
            else if (code == "Valid login"){
                // Send data to wear
                val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
                viewModel.sendUserNameAndImageToWear(dataClient)

                // Navigate to my space
                Navigation.findNavController(binding.root).navigate(R.id.action_loginFragment_to_mySpaceFragment)
            }
            else if (code == "Profile already existing"){
                Toast.makeText(context,"Profile already existing", Toast.LENGTH_LONG).show()
                viewModel.resetUserData()
            }
            else if (code == "Ready to create profile"){
                println("createprofile Call")
                viewModel.createProfile(activity?.applicationContext)
                println("creatprofile End")
            }
            else if (code == "Profile created"){
                // Send data to wear
                val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
                viewModel.sendUserNameAndImageToWear(dataClient)

                // Navigate to my space
                Navigation.findNavController(binding.root).navigate(R.id.action_loginFragment_to_mySpaceFragment)
            }
            else if (code == "Exception"){
                Toast.makeText(context,"Oupsi, something went wrong", Toast.LENGTH_LONG).show()
                viewModel.resetUserData()
            }
        })

        return binding.root
    }


    // Function to handle user image selection
    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                viewModel.imageUri = imageUri
                binding.Userimage.setImageURI(imageUri)
            }
        }


    // Start HeartBeatTime
    override fun onStart() {
        super.onStart()
        println("Login started")
        viewModel.startHeartBeatTimer()
    }


    // Stop and destroy HeartBeatTimer
    override fun onStop() {
        super.onStop()
        println("Login stopped")
        viewModel.stopHeartBeatTimer()
    }

     private fun checkForInternet(context: Context?): Boolean {

        // register activity with the connectivity manager service
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // if the android version is equal to M
        // or greater we need to use the
        // NetworkCapabilities to check what type of
        // network has the internet connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Returns a Network object corresponding to
            // the currently active default data network.
            val network = connectivityManager.activeNetwork ?: return false

            // Representation of the capabilities of an active network.
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                // Indicates this network uses a Wi-Fi transport,
                // or WiFi has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                // Indicates this network uses a Cellular transport. or
                // Cellular has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                // else return false
                else -> false
            }
        } else {
            // if the android version is below M
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}
