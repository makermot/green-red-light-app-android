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
    var imageUri: Uri?
    var username: String
    var key: String = ""
    var password: String = ""


    // Live data
    // TODO add an observer to check for success
    private val _imageUploadSuccsess = MutableLiveData<Boolean?>()
    val imageUploadSuccsess: LiveData<Boolean?>
        get() = _imageUploadSuccsess

    private val _profilePresent = MutableLiveData<Boolean?>()
    val profilePresent: LiveData<Boolean?>
        get() = _profilePresent

    private val _imageBitmap = MutableLiveData<Bitmap?>()
    val imageBitmap: LiveData<Bitmap?>
        get() = _imageBitmap


    // FIREBASE
    var storageRef = FirebaseStorage.getInstance().reference
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")


    // Init variable
    init {
        imageUri = null
        username = ""
        _profilePresent.value = false
    }


    // Fetch profile in the FireBase
    fun fetchProfile() : Boolean {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username").getValue(String::class.java)!!
                    if (username == usernameDatabase) {
                        val passwordDatabase = user.child("password").getValue(String::class.java)!!
                        if (password == passwordDatabase) {
                            // User is authentified
                            key = user.key.toString()
                            _profilePresent.value = true

                            // Fetch image : find reference then fetch it in cloud storage
                            val imageReference = storageRef.child("ProfileImages/" + username + ".jpg")
                            val ONE_MEGABYTE: Long = 1024 * 1024
                            imageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { receivedImage ->
                                // Data for "ProfileImages/username.jpg" is returned, use this as needed
                                _imageBitmap.value = BitmapFactory.decodeByteArray(receivedImage, 0, receivedImage.size)
                            }.addOnFailureListener {
                                // TODO Handle any errors
                            }
                            Thread.sleep(5_000) // TODO améliorer ça Si on va trop vite -> on a pas le temps de fetch l'image qu'on à déjà call sendDataToWear et l'image n'est pas envoyé
                            break
                        }
                    }
                }
                if(_profilePresent.value != true){
                    _profilePresent.value = false
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        return _profilePresent.value!!
    }


    // Create Profile in the FireBase
    fun createProfile(context: Context?) {
        key = username
        profileRef.child(key).child("username").setValue(username)
        profileRef.child(key).child("password").setValue(password)

        val profileImageRef = storageRef.child("ProfileImages/" + username + ".jpg")
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

        // update livedata
        _imageBitmap.value = imageBitmap

        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()
        val uploadProfileImage = profileImageRef.putBytes(imageByteArray)

        uploadProfileImage.addOnFailureListener {
            _imageUploadSuccsess.value = false
        }.addOnSuccessListener { taskSnapshot ->
            profileRef.child(key).child("imageURL").setValue(
                (FirebaseStorage.getInstance()
                    .getReference()).toString() + "ProfileImages/" + username + ".jpg"
            )
            _imageUploadSuccsess.value = true
        }
    }


    // Send Start command to Wear
    fun sendStartToWear(context: Context?, dataClient: DataClient) {
        val request: PutDataRequest = PutDataMapRequest.create("/command").run {
            dataMap.putString("startCommand", "start")
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
        _imageUploadSuccsess.value = null
        _profilePresent.value = false
    }
}