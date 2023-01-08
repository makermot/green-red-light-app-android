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
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask


class SharedViewModel : ViewModel(), DataClient.OnDataChangedListener {

    // User data
    var imageUri: Uri? = null
    var username: String = ""
    var key: String = ""
    var password: String = ""
    var heartBeatTimer: Timer? = null
    var startTime: Long? = null
    var stopTime: Long? = null
    var playWithFriends : Int = 0
    var friendsWePlayWith : ArrayList<String> = ArrayList<String>()
    var friendsPos : ArrayList<LatLng> = ArrayList<LatLng>()
    var friendsName : ArrayList<String> = ArrayList<String>()
    var elapse : Long = 0

    private var mHandler: Handler = object : Handler(){}

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

    //localisation of the wear
    private val _shouldSendUserInfoToWear = MutableLiveData<Boolean>()
    val shouldSendUserInfoToWear: LiveData<Boolean>
        get() = _shouldSendUserInfoToWear

    // String to check how far we are in the playing with Friend process : It can take the value :
    // send play request successfully added, send request already sent, Friend profile don't exist, you can't play with yourself, you're not friends, null
    private val _playWithFriendStatus = MutableLiveData<String?>()
    val playWithFriendStatus: LiveData<String?>
        get() = _playWithFriendStatus

    // Count the number of friends response
    private val _friendsResponse = MutableLiveData<Int?>()
    val friendsResponse: LiveData<Int?>
        get() = _friendsResponse

    private val _gameOwner = MutableLiveData<String?>()
    val gameOwner: LiveData<String?>
        get() = _gameOwner

    // update friends position
    private val _newFriendsPos = MutableLiveData<Int?>()
    val newFriendsPos: LiveData<Int?>
        get() = _newFriendsPos

    // Winner of the game
    private val _winner = MutableLiveData<String>()
    val winner: LiveData<String>
        get() = _winner


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
        _shouldSendUserInfoToWear.value = false
        _gameOwner.value = null
        _newFriendsPos.value = 0
        _winner.value = "winner"
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

    // Add parameters of the race to the database
    fun addRaceToDataBase(){
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener{
            val rightNow = Calendar.getInstance()
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE)
            val formattedDate: String = df.format(rightNow.time)
            var activityKey: String = Random().nextInt().toString()
            val elapse = stopTime?.minus(startTime!!)
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                profileRef.child(key).child("/races").child(activityKey).child("date").setValue(formattedDate)
                profileRef.child(key).child("/races").child(activityKey).child("elapsed time").setValue(elapse.toString())
                profileRef.child(key).child("/races").child(activityKey).child("finish coordinates").setValue(goalPosition.toString())
                profileRef.child(key).child("/races").child(activityKey).child("start coordinates").setValue(playerPosition.toString())
                profileRef.child(key).child("/races").child(activityKey).child("/players").child(key).setValue(key)
                for (user in friendsWePlayWith){
                    profileRef.child(key).child("/races").child(activityKey).child("/players").child(user).setValue(user)
                }
                profileRef.child(key).child("/races").child(activityKey).child("winner").setValue(_winner.value)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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
        println("We are in send User Image and name to wear")
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
        }.addOnFailureListener {
            println("Fuck! : drop the Heart beat")
        }.addOnCanceledListener {
            println("Wtf... we canceled the heart beat")
        }

