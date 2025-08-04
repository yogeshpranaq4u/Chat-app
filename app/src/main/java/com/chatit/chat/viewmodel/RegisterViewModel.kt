package com.chatit.chat.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.chatit.chat.data.User
import com.chatit.chat.intent.RegisterIntent
import com.chatit.chat.state.RegisterState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {
    private val usersCollection = firestore.collection("users")
    private val storageRef = storage.reference

    private val _state = mutableStateOf<RegisterState>(RegisterState.Idle)
    val state: State<RegisterState> = _state

    fun process(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.RegisterUser -> registerUser(intent.email, intent.imageUriString)
        }
    }

    private fun registerUser(email: String, imageUriStr: String?) {
        _state.value = RegisterState.Loading
        val currentUser = auth.currentUser
        println("RegisterScreen.registerUser .....  ${currentUser}")

        if (currentUser == null) {
            // For demo, sign in anonymously and then save user info
            auth.signInAnonymously()
                .addOnSuccessListener {
                    saveUser(it.user?.uid ?: "", email, imageUriStr)
                }
                .addOnFailureListener {
                    _state.value = RegisterState.Error(it.message ?: "Registration failed")
                    println("RegisterScreen.registerUser ... ${it.cause} // ${it.message}")
                }
        } else {
            saveUser(currentUser.uid, email, imageUriStr)
        }
    }

    private fun saveUser(uid: String, email: String, imageUriStr: String?) {
        if (imageUriStr == null) {
            saveUserToFirestore(uid, email, "")
        } else {
            val uri = Uri.parse(imageUriStr)
            val imgRef = storageRef.child("profile_images/$uid.jpg")
            imgRef.putFile(uri)
                .addOnSuccessListener {
                    imgRef.downloadUrl.addOnSuccessListener { url ->
                        saveUserToFirestore(uid, email, url.toString())
                    }.addOnFailureListener {
                        _state.value = RegisterState.Error("Failed to get image download URL")
                    }
                }
                .addOnFailureListener {
                    _state.value = RegisterState.Error(it.message ?: "Image upload failed")
                }
        }
    }

    private fun saveUserToFirestore(uid: String, email: String, imageUrl: String) {
        val user = User(uid = uid, email = email, imageUrl = imageUrl)
        usersCollection.document(uid)
            .set(user)
            .addOnSuccessListener {
                _state.value = RegisterState.Success(user)
            }
            .addOnFailureListener {
                _state.value = RegisterState.Error(it.message ?: "Firestore update failed")
            }
    }
}
