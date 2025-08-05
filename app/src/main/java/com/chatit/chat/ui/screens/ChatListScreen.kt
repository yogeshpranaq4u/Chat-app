package com.chatit.chat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.chatit.chat.data.Chat
import com.chatit.chat.data.User
import com.chatit.chat.intent.ChatListIntent
import com.chatit.chat.sealed.Screen
import com.chatit.chat.state.ChatListState
import com.chatit.chat.viewmodel.ChatListViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatListScreen(
    navController: NavHostController,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val state by viewModel.state
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uid = currentUser?.uid

    LaunchedEffect(Unit) { viewModel.process(ChatListIntent.LoadChats) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.UserList.route) }) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state) {
                is ChatListState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ChatListState.Error -> Text(
                    text = (state as ChatListState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
                is ChatListState.Loaded -> {
                    val chats = (state as ChatListState.Loaded).chats
                    val users = (state as ChatListState.Loaded).users

                    println("ChatListScreen .. $chats")

                    if (chats.isEmpty()) {
                        Text("No chats found", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn {
                            itemsIndexed(chats) { index, chat ->
                                val otherUserId = chat.members.firstOrNull { it != uid }.orEmpty()
                                val otherUser = users[otherUserId]

                                if (otherUser != null) {
                                    ChatListItem(chat, otherUser) {
                                        // You can use `index` here if needed
                                        println("Clicked chat at position: $chat // $otherUser")
                                        navController.navigate(Screen.ChatPage.createRoute(chat.chatId, otherUserId))
                                    }
                                } else {
                                    // Optional placeholder or skip
                                    Text(
                                        "Unknown user at position $index",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(chat: Chat, user: User?, onClick: () -> Unit) {
    val imagePainter = rememberAsyncImagePainter(user?.imageUrl)
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Image(painter = imagePainter, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = user?.email ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
            Text(text = chat.lastMessage, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}
