package com.epfl.esl.a1_c_green_red_light

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.epfl.esl.a1_c_green_red_light.databinding.FragmentLoginBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import java.io.ByteArrayOutputStream

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
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

        // Initialise viewModel
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.name_app)

        binding.SignUp.setOnClickListener { view: View ->

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
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()

                //(activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)

                if(!viewModel.fetchProfile()){
                    viewModel.createProfile(activity?.applicationContext)
                    view.let {
                        // Send data to wear
                        val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
                        sendUserNameAndImageToWear(activity?.applicationContext, dataClient)

                        // Navigate to my space
                        Navigation.findNavController(it)
                            .navigate(R.id.action_loginFragment_to_mySpaceFragment)
                    }
                }else{
                    Toast.makeText(context,"Profile already taken", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.SignIn.setOnClickListener { view : View ->
            if (binding.Username.text.toString() == "") {
                Toast.makeText(context,"Enter username", Toast.LENGTH_SHORT).show()
            }
            else if (binding.Password.text.toString() == "") {
                Toast.makeText(context,"Enter password dumbass", Toast.LENGTH_SHORT).show()
            }
            else {
                viewModel.username = binding.Username.text.toString()
                viewModel.password = binding.Password.text.toString()

                if(viewModel.fetchProfile()){
                    view.let {
                        // Send data to wear
                        val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
                        sendUserNameAndImageToWear(activity?.applicationContext, dataClient)

                        // Navigate to my space
                        Navigation.findNavController(it)
                            .navigate(R.id.action_loginFragment_to_mySpaceFragment)
                    }
                }else{
                    Toast.makeText(context,"Incorrect password/username", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.Userimage.setOnClickListener {
            val imgIntent = Intent(Intent.ACTION_GET_CONTENT)
            imgIntent.setType("image/*")
            resultLauncher.launch(imgIntent)
        }

        // Button to test Data client
        binding.TestButton.setOnClickListener {
            username = "ça marche !"
            imageUri=viewModel.imageUri
            val dataClient: DataClient = Wearable.getDataClient(activity as AppCompatActivity)
            sendUserNameAndImageToWear(activity?.applicationContext, dataClient)
        }

        return binding.root
    }
    
    fun sendUserNameAndImageToWear(context: Context?, dataClient: DataClient) {
        val matrix = Matrix()
        var imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, viewModel.imageUri)
        val ratio: Float = 13F
        val imageBitmapScaled = Bitmap.createScaledBitmap(
            imageBitmap,
            (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(),
            false
        )
        imageBitmap = Bitmap.createBitmap(
            imageBitmapScaled,
            0,
            0,
            (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(),
            matrix,
            true
        )
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        val request: PutDataRequest = PutDataMapRequest.create("/userInfo").run {
            dataMap.putByteArray("profileImage", imageByteArray)
            dataMap.putString("username", viewModel.username)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
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


    // Reset user data when the App is paused
    override fun onPause() {
        super.onPause()
        viewModel.resetUserData()
    }

}