package com.epfl.esl.a1_c_green_red_light

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class StatViewModel: ViewModel() {

    //val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    //val profileRef: DatabaseReference = database.getReference("Profiles")

    // FIREBASE
    //var storageRef = FirebaseStorage.getInstance().reference
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")


    var itemAdapter: ItemAdapter? = null

    private val _statUpdate = MutableLiveData<Boolean?>()
    val statUpdate: LiveData<Boolean?>
        get() = _statUpdate

    init {
        _statUpdate.value = false
    }

    fun listenForStat(context:Context?, username: String?) {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                print("user :")
                println(username)

                if(dataSnapshot.hasChild(username!!)) {
                    println("")
                    println("Current username found in database in StatViewModel")

                    val dateTimeList: ArrayList<String> = ArrayList<String>()
                    val coordinatesStartList: ArrayList<String> = ArrayList<String>()
                    val coordinatesFinishList: ArrayList<String> = ArrayList<String>()
                    val elapsedTimeList: ArrayList<String> = ArrayList<String>()
                    val playersList: ArrayList<String> = ArrayList<String>()

                    print("user :")
                    println(username)

                    for (race in dataSnapshot.child("/"+username).child("/races").children){
                        println("Racing")
                        println(race)
                        val dateDatabase = race
                            .child("date")
                            .getValue(String::class.java)!!
                        println(dateDatabase)
                        dateTimeList.add(dateDatabase)
                        val coordinatesStartDatabase = race
                            .child("start coordinates")
                            .getValue(String::class.java)!!
                        coordinatesStartList.add(coordinatesStartDatabase)
                        val coordinatesFinishDatabase = race
                            .child("finish coordinates")
                            .getValue(String::class.java)!!
                        coordinatesFinishList.add(coordinatesFinishDatabase)
                        val elapsedTimeDatabase = race
                            .child("elapsed time")
                            .getValue(String::class.java)!!
                        elapsedTimeList.add(elapsedTimeDatabase)
                        var playerString = ""
                        for (player in race.child("/players").children) {
                            val playersDatabase = player
                                .getValue(String::class.java)!!
                            playerString = "$playerString, $playersDatabase"
                        }
                        playersList.add(playerString)
                        print("The players of this race are: ")
                        println(playerString)
                    }

                    print("Date :")
                    println(dateTimeList)
                    print("Player :")
                    println(playersList)

                    // Adapter class is initialized and list is passed in the param.
                    itemAdapter = context?.let {
                        ItemAdapter(
                            context = it,
                            items = dateTimeList,
                            items_2 = coordinatesStartList,
                            items_3 = coordinatesFinishList,
                            items_4 = elapsedTimeList,
                            items_5 = playersList
                        )
                    }
                    _statUpdate.value = true
                }
                else{println("I can't recover the current username in StatViewModel")}
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun resetUpdate(){_statUpdate.value = false}
}