package com.chatit.chat.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.chatit.chat.data.Message
import com.chatit.chat.intent.ChatPageIntent
import com.chatit.chat.state.ChatPageState
import com.chatit.chat.viewmodel.ChatPageViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatPageScreen(
    navController: NavHostController,
    chatId: String,
    otherUserId: String,
    viewModel: ChatPageViewModel = hiltViewModel()
) {
    val state by viewModel.state
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uid = currentUser?.uid ?: ""
    var messageText by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            viewModel.process(
                ChatPageIntent.SendMessage(chatId, "[Image]", "image", it.toString()),
                otherUserId, context
            )
        }
    }

    LaunchedEffect(chatId) {
        println("Clicked chat .. 11 // $chatId // $otherUserId")
        viewModel.process(ChatPageIntent.LoadMessages(chatId), otherUserId, context)
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (state) {
                is ChatPageState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ChatPageState.Error -> Text(
                    text = (state as ChatPageState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
                is ChatPageState.Loaded -> {
                    val messages = (state as ChatPageState.Loaded).messages
                    LazyColumn(
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages.size) { index ->
                            val msg = messages[messages.size - 1 - index]
                            MessageBubble(msg = msg, isSender = msg.sender == uid)
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f),
                placeholder = { Text("Type a message") }
            )
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.process(
                            ChatPageIntent.SendMessage(chatId, messageText, "text", null,),
                            otherUserId,
                            context
                        )
                        messageText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
            IconButton(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Image"
                ) // For demonstration. Ideally you use an "Attach"/"Image" icon here.
            }
        }
    }
}

@Composable
fun MessageBubble(msg: Message, isSender: Boolean) {
    val alignment = if (isSender) Arrangement.End else Arrangement.Start
    val backgroundColor = if (isSender) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    Row(
        horizontalArrangement = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(max = 250.dp)
        ) {
            if (msg.type == "image" && msg.mediaUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(msg.mediaUrl),
                    contentDescription = null,
                    modifier = Modifier.size(180.dp).padding(8.dp)
                )
            } else {
                Text(
                    text = msg.content,
                    color = if (isSender) Color.White else Color.Black,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
