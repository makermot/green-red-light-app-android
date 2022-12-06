package com.epfl.esl.a1_c_green_red_light

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class SharedViewModel : ViewModel() {

    // User data
    var imageUri: Uri?
    var username: String
    var key: String = ""
    var password: String = ""
    private val _uploadSuccess = MutableLiveData<Boolean?>()
    val uploadSuccess: LiveData<Boolean?>
        get() = _uploadSuccess

    private val _profilePresent = MutableLiveData<Boolean?>()
    val profilePresent: LiveData<Boolean?>
        get() = _profilePresent

    // FIREBASE
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")


    init {
        imageUri = null
        username = ""
        _profilePresent.value = false
    }

    fun fetchProfile() : Boolean {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username").getValue(String::class.java)!!
                    if (username == usernameDatabase) {
                        val passwordDatabase = user.child("password").getValue(String::class.java)!!
                        if (password == passwordDatabase) {
                            key = user.key.toString()
                            _profilePresent.value = true
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

    fun createProfile(context: LoginFragment) {
        key = username
        profileRef.child(key).child("username").setValue(username)
        profileRef.child(key).child("password").setValue(password)
    }
}