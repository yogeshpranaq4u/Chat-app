package com.chatit.chat.state

import com.chatit.chat.data.Chat
import com.chatit.chat.data.User

sealed class ChatListState {
    object Loading : ChatListState()
    data class Loaded(val chats: List<Chat>, val users: Map<String, User>) : ChatListState()
    data class Error(val message: String) : ChatListState()
}