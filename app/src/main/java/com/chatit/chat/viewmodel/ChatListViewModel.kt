package com.chatit.chat.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.chatit.chat.data.Chat
import com.chatit.chat.data.User
import com.chatit.chat.intent.ChatListIntent
import com.chatit.chat.state.ChatListState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: DatabaseReference
) : ViewModel() {

    private val _state = mutableStateOf<ChatListState>(ChatListState.Loading)
    val state: State<ChatListState> = _state

    private var chatListener: ValueEventListener? = null
    private var firestoreUsersListener: ListenerRegistration? = null
    private var firestoreChatListener: ListenerRegistration? = null

    // Hold latest data
    private val chatList = mutableListOf<Chat>()
    private val allUsers = mutableMapOf<String, User>()
    private val usersAlreadyInChat = mutableSetOf<String>()

    fun process(intent: ChatListIntent) {
        if (intent is ChatListIntent.LoadChats) {
            loadChats()
            listenToUsers()
        }
    }

    private fun loadChats() {
        val uid = auth.currentUser?.uid ?: return

        firestoreChatListener?.remove() // detach the previous listener if any

        firestoreChatListener = FirebaseFirestore.getInstance()
            .collection("chats")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.value = ChatListState.Error(error.message ?: "Error listening chats")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    chatList.clear()
                    usersAlreadyInChat.clear()

                    for (doc in snapshot.documents) {
                        val chat = doc.toObject(Chat::class.java)
                        if (chat != null) {
                            chatList.add(chat)
                            usersAlreadyInChat.addAll(chat.members.filter { it != uid })
                        }
                    }

                    combineAndEmitState(uid)
                }
            }
    }

    private fun listenToUsers() {
        val uid = auth.currentUser?.uid ?: return

        firestoreUsersListener?.remove() // detach previous listener if any

        firestoreUsersListener = FirebaseFirestore.getInstance()
            .collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.value = ChatListState.Error(error.message ?: "Error listening users")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allUsers.clear()
                    for (doc in snapshot.documents) {
                        val user = doc.toObject(User::class.java)
                        if (user != null) {
                            allUsers[user.uid] = user
                        }
                    }
                    combineAndEmitState(uid)
                }
            }
    }

    private fun combineAndEmitState(currentUserId: String) {
        // Filter users excluding current user and users already in chat
        val filteredUsers = allUsers.filterKeys { it != currentUserId && !usersAlreadyInChat.contains(it) }

        println("ChatListViewModel.combineAndEmitState ... $chatList")

        _state.value = ChatListState.Loaded(
            chats = chatList.sortedByDescending { it.timestamp },
            users = filteredUsers
        )
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.let { db.child("chats").removeEventListener(it) }
        firestoreUsersListener?.remove()
    }
}
