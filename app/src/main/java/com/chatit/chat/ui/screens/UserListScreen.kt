package com.chatit.chat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
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
import com.chatit.chat.intent.ChatListIntent
import com.chatit.chat.sealed.Screen
import com.chatit.chat.state.ChatListState
import com.chatit.chat.viewmodel.ChatListViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun UserListScreen(
    navController: NavHostController,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val state by viewModel.state
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uid = currentUser?.uid

    LaunchedEffect(Unit) { viewModel.process(ChatListIntent.LoadChats) } // reusing same fetch

    when (val s = state) {
        is ChatListState.Loaded -> {
            val users = s.users.values.filter { it.uid != uid }
            println("UserListScreen .. $users")
            LazyColumn {
                itemsIndexed(users) { index, user ->
                    ListItem(
                        headlineContent = { Text(user.email) },
                        leadingContent = {
                            Image(
                                painter = rememberAsyncImagePainter(user.imageUrl),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                            )
                        },
                        modifier = Modifier.clickable {
                            val chatId = generateChatId(uid ?: "", user.uid)
                            // You can use `index` here if needed, e.g. for logging or animations
                            navController.navigate(Screen.ChatPage.createRoute(chatId, user.uid))
                        }
                    )
                    Divider()
                }
            }
        }
        is ChatListState.Loading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(
            Alignment.Center)) }
        is ChatListState.Error -> Text(s.message, modifier = Modifier.fillMaxSize(), color = Color.Red)
    }
}

private fun generateChatId(uid1: String, uid2: String): String =
    if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
