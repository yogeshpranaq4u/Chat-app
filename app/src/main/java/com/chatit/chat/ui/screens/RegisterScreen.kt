package com.chatit.chat.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.chatit.chat.intent.RegisterIntent
import com.chatit.chat.state.RegisterState
import com.chatit.chat.viewmodel.RegisterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onRegistered: () -> Unit
) {
    val state by viewModel.state
    var email by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            imagePickerLauncher.launch("image/*")
        }) {
            Text("Pick Profile Image")
        }
        imageUri?.let {
            Image(painter = rememberAsyncImagePainter(it), contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.Gray, CircleShape))
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            if (email.isNotBlank()) {
                viewModel.process(RegisterIntent.RegisterUser(email, imageUri?.toString()))
            } else {
                Toast.makeText(context, "Enter email", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Register")
        }

        when (state) {
            is RegisterState.Loading -> CircularProgressIndicator()
            is RegisterState.Error -> {
                Text(text = (state as RegisterState.Error).message, color = Color.Red)
                println("RegisterScreen...  ${(state as RegisterState.Error).message}")
            }
            is RegisterState.Success -> LaunchedEffect(Unit) {
                onRegistered()
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(mapOf("fcmToken" to token), SetOptions.merge())
                        .addOnSuccessListener { println("FCM token updated ... $uid") }
                        .addOnFailureListener { e -> println("Failed to update FCM token: ${e.message}") }
                }
            }
            else -> {}
        }
    }
}
