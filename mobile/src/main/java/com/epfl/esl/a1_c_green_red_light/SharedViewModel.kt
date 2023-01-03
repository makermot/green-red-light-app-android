package com.epfl.esl.a1_c_green_red_light

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class SharedViewModel : ViewModel() {

    // User data
    var imageUri: Uri? = null
    var username: String = ""
    var key: String = ""
    var password: String = ""


    // Live data
    // validLogin : True is valid, false if not, null otherwise
    private val _validLogin = MutableLiveData<Boolean?>()
    val validLogin: LiveData<Boolean?>
        get() = _validLogin

    // imageBitmap : Bitmap value of profile image if valid, else null
    private val _imageBitmap = MutableLiveData<Bitmap?>()
    val imageBitmap: LiveData<Bitmap?>
        get() = _imageBitmap


    // FIREBASE
    var storageRef = FirebaseStorage.getInstance().reference
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")


    // Init variable
    init {
        _validLogin.value = null
    }


    // Fetch profile in the FireBase
    fun fetchProfile()  {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username").getValue(String::class.java)!!
                    if (username == usernameDatabase) {
                        val passwordDatabase = user.child("password").getValue(String::class.java)!!
                        if (password == passwordDatabase) {
                            // User is authentified
                            key = user.key.toString()
                            //_profilePresent.value = true

                            // Fetch image : find reference then fetch it in cloud storage
                            val imageReference = storageRef.child("ProfileImages/" + username + ".jpg")
                            val ONE_MEGABYTE: Long = 1024 * 1024
                            imageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { receivedImage ->
                                // Data for "ProfileImages/username.jpg" is returned, use this as needed
                                _imageBitmap.value = BitmapFactory.decodeByteArray(receivedImage, 0, receivedImage.size)
                                _validLogin.value = true
                            }.addOnFailureListener {
                                // TODO Handle any errors
                                _validLogin.value = false
                            }
                            //Thread.sleep(5_000) // TODO améliorer ça Si on va trop vite -> on a pas le temps de fetch l'image qu'on à déjà call sendDataToWear et l'image n'est pas envoyé
                            break
                        }
                        else{
                            _validLogin.value = false
                        }
                    }
                    else {
                        _validLogin.value = false
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    // Create Profile in the FireBase
    fun createProfile(context: Context?) {
        println("Creating profile")
        var error: Boolean = false

        // Check if username is already taken in the database
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username").getValue(String::class.java)!!
                    if (username == usernameDatabase) {
                        //Username is already taken -> abort profile creation
                        println("Existing Profile found")
                        error = true
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        println("waiting")
        Thread.sleep(1_000)
        println("finnished")
        if(!error) {
            println("Start creating profile")
            // Create key and add name and password to realtime database
            key = username
            profileRef.child(key).child("username").setValue(username)
            profileRef.child(key).child("password").setValue(password)

            // Compress user image to bitmap and then PNG
            val matrix = Matrix()
            matrix.postRotate(0F)
            var imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri)
            val ratio: Float = 13F
            val imageBitmapScaled = Bitmap.createScaledBitmap(
                imageBitmap,
                (imageBitmap.width / ratio).toInt(), (imageBitmap.height / ratio).toInt(), false
            )
            imageBitmap = Bitmap.createBitmap(
                imageBitmapScaled, 0, 0,
                (imageBitmap.width / ratio).toInt(), (imageBitmap.height / ratio).toInt(),
                matrix, true
            )
            _imageBitmap.value =
                imageBitmap // update livedata -> save the bitmap user image to be shown later
            val stream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageByteArray = stream.toByteArray()

            // Upload image to cloud storage and then create link in realtime database
            val profileImageRef = storageRef.child("ProfileImages/" + username + ".jpg")
            val uploadProfileImage = profileImageRef.putBytes(imageByteArray)
            uploadProfileImage.addOnFailureListener {
                _validLogin.value = false
            }.addOnSuccessListener { taskSnapshot ->
                profileRef.child(key).child("imageURL").setValue(
                    (FirebaseStorage.getInstance()
                        .getReference()).toString() + "ProfileImages/" + username + ".jpg"
                )
                _validLogin.value = true
            }
        }
        else{
            // We return validLogin only at the end to avoid being interupted by its obersver
            _validLogin.value = false
        }
    }


    // Send image and user name to wear
    fun sendUserNameAndImageToWear(context: Context?, dataClient: DataClient) {
        val stream = ByteArrayOutputStream()
        var imageBitmap = imageBitmap.value
        imageBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        val request: PutDataRequest = PutDataMapRequest.create("/userInfo").run {
            dataMap.putByteArray("profileImage", imageByteArray)
            dataMap.putString("username", username)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
    }


    // Send Start command to Wear
    fun sendStartToWear(dataClient: DataClient) {
        val request: PutDataRequest = PutDataMapRequest.create("/command").run {
            val startCommand: String = "start"
            dataMap.putString("startCommand", startCommand)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
    }

    fun resetUserData(){
        username = ""
        password = ""
        imageUri = null
        key = ""
        _validLogin.value = null
    }
}