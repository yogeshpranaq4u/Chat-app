package com.chatit.chat.state

import com.chatit.chat.data.Message

sealed class ChatPageState {
    object Loading : ChatPageState()
    data class Loaded(val messages: List<Message>) : ChatPageState()
    data class Error(val message: String) : ChatPageState()
}