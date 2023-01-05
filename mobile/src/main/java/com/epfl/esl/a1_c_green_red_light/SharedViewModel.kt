package com.epfl.esl.a1_c_green_red_light

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.concurrent.timerTask


class SharedViewModel : ViewModel(), DataClient.OnDataChangedListener {

    // User data
    var imageUri: Uri? = null
    var username: String = ""
    var key: String = ""
    var password: String = ""
    var timer = Timer()

    private var mHandler: Handler = object : Handler(){}
    private var shouldSendUserInfoToWear: Boolean = false

    // Live data
    // String to check how far we are in the authentification process : It can take the value :
    // Invalid login, Valid login, Profile already existing, Ready to create profile, Profile created, Exception, null
    private val _authentification = MutableLiveData<String?>()
    val authentification: LiveData<String?>
        get() = _authentification

    // String to check how far we are in the adding Friend process : It can take the value :
    // Friend successfully added, Friend already present, Friend profile don't exist, null
    private val _addFriendStatus = MutableLiveData<String?>()
    val addFriendStatus: LiveData<String?>
        get() = _addFriendStatus

    // imageBitmap : Bitmap value of profile image if valid, else null
    private val _imageBitmap = MutableLiveData<Bitmap?>()
    val imageBitmap: LiveData<Bitmap?>
        get() = _imageBitmap

    //localisation of the wear
    private val _receivedPosition = MutableLiveData<LatLng>()
    val receivedPosition: LiveData<LatLng>
        get() = _receivedPosition

    // Localisation of beginning of the race
    var goalPosition: LatLng = LatLng(46.520444, 6.567717)

    // Localisation of the goal of the race
    var playerPosition: LatLng = LatLng(46.520444, 6.567717)

    //localisation of the wear
    private val _heartBeat = MutableLiveData<Int>()
    val heartBeat: LiveData<Int>
        get() = _heartBeat

    // FIREBASE
    var storageRef = FirebaseStorage.getInstance().reference
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")


    // Init variable
    init {
        _authentification.value = null
        _heartBeat.value = 0
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

                            // Fetch image : find reference then fetch it in cloud storage
                            val imageReference = storageRef.child("ProfileImages/" + username + ".jpg")
                            val ONE_MEGABYTE: Long = 1024 * 1024
                            imageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener { receivedImage ->
                                // Data for "ProfileImages/username.jpg" is returned, use this as needed
                                _imageBitmap.value = BitmapFactory.decodeByteArray(receivedImage, 0, receivedImage.size)
                                _authentification.value = "Valid login"
                            }.addOnFailureListener {
                                _authentification.value = "Exception"
                            }
                            return
                        }
                    }
                }
                println("boucle for terminée")
                _authentification.value = "Invalid login"
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    // Check if Profile is already taken in the FireBase
    fun checkProfile() {
        println("checkProfile")
        // Check if username is already taken in the database
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username").getValue(String::class.java)!!
                    println("profile Fetched")
                    println(usernameDatabase)
                    if (username == usernameDatabase) {
                        //Username is already taken -> abort profile creation
                        println("Existing Profile found")
                        _authentification.value = "Profile already existing"
                        return
                    }
                }
                println("boucle for terminée")
                _authentification.value = "Ready to create profile"
            }
            override fun onCancelled(databaseError: DatabaseError) {
                println("An error in create profile -> on datachanged occured")
            }
        })
    }


    // Create Profile in the FireBase
    fun createProfile(context: Context?) {
        println("Start creating profile")
        // Create key and push name and password to realtime database
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
        uploadProfileImage.addOnSuccessListener { taskSnapshot ->
            _authentification.value = "Profile created"
        }.addOnFailureListener {
            _authentification.value = "Exception"
        }
    }


    // Add friend to profile
    fun addFriend(friendUsername : String) {
        // Profile ref -> branche profile de la realtime database
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.hasChild(friendUsername)){
                    println("Friend's profile Found")
                    if(dataSnapshot.child(key).child("friend").hasChild(friendUsername)){
                        println("Oh no... You're already friends")
                        _addFriendStatus.value = "Friend already present"
                    }
                    else{
                        println("Let's add your friend")
                        profileRef.child(key).child("friend").child(friendUsername).setValue(friendUsername)
                        _addFriendStatus.value = "Friend successfully added"
                    }
                }
                else{
                    println("Oh no... friend's profile not found")
                    _addFriendStatus.value = "Friend profile don't exist"
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    // Send image and user name to wear
    fun sendUserNameAndImageToWear(dataClient: DataClient) {
        // Add a timestamp to the message, so its truly different each time !
        val tsLong = System.currentTimeMillis() / 1000
        val timestamp = tsLong.toString()

        val stream = ByteArrayOutputStream()
        var imageBitmap = imageBitmap.value
        imageBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        val request: PutDataRequest = PutDataMapRequest.create("/userInfo").run {
            dataMap.putString("timeStamp", timestamp)
            dataMap.putByteArray("profileImage", imageByteArray)
            dataMap.putString("username", username)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
        putTask.addOnSuccessListener {
            println("Great Succes! : Image and user name sent to wear")
        }
    }


    // Send string command to Wear
    fun sendCommandToWear(dataClient: DataClient, command : String) {
        // Add a timestamp to the message, so its truly different each time !
        val tsLong = System.currentTimeMillis() / 1000
        val timestamp = tsLong.toString()

        val request: PutDataRequest = PutDataMapRequest.create("/command").run {
            dataMap.putString("timeStamp", timestamp)
            dataMap.putString("command", command)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
        putTask.addOnSuccessListener {
            println("Great Succes! : Command sent to wear")
        }

    }


    // Send wear state machine status to Wear
    fun sendStateMachineToWear(dataClient: DataClient, wearStateMachine : String) {
        // Add a timestamp to the message, so its truly different each time !
        val tsLong = System.currentTimeMillis() / 1000
        val timestamp = tsLong.toString()

        val request: PutDataRequest = PutDataMapRequest.create("/state").run {
            dataMap.putString("timeStamp", timestamp)
            dataMap.putString("state", wearStateMachine)
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
        putTask.addOnSuccessListener {
            println("Great Succes! : Heart beat sent to wear")
        }

        if(shouldSendUserInfoToWear){
            shouldSendUserInfoToWear = false
            sendUserNameAndImageToWear(dataClient)
        }
    }


    // Function that receive GPS command from wear
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        println("We recceived data from wear")

        dataEvents
            .filter {it.dataItem.uri.path == "/GPS_data" }
            .forEach { event ->
                print("We received location :")

                val latitude = DataMapItem.fromDataItem(event.dataItem).dataMap.getDouble("latitude")
                val longitude = DataMapItem.fromDataItem(event.dataItem).dataMap.getDouble("longitude")

                _receivedPosition.value = LatLng(latitude, longitude)
                println(_receivedPosition.value)
            }

        dataEvents
            .filter {it.dataItem.uri.path == "/request_user_info" }
            .forEach { event ->
                print("We received info request from wear :")
                shouldSendUserInfoToWear = true
            }
    }


    fun resetUserData(){
        username = ""
        password = ""
        imageUri = null
        key = ""
        _authentification.value = null
    }


    fun resetAuthentification(){
        _authentification.value = null
    }


    fun resetAddFriendStatus(){
        _addFriendStatus.value = null
    }


    // Start thread to update heart beat
    fun startHeartBeat(){
        timer.schedule(timerTask {
            println("Heart Beat")
            mHandler.post( Runnable() {
                run {
                    _heartBeat.value = _heartBeat.value?.plus(1)
                }
            })
        }, 0, 3000)
    }
}