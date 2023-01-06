package com.epfl.esl.a1_c_green_red_light

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class FriendsViewModel: ViewModel() {

    // FIREBASE
    var storageRef = FirebaseStorage.getInstance().reference
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")

    var numberFriends:Int = 0
    var itemAdapterFriends: ItemAdapterFriends? = null

    private val _friendsUpdate = MutableLiveData<Boolean?>()
    val friendsUpdate: LiveData<Boolean?>
        get() = _friendsUpdate

    private val _nbFriends = MutableLiveData<Int?>()
    val nbFriends: LiveData<Int?>
        get() = _nbFriends

    var friendsList: ArrayList<String> = ArrayList<String>()
    var imagesList: ArrayList<Bitmap> = ArrayList<Bitmap>()

    init {
        _friendsUpdate.value = false
    }

    fun listenForFriends(context:Context?, username: String?) {
        _nbFriends.value = 0
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                print("user :")
                println(username)

                if(dataSnapshot.hasChild(username!!)) {

                    friendsList.clear()
                    var counter = 0
                    numberFriends = 0

                    // count how many friends user has
                    for (friend in dataSnapshot.child("/"+username).child("/friend").children){
                        numberFriends += 1
                    }

                    for (friend in dataSnapshot.child("/"+username).child("/friend").children){
                        val friendsDatabase = friend
                            .getValue(String::class.java)!!
                        println(friendsDatabase)
                        friendsList.add(friendsDatabase)
                        val imageFriend = storageRef.child("ProfileImages/" + friendsDatabase + ".jpg")
                        val ONE_MEGABYTE: Long = 1024 * 1024
                        imageFriend.getBytes(ONE_MEGABYTE).addOnSuccessListener { receivedImage ->
                            // Data for "ProfileImages/username.jpg" is returned, use this as needed
                            imagesList.add(BitmapFactory.decodeByteArray(receivedImage, 0, receivedImage.size))
                            counter = counter.plus(1)
                            print("counter is: ")
                            println(counter)
                            _nbFriends.value = counter
                        }
                    }
                }
                else{println("I can't recover the current username in StatViewModel")}
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun sendToItemAdapter(context:Context?){
        // Adapter class is initialized and list is passed in the param.
        println("is inside sendToItemAdapter")
        itemAdapterFriends = context?.let {
            ItemAdapterFriends(
                context = it,
                items = friendsList,
                items_2 = imagesList,
            )
        }
        _friendsUpdate.value = true
    }

    fun resetUpdate(){_friendsUpdate.value = false}
}