package com.chatit.chat.data

data class User(val uid: String = "", val email: String = "", val imageUrl: String = "")
data class Chat(val chatId: String = "", val members: List<String> = emptyList(), val lastMessage: String = "", val timestamp: Long = 0L)
data class Message(
    val msgId: String = "",
    val sender: String = "",
    val content: String = "",
    val type: String = "text",
    val mediaUrl: String? = null,
    val timestamp: Long = 0L
)
