package com.epfl.esl.a1_c_green_red_light

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class FriendsViewModel: ViewModel() {

    // FIREBASE
    //var storageRef = FirebaseStorage.getInstance().reference
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")


    var itemAdapterFriends: ItemAdapterFriends? = null

    private val _friendsUpdate = MutableLiveData<Boolean?>()
    val friendsUpdate: LiveData<Boolean?>
        get() = _friendsUpdate

    init {
        _friendsUpdate.value = false
    }

    fun listenForFriends(context:Context?, username: String?) {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                print("user :")
                println(username)

                if(dataSnapshot.hasChild(username!!)) {
                    println("")
                    println("Current username found in database in FriendsViewModel")

                    val friendsList: ArrayList<String> = ArrayList<String>()

                    print("user :")
                    println(username)

                    for (friend in dataSnapshot.child("/"+username).child("/friend").children){
                        val friendsDatabase = friend
                            .getValue(String::class.java)!!
                        println(friendsDatabase)
                        friendsList.add(friendsDatabase)
                    }

                    // Adapter class is initialized and list is passed in the param.
                    itemAdapterFriends = context?.let {
                        ItemAdapterFriends(
                            context = it,
                            items = friendsList
                        )
                    }
                    _friendsUpdate.value = true
                }
                else{println("I can't recover the current username in StatViewModel")}
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun resetUpdate(){_friendsUpdate.value = false}
}