        /*
        if(shouldSendUserInfoToWear){
            shouldSendUserInfoToWear = false
            println("we call send user name and image to wear from send State machine")
            sendUserNameAndImageToWear(dataClient)
        }*/
    }


    // Function that receive GPS command from wear
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        //print("We recceived data from wear :")

        dataEvents
            .filter {it.dataItem.uri.path == "/GPS_data" }
            .forEach { event ->
                print("We received location from wear :")

                val latitude = DataMapItem.fromDataItem(event.dataItem).dataMap.getDouble("latitude")
                val longitude = DataMapItem.fromDataItem(event.dataItem).dataMap.getDouble("longitude")

                _receivedPosition.value = LatLng(latitude, longitude)
                println(_receivedPosition.value)
            }

        dataEvents
            .filter {it.dataItem.uri.path == "/request_user_info" }
            .forEach { event ->
                println("We received info request from wear :")
                val timestamp = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("timeStamp")

                _shouldSendUserInfoToWear.value = _shouldSendUserInfoToWear.value != true
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


    fun resetPlayWithFriendStatus(){
        _playWithFriendStatus.value = null
    }


    // Start thread to update heart beat
    fun startHeartBeatTimer(){
        // reset timer if present
        if(heartBeatTimer != null) {
            heartBeatTimer!!.cancel()
            heartBeatTimer!!.purge()
            heartBeatTimer = null
        }

        heartBeatTimer = Timer()
        heartBeatTimer?.schedule(timerTask {
            println("Heart Beat")
            mHandler.post( Runnable() {
                run {
                    _heartBeat.value = _heartBeat.value?.plus(1)
                }
            })
        }, 0, 3000)
    }


    // Stop heart beat thread
    fun stopHeartBeatTimer(){
        // reset timer if present
        if(heartBeatTimer != null) {
            heartBeatTimer!!.cancel()
            heartBeatTimer!!.purge()
            heartBeatTimer = null
        }
    }


    // Send Request to friend to play with
    fun requestFriendToPlayWith(friendUserName : String){
        if(username == friendUserName){
            _playWithFriendStatus.value = "you can't play with yourself"
            return
        }
        // Profile ref -> branche profile de la realtime database
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(!dataSnapshot.child(username).child("friend").hasChild(friendUserName)){
                    _playWithFriendStatus.value = "you're not friends"
                    return
                }
                if(dataSnapshot.hasChild(friendUserName)){
                    println("Friend's profile Found")
                    if(dataSnapshot.child(friendUserName).child("play request").hasChildren() &&
                        username == dataSnapshot.child(friendUserName).child("play request").child("request").getValue(String::class.java)!!){
                        println("Oh no... You're already asked him to play with you")
                        _playWithFriendStatus.value = "send request already sent"
                    }
                    else{
                        println("Let's send play request to your friend")
                        profileRef.child(username).child("accepted request").child(friendUserName).setValue("no")
                        profileRef.child(friendUserName).child("play request").child("request").setValue(username)
                        playWithFriends += 1
                        friendsWePlayWith.add(friendUserName)
                        _playWithFriendStatus.value = "send play request successfully added"
                    }
                }
                else{
                    println("Oh no... friend's profile not found")
                    _playWithFriendStatus.value = "Friend profile don't exist"
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    // Get the number of friends whom accepted the play request
    fun getFriendsResponse(){
        println("Get friends response")
        // Profile ref -> branche profile de la realtime database
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                println("On dataChange")
                if(dataSnapshot.hasChild(username)){
                    println("Found user")
                    if(dataSnapshot.child(username).child("accepted request").hasChildren()){
                        println("Request has children")
                        var count : Int = 0
                        for (playerReady in dataSnapshot.child(username).child("accepted request").children){
                            val playerName: String? = playerReady.getKey()
                            print("Player name : ")
                            println(playerName)
                            if(friendsWePlayWith.contains(playerName) && playerReady.getValue(String::class.java) == "yes"){
                                println("sucessfully increment counter")
                                count += 1
                            }
                        }
                        _friendsResponse.value = count
                    }
                    else{
                        _friendsResponse.value = 0
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    // Reset the friends demand in firebase
    fun resetFriendsPlayDemand(){
        playWithFriends = 0
        friendsWePlayWith.clear()

        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.hasChild(username)){
                    if(dataSnapshot.child(username).child("accepted request").hasChildren()){
                        for (playerReady in dataSnapshot.child(username).child("accepted request").children){
                            // Get friends name
                            val playerName: String? = playerReady.getKey()

                            // Reset accepted demand in our user profil
                            profileRef.child(username).child("accepted request").child(playerName!!).setValue(null)

                            // Reset play demand in friend profil
                            if( dataSnapshot.child(playerName!!).hasChild("play request") &&
                                dataSnapshot.child(playerName!!).child("play request").child("request").getValue(String::class.java) == username){
                                profileRef.child(playerName!!).child("play request").child("request").setValue(null)
                            }
                        }
                    }
                    if(dataSnapshot.child(username).hasChild("currentRacePosition")){
                        // TODO UNCOMMENT FOR FINAL EVALUATION
                        //profileRef.child(username).child("currentRacePosition").setValue(null)
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    // Find if a player has request a game with you
    fun getPlayRequest(){
        profileRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if( dataSnapshot.hasChild(username) &&
                    dataSnapshot.child(username).hasChild("play request")){
                    _gameOwner.value = dataSnapshot.child(username).child("play request").child("request").getValue(String::class.java)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    // Find winning condition in multiplayer game has request a game with you
    fun getWinnerMultiPlayer(){
        profileRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if( dataSnapshot.hasChild(_gameOwner.value!!) &&
                    dataSnapshot.child(_gameOwner.value!!).hasChild("currentRacePosition") &&
                    dataSnapshot.child(_gameOwner.value!!).child("currentRacePosition").hasChild("winner")){
                    elapse = dataSnapshot.child(_gameOwner.value!!).child("currentRacePosition").child("elapse").getValue(Long::class.java)!!
                    _winner.value = dataSnapshot.child(_gameOwner.value!!).child("currentRacePosition").child("winner").getValue(String::class.java)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // write winning condition in multiplayer game has request a game with you
    fun setWinnerAndTimeMultiPlayer(winner : String){
        val elapse = stopTime?.minus(startTime!!)
        profileRef.child(username).child("currentRacePosition").child("elapse").setValue(elapse)
        profileRef.child(username).child("currentRacePosition").child("winner").setValue(winner)
    }


    // Send current position while multiplayer
    fun addPositionMultiplayer(){
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                profileRef.child(_gameOwner.value!!).child("/currentRacePosition").child(username).setValue(playerPosition)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    // Set a permanent listener to update the player data
    fun getFriendUpdatePosition(){
        profileRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                println(" We have friend updated position call !!!!")
                if( dataSnapshot.child(username).hasChild("currentRacePosition") &&
                    dataSnapshot.child(username).child("currentRacePosition").hasChildren()){
                    friendsPos.clear()
                    friendsName.clear()
                    for (player in dataSnapshot.child(username).child("currentRacePosition").children){
                        if(player.hasChild("latitude") && player.hasChild("latitude")){
                            val latitude = player.child("latitude").getValue(Double::class.java)
                            val longitude = player.child("longitude").getValue(Double::class.java)
                            LatLng(latitude!!, longitude!!)
                            friendsPos.add(LatLng(latitude!!, longitude!!))
                            friendsName.add(player.getKey()!!)
                        }
                    }
                    _newFriendsPos.value = _newFriendsPos.value?.plus(1)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }


    // Accept play request in Firebase
    fun acceptPlayRequest(){
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                profileRef.child(_gameOwner.value!!).child("accepted request").child(username).setValue("yes")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    // Setter function for winner
    fun setWinner(winner : String?){
        _winner.value = winner
    }
